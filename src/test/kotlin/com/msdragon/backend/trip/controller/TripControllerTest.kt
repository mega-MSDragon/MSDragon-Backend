package com.msdragon.backend.trip.controller

import com.jayway.jsonpath.JsonPath
import com.msdragon.backend.auth.entity.AgeBand
import com.msdragon.backend.auth.entity.GenderType
import com.msdragon.backend.auth.entity.OAuthProvider
import com.msdragon.backend.auth.entity.User
import com.msdragon.backend.auth.entity.UserRole
import com.msdragon.backend.auth.repository.UserRefreshTokenRepository
import com.msdragon.backend.auth.repository.UserRepository
import com.msdragon.backend.auth.service.TokenService
import com.msdragon.backend.family.entity.Family
import com.msdragon.backend.family.entity.FamilyMember
import com.msdragon.backend.family.repository.FamilyCodeRepository
import com.msdragon.backend.family.repository.FamilyCodeUsageRepository
import com.msdragon.backend.family.repository.FamilyMemberRepository
import com.msdragon.backend.family.repository.FamilyRepository
import com.msdragon.backend.parentprofile.entity.FoodPreference
import com.msdragon.backend.parentprofile.entity.ParentProfile
import com.msdragon.backend.parentprofile.entity.ParentProfileStatus
import com.msdragon.backend.parentprofile.entity.TravelPersonalityTypeCode
import com.msdragon.backend.parentprofile.entity.TravelThemeCode
import com.msdragon.backend.parentprofile.entity.WalkingPace
import com.msdragon.backend.parentprofile.repository.ParentProfileRepository
import com.msdragon.backend.trip.repository.TripDayRepository
import com.msdragon.backend.trip.repository.TripParticipantRepository
import com.msdragon.backend.trip.repository.TripRepository
import com.msdragon.backend.trip.repository.TripStopRepository
import com.msdragon.backend.trip.tourapi.TourApiAccessibility
import com.msdragon.backend.trip.tourapi.TourApiClient
import com.msdragon.backend.trip.tourapi.TourApiPlaceDetail
import com.msdragon.backend.trip.tourapi.TourApiPlaceSearch
import com.msdragon.backend.trip.tourapi.TourApiPlaceSummary
import org.springframework.boot.test.context.TestConfiguration
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

@SpringBootTest
@AutoConfigureMockMvc
@Import(TripControllerTest.TourApiTestConfig::class)
class TripControllerTest {
	@Autowired
	private lateinit var mockMvc: MockMvc

	@Autowired
	private lateinit var tokenService: TokenService

	@Autowired
	private lateinit var userRepository: UserRepository

	@Autowired
	private lateinit var userRefreshTokenRepository: UserRefreshTokenRepository

	@Autowired
	private lateinit var familyRepository: FamilyRepository

	@Autowired
	private lateinit var familyMemberRepository: FamilyMemberRepository

	@Autowired
	private lateinit var familyCodeRepository: FamilyCodeRepository

	@Autowired
	private lateinit var familyCodeUsageRepository: FamilyCodeUsageRepository

	@Autowired
	private lateinit var parentProfileRepository: ParentProfileRepository

	@Autowired
	private lateinit var tripDayRepository: TripDayRepository

	@Autowired
	private lateinit var tripParticipantRepository: TripParticipantRepository

	@Autowired
	private lateinit var tripStopRepository: TripStopRepository

	@Autowired
	private lateinit var tripRepository: TripRepository

	@Autowired
	private lateinit var fakeTourApiClient: FakeTourApiClient

	@BeforeEach
	fun setUp() {
		fakeTourApiClient.reset()
		tripStopRepository.deleteAll()
		tripDayRepository.deleteAll()
		tripParticipantRepository.deleteAll()
		tripRepository.deleteAll()
		parentProfileRepository.deleteAll()
		familyCodeUsageRepository.deleteAll()
		familyMemberRepository.deleteAll()
		familyCodeRepository.deleteAll()
		familyRepository.deleteAll()
		userRefreshTokenRepository.deleteAll()
		userRepository.deleteAll()
	}

