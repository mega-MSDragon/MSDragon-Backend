package com.msdragon.backend.home.controller

import com.msdragon.backend.auth.entity.AgeBand
import com.msdragon.backend.auth.entity.GenderType
import com.msdragon.backend.auth.entity.OAuthProvider
import com.msdragon.backend.auth.entity.User
import com.msdragon.backend.auth.entity.UserRole
import com.msdragon.backend.auth.repository.UserRepository
import com.msdragon.backend.auth.service.TokenService
import com.msdragon.backend.family.entity.Family
import com.msdragon.backend.family.entity.FamilyMember
import com.msdragon.backend.family.repository.FamilyMemberRepository
import com.msdragon.backend.family.repository.FamilyRepository
import com.msdragon.backend.feedback.entity.FeedbackBodyCondition
import com.msdragon.backend.feedback.entity.TripFeedback
import com.msdragon.backend.feedback.repository.TripFeedbackRepository
import com.msdragon.backend.home.tourapi.HomeTourApiAttraction
import com.msdragon.backend.home.tourapi.HomeTourApiClient
import com.msdragon.backend.home.tourapi.HomeTourApiFestival
import com.msdragon.backend.parentprofile.entity.FoodPreference
import com.msdragon.backend.parentprofile.entity.ParentProfile
import com.msdragon.backend.parentprofile.entity.ParentProfileStatus
import com.msdragon.backend.parentprofile.entity.TravelPersonalityTypeCode
import com.msdragon.backend.parentprofile.entity.TravelThemeCode
import com.msdragon.backend.parentprofile.entity.WalkingPace
import com.msdragon.backend.parentprofile.repository.ParentProfileRepository
import com.msdragon.backend.trip.dto.TripParentProfileSnapshotResponse
import com.msdragon.backend.trip.dto.TripRecommendationSnapshotResponse
import com.msdragon.backend.trip.entity.Trip
import com.msdragon.backend.trip.entity.TripDestinationCode
import com.msdragon.backend.trip.entity.TripStatus
import com.msdragon.backend.trip.entity.TripParticipant
import com.msdragon.backend.trip.repository.TripParticipantRepository
import com.msdragon.backend.trip.repository.TripRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

@SpringBootTest
@AutoConfigureMockMvc
@Import(HomeControllerTest.HomeTourApiTestConfig::class)
@Transactional
class HomeControllerTest {
	@Autowired
	private lateinit var mockMvc: MockMvc

	@Autowired
	private lateinit var tokenService: TokenService

	@Autowired
	private lateinit var objectMapper: ObjectMapper

	@Autowired
	private lateinit var userRepository: UserRepository

	@Autowired
	private lateinit var familyRepository: FamilyRepository

	@Autowired
	private lateinit var familyMemberRepository: FamilyMemberRepository

	@Autowired
	private lateinit var parentProfileRepository: ParentProfileRepository

	@Autowired
	private lateinit var tripRepository: TripRepository

	@Autowired
	private lateinit var tripFeedbackRepository: TripFeedbackRepository

	@Autowired
	private lateinit var tripParticipantRepository: TripParticipantRepository

