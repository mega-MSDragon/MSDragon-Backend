package com.msdragon.backend.home.service

import com.msdragon.backend.auth.entity.GenderType
import com.msdragon.backend.auth.entity.User
import com.msdragon.backend.auth.entity.UserRole
import com.msdragon.backend.auth.repository.UserRepository
import com.msdragon.backend.auth.support.AuthenticatedUser
import com.msdragon.backend.common.exception.UnAuthorizedException
import com.msdragon.backend.family.repository.FamilyMemberRepository
import com.msdragon.backend.home.dto.HomeProfileGuideResponse
import com.msdragon.backend.home.dto.HomeProfileGuideType
import com.msdragon.backend.home.dto.HomeProfileTargetResponse
import com.msdragon.backend.home.dto.HomeResponse
import com.msdragon.backend.home.dto.HomeTripIntensity
import com.msdragon.backend.home.dto.HomeTripSummaryResponse
import com.msdragon.backend.parentprofile.entity.ParentProfileStatus
import com.msdragon.backend.parentprofile.entity.TravelThemeCode
import com.msdragon.backend.parentprofile.entity.WalkingPace
import com.msdragon.backend.parentprofile.repository.ParentProfileRepository
import com.msdragon.backend.trip.dto.TripDestinationResponse
import com.msdragon.backend.trip.dto.TripRecommendationSnapshotResponse
import com.msdragon.backend.trip.entity.Trip
import com.msdragon.backend.trip.entity.TripStatus
import com.msdragon.backend.trip.repository.TripRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

@Service
class HomeService(
	private val homeDataService: HomeDataService,
	private val homeDiscoveryService: HomeDiscoveryService,
) {
	fun getHome(currentUser: AuthenticatedUser): HomeResponse {
		val today = LocalDate.now(SERVICE_ZONE_ID)
		val data = homeDataService.getHomeData(currentUser, today)
		val discovery = homeDiscoveryService.getDiscovery(today)
		return HomeResponse(
			familyId = data.familyId,
			userRole = data.userRole,
			canCreateTrip = data.canCreateTrip,
			profileGuide = data.profileGuide,
			trips = data.trips,
			recommendationMonth = discovery.recommendationMonth,
			recommendedCities = discovery.recommendedCities,
			festivals = discovery.festivals,
		)
	}

	companion object {
		private val SERVICE_ZONE_ID: ZoneId = ZoneId.of("Asia/Seoul")
	}
}

