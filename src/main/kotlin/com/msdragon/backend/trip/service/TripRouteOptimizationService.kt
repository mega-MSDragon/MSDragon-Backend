package com.msdragon.backend.trip.service

import com.msdragon.backend.auth.entity.User
import com.msdragon.backend.auth.repository.UserRepository
import com.msdragon.backend.auth.support.AuthenticatedUser
import com.msdragon.backend.common.exception.BadRequestException
import com.msdragon.backend.common.exception.ForbiddenException
import com.msdragon.backend.common.exception.NotFoundException
import com.msdragon.backend.common.exception.UnAuthorizedException
import com.msdragon.backend.family.repository.FamilyMemberRepository
import com.msdragon.backend.trip.config.TmapProperties
import com.msdragon.backend.trip.dto.TripCourseResponse
import com.msdragon.backend.trip.entity.ExternalApiProvider
import com.msdragon.backend.trip.entity.StopType
import com.msdragon.backend.trip.entity.Trip
import com.msdragon.backend.trip.entity.TripStop
import com.msdragon.backend.trip.repository.TripDayRepository
import com.msdragon.backend.trip.repository.TripRepository
import com.msdragon.backend.trip.repository.TripStopRepository
import com.msdragon.backend.trip.tmap.TmapRouteCoordinate
import com.msdragon.backend.trip.tmap.TmapRouteClient
import com.msdragon.backend.trip.tmap.TmapRouteOptimizationRequest
import com.msdragon.backend.trip.tmap.TmapRouteOptimizationResult
import com.msdragon.backend.trip.tmap.TmapRouteStop
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import java.time.LocalDateTime

