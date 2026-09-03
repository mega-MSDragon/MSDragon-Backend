package com.msdragon.backend.home.service

import com.msdragon.backend.auth.entity.GenderType
import com.msdragon.backend.auth.entity.User
import com.msdragon.backend.auth.entity.UserRole
import com.msdragon.backend.auth.repository.UserRepository
import com.msdragon.backend.auth.support.AuthenticatedUser
import com.msdragon.backend.common.exception.UnAuthorizedException
import com.msdragon.backend.family.repository.FamilyMemberRepository
import com.msdragon.backend.feedback.entity.TripFeedback
import com.msdragon.backend.feedback.repository.TripFeedbackRepository
import com.msdragon.backend.home.dto.HomeFestivalsResponse
import com.msdragon.backend.home.dto.HomeMonthlyRecommendationsResponse
import com.msdragon.backend.home.dto.HomeMyTripsResponse
import com.msdragon.backend.home.dto.HomeSectionsResponse
import com.msdragon.backend.home.dto.HomeParentProfileResponse
import com.msdragon.backend.home.dto.HomeTripIntensity
import com.msdragon.backend.home.dto.HomeTripRatingResponse
import com.msdragon.backend.home.dto.HomeTripSummaryResponse
import com.msdragon.backend.parentprofile.entity.ParentProfileStatus
import com.msdragon.backend.parentprofile.entity.TravelThemeCode
import com.msdragon.backend.parentprofile.entity.WalkingPace
import com.msdragon.backend.parentprofile.repository.ParentProfileRepository
import com.msdragon.backend.trip.dto.TripDestinationResponse
import com.msdragon.backend.trip.dto.TripRecommendationSnapshotResponse
import com.msdragon.backend.trip.dto.relationLabelOf
import com.msdragon.backend.trip.entity.Trip
import com.msdragon.backend.trip.entity.TripStatus
import com.msdragon.backend.trip.repository.TripParticipantRepository
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
	fun getMyTrips(currentUser: AuthenticatedUser): HomeMyTripsResponse {
		val today = LocalDate.now(SERVICE_ZONE_ID)
		return homeDataService.getMyTrips(currentUser, today)
	}

	fun getMonthlyRecommendations(): HomeMonthlyRecommendationsResponse =
		homeDiscoveryService.getMonthlyRecommendations(LocalDate.now(SERVICE_ZONE_ID))

	fun getFestivals(): HomeFestivalsResponse =
		homeDiscoveryService.getFestivals(LocalDate.now(SERVICE_ZONE_ID))

	fun getSections(): HomeSectionsResponse =
		homeDiscoveryService.getSections(LocalDate.now(SERVICE_ZONE_ID))

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
	private val tripFeedbackRepository: TripFeedbackRepository,
	private val tripParticipantRepository: TripParticipantRepository,
	private val objectMapper: ObjectMapper,
) {
	private val logger = LoggerFactory.getLogger(javaClass)

	@Transactional
	fun getMyTrips(currentUser: AuthenticatedUser, today: LocalDate): HomeMyTripsResponse {
		val user = getLoginUser(currentUser.id)
		val myMember = familyMemberRepository.findByUserId(currentUser.id)
		val familyId = myMember?.family?.id
		val trips = familyId?.let { getHomeTrips(user, it, today) }.orEmpty()

		return HomeMyTripsResponse(
			familyId = familyId,
			userRole = user.role,
			parentProfiles = getParentProfiles(user, familyId),
			trips = trips,
		)
	}

	private fun getHomeTrips(
		user: User,
		familyId: Long,
		today: LocalDate,
	): List<HomeTripSummaryResponse> {
		val candidates = tripRepository.findAllByFamilyIdAndDeletedAtIsNullOrderByStartDateAscIdAsc(familyId)
			.onEach { it.synchronizeStatus(today) }
			.filter { it.status in HOME_TRIP_STATUSES }
		val feedbacksByTripId = tripFeedbackRepository.findAllByTripIdIn(candidates.map { requireNotNull(it.id) })
			.sortedBy { it.parentUser.id }
			.groupBy { requireNotNull(it.trip.id) }
		val myParticipatingTripIds = if (user.role == UserRole.PARENT) {
			tripParticipantRepository.findAllByUserId(requireNotNull(user.id))
				.mapTo(mutableSetOf()) { requireNotNull(it.trip.id) }
		} else {
			emptySet()
		}

		val trips = candidates
			.filter { trip ->
				isVisibleOnHome(trip, user, feedbacksByTripId[trip.id].orEmpty(), myParticipatingTripIds)
			}
			.sortedWith(
				compareBy<Trip> { if (it.status == TripStatus.IN_PROGRESS) 0 else 1 }
					.thenBy { it.startDate }
					.thenBy { it.id },
			)
		return trips.map { trip -> toHomeTrip(trip, today, feedbacksByTripId[trip.id].orEmpty()) }
	}

	/**
	 * 완료된 여행을 홈에 남길지 판단한다. 완료 전 여행은 역할과 무관하게 모두 보인다.
	 *
	 * - 자녀: 완료 여행을 홈에서 내린다. 기록 탭에서 확인한다.
	 * - 부모: **아직 평가하지 않은 완료 여행만** 남겨 평가를 유도한다. 제출하면 홈에서 사라진다.
	 * - 부모가 참여자가 아니면 평가할 수 없으므로 남기지 않는다. 남기면 사라지지 않는 카드가 된다.
	 */
	private fun isVisibleOnHome(
		trip: Trip,
		user: User,
		feedbacks: List<TripFeedback>,
		myParticipatingTripIds: Set<Long>,
	): Boolean {
		if (trip.status != TripStatus.COMPLETED) {
			return true
		}
		if (user.role != UserRole.PARENT) {
			return false
		}
		val tripId = requireNotNull(trip.id)
		return tripId in myParticipatingTripIds && feedbacks.none { it.parentUser.id == user.id }
	}

	private fun toHomeTrip(
		trip: Trip,
		today: LocalDate,
		feedbacks: List<TripFeedback>,
	): HomeTripSummaryResponse {
		val snapshot = readRecommendationSnapshot(trip)
		return HomeTripSummaryResponse(
			id = requireNotNull(trip.id),
			title = trip.title,
			destination = TripDestinationResponse.from(trip.destinationCode),
			startDate = trip.startDate,
			endDate = trip.endDate,
			dayTrip = trip.startDate == trip.endDate,
			status = trip.status,
			dDay = trip.startDate.takeIf(today::isBefore)
				?.let { ChronoUnit.DAYS.between(today, it).toInt() },
			primaryTheme = snapshot?.let(::primaryThemeOf),
			intensity = snapshot?.let(::intensityOf),
			ratings = feedbacks.map { feedback ->
				HomeTripRatingResponse(
					parentUserId = requireNotNull(feedback.parentUser.id),
					displayName = feedback.parentUser.displayName,
					relationLabel = relationLabelOf(feedback.parentUser),
					overallRating = feedback.overallRating,
				)
			},
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

	private fun getParentProfiles(user: User, familyId: Long?): List<HomeParentProfileResponse> {
		val parents = when (user.role) {
			UserRole.PARENT -> listOf(user)
			UserRole.CHILD -> familyId?.let {
				familyMemberRepository.findAllByFamilyIdOrderByJoinedAtAsc(it)
					.filter { member -> member.memberRole == UserRole.PARENT }
					.map { member -> member.user }
			}.orEmpty()
		}
		return parents.map(::parentProfileOf)
	}

	private fun parentProfileOf(parent: User): HomeParentProfileResponse =
		HomeParentProfileResponse(
			userId = requireNotNull(parent.id),
			displayName = parent.displayName,
			relationLabel = when (parent.gender) {
				GenderType.FEMALE -> "엄마"
				GenderType.MALE -> "아빠"
				GenderType.UNDISCLOSED -> null
			},
			profileCompleted = parentProfileRepository.findByUserId(requireNotNull(parent.id))
				?.status == ParentProfileStatus.COMPLETED,
		)

	private fun getLoginUser(userId: Long): User =
		userRepository.findByIdAndDeletedAtIsNull(userId)
			?.takeIf { it.isSignupCompleted() }
			?: throw UnAuthorizedException("로그인할 수 없는 사용자입니다.")

	companion object {
		private val HOME_TRIP_STATUSES = setOf(
			TripStatus.PLANNING,
			TripStatus.READY,
			TripStatus.IN_PROGRESS,
			TripStatus.COMPLETED,
		)
	}
}
