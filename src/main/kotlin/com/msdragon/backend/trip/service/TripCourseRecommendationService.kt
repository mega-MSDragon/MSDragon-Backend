package com.msdragon.backend.trip.service

import com.msdragon.backend.auth.entity.User
import com.msdragon.backend.auth.repository.UserRepository
import com.msdragon.backend.auth.support.AuthenticatedUser
import com.msdragon.backend.common.exception.BadRequestException
import com.msdragon.backend.common.exception.ForbiddenException
import com.msdragon.backend.common.exception.NotFoundException
import com.msdragon.backend.common.exception.UnAuthorizedException
import com.msdragon.backend.family.repository.FamilyMemberRepository
import com.msdragon.backend.parentprofile.entity.FoodPreference
import com.msdragon.backend.parentprofile.entity.WalkingPace
import com.msdragon.backend.trip.dto.TripCourseResponse
import com.msdragon.backend.trip.dto.TripParentProfileSnapshotResponse
import com.msdragon.backend.trip.dto.TripRecommendationSnapshotResponse
import com.msdragon.backend.trip.entity.ExternalApiProvider
import com.msdragon.backend.trip.entity.StopType
import com.msdragon.backend.trip.entity.Trip
import com.msdragon.backend.trip.entity.TripDay
import com.msdragon.backend.trip.entity.TripStatus
import com.msdragon.backend.trip.entity.TripStop
import com.msdragon.backend.trip.repository.TripDayRepository
import com.msdragon.backend.trip.repository.TripRepository
import com.msdragon.backend.trip.repository.TripStopRepository
import com.msdragon.backend.trip.tourapi.DestinationTourApiPolicy
import com.msdragon.backend.trip.tourapi.TourApiAccessibility
import com.msdragon.backend.trip.tourapi.TourApiClient
import com.msdragon.backend.trip.tourapi.TourApiContentType
import com.msdragon.backend.trip.tourapi.TourApiPlaceSearch
import com.msdragon.backend.trip.tourapi.TourApiPlaceSummary
import com.msdragon.backend.trip.tourapi.TourApiProfileSignals
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import kotlin.math.max