	@Test
	fun `자녀 홈은 섹션별로 여행과 추천 콘텐츠를 반환한다`() {
		val today = LocalDate.now(SERVICE_ZONE_ID)
		val child = saveUser(UserRole.CHILD, "home-child", "혜린", GenderType.FEMALE)
		val mother = saveUser(UserRole.PARENT, "home-mother", "김영희", GenderType.FEMALE)
		val father = saveUser(UserRole.PARENT, "home-father", "김철수", GenderType.MALE)
		val family = connectFamily(child, mother, father)
		val motherProfile = saveCompletedProfile(mother, WalkingPace.SLOW, TravelThemeCode.HISTORY_CULTURE)

		val completedTrip = saveTrip(
			family = family,
			child = child,
			title = "지난 여행",
			startDate = today.minusDays(5),
			endDate = today.minusDays(5),
			snapshot = snapshot(mother, motherProfile, WalkingPace.SLOW, TravelThemeCode.HISTORY_CULTURE, today),
		)
		tripFeedbackRepository.save(
			TripFeedback(
				trip = completedTrip,
				parentUser = mother,
				overallRating = "4.5".toBigDecimal(),
				bodyCondition = FeedbackBodyCondition.COMFORTABLE,
				bestTripStopId = 1,
				bestPlaceNameSnapshot = "첨성대",
				freeComment = null,
				submittedAt = LocalDateTime.now(),
			),
		)
		saveTrip(
			family = family,
			child = child,
			title = "경주 가족 여행",
			startDate = today,
			endDate = today.plusDays(1),
			snapshot = snapshot(mother, motherProfile, WalkingPace.SLOW, TravelThemeCode.HISTORY_CULTURE, today),
		)
		saveTrip(
			family = family,
			child = child,
			title = "우리 가족 힐링 여행",
			startDate = today.plusDays(48),
			endDate = today.plusDays(50),
			snapshot = snapshot(mother, motherProfile, WalkingPace.FAST, TravelThemeCode.NATURE_SCENERY, today),
		)
		saveTrip(
			family = family,
			child = child,
			title = "보관 여행",
			startDate = today.plusDays(60),
			endDate = today.plusDays(61),
			snapshot = null,
			status = TripStatus.ARCHIVED,
		)

		mockMvc.perform(
			get("/api/v1/home/my-trips")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(child)}"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.status").value(200))
			.andExpect(jsonPath("$.data.userRole").value("child"))
			.andExpect(jsonPath("$.data.canCreateTrip").doesNotExist())
			.andExpect(jsonPath("$.data.profileGuide").doesNotExist())
			.andExpect(jsonPath("$.data.parentProfiles.length()").value(2))
			.andExpect(jsonPath("$.data.parentProfiles[0].displayName").value("김영희"))
			.andExpect(jsonPath("$.data.parentProfiles[0].relationLabel").value("엄마"))
			.andExpect(jsonPath("$.data.parentProfiles[0].profileCompleted").value(true))
			.andExpect(jsonPath("$.data.parentProfiles[1].displayName").value("김철수"))
			.andExpect(jsonPath("$.data.parentProfiles[1].relationLabel").value("아빠"))
			.andExpect(jsonPath("$.data.parentProfiles[1].profileCompleted").value(false))
			// 완료된 여행은 자녀 홈에서 내린다. 기록 탭에서 확인한다.
			.andExpect(jsonPath("$.data.trips.length()").value(2))
			.andExpect(jsonPath("$.data.trips[*].title").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("지난 여행"))))
			.andExpect(jsonPath("$.data.trips[0].title").value("경주 가족 여행"))
			.andExpect(jsonPath("$.data.trips[0].status").value("in_progress"))
			.andExpect(jsonPath("$.data.trips[0].primaryTheme").value("history_culture"))
			.andExpect(jsonPath("$.data.trips[0].intensity").value("low"))
			.andExpect(jsonPath("$.data.trips[0].ratings").isEmpty)
			.andExpect(jsonPath("$.data.trips[1].title").value("우리 가족 힐링 여행"))
			.andExpect(jsonPath("$.data.trips[1].dDay").value(48))
			.andExpect(jsonPath("$.data.trips[1].primaryTheme").value("nature_scenery"))
			.andExpect(jsonPath("$.data.trips[1].intensity").value("high"))

		mockMvc.perform(
			get("/api/v1/home/monthly-recommendations")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(child)}"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.data.recommendationMonth").value(today.monthValue))
			.andExpect(jsonPath("$.data.recommendedCities.length()").value(5))
			.andExpect(jsonPath("$.data.recommendedCities[0].imageUrl").isString)

		mockMvc.perform(
			get("/api/v1/home/festivals")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(child)}"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.data.festivals.length()").value(1))
			.andExpect(jsonPath("$.data.festivals[0].title").value("안동 선유줄불놀이"))
			.andExpect(jsonPath("$.data.festivals[0].tags[0]").value("안동"))
			.andExpect(jsonPath("$.data.festivals[0].tags[1]").value("축제"))
	}

	@Test
	fun `부모 나의 여행은 본인 프로필 완성 여부를 반환한다`() {
		val parent = saveUser(UserRole.PARENT, "home-parent", "김영희", GenderType.FEMALE)

		mockMvc.perform(
			get("/api/v1/home/my-trips")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(parent)}"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.data.familyId").doesNotExist())
			.andExpect(jsonPath("$.data.userRole").value("parent"))
			.andExpect(jsonPath("$.data.canCreateTrip").doesNotExist())
			.andExpect(jsonPath("$.data.parentProfiles.length()").value(1))
			.andExpect(jsonPath("$.data.parentProfiles[0].userId").value(parent.id))
			.andExpect(jsonPath("$.data.parentProfiles[0].relationLabel").value("엄마"))
			.andExpect(jsonPath("$.data.parentProfiles[0].profileCompleted").value(false))
			.andExpect(jsonPath("$.data.trips").isEmpty)
	}

	private fun saveUser(
		role: UserRole,
		subject: String,
		displayName: String,
		gender: GenderType,
	): User =
		userRepository.save(
			User(
				role = role,
				oauthProvider = OAuthProvider.KAKAO,
				oauthSubject = subject,
				displayName = displayName,
				ageBand = AgeBand.AGE_50S,
				gender = gender,
				signupCompletedAt = LocalDateTime.now(),
			),
		)

	private fun connectFamily(child: User, vararg parents: User): Family {
		val family = familyRepository.save(Family(ownerUser = child))
		familyMemberRepository.save(FamilyMember(family, child, UserRole.CHILD))
		parents.forEach { parent ->
			familyMemberRepository.save(FamilyMember(family, parent, UserRole.PARENT))
		}
		return family
	}

	private fun saveCompletedProfile(
		parent: User,
		walkingPace: WalkingPace,
		theme: TravelThemeCode,
	): ParentProfile =
		parentProfileRepository.save(
			ParentProfile(
				user = parent,
				status = ParentProfileStatus.COMPLETED,
				currentStep = 3,
				walkingPace = walkingPace,
				foodPreference = FoodPreference.FAMILIAR,
				needsMobilityAssistance = false,
				travelThemes = mutableSetOf(theme.value),
				personalityType = TravelPersonalityTypeCode.HERITAGE_WALKER,
				completionPercent = 100,
				completedAt = LocalDateTime.now(),
			),
		)

	@Test
	fun `완료된 여행은 평가하지 않은 부모에게만 홈에 보인다`() {
		val today = LocalDate.now(ZoneId.of("Asia/Seoul"))
		val child = saveUser(UserRole.CHILD, "child-visible", "혜린", GenderType.FEMALE)
		val mother = saveUser(UserRole.PARENT, "mother-visible", "김영희", GenderType.FEMALE)
		val father = saveUser(UserRole.PARENT, "father-visible", "김철수", GenderType.MALE)
		val family = familyRepository.save(Family(ownerUser = child))
		familyMemberRepository.saveAll(
			listOf(
				FamilyMember(family = family, user = child, memberRole = UserRole.CHILD),
				FamilyMember(family = family, user = mother, memberRole = UserRole.PARENT),
				FamilyMember(family = family, user = father, memberRole = UserRole.PARENT),
			),
		)
		val completedTrip = saveTrip(
			family = family,
			child = child,
			title = "지난 여행",
			startDate = today.minusDays(5),
			endDate = today.minusDays(5),
			snapshot = null,
			status = TripStatus.COMPLETED,
		)
		tripParticipantRepository.saveAll(
			listOf(
				TripParticipant(trip = completedTrip, user = mother),
				TripParticipant(trip = completedTrip, user = father),
			),
		)
		// 엄마만 평가를 제출했다.
		tripFeedbackRepository.save(
			TripFeedback(
				trip = completedTrip,
				parentUser = mother,
				overallRating = "4.5".toBigDecimal(),
				bodyCondition = FeedbackBodyCondition.COMFORTABLE,
				bestTripStopId = 1,
				bestPlaceNameSnapshot = "첨성대",
				freeComment = null,
				submittedAt = LocalDateTime.now(),
			),
		)

		// 자녀: 완료 여행을 홈에서 내린다.
		mockMvc.perform(
			get("/api/v1/home/my-trips")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(child)}"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.data.trips").isEmpty)

		// 평가를 제출한 엄마: 홈에서 사라진다.
		mockMvc.perform(
			get("/api/v1/home/my-trips")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(mother)}"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.data.trips").isEmpty)

		// 아직 평가하지 않은 아빠: 평가를 유도하기 위해 남는다.
		mockMvc.perform(
			get("/api/v1/home/my-trips")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(father)}"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.data.trips.length()").value(1))
			.andExpect(jsonPath("$.data.trips[0].title").value("지난 여행"))
			.andExpect(jsonPath("$.data.trips[0].status").value("completed"))
			.andExpect(jsonPath("$.data.trips[0].ratings.length()").value(1))
			.andExpect(jsonPath("$.data.trips[0].ratings[0].displayName").value("김영희"))
			.andExpect(jsonPath("$.data.trips[0].ratings[0].overallRating").value(4.5))
	}

	@Test
	fun `참여하지 않은 완료 여행은 부모 홈에 남지 않는다`() {
		val today = LocalDate.now(ZoneId.of("Asia/Seoul"))
		val child = saveUser(UserRole.CHILD, "child-nonparticipant", "혜린", GenderType.FEMALE)
		val mother = saveUser(UserRole.PARENT, "mother-nonparticipant", "김영희", GenderType.FEMALE)
		val family = familyRepository.save(Family(ownerUser = child))
		familyMemberRepository.saveAll(
			listOf(
				FamilyMember(family = family, user = child, memberRole = UserRole.CHILD),
				FamilyMember(family = family, user = mother, memberRole = UserRole.PARENT),
			),
		)
		// 엄마는 참여자가 아니라 평가할 수 없다. 남기면 사라지지 않는 카드가 된다.
		saveTrip(
			family = family,
			child = child,
			title = "엄마가 빠진 여행",
			startDate = today.minusDays(5),
			endDate = today.minusDays(5),
			snapshot = null,
			status = TripStatus.COMPLETED,
		)

		mockMvc.perform(
			get("/api/v1/home/my-trips")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(mother)}"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.data.trips").isEmpty)
	}

	private fun saveTrip(
		family: Family,
		child: User,
		title: String,
		startDate: LocalDate,
		endDate: LocalDate,
		snapshot: TripRecommendationSnapshotResponse?,
		status: TripStatus = TripStatus.PLANNING,
	): Trip =
		tripRepository.save(
			Trip(
				family = family,
				createdByUser = child,
				destinationCode = TripDestinationCode.GYEONGJU,
				title = title,
				startDate = startDate,
				endDate = endDate,
				status = status,
				recommendationSnapshot = snapshot?.let(objectMapper::writeValueAsString),
			),
		)

	private fun snapshot(
		parent: User,
		profile: ParentProfile,
		walkingPace: WalkingPace,
		theme: TravelThemeCode,
		date: LocalDate,
	): TripRecommendationSnapshotResponse =
		TripRecommendationSnapshotResponse(
			policyVersion = "parent-travel-mbti-v1",
			capturedAt = LocalDateTime.now(),
			destinationCode = TripDestinationCode.GYEONGJU,
			startDate = date,
			endDate = date.plusDays(1),
			parents = listOf(
				TripParentProfileSnapshotResponse(
					parentUserId = requireNotNull(parent.id),
					parentProfileId = requireNotNull(profile.id),
					displayName = parent.displayName,
					relationLabel = "엄마",
					walkingPace = walkingPace,
					needsMobilityAssistance = false,
					travelThemes = listOf(theme),
					foodPreference = FoodPreference.FAMILIAR,
					personalityType = TravelPersonalityTypeCode.HERITAGE_WALKER,
					profileCompletedAt = profile.completedAt,
				),
			),
		)

	companion object {
		private val SERVICE_ZONE_ID: ZoneId = ZoneId.of("Asia/Seoul")
	}

	@TestConfiguration
	class HomeTourApiTestConfig {
		@Bean
		@Primary
		fun fakeHomeTourApiClient(): HomeTourApiClient = object : HomeTourApiClient {
			override fun findDestinationImage(destination: TripDestinationCode): String =
				"https://example.com/${destination.value}.jpg"

			override fun findAttractions(
				destinations: List<TripDestinationCode>,
				limitPerDestination: Int,
			): List<HomeTourApiAttraction> =
				destinations.take(limitPerDestination).map { destination ->
					HomeTourApiAttraction(
						contentId = "attraction-${destination.value}",
						title = "${destination.displayName} 명소",
						imageUrl = "https://example.com/${destination.value}-attraction.jpg",
						address = "${destination.displayName}시 어딘가",
						regionName = destination.displayName,
						destination = destination,
					)
				}

			override fun findFestivals(
				startDate: LocalDate,
				endDate: LocalDate,
				limit: Int,
			): List<HomeTourApiFestival> =
				listOf(
					HomeTourApiFestival(
						contentId = "250119",
						title = "안동 선유줄불놀이",
						summary = "낙동강 위로 불꽃이 이어지는 전통 축제입니다.",
						imageUrl = "https://example.com/festival.jpg",
						address = "경상북도 안동시 풍천면",
						regionName = "안동",
						eventStartDate = startDate,
						eventEndDate = startDate.plusDays(2),
					),
				)
		}
	}
}