@Service
class TripRouteOptimizationService(
	private val userRepository: UserRepository,
	private val familyMemberRepository: FamilyMemberRepository,
	private val tripRepository: TripRepository,
	private val tripDayRepository: TripDayRepository,
	private val tripStopRepository: TripStopRepository,
	private val tripService: TripService,
	private val tmapProperties: TmapProperties,
	private val tmapRouteClient: TmapRouteClient,
	private val objectMapper: ObjectMapper,
) {
	@Transactional
	fun optimizeDayRoute(
		currentUser: AuthenticatedUser,
		tripId: Long,
		dayNumber: Int,
	): TripCourseResponse {
		val child = getLoginUser(currentUser.id)
		val trip = tripRepository.findByIdAndDeletedAtIsNull(tripId)
			?: throw NotFoundException("여행을 찾을 수 없습니다.")
		validateTripReadable(currentUser.id, trip)
		tripService.validateCourseEditable(child, trip)
		val tripDay = tripDayRepository.findByTripIdAndDayNumber(tripId, dayNumber)
			?: throw NotFoundException("여행 일자를 찾을 수 없습니다.")
		val stops = tripStopRepository.findAllByTripDayIdOrderBySortOrderAsc(requireNotNull(tripDay.id))
		validateStops(stops)

		val routeStops = stops.map(::routeStop)
		val routeCandidates = allStartEndCandidates(routeStops, tripDay.travelDate.atTime(tmapProperties.defaultStartTime))
		val bestRoute = routeCandidates
			.minWithOrNull(compareBy<RouteCandidate> { it.result.totalDurationSeconds }.thenBy { it.result.totalDistanceMeters })
			?: throw BadRequestException("경로 최적화할 방문지가 없습니다.")
		val orderedStops = reorderStops(stops, bestRoute.result.orderedStopIds)
		applyOptimizedStopOrder(orderedStops, bestRoute.result)

		tripDay.applyRouteOptimization(
			provider = ExternalApiProvider.TMAP,
			totalDistanceMeters = bestRoute.result.totalDistanceMeters,
			totalDurationSeconds = bestRoute.result.totalDurationSeconds,
			polyline = writePolyline(bestRoute.result.polyline),
			sourcePayload = writeRouteSourcePayload(
				stopCount = routeStops.size,
				combinationCount = routeCandidates.size,
				bestRoute = bestRoute,
			),
			optimizedAt = LocalDateTime.now(),
		)

		return tripService.getTripCourse(currentUser, tripId)
	}

	private fun validateStops(stops: List<TripStop>) {
		if (stops.size < MIN_OPTIMIZATION_STOP_COUNT) {
			throw BadRequestException("경로 최적화는 좌표가 있는 방문지 ${MIN_OPTIMIZATION_STOP_COUNT}곳 이상일 때 가능합니다.")
		}
		if (stops.size > MAX_OPTIMIZATION_STOP_COUNT) {
			throw BadRequestException("경로 최적화는 하루 방문지 ${MAX_OPTIMIZATION_STOP_COUNT}곳 이하만 가능합니다.")
		}
		val stopWithoutCoordinate = stops.firstOrNull { it.latitude == null || it.longitude == null }
		if (stopWithoutCoordinate != null) {
			throw BadRequestException("좌표가 없는 방문지는 경로 최적화할 수 없습니다: ${stopWithoutCoordinate.name}")
		}
	}

	private fun routeStop(stop: TripStop): TmapRouteStop =
		TmapRouteStop(
			stopId = requireNotNull(stop.id),
			name = stop.name,
			longitude = requireNotNull(stop.longitude),
			latitude = requireNotNull(stop.latitude),
			dwellSeconds = (stop.dwellMinutes ?: defaultDwellMinutes(stop.stopType)) * SECONDS_PER_MINUTE,
		)

	private fun allStartEndCandidates(
		stops: List<TmapRouteStop>,
		startTime: LocalDateTime,
	): List<RouteCandidate> =
		stops.flatMap { start ->
			stops.filter { it.stopId != start.stopId }.map { end ->
				val viaPoints = stops.filter { it.stopId != start.stopId && it.stopId != end.stopId }
				val request = TmapRouteOptimizationRequest(
					start = start,
					end = end,
					viaPoints = viaPoints,
					startTime = startTime,
				)
				RouteCandidate(
					request = request,
					result = tmapRouteClient.optimizeRoute(request),
				)
			}
		}

	private fun reorderStops(stops: List<TripStop>, orderedStopIds: List<Long>): List<TripStop> {
		val stopsById = stops.associateBy { requireNotNull(it.id) }
		val ordered = orderedStopIds.mapNotNull(stopsById::get)
		val missing = stops.filter { requireNotNull(it.id) !in orderedStopIds }
		return ordered + missing
	}

	private fun applyOptimizedStopOrder(orderedStops: List<TripStop>, result: TmapRouteOptimizationResult) {
		orderedStops.forEachIndexed { index, stop ->
			stop.sortOrder = -(index + 1)
			stop.arrivalTime = result.arrivalTimes[requireNotNull(stop.id)]
			stop.dwellMinutes = stop.dwellMinutes ?: defaultDwellMinutes(stop.stopType)
		}
		tripStopRepository.flush()

		orderedStops.forEachIndexed { index, stop ->
			stop.sortOrder = index + 1
		}
		tripStopRepository.flush()
	}

	private fun defaultDwellMinutes(stopType: StopType): Int =
		when (stopType) {
			StopType.CAFE,
			StopType.REST -> 40
			StopType.SIGHTSEEING,
			StopType.MEAL -> 60
		}

	private fun writePolyline(polyline: List<TmapRouteCoordinate>): String? =
		polyline.takeIf { it.isNotEmpty() }
			?.let { objectMapper.writeValueAsString(it) }

	private fun writeRouteSourcePayload(stopCount: Int, combinationCount: Int, bestRoute: RouteCandidate): String =
		objectMapper.writeValueAsString(
			mapOf(
				"provider" to ExternalApiProvider.TMAP.value,
				"operation" to "routeOptimization10",
				"policyVersion" to ROUTE_OPTIMIZATION_POLICY_VERSION,
				"stopCount" to stopCount,
				"combinationCount" to combinationCount,
				"startStopId" to bestRoute.request.start.stopId,
				"endStopId" to bestRoute.request.end.stopId,
				"orderedStopIds" to bestRoute.result.orderedStopIds,
				"searchOption" to tmapProperties.searchOption,
				"carType" to tmapProperties.carType,
				"rawProperties" to bestRoute.result.rawProperties,
			),
		)

	private fun validateTripReadable(userId: Long, trip: Trip) {
		val myMember = familyMemberRepository.findByUserId(userId)
			?: throw ForbiddenException("여행 조회 권한이 없습니다.")
		if (myMember.family.id != trip.family.id) {
			throw ForbiddenException("여행 조회 권한이 없습니다.")
		}
	}

	private fun getLoginUser(userId: Long): User =
		userRepository.findByIdAndDeletedAtIsNull(userId)
			?.takeIf { it.isSignupCompleted() }
			?: throw UnAuthorizedException("로그인할 수 없는 사용자입니다.")

	private data class RouteCandidate(
		val request: TmapRouteOptimizationRequest,
		val result: TmapRouteOptimizationResult,
	)

	companion object {
		private const val MIN_OPTIMIZATION_STOP_COUNT = 3
		private const val MAX_OPTIMIZATION_STOP_COUNT = 10
		private const val SECONDS_PER_MINUTE = 60
		private const val ROUTE_OPTIMIZATION_POLICY_VERSION = "tmap-route-optimization-v1"
	}
}