@Service
class TripCourseRecommendationService(
	private val userRepository: UserRepository,
	private val familyMemberRepository: FamilyMemberRepository,
	private val tripRepository: TripRepository,
	private val tripDayRepository: TripDayRepository,
	private val tripStopRepository: TripStopRepository,
	private val tripService: TripService,
	private val tourApiClient: TourApiClient,
	private val objectMapper: ObjectMapper,
) {
	@Transactional
	fun recommendCourse(currentUser: AuthenticatedUser, tripId: Long): TripCourseResponse {
		getLoginUser(currentUser.id)
		val trip = tripRepository.findByIdAndDeletedAtIsNull(tripId)
			?: throw NotFoundException("여행을 찾을 수 없습니다.")
		validateTripReadable(currentUser.id, trip)

		val tripDays = tripDayRepository.findAllByTripIdOrderByDayNumberAsc(tripId)
		if (tripDays.isEmpty()) {
			throw BadRequestException("여행 일자가 없어 추천 코스를 만들 수 없습니다.")
		}

		val snapshot = trip.recommendationSnapshot
			?.let { objectMapper.readValue(it, TripRecommendationSnapshotResponse::class.java) }
			?: throw BadRequestException("여행 추천 입력 스냅샷이 없습니다.")
		if (snapshot.parents.isEmpty()) {
			throw BadRequestException("여행 대상 부모 정보가 없어 추천 코스를 만들 수 없습니다.")
		}

		val placesPerDay = placesPerDay(snapshot.parents)
		val requiredTotal = tripDays.size * placesPerDay
		val candidates = collectCandidates(trip, snapshot.parents, requiredTotal)
		if (candidates.isEmpty()) {
			throw BadRequestException("추천할 관광지를 찾을 수 없습니다.")
		}

		val scoredCandidates = scoreCandidates(candidates, snapshot.parents, requiredTotal)
		val selectedByDay = distributeByDay(scoredCandidates, tripDays, placesPerDay)
		if (selectedByDay.values.all { it.isEmpty() }) {
			throw BadRequestException("추천할 관광지를 찾을 수 없습니다.")
		}

		val existingStops = tripStopRepository.findAllByTripDayTripIdOrderByTripDayDayNumberAscSortOrderAsc(tripId)
		if (existingStops.isNotEmpty()) {
			tripStopRepository.deleteAllInBatch(existingStops)
			tripStopRepository.flush()
		}
		tripDays.forEach { it.clearRouteOptimization() }

		selectedByDay.forEach { (day, selectedCandidates) ->
			val stops = selectedCandidates.mapIndexed { index, candidate ->
				val detail = tourApiClient.getPlaceDetail(candidate.summary.contentId)
				val accessibility = candidate.accessibility ?: tourApiClient.getAccessibility(candidate.summary.contentId)
				val contentType = TourApiContentType.fromId(candidate.summary.contentTypeId)
				TripStop(
					tripDay = day,
					sortOrder = index + 1,
					stopType = contentType?.stopType ?: StopType.SIGHTSEEING,
					sourceProvider = ExternalApiProvider.TOUR_API,
					externalPlaceId = candidate.summary.contentId,
					contentTypeId = candidate.summary.contentTypeId,
					name = candidate.summary.title,
					category = contentType?.displayName,
					address = candidate.summary.address,
					latitude = candidate.summary.latitude,
					longitude = candidate.summary.longitude,
					phone = candidate.summary.tel,
					homepageUrl = detail?.homepage.trimToNull(),
					imageUrl = candidate.summary.firstImage ?: candidate.summary.firstImageThumbnail,
					overview = detail?.overview.trimToNull(),
					dwellMinutes = defaultDwellMinutes(contentType),
					recommendationReason = recommendationReason(candidate, accessibility),
					recommendationTags = writeRecommendationTags(recommendationTags(candidate, accessibility)),
					sourcePayload = writeSourcePayload(candidate, detail?.raw, accessibility),
					isManualAdded = false,
				)
			}
			tripStopRepository.saveAll(stops)
		}

		if (trip.status == TripStatus.PLANNING) {
			trip.status = TripStatus.READY
		}

		return tripService.getTripCourse(currentUser, tripId)
	}

	private fun collectCandidates(
		trip: Trip,
		parents: List<TripParentProfileSnapshotResponse>,
		requiredTotal: Int,
	): List<TourApiPlaceSummary> {
		val destinationPolicy = DestinationTourApiPolicy.of(trip.destinationCode)
		if (destinationPolicy.regions.isEmpty()) {
			throw BadRequestException("지원하지 않는 여행 도시입니다.")
		}

		val rowsPerRequest = minOf(50, max(20, requiredTotal))
		return destinationPolicy.regions
			.flatMap { region ->
				TourApiContentType.recommendationTargets.flatMap { contentType ->
					tourApiClient.findPlaces(
						TourApiPlaceSearch(
							region = region,
							contentTypeId = contentType.id,
							numOfRows = rowsPerRequest,
						),
					)
				}
			}
			.filter { TourApiContentType.fromId(it.contentTypeId) != null }
			.distinctBy { it.contentId }
			.sortedByDescending { baseScore(it, parents) }
	}

	private fun scoreCandidates(
		candidates: List<TourApiPlaceSummary>,
		parents: List<TripParentProfileSnapshotResponse>,
		requiredTotal: Int,
	): List<ScoredTourApiCandidate> {
		val mobilityNeeded = parents.any { it.needsMobilityAssistance }
		val accessibilityLookupLimit = max(40, requiredTotal * 5)
		return candidates.mapIndexed { index, summary ->
			val accessibility = if (mobilityNeeded && index < accessibilityLookupLimit) {
				tourApiClient.getAccessibility(summary.contentId)
			} else {
				null
			}
			ScoredTourApiCandidate(
				summary = summary,
				score = baseScore(summary, parents) + accessibilityScore(accessibility, mobilityNeeded),
				accessibility = accessibility,
			)
		}.sortedWith(
			compareByDescending<ScoredTourApiCandidate> { it.score }
				.thenBy { it.summary.contentId }
				.thenBy { it.summary.title },
		)
	}

	private fun baseScore(
		summary: TourApiPlaceSummary,
		parents: List<TripParentProfileSnapshotResponse>,
	): Int {
		val system = summary.systemCode()
		var score = BASE_SCORE
		parents.forEach { parent ->
			val signals = TourApiProfileSignals.of(parent.travelThemes, parent.personalityType, parent.foodPreference)
			if (system != null && system in signals.themeSystems) {
				score += THEME_MATCH_SCORE
			}
			if (system != null && system in signals.personalitySystems) {
				score += PERSONALITY_MATCH_SCORE
			}
			if (summary.contentTypeId == TourApiContentType.FOOD.id) {
				score += foodScore(signals.foodPreference, summary)
			}
		}
		score += walkingPaceScore(summary, slowestWalkingPace(parents))
		if (!summary.firstImage.isNullOrBlank() || !summary.firstImageThumbnail.isNullOrBlank()) {
			score += IMAGE_SCORE
		}
		return score
	}

	private fun foodScore(foodPreference: FoodPreference, summary: TourApiPlaceSummary): Int =
		when (foodPreference) {
			FoodPreference.KOREAN -> if (summary.hasKoreanFoodText()) 12 else 8
			FoodPreference.FAMILIAR -> 8
			FoodPreference.ADVENTUROUS -> 6
		}

	private fun walkingPaceScore(summary: TourApiPlaceSummary, walkingPace: WalkingPace): Int {
		val system = summary.systemCode()
		return when (walkingPace) {
			WalkingPace.SLOW -> when (system) {
				"NA", "HS", "VE", "FD" -> 4
				"LS" -> -8
				else -> 0
			}

			WalkingPace.NORMAL -> when (system) {
				"NA", "HS", "VE", "FD" -> 2
				else -> 0
			}

			WalkingPace.FAST -> when (system) {
				"LS", "EX", "EV" -> 6
				else -> 0
			}
		}
	}

	private fun accessibilityScore(accessibility: TourApiAccessibility?, mobilityNeeded: Boolean): Int {
		if (!mobilityNeeded) {
			return if (accessibility?.hasAnyPriorityInfo() == true) 2 else 0
		}
		if (accessibility == null || !accessibility.hasAnyPriorityInfo()) {
			return -8
		}

		var score = 0
		if (!accessibility.route.isNullOrBlank()) score += 12
		if (!accessibility.exit.isNullOrBlank()) score += 12
		if (!accessibility.restroom.isNullOrBlank()) score += 10
		if (!accessibility.wheelchair.isNullOrBlank()) score += 8
		if (!accessibility.elevator.isNullOrBlank()) score += 8
		if (!accessibility.parking.isNullOrBlank()) score += 6
		if (!accessibility.publicTransport.isNullOrBlank()) score += 4
		return score
	}

	private fun distributeByDay(
		scoredCandidates: List<ScoredTourApiCandidate>,
		tripDays: List<TripDay>,
		placesPerDay: Int,
	): Map<TripDay, List<ScoredTourApiCandidate>> {
		val usedContentIds = mutableSetOf<String>()
		val mealQueue = ArrayDeque(scoredCandidates.filter { it.summary.contentTypeId == TourApiContentType.FOOD.id })
		val nonMealQueue = ArrayDeque(scoredCandidates.filter { it.summary.contentTypeId != TourApiContentType.FOOD.id })
		val fallbackQueue = ArrayDeque(scoredCandidates)

		return tripDays.associateWith { _ ->
			val selected = mutableListOf<ScoredTourApiCandidate>()
			repeat(max(placesPerDay - 1, 0)) {
				pollNext(nonMealQueue, usedContentIds)?.let(selected::add)
			}
			pollNext(mealQueue, usedContentIds)?.let { meal ->
				val mealIndex = minOf(1, selected.size)
				selected.add(mealIndex, meal)
			}
			while (selected.size < placesPerDay) {
				selected.add(pollNext(fallbackQueue, usedContentIds) ?: break)
			}
			selected
		}
	}

	private fun pollNext(
		queue: ArrayDeque<ScoredTourApiCandidate>,
		usedContentIds: MutableSet<String>,
	): ScoredTourApiCandidate? {
		while (queue.isNotEmpty()) {
			val candidate = queue.removeFirst()
			if (usedContentIds.add(candidate.summary.contentId)) {
				return candidate
			}
		}
		return null
	}

	private fun placesPerDay(parents: List<TripParentProfileSnapshotResponse>): Int =
		when (slowestWalkingPace(parents)) {
			WalkingPace.SLOW -> 3
			WalkingPace.NORMAL -> 4
			WalkingPace.FAST -> 5
		}

	private fun slowestWalkingPace(parents: List<TripParentProfileSnapshotResponse>): WalkingPace =
		parents.map { it.walkingPace }.minBy { walkingPaceOrder[it] ?: 0 }

	private fun recommendationReason(
		candidate: ScoredTourApiCandidate,
		accessibility: TourApiAccessibility?,
	): String =
		when {
			candidate.summary.contentTypeId == TourApiContentType.FOOD.id ->
				"부모님 음식 취향을 반영한 식사 장소입니다."

			accessibility?.hasAnyPriorityInfo() == true ->
				"부모님 선호 테마와 무장애 정보를 함께 반영한 추천 장소입니다."

			else ->
				"부모님 선호 테마와 여행 성향을 반영한 추천 장소입니다."
		}

	private fun recommendationTags(
		candidate: ScoredTourApiCandidate,
		accessibility: TourApiAccessibility?,
	): List<String> =
		listOfNotNull(
			"tour_api",
			"type:${candidate.summary.contentTypeId}",
			candidate.summary.systemCode()?.lowercase(),
			if (candidate.summary.contentTypeId == TourApiContentType.FOOD.id) "meal" else null,
			if (accessibility?.hasAnyPriorityInfo() == true) "mobility_info" else null,
		)

	private fun writeRecommendationTags(tags: List<String>): String? =
		tags.mapNotNull { it.trimToNull() }
			.takeIf { it.isNotEmpty() }
			?.let { objectMapper.writeValueAsString(it) }

	private fun writeSourcePayload(
		candidate: ScoredTourApiCandidate,
		detailRaw: Map<String, Any?>?,
		accessibility: TourApiAccessibility?,
	): String =
		objectMapper.writeValueAsString(
			mapOf(
				"provider" to "tour_api",
				"summary" to candidate.summary.raw,
				"detailCommon" to detailRaw,
				"accessibility" to accessibility?.raw,
				"recommendation" to mapOf(
					"policyVersion" to COURSE_RECOMMENDATION_POLICY_VERSION,
					"score" to candidate.score,
				),
			),
		)

	private fun defaultDwellMinutes(contentType: TourApiContentType?): Int =
		when (contentType) {
			TourApiContentType.FOOD -> 60
			else -> 60
		}

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

	private data class ScoredTourApiCandidate(
		val summary: TourApiPlaceSummary,
		val score: Int,
		val accessibility: TourApiAccessibility?,
	)

	companion object {
		private const val COURSE_RECOMMENDATION_POLICY_VERSION = "tour-api-course-recommendation-v1"
		private const val BASE_SCORE = 10
		private const val THEME_MATCH_SCORE = 15
		private const val PERSONALITY_MATCH_SCORE = 8
		private const val IMAGE_SCORE = 2

		private val walkingPaceOrder: Map<WalkingPace, Int> = mapOf(
			WalkingPace.SLOW to 0,
			WalkingPace.NORMAL to 1,
			WalkingPace.FAST to 2,
		)
	}
}

private fun TourApiPlaceSummary.systemCode(): String? =
	lclsSystm1 ?: when (contentTypeId) {
		TourApiContentType.CULTURE_FACILITY.id -> "VE"
		TourApiContentType.FESTIVAL_EVENT.id -> "EV"
		TourApiContentType.LEPORTS.id -> "LS"
		TourApiContentType.SHOPPING.id -> "SH"
		TourApiContentType.FOOD.id -> "FD"
		else -> null
	}

private fun TourApiPlaceSummary.hasKoreanFoodText(): Boolean =
	listOf(title, address.orEmpty(), raw["cat3"]?.toString().orEmpty(), raw["lclsSystm3"]?.toString().orEmpty())
		.any { value -> koreanFoodKeywords.any { keyword -> value.contains(keyword, ignoreCase = true) } }

private val koreanFoodKeywords = listOf("한식", "백반", "국밥", "갈비", "불고기", "전통", "향토", "한정식")

private fun String?.trimToNull(): String? =
	this?.trim()?.takeIf { it.isNotEmpty() }
