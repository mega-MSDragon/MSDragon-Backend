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

	@Test
	fun `자녀 홈은 진행 예정 여행과 부모 프로필 안내를 반환한다`() {
		val today = LocalDate.now(SERVICE_ZONE_ID)
		val child = saveUser(UserRole.CHILD, "home-child", "혜린", GenderType.FEMALE)
		val mother = saveUser(UserRole.PARENT, "home-mother", "김영희", GenderType.FEMALE)
		val father = saveUser(UserRole.PARENT, "home-father", "김철수", GenderType.MALE)
		val family = connectFamily(child, mother, father)
		val motherProfile = saveCompletedProfile(mother, WalkingPace.SLOW, TravelThemeCode.HISTORY_CULTURE)

		saveTrip(
			family = family,
			child = child,
			title = "지난 여행",
			startDate = today.minusDays(5),
			endDate = today.minusDays(4),
			snapshot = snapshot(mother, motherProfile, WalkingPace.SLOW, TravelThemeCode.HISTORY_CULTURE, today),
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
			get("/api/v1/home")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(child)}"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.status").value(200))
			.andExpect(jsonPath("$.data.userRole").value("child"))
			.andExpect(jsonPath("$.data.canCreateTrip").value(true))
			.andExpect(jsonPath("$.data.profileGuide.type").value("request_parent_profile"))
			.andExpect(jsonPath("$.data.profileGuide.targets.length()").value(1))
			.andExpect(jsonPath("$.data.profileGuide.targets[0].displayName").value("김철수"))
			.andExpect(jsonPath("$.data.profileGuide.targets[0].relationLabel").value("아빠"))
			.andExpect(jsonPath("$.data.trips.length()").value(2))
			.andExpect(jsonPath("$.data.trips[0].title").value("경주 가족 여행"))
			.andExpect(jsonPath("$.data.trips[0].status").value("in_progress"))
			.andExpect(jsonPath("$.data.trips[0].primaryTheme").value("history_culture"))
			.andExpect(jsonPath("$.data.trips[0].intensity").value("low"))
			.andExpect(jsonPath("$.data.trips[1].title").value("우리 가족 힐링 여행"))
			.andExpect(jsonPath("$.data.trips[1].dDay").value(48))
			.andExpect(jsonPath("$.data.trips[1].primaryTheme").value("nature_scenery"))
			.andExpect(jsonPath("$.data.trips[1].intensity").value("high"))
			.andExpect(jsonPath("$.data.recommendationMonth").value(today.monthValue))
			.andExpect(jsonPath("$.data.recommendedCities.length()").value(3))
			.andExpect(jsonPath("$.data.recommendedCities[0].imageUrl").isString)
			.andExpect(jsonPath("$.data.festivals.length()").value(1))
			.andExpect(jsonPath("$.data.festivals[0].title").value("안동 선유줄불놀이"))
			.andExpect(jsonPath("$.data.festivals[0].tags[0]").value("안동"))
			.andExpect(jsonPath("$.data.festivals[0].tags[1]").value("축제"))
	}

	@Test
	fun `부모 홈은 여행 생성 권한 없이 본인 프로필 작성 안내를 반환한다`() {
		val parent = saveUser(UserRole.PARENT, "home-parent", "김영희", GenderType.FEMALE)

		mockMvc.perform(
			get("/api/v1/home")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(parent)}"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.data.familyId").doesNotExist())
			.andExpect(jsonPath("$.data.userRole").value("parent"))
			.andExpect(jsonPath("$.data.canCreateTrip").value(false))
			.andExpect(jsonPath("$.data.profileGuide.type").value("complete_my_profile"))
			.andExpect(jsonPath("$.data.profileGuide.targets[0].userId").value(parent.id))
			.andExpect(jsonPath("$.data.profileGuide.targets[0].relationLabel").value("엄마"))
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
