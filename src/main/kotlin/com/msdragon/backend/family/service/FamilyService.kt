package com.msdragon.backend.family.service

import com.msdragon.backend.auth.entity.User
import com.msdragon.backend.auth.entity.UserRole
import com.msdragon.backend.auth.repository.UserRepository
import com.msdragon.backend.common.exception.BadRequestException
import com.msdragon.backend.common.exception.InternalServerException
import com.msdragon.backend.common.exception.NotFoundException
import com.msdragon.backend.common.exception.UnAuthorizedException
import com.msdragon.backend.family.dto.FamilyCodeResponse
import com.msdragon.backend.family.dto.FamilyMatchResponse
import com.msdragon.backend.family.dto.MyFamilyResponse
import com.msdragon.backend.family.dto.MatchFamilyCodeRequest
import com.msdragon.backend.family.entity.Family
import com.msdragon.backend.family.entity.FamilyCode
import com.msdragon.backend.family.entity.FamilyCodeUsage
import com.msdragon.backend.family.entity.FamilyMember
import com.msdragon.backend.family.repository.FamilyCodeRepository
import com.msdragon.backend.auth.entity.AgeBand
import com.msdragon.backend.auth.entity.GenderType
import com.msdragon.backend.auth.entity.OAuthProvider
import com.msdragon.backend.family.config.FamilyProperties
import com.msdragon.backend.family.repository.FamilyCodeUsageRepository
import com.msdragon.backend.parentprofile.entity.FoodPreference
import com.msdragon.backend.parentprofile.entity.ParentProfile
import com.msdragon.backend.parentprofile.entity.ParentProfileStatus
import com.msdragon.backend.parentprofile.entity.TravelPersonalityTypeCode
import com.msdragon.backend.parentprofile.entity.TravelThemeCode
import com.msdragon.backend.parentprofile.entity.WalkingPace
import com.msdragon.backend.parentprofile.repository.ParentProfileRepository
import com.msdragon.backend.trip.entity.ExternalApiProvider
import com.msdragon.backend.trip.entity.StopType
import com.msdragon.backend.trip.entity.Trip
import com.msdragon.backend.trip.entity.TripDay
import com.msdragon.backend.trip.entity.TripDestinationCode
import com.msdragon.backend.trip.entity.TripParticipant
import com.msdragon.backend.trip.entity.TripStatus
import com.msdragon.backend.trip.entity.TripStop
import com.msdragon.backend.trip.repository.TripDayRepository
import com.msdragon.backend.trip.repository.TripParticipantRepository
import com.msdragon.backend.trip.repository.TripRepository
import com.msdragon.backend.trip.repository.TripStopRepository
import org.slf4j.LoggerFactory
import com.msdragon.backend.family.repository.FamilyMemberRepository
import com.msdragon.backend.family.repository.FamilyRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID

@Service
class FamilyService(
	private val userRepository: UserRepository,
	private val familyRepository: FamilyRepository,
	private val familyMemberRepository: FamilyMemberRepository,
	private val familyCodeRepository: FamilyCodeRepository,
	private val familyCodeUsageRepository: FamilyCodeUsageRepository,
	private val parentProfileRepository: ParentProfileRepository,
	private val familyProperties: FamilyProperties,
	private val tripRepository: TripRepository,
	private val tripParticipantRepository: TripParticipantRepository,
	private val tripDayRepository: TripDayRepository,
	private val tripStopRepository: TripStopRepository,
) {
	private val random = SecureRandom()

	@Transactional
	fun issueMyCode(userId: Long): FamilyCodeResponse {
		val user = getLoginUser(userId)
		val familyCode = familyCodeRepository.findByUserId(userId)
			?: familyCodeRepository.save(FamilyCode(user = user, code = generateUniqueCode()))

		return FamilyCodeResponse(code = familyCode.code)
	}

	@Transactional(readOnly = true)
	fun getMyFamily(userId: Long): MyFamilyResponse {
		getLoginUser(userId)
		val myCode = familyCodeRepository.findByUserId(userId)?.code
		val myMember = familyMemberRepository.findByUserId(userId)
			?: return MyFamilyResponse.empty(myCode)
		val members = familyMembers(myMember.family)
		return MyFamilyResponse.of(
			family = myMember.family,
			myCode = myCode,
			members = members,
			profilesByUserId = parentProfilesOf(members),
		)
	}

	@Transactional
	fun matchByCode(userId: Long, request: MatchFamilyCodeRequest): FamilyMatchResponse {
		val requester = getLoginUser(userId)
		if (isReviewCode(request.code)) {
			return connectReviewFamily(requester)
		}
		val targetCode = familyCodeRepository.findByCodeAndIsActiveTrue(normalizeCode(request.code))
			?: throw NotFoundException("가족 코드를 찾을 수 없습니다.")
		val targetUser = targetCode.user

		if (targetUser.deletedAt != null || !targetUser.isSignupCompleted()) {
			throw NotFoundException("가족 코드를 찾을 수 없습니다.")
		}
		if (requester.id == targetUser.id) {
			throw BadRequestException("내 코드는 입력할 수 없습니다.")
		}
		if (requester.role == targetUser.role) {
			throw BadRequestException("부모와 자녀만 가족으로 연결할 수 있습니다.")
		}

		val child = if (requester.role == UserRole.CHILD) requester else targetUser
		val parent = if (requester.role == UserRole.PARENT) requester else targetUser
		val childMember = familyMemberRepository.findByUserId(requireNotNull(child.id))
		val parentMember = familyMemberRepository.findByUserId(requireNotNull(parent.id))

		if (parentMember != null && childMember != null && parentMember.family.id != childMember.family.id) {
			throw BadRequestException("이미 다른 가족과 연결된 사용자입니다.")
		}
		if (parentMember != null && childMember == null) {
			throw BadRequestException("이미 다른 가족과 연결된 부모입니다.")
		}

		val family = childMember?.family ?: createChildFamily(child)
		if (parentMember != null) {
			recordCodeUsageIfNeeded(targetCode, requester, family)
			val matchedMembers = familyMembers(family)
			return FamilyMatchResponse.of(family, targetUser, matchedMembers, parentProfilesOf(matchedMembers))
		}

		val familyId = requireNotNull(family.id)
		if (familyMemberRepository.countByFamilyIdAndMemberRole(familyId, UserRole.PARENT) >= MAX_PARENT_COUNT) {
			throw BadRequestException("가족에는 부모를 최대 2명까지만 연결할 수 있습니다.")
		}

		familyMemberRepository.save(
			FamilyMember(
				family = family,
				user = parent,
				memberRole = UserRole.PARENT,
			),
		)
		recordCodeUsageIfNeeded(targetCode, requester, family)

		val resultMembers = familyMembers(family)
		return FamilyMatchResponse.of(family, targetUser, resultMembers, parentProfilesOf(resultMembers))
	}

	/**
	 * 앱 스토어 심사용 코드로 연결한다. 심사자마다 **새 1:1 데모 가족**을 만들어 붙이므로
	 * 재심사가 반복되어도 부모 슬롯이 소진되지 않는다.
	 *
	 * 일반 코드는 자녀 1명 + 부모 최대 2명 제약이 있어 데모 계정 하나를 공유하면
	 * 자녀 데모는 2회, 부모 데모는 1회만 연결되고 이후 심사가 막힌다.
	 *
	 * 데모 부모에게는 완료된 프로필과 여행 MBTI를 넣어 심사자가 바로 여행을 만들 수 있게 한다.
	 */
	private fun isReviewCode(code: String): Boolean =
		familyProperties.isReviewCodeEnabled() &&
			normalizeCode(code) == normalizeCode(familyProperties.reviewCode)

	private fun connectReviewFamily(requester: User): FamilyMatchResponse {
		val existingMember = familyMemberRepository.findByUserId(requireNotNull(requester.id))
		if (existingMember != null) {
			// 심사자가 두 번 눌러도 오류로 막지 않고 현재 가족을 그대로 보여준다.
			val members = familyMembers(existingMember.family)
			val counterpart = members.firstOrNull { it.user.id != requester.id }?.user ?: requester
			return FamilyMatchResponse.of(
				existingMember.family,
				counterpart,
				members,
				parentProfilesOf(members),
			)
		}

		val demoUser = userRepository.save(demoCounterpart(requester.role))
		val child = if (requester.role == UserRole.CHILD) requester else demoUser
		val parent = if (requester.role == UserRole.PARENT) requester else demoUser

		val family = createChildFamily(child)
		familyMemberRepository.save(
			FamilyMember(family = family, user = parent, memberRole = UserRole.PARENT),
		)
		if (parent.id == demoUser.id) {
			parentProfileRepository.save(completedDemoProfile(demoUser))
		}
		createDemoTrip(family, child, listOf(child, parent))

		log.info("심사용 데모 가족을 생성했습니다. familyId={} requesterUserId={}", family.id, requester.id)

		val members = familyMembers(family)
		return FamilyMatchResponse.of(family, demoUser, members, parentProfilesOf(members))
	}

	/**
	 * 심사자와 반대 역할의 데모 사용자. 실제 소셜 계정이 아니므로 `oauth_subject`에
	 * [REVIEW_DEMO_SUBJECT_PREFIX]를 붙여 식별하고, 심사 후 이 접두사로 정리할 수 있다.
	 * 카카오 회원번호는 숫자 문자열이라 충돌하지 않는다.
	 */
	private fun demoCounterpart(requesterRole: UserRole): User {
		val demoRole = if (requesterRole == UserRole.CHILD) UserRole.PARENT else UserRole.CHILD
		val now = LocalDateTime.now()
		return User(
			role = demoRole,
			oauthProvider = OAuthProvider.KAKAO,
			oauthSubject = "$REVIEW_DEMO_SUBJECT_PREFIX${UUID.randomUUID()}",
			displayName = if (demoRole == UserRole.PARENT) "엄마" else "하늘",
			ageBand = if (demoRole == UserRole.PARENT) AgeBand.AGE_60S else AgeBand.AGE_20S,
			gender = GenderType.FEMALE,
			signupCompletedAt = now,
			lastLoginAt = now,
		)
	}

	/** 심사자가 곧바로 여행을 만들 수 있도록 완료 상태로 만든다. */
	private fun completedDemoProfile(parent: User): ParentProfile =
		ParentProfile(
			user = parent,
			status = ParentProfileStatus.COMPLETED,
			currentStep = 3,
			walkingPace = WalkingPace.NORMAL,
			foodPreference = FoodPreference.FAMILIAR,
			needsMobilityAssistance = false,
			travelThemes = mutableSetOf(TravelThemeCode.NATURE_SCENERY.value),
			personalityType = TravelPersonalityTypeCode.HEALING_TRAVELER,
			completionPercent = 100,
			completedAt = LocalDateTime.now(),
		)

	/**
	 * 심사자가 볼 여행을 하나 만든다. 기간을 `오늘-2 ~ 오늘`로 잡아 **여행 모드와 피드백을 동시에** 연다.
	 *
	 * - 오늘이 기간에 포함되므로 조회 시 `in_progress`가 되어 여행 모드와 AI 챗봇을 볼 수 있다.
	 * - 종료일이 오늘이라 부모가 바로 피드백을 제출할 수 있고, 데모 가족은 부모가 한 명이라
	 *   그 제출로 여행이 완료되어 기록과 효도 리포트까지 이어진다.
	 * - 부모로 가입한 심사자는 여행을 만들 수 없다(생성은 대표 자녀 권한). 이 여행이 없으면
	 *   부모 경로에서는 볼 화면이 없다.
	 *
	 * 방문지는 TourAPI를 호출하지 않고 고정값으로 넣는다. 심사 확인용이라 실제 조회가 필요 없고
	 * 외부 API 장애가 심사를 막지 않아야 한다.
	 */
	private fun createDemoTrip(family: Family, child: User, members: List<User>) {
		val today = LocalDate.now(SERVICE_ZONE_ID)
		val startDate = today.minusDays(2)
		val trip = tripRepository.save(
			Trip(
				family = family,
				createdByUser = child,
				destinationCode = TripDestinationCode.GYEONGJU,
				title = "부모님과 경주 여행",
				startDate = startDate,
				endDate = today,
				status = TripStatus.READY,
			),
		)
		tripParticipantRepository.saveAll(members.map { TripParticipant(trip = trip, user = it) })

		DEMO_COURSE.forEach { (dayOffset, stops) ->
			val day = tripDayRepository.save(
				TripDay(trip = trip, dayNumber = dayOffset + 1, travelDate = startDate.plusDays(dayOffset.toLong())),
			)
			day.applyRouteOptimization(
				provider = ExternalApiProvider.TMAP,
				totalDistanceMeters = 12_400,
				totalDurationSeconds = 2_100,
				polyline = null,
				sourcePayload = null,
				optimizedAt = LocalDateTime.now(),
			)
			tripDayRepository.save(day)

			stops.forEachIndexed { index, stop ->
				tripStopRepository.save(
					TripStop(
						tripDay = day,
						sortOrder = index + 1,
						stopType = stop.stopType,
						name = stop.name,
						category = stop.category,
						address = "경상북도 경주시",
						latitude = stop.latitude,
						longitude = stop.longitude,
						arrivalTime = stop.arrivalTime,
						dwellMinutes = stop.dwellMinutes,
					),
				)
			}
		}
	}

	private data class DemoStop(
		val name: String,
		val category: String,
		val stopType: StopType,
		val latitude: BigDecimal,
		val longitude: BigDecimal,
		val arrivalTime: LocalTime,
		val dwellMinutes: Int,
	)

	private fun getLoginUser(userId: Long): User =
		userRepository.findByIdAndDeletedAtIsNull(userId)
			?.takeIf { it.isSignupCompleted() }
			?: throw UnAuthorizedException("로그인할 수 없는 사용자입니다.")

	private fun createChildFamily(child: User): Family {
		val family = familyRepository.save(Family(ownerUser = child))
		familyMemberRepository.save(
			FamilyMember(
				family = family,
				user = child,
				memberRole = UserRole.CHILD,
			),
		)
		return family
	}

	private fun recordCodeUsageIfNeeded(
		familyCode: FamilyCode,
		requester: User,
		family: Family,
	) {
		val familyCodeId = requireNotNull(familyCode.id)
		val requesterUserId = requireNotNull(requester.id)
		if (!familyCodeUsageRepository.existsByFamilyCodeIdAndRequesterUserId(familyCodeId, requesterUserId)) {
			familyCodeUsageRepository.save(
				FamilyCodeUsage(
					familyCode = familyCode,
					requesterUser = requester,
					family = family,
				),
			)
		}
	}

	/** 마이페이지 프로필 카드가 여행 MBTI와 미입력 여부를 함께 보여주므로 부모 프로필을 함께 읽는다. */
	private fun parentProfilesOf(members: List<FamilyMember>): Map<Long, ParentProfile> =
		members
			.filter { it.memberRole == UserRole.PARENT }
			.mapNotNull { member ->
				parentProfileRepository.findByUserId(requireNotNull(member.user.id))
					?.let { requireNotNull(member.user.id) to it }
			}
			.toMap()

	private fun familyMembers(family: Family): List<FamilyMember> =
		familyMemberRepository.findAllByFamilyIdOrderByJoinedAtAsc(requireNotNull(family.id))

	private fun generateUniqueCode(): String {
		repeat(CODE_GENERATION_MAX_ATTEMPTS) {
			val code = "MSH-%04d".format(random.nextInt(CODE_NUMBER_BOUND))
			if (!familyCodeRepository.existsByCode(code)) {
				return code
			}
		}
		throw InternalServerException("가족 코드를 생성할 수 없습니다.")
	}

	private fun normalizeCode(code: String): String {
		val compactCode = code.uppercase().replace("-", "")
		return "${compactCode.take(3)}-${compactCode.drop(3)}"
	}

	companion object {
		private val log = LoggerFactory.getLogger(FamilyService::class.java)

		private val SERVICE_ZONE_ID: ZoneId = ZoneId.of("Asia/Seoul")

		/** 심사용 데모 사용자 식별 접두사. 심사 후 정리 쿼리에 사용한다. */
		const val REVIEW_DEMO_SUBJECT_PREFIX = "review-demo:"

		/** 일차 offset에 대한 고정 방문지. 좌표는 경주 실제 위치라 지도와 주변 시설 조회가 동작한다. */
		private val DEMO_COURSE: Map<Int, List<DemoStop>> = mapOf(
			0 to listOf(
				DemoStop("대릉원", "관광지", StopType.SIGHTSEEING, BigDecimal("35.8383"), BigDecimal("129.2126"), LocalTime.of(10, 0), 60),
				DemoStop("황리단길 식당", "음식점", StopType.MEAL, BigDecimal("35.8371"), BigDecimal("129.2098"), LocalTime.of(12, 0), 60),
				DemoStop("동궁과 월지", "관광지", StopType.SIGHTSEEING, BigDecimal("35.8348"), BigDecimal("129.2263"), LocalTime.of(14, 30), 60),
			),
			1 to listOf(
				DemoStop("불국사", "관광지", StopType.SIGHTSEEING, BigDecimal("35.7900"), BigDecimal("129.3320"), LocalTime.of(10, 0), 60),
				DemoStop("경주 한정식", "음식점", StopType.MEAL, BigDecimal("35.7955"), BigDecimal("129.3301"), LocalTime.of(12, 30), 60),
			),
			2 to listOf(
				DemoStop("첨성대", "관광지", StopType.SIGHTSEEING, BigDecimal("35.8347"), BigDecimal("129.2190"), LocalTime.of(10, 30), 60),
				DemoStop("카페 쉼", "카페", StopType.CAFE, BigDecimal("35.8360"), BigDecimal("129.2145"), LocalTime.of(13, 0), 40),
			),
		)
		private const val MAX_PARENT_COUNT = 2L
		private const val CODE_GENERATION_MAX_ATTEMPTS = 30
		private const val CODE_NUMBER_BOUND = 10_000
	}
}