	@Test
	fun `여행 대상 부모 후보를 조회한다`() {
		val child = saveUser(UserRole.CHILD, "child-1", "혜린")
		val mother = saveUser(UserRole.PARENT, "parent-1", "엄마", GenderType.FEMALE)
		val father = saveUser(UserRole.PARENT, "parent-2", "아빠", GenderType.MALE)
		connectFamily(child, mother, father)
		saveCompletedParentProfile(mother)

		mockMvc.perform(
			get("/api/v1/trips/parent-candidates")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(child)}"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.data.familyId").isNumber)
			.andExpect(jsonPath("$.data.parents.length()").value(2))
			.andExpect(jsonPath("$.data.parents[0].relationLabel").value("엄마"))
			.andExpect(jsonPath("$.data.parents[0].profileCompleted").value(true))
			.andExpect(jsonPath("$.data.parents[1].relationLabel").value("아빠"))
			.andExpect(jsonPath("$.data.parents[1].profileCompleted").value(false))
	}

	@Test
	fun `여행 도시 목록을 조회한다`() {
		val child = saveUser(UserRole.CHILD, "child-1", "혜린")

		mockMvc.perform(
			get("/api/v1/trips/destinations")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(child)}"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.data.length()").value(12))
			.andExpect(jsonPath("$.data[2].code").value("gyeongju"))
			.andExpect(jsonPath("$.data[2].displayName").value("경주"))
	}

	@Test
	fun `자녀가 여행을 생성한다`() {
		val child = saveUser(UserRole.CHILD, "child-1", "혜린")
		val mother = saveUser(UserRole.PARENT, "parent-1", "엄마", GenderType.FEMALE)
		connectFamily(child, mother)
		saveCompletedParentProfile(mother)
		val startDate = futureDate(10)
		val endDate = startDate.plusDays(1)

		mockMvc.perform(
			post("/api/v1/trips")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(child)}")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "parentUserIds": [${requireNotNull(mother.id)}],
					  "destinationCode": "gyeongju",
					  "startDate": "$startDate",
					  "endDate": "$endDate"
					}
					""".trimIndent(),
				),
		)
			.andExpect(status().isCreated)
			.andExpect(jsonPath("$.data.title").value("경주 여행"))
			.andExpect(jsonPath("$.data.destination.code").value("gyeongju"))
			.andExpect(jsonPath("$.data.status").value("planning"))
			.andExpect(jsonPath("$.data.participants.length()").value(2))
			.andExpect(jsonPath("$.data.participants[1].relationLabel").value("엄마"))
			.andExpect(jsonPath("$.data.recommendationSnapshot.policyVersion").value("parent-travel-mbti-v1"))
			.andExpect(jsonPath("$.data.recommendationSnapshot.destinationCode").value("gyeongju"))
			.andExpect(jsonPath("$.data.recommendationSnapshot.parents.length()").value(1))
			.andExpect(jsonPath("$.data.recommendationSnapshot.parents[0].parentUserId").value(requireNotNull(mother.id).toInt()))
			.andExpect(jsonPath("$.data.recommendationSnapshot.parents[0].walkingPace").value("slow"))
			.andExpect(jsonPath("$.data.recommendationSnapshot.parents[0].needsMobilityAssistance").value(false))
			.andExpect(jsonPath("$.data.recommendationSnapshot.parents[0].travelThemes[0]").value("nature_scenery"))
			.andExpect(jsonPath("$.data.recommendationSnapshot.parents[0].foodPreference").value("familiar"))
			.andExpect(jsonPath("$.data.recommendationSnapshot.parents[0].personalityType").value("healing_traveler"))
			.andExpect(jsonPath("$.data.days.length()").value(2))
				.andExpect(jsonPath("$.data.days[0].dayNumber").value(1))
				.andExpect(jsonPath("$.data.days[1].dayNumber").value(2))
	}

	@Test
	fun `여행 기간은 3일 이상도 생성한다`() {
		val child = saveUser(UserRole.CHILD, "child-1", "혜린")
		val mother = saveUser(UserRole.PARENT, "parent-1", "엄마", GenderType.FEMALE)
		connectFamily(child, mother)
		saveCompletedParentProfile(mother)
		val startDate = futureDate(20)
		val endDate = startDate.plusDays(3)

		mockMvc.perform(
			post("/api/v1/trips")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(child)}")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "parentUserIds": [${requireNotNull(mother.id)}],
					  "destinationCode": "busan",
					  "startDate": "$startDate",
					  "endDate": "$endDate"
					}
					""".trimIndent(),
				),
		)
			.andExpect(status().isCreated)
			.andExpect(jsonPath("$.data.days.length()").value(4))
			.andExpect(jsonPath("$.data.days[3].dayNumber").value(4))
			.andExpect(jsonPath("$.data.days[3].travelDate").value(endDate.toString()))
	}

	@Test
	fun `부모 프로필이 완료되지 않으면 여행 생성을 거절한다`() {
		val child = saveUser(UserRole.CHILD, "child-1", "혜린")
		val mother = saveUser(UserRole.PARENT, "parent-1", "엄마", GenderType.FEMALE)
		connectFamily(child, mother)
		val startDate = futureDate(10)

		mockMvc.perform(
			post("/api/v1/trips")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(child)}")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "parentUserIds": [${requireNotNull(mother.id)}],
					  "destinationCode": "gyeongju",
					  "startDate": "$startDate",
					  "endDate": "$startDate"
					}
					""".trimIndent(),
				),
		)
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.message").value("부모님 상세 프로필 작성이 필요합니다."))
	}

	@Test
	fun `같은 가족의 겹치는 날짜 여행 생성을 거절한다`() {
		val child = saveUser(UserRole.CHILD, "child-1", "혜린")
		val mother = saveUser(UserRole.PARENT, "parent-1", "엄마", GenderType.FEMALE)
		connectFamily(child, mother)
		saveCompletedParentProfile(mother)
		val startDate = futureDate(10)
		createTrip(child, mother, startDate, startDate.plusDays(1))

		mockMvc.perform(
			post("/api/v1/trips")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(child)}")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "parentUserIds": [${requireNotNull(mother.id)}],
					  "destinationCode": "busan",
					  "startDate": "${startDate.plusDays(1)}",
					  "endDate": "${startDate.plusDays(1)}"
					}
					""".trimIndent(),
				),
		)
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.message").value("선택한 날짜에 이미 등록된 여행이 있습니다."))
	}

	@Test
	fun `가족 구성원은 여행 상세와 목록을 조회한다`() {
		val child = saveUser(UserRole.CHILD, "child-1", "혜린")
		val mother = saveUser(UserRole.PARENT, "parent-1", "엄마", GenderType.FEMALE)
		connectFamily(child, mother)
		saveCompletedParentProfile(mother)
		val tripId = createTrip(child, mother, futureDate(10), futureDate(10))

		mockMvc.perform(
			get("/api/v1/trips/$tripId")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(mother)}"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.data.id").value(tripId))
			.andExpect(jsonPath("$.data.participants.length()").value(2))

		mockMvc.perform(
			get("/api/v1/trips")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(child)}"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.data.trips.length()").value(1))
			.andExpect(jsonPath("$.data.trips[0].id").value(tripId))
	}

	@Test
	fun `가족 구성원은 여행 코스를 저장하고 조회한다`() {
		val child = saveUser(UserRole.CHILD, "child-1", "혜린")
		val mother = saveUser(UserRole.PARENT, "parent-1", "엄마", GenderType.FEMALE)
		connectFamily(child, mother)
		saveCompletedParentProfile(mother)
		val startDate = futureDate(10)
		val tripId = createTrip(child, mother, startDate, startDate.plusDays(1))

		mockMvc.perform(
			put("/api/v1/trips/$tripId/course")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(child)}")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "days": [
					    {
					      "dayNumber": 1,
					      "stops": [
					        {
					          "stopType": "sightseeing",
					          "sourceProvider": "tour_api",
					          "externalPlaceId": "988449",
					          "contentTypeId": "12",
					          "name": "오도리 공원",
					          "category": "관광지",
					          "address": "대구광역시 동구 효목동",
					          "latitude": 35.8821234,
					          "longitude": 128.6212345,
					          "phone": "053-123-4567",
					          "homepageUrl": "https://example.com",
					          "imageUrl": "https://example.com/park.jpg",
					          "overview": "짧은 산책을 즐기기 좋은 공원입니다.",
					          "arrivalTime": "10:30",
					          "dwellMinutes": 60,
					          "note": "부모님과 사진 찍기",
					          "recommendationReason": "짧은 산책과 휴식에 적합합니다.",
					          "recommendationTags": ["nature_scenery", "low_slope"],
					          "sourcePayload": {
					            "contentid": "988449",
					            "route": "출입구까지 경사로가 설치되어 있음"
					          },
					          "isManualAdded": false
					        },
					        {
					          "stopType": "meal",
					          "sourceProvider": "kakao_map",
					          "externalPlaceId": "restaurant-1",
					          "name": "경주 한식당",
					          "category": "한식",
					          "isManualAdded": true
					        }
					      ]
					    }
					  ]
					}
					""".trimIndent(),
				),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.data.tripId").value(tripId))
			.andExpect(jsonPath("$.data.days.length()").value(2))
			.andExpect(jsonPath("$.data.days[0].stops.length()").value(2))
			.andExpect(jsonPath("$.data.days[0].stops[0].sortOrder").value(1))
			.andExpect(jsonPath("$.data.days[0].stops[0].name").value("오도리 공원"))
			.andExpect(jsonPath("$.data.days[0].stops[0].stopType").value("sightseeing"))
			.andExpect(jsonPath("$.data.days[0].stops[0].sourceProvider").value("tour_api"))
			.andExpect(jsonPath("$.data.days[0].stops[0].recommendationTags[0]").value("nature_scenery"))
			.andExpect(jsonPath("$.data.days[0].stops[0].sourcePayload.route").value("출입구까지 경사로가 설치되어 있음"))
			.andExpect(jsonPath("$.data.days[0].stops[1].sortOrder").value(2))
			.andExpect(jsonPath("$.data.days[0].stops[1].isManualAdded").value(true))
			.andExpect(jsonPath("$.data.days[1].stops.length()").value(0))

		mockMvc.perform(
			get("/api/v1/trips/$tripId/course")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(mother)}"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.data.days[0].stops.length()").value(2))
			.andExpect(jsonPath("$.data.days[0].stops[0].externalPlaceId").value("988449"))
			.andExpect(jsonPath("$.data.days[0].stops[0].arrivalTime").value("10:30:00"))
	}

	@Test
	fun `존재하지 않는 여행 일자에 코스를 저장할 수 없다`() {
		val child = saveUser(UserRole.CHILD, "child-1", "혜린")
		val mother = saveUser(UserRole.PARENT, "parent-1", "엄마", GenderType.FEMALE)
		connectFamily(child, mother)
		saveCompletedParentProfile(mother)
		val tripId = createTrip(child, mother, futureDate(10), futureDate(10))

		mockMvc.perform(
			put("/api/v1/trips/$tripId/course")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(child)}")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "days": [
					    {
					      "dayNumber": 2,
					      "stops": [
					        {
					          "name": "없는 일자 방문지"
					        }
					      ]
					    }
					  ]
					}
					""".trimIndent(),
				),
		)
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.message").value("여행에 존재하지 않는 일자입니다: 2일차"))
	}

	@Test
	fun `여행 추천 코스를 생성하면 부모 프로필 기준으로 일자별 방문지를 저장한다`() {
		val child = saveUser(UserRole.CHILD, "child-1", "혜린")
		val mother = saveUser(UserRole.PARENT, "parent-1", "엄마", GenderType.FEMALE)
		connectFamily(child, mother)
		saveCompletedParentProfile(mother)
		val startDate = futureDate(10)
		val tripId = createTrip(child, mother, startDate, startDate.plusDays(1))
		fakeTourApiClient.placesByContentType["12"] = listOf(
			tourPlace("nature-1", "12", "경주 산책 공원", "NA"),
			tourPlace("nature-2", "12", "보문 호수길", "NA"),
			tourPlace("nature-3", "12", "불국사 숲길", "NA"),
			tourPlace("nature-4", "12", "월정교 산책길", "NA"),
		)
		fakeTourApiClient.placesByContentType["14"] = listOf(
			tourPlace("culture-1", "14", "경주 문화관", "VE"),
			tourPlace("culture-2", "14", "신라 전시관", "VE"),
		)
		fakeTourApiClient.placesByContentType["39"] = listOf(
			tourPlace("food-1", "39", "경주 한식당", "FD"),
			tourPlace("food-2", "39", "황리단길 밥집", "FD"),
		)

		mockMvc.perform(
			post("/api/v1/trips/$tripId/course/recommendation")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(child)}"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.data.tripId").value(tripId))
			.andExpect(jsonPath("$.data.status").value("ready"))
			.andExpect(jsonPath("$.data.days.length()").value(2))
			.andExpect(jsonPath("$.data.days[0].stops.length()").value(3))
			.andExpect(jsonPath("$.data.days[1].stops.length()").value(3))
			.andExpect(jsonPath("$.data.days[0].stops[0].sourceProvider").value("tour_api"))
			.andExpect(jsonPath("$.data.days[0].stops[0].isManualAdded").value(false))
			.andExpect(jsonPath("$.data.days[0].stops[0].sourcePayload.recommendation.policyVersion").value("tour-api-course-recommendation-v1"))
			.andExpect(jsonPath("$.data.days[0].stops[1].stopType").value("meal"))
			.andExpect(jsonPath("$.data.days[0].stops[1].recommendationTags[2]").value("fd"))
			.andExpect(jsonPath("$.data.days[1].stops[1].stopType").value("meal"))
	}

	@Test
	fun `부모는 여행을 생성할 수 없다`() {
		val parent = saveUser(UserRole.PARENT, "parent-1", "엄마", GenderType.FEMALE)

		mockMvc.perform(
			post("/api/v1/trips")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(parent)}")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "parentUserIds": [${requireNotNull(parent.id)}],
					  "destinationCode": "gyeongju",
					  "startDate": "${futureDate(10)}",
					  "endDate": "${futureDate(10)}"
					}
					""".trimIndent(),
				),
		)
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.message").value("자녀 사용자만 여행을 만들 수 있습니다."))
	}

	private fun createTrip(child: User, parent: User, startDate: LocalDate, endDate: LocalDate): Int {
		val response = mockMvc.perform(
			post("/api/v1/trips")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(child)}")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "parentUserIds": [${requireNotNull(parent.id)}],
					  "destinationCode": "gyeongju",
					  "startDate": "$startDate",
					  "endDate": "$endDate"
					}
					""".trimIndent(),
				),
		)
			.andExpect(status().isCreated)
			.andReturn()
			.response
			.contentAsString

		return JsonPath.read(response, "$.data.id")
	}

	private fun saveCompletedParentProfile(parent: User) {
		parentProfileRepository.save(
			ParentProfile(
				user = parent,
				status = ParentProfileStatus.COMPLETED,
				currentStep = 3,
				walkingPace = WalkingPace.SLOW,
				foodPreference = FoodPreference.FAMILIAR,
				needsMobilityAssistance = false,
				travelThemes = mutableSetOf(TravelThemeCode.NATURE_SCENERY.value),
				personalityType = TravelPersonalityTypeCode.HEALING_TRAVELER,
				completionPercent = 100,
				completedAt = LocalDateTime.now(),
			),
		)
	}

	private fun connectFamily(child: User, vararg parents: User) {
		val family = familyRepository.save(Family(ownerUser = child))
		familyMemberRepository.save(
			FamilyMember(
				family = family,
				user = child,
				memberRole = UserRole.CHILD,
			),
		)
		parents.forEach { parent ->
			familyMemberRepository.save(
				FamilyMember(
					family = family,
					user = parent,
					memberRole = UserRole.PARENT,
				),
			)
		}
	}

	private fun saveUser(
		role: UserRole,
		subject: String,
		displayName: String,
		gender: GenderType = GenderType.UNDISCLOSED,
	): User =
		userRepository.save(
			User(
				role = role,
				oauthProvider = OAuthProvider.KAKAO,
				oauthSubject = subject,
				displayName = displayName,
				ageBand = if (role == UserRole.CHILD) AgeBand.AGE_20S else AgeBand.AGE_60S,
				gender = gender,
				signupCompletedAt = LocalDateTime.now(),
			),
		)

	private fun futureDate(daysFromToday: Long): LocalDate =
		LocalDate.now(ZoneId.of("Asia/Seoul")).plusDays(daysFromToday)

	private fun tourPlace(
		contentId: String,
		contentTypeId: String,
		title: String,
		lclsSystm1: String,
	): TourApiPlaceSummary =
		TourApiPlaceSummary(
			contentId = contentId,
			contentTypeId = contentTypeId,
			title = title,
			address = "경상북도 경주시",
			latitude = "35.8562".toBigDecimal(),
			longitude = "129.2247".toBigDecimal(),
			tel = "054-000-0000",
			firstImage = "https://example.com/$contentId.jpg",
			firstImageThumbnail = "https://example.com/$contentId-thumb.jpg",
			lclsSystm1 = lclsSystm1,
			lclsSystm2 = null,
			lclsSystm3 = null,
			raw = mapOf(
				"contentid" to contentId,
				"contenttypeid" to contentTypeId,
				"title" to title,
				"lclsSystm1" to lclsSystm1,
			),
		)

	@TestConfiguration
	class TourApiTestConfig {
		@Bean
		@Primary
		fun fakeTourApiClient(): FakeTourApiClient = FakeTourApiClient()
	}

	class FakeTourApiClient : TourApiClient {
		val placesByContentType: MutableMap<String, List<TourApiPlaceSummary>> = mutableMapOf()
		val detailsByContentId: MutableMap<String, TourApiPlaceDetail> = mutableMapOf()
		val accessibilityByContentId: MutableMap<String, TourApiAccessibility> = mutableMapOf()

		override fun findPlaces(search: TourApiPlaceSearch): List<TourApiPlaceSummary> =
			placesByContentType[search.contentTypeId].orEmpty()

		override fun getPlaceDetail(contentId: String): TourApiPlaceDetail? =
			detailsByContentId[contentId] ?: TourApiPlaceDetail(
				homepage = "https://example.com/$contentId",
				overview = "$contentId 소개",
				raw = mapOf("contentid" to contentId),
			)

		override fun getAccessibility(contentId: String): TourApiAccessibility? =
			accessibilityByContentId[contentId]

		fun reset() {
			placesByContentType.clear()
			detailsByContentId.clear()
			accessibilityByContentId.clear()
		}
	}
}