@Service
class HomeDataService(
	private val userRepository: UserRepository,
	private val familyMemberRepository: FamilyMemberRepository,
	private val parentProfileRepository: ParentProfileRepository,
	private val tripRepository: TripRepository,
	private val objectMapper: ObjectMapper,
) {
	private val logger = LoggerFactory.getLogger(javaClass)

	@Transactional
	fun getHomeData(currentUser: AuthenticatedUser, today: LocalDate): HomeData {
		val user = getLoginUser(currentUser.id)
		val myMember = familyMemberRepository.findByUserId(currentUser.id)
		val familyId = myMember?.family?.id
		val trips = familyId?.let { getHomeTrips(it, today) }.orEmpty()
		val profileGuide = getProfileGuide(user, familyId)

		return HomeData(
			familyId = familyId,
			userRole = user.role,
			canCreateTrip = user.role == UserRole.CHILD,
			profileGuide = profileGuide,
			trips = trips,
		)
	}

	private fun getHomeTrips(familyId: Long, today: LocalDate): List<HomeTripSummaryResponse> =
		tripRepository.findAllByFamilyIdAndDeletedAtIsNullOrderByStartDateAscIdAsc(familyId)
			.onEach { it.synchronizeStatus(today) }
			.filter { it.status in HOME_TRIP_STATUSES }
			.sortedWith(
				compareBy<Trip> { if (it.status == TripStatus.IN_PROGRESS) 0 else 1 }
					.thenBy { it.startDate }
					.thenBy { it.id },
			)
			.map { trip -> toHomeTrip(trip, today) }

	private fun toHomeTrip(trip: Trip, today: LocalDate): HomeTripSummaryResponse {
		val snapshot = readRecommendationSnapshot(trip)
		return HomeTripSummaryResponse(
			id = requireNotNull(trip.id),
			title = trip.title,
			destination = TripDestinationResponse.from(trip.destinationCode),
			startDate = trip.startDate,
			endDate = trip.endDate,
			status = trip.status,
			dDay = trip.startDate.takeIf(today::isBefore)
				?.let { ChronoUnit.DAYS.between(today, it).toInt() },
			primaryTheme = snapshot?.let(::primaryThemeOf),
			intensity = snapshot?.let(::intensityOf),
		)
	}

	private fun readRecommendationSnapshot(trip: Trip): TripRecommendationSnapshotResponse? {
		val value = trip.recommendationSnapshot ?: return null
		return try {
			objectMapper.readValue(value, TripRecommendationSnapshotResponse::class.java)
		} catch (exception: Exception) {
			logger.warn("홈 여행 카드 추천 스냅샷 해석 실패: tripId={}", trip.id, exception)
			null
		}
	}

	private fun primaryThemeOf(snapshot: TripRecommendationSnapshotResponse): TravelThemeCode? {
		val counts = snapshot.parents
			.flatMap { it.travelThemes }
			.groupingBy { it }
			.eachCount()
		val highestCount = counts.values.maxOrNull() ?: return null
		return TravelThemeCode.entries.firstOrNull { counts[it] == highestCount }
	}

	private fun intensityOf(snapshot: TripRecommendationSnapshotResponse): HomeTripIntensity? {
		val walkingPaces = snapshot.parents.map { it.walkingPace }
		return when {
			walkingPaces.isEmpty() -> null
			WalkingPace.SLOW in walkingPaces -> HomeTripIntensity.LOW
			WalkingPace.NORMAL in walkingPaces -> HomeTripIntensity.NORMAL
			else -> HomeTripIntensity.HIGH
		}
	}

	private fun getProfileGuide(user: User, familyId: Long?): HomeProfileGuideResponse? =
		when (user.role) {
			UserRole.PARENT -> {
				val completed = parentProfileRepository.findByUserId(requireNotNull(user.id))
					?.status == ParentProfileStatus.COMPLETED
				if (completed) null else HomeProfileGuideResponse(
					type = HomeProfileGuideType.COMPLETE_MY_PROFILE,
					targets = listOf(profileTargetOf(user)),
				)
			}
			UserRole.CHILD -> {
				val incompleteParents = familyId?.let {
					familyMemberRepository.findAllByFamilyIdOrderByJoinedAtAsc(it)
						.asSequence()
						.filter { member -> member.memberRole == UserRole.PARENT }
						.map { member -> member.user }
						.filter { parent ->
							parentProfileRepository.findByUserId(requireNotNull(parent.id))
								?.status != ParentProfileStatus.COMPLETED
						}
						.map(::profileTargetOf)
						.toList()
				}.orEmpty()
				incompleteParents.takeIf(List<HomeProfileTargetResponse>::isNotEmpty)?.let {
					HomeProfileGuideResponse(
						type = HomeProfileGuideType.REQUEST_PARENT_PROFILE,
						targets = it,
					)
				}
			}
		}

	private fun profileTargetOf(parent: User): HomeProfileTargetResponse =
		HomeProfileTargetResponse(
			userId = requireNotNull(parent.id),
			displayName = parent.displayName,
			relationLabel = when (parent.gender) {
				GenderType.FEMALE -> "엄마"
				GenderType.MALE -> "아빠"
				GenderType.UNDISCLOSED -> null
			},
		)

	private fun getLoginUser(userId: Long): User =
		userRepository.findByIdAndDeletedAtIsNull(userId)
			?.takeIf { it.isSignupCompleted() }
			?: throw UnAuthorizedException("로그인할 수 없는 사용자입니다.")

	companion object {
		private val HOME_TRIP_STATUSES = setOf(TripStatus.PLANNING, TripStatus.READY, TripStatus.IN_PROGRESS)
	}
}

data class HomeData(
	val familyId: Long?,
	val userRole: UserRole,
	val canCreateTrip: Boolean,
	val profileGuide: HomeProfileGuideResponse?,
	val trips: List<HomeTripSummaryResponse>,
)
