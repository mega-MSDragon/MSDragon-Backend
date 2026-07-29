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
import com.msdragon.backend.pledge.repository.PledgeItemRepository
import com.msdragon.backend.pledge.repository.PledgeSignatureRepository
import com.msdragon.backend.pledge.repository.TripPledgeRepository
import com.msdragon.backend.supportfacility.entity.SupportFacility
import com.msdragon.backend.supportfacility.entity.SupportFacilityType
import com.msdragon.backend.supportfacility.repository.SupportFacilityRepository
import com.msdragon.backend.trip.entity.TripDay
import com.msdragon.backend.trip.entity.ExternalApiProvider
import com.msdragon.backend.trip.entity.TripStatus
import com.msdragon.backend.trip.repository.TripDayRepository
import com.msdragon.backend.trip.repository.TripParticipantRepository
import com.msdragon.backend.trip.repository.TripRepository
import com.msdragon.backend.trip.repository.TripStopRepository
import com.msdragon.backend.trip.tourapi.TourApiAccessibility
import com.msdragon.backend.trip.tourapi.TourApiClient
import com.msdragon.backend.trip.tourapi.TourApiKeywordSearch
import com.msdragon.backend.trip.tourapi.TourApiPlaceDetail
import com.msdragon.backend.trip.tourapi.TourApiPlaceSearch
import com.msdragon.backend.trip.tourapi.TourApiPlaceSummary
import com.msdragon.backend.trip.tmap.TmapRouteClient
import com.msdragon.backend.trip.tmap.TmapRouteCoordinate
import com.msdragon.backend.trip.tmap.TmapRouteOptimizationRequest
import com.msdragon.backend.trip.tmap.TmapRouteOptimizationResult
import org.springframework.boot.test.context.TestConfiguration
import org.junit.jupiter.api.AfterEach
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
import java.time.LocalTime
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
	private lateinit var supportFacilityRepository: SupportFacilityRepository

	@Autowired
	private lateinit var tripDayRepository: TripDayRepository

	@Autowired
	private lateinit var tripParticipantRepository: TripParticipantRepository

	@Autowired
	private lateinit var tripStopRepository: TripStopRepository

	@Autowired
	private lateinit var tripRepository: TripRepository

	@Autowired
	private lateinit var pledgeSignatureRepository: PledgeSignatureRepository

	@Autowired
	private lateinit var pledgeItemRepository: PledgeItemRepository

	@Autowired
	private lateinit var tripPledgeRepository: TripPledgeRepository

	@Autowired
	private lateinit var fakeTourApiClient: FakeTourApiClient

	@Autowired
	private lateinit var fakeTmapRouteClient: FakeTmapRouteClient

	@BeforeEach
	fun setUp() {
		fakeTourApiClient.reset()
		fakeTmapRouteClient.reset()
		cleanDatabase()
	}

	@AfterEach
	fun tearDown() {
		cleanDatabase()
	}

	private fun cleanDatabase() {
		supportFacilityRepository.deleteAll()
		pledgeSignatureRepository.deleteAll()
		pledgeItemRepository.deleteAll()
		tripPledgeRepository.deleteAll()
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
	fun `오늘 시작한 여행은 여행 모드에서 진행 중으로 자동 전환된다`() {
		val child = saveUser(UserRole.CHILD, "child-1", "혜린")
		val mother = saveUser(UserRole.PARENT, "parent-1", "엄마", GenderType.FEMALE)
		connectFamily(child, mother)
		saveCompletedParentProfile(mother)
		val today = futureDate(0)
		val tripId = createTrip(child, mother, today, today.plusDays(1))

		mockMvc.perform(
			get("/api/v1/trips/$tripId/travel-mode")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(mother)}"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.data.status").value("in_progress"))
			.andExpect(jsonPath("$.data.currentDayNumber").value(1))
			.andExpect(jsonPath("$.data.currentTripDayId").isNumber)
			.andExpect(jsonPath("$.data.isLastDay").value(false))
			.andExpect(jsonPath("$.data.pledgeCompleted").value(false))
			.andExpect(jsonPath("$.data.days.length()").value(2))

		mockMvc.perform(
			get("/api/v1/trips")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(child)}"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.data.trips[0].status").value("in_progress"))
	}

	@Test
	fun `여행 중에는 현재 위치에서 가까운 공중화장실을 최대 10개 조회한다`() {
		val child = saveUser(UserRole.CHILD, "child-1", "혜린")
		val mother = saveUser(UserRole.PARENT, "parent-1", "엄마", GenderType.FEMALE)
		connectFamily(child, mother)
		saveCompletedParentProfile(mother)
		val tripId = createTrip(child, mother, futureDate(0), futureDate(0))
		val latitude = 37.5758692
		val longitude = 126.9684817
		supportFacilityRepository.saveAll(
			(1..11).map { index ->
				SupportFacility(
					facilityType = SupportFacilityType.RESTROOM,
					provider = ExternalApiProvider.LOCAL_EXCEL,
					sourceId = "restroom-$index",
					name = "화장실 $index",
					address = "서울특별시 종로구 $index",
					latitude = latitude.toBigDecimal(),
					longitude = (longitude + index * 0.001).toBigDecimal(),
					operatingHours = "상시",
				)
			} + SupportFacility(
				facilityType = SupportFacilityType.RESTROOM,
				provider = ExternalApiProvider.LOCAL_EXCEL,
				sourceId = "restroom-far",
				name = "범위 밖 화장실",
				address = "서울특별시 외곽",
				latitude = latitude.toBigDecimal(),
				longitude = (longitude + 0.1).toBigDecimal(),
			),
		)

		mockMvc.perform(
			get("/api/v1/trips/$tripId/nearby-restrooms")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(mother)}")
				.param("latitude", latitude.toString())
				.param("longitude", longitude.toString()),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.data.length()").value(10))
			.andExpect(jsonPath("$.data[0].name").value("화장실 1"))
			.andExpect(jsonPath("$.data[0].distanceMeters").value(88))
			.andExpect(jsonPath("$.data[9].name").value("화장실 10"))
			.andExpect(jsonPath("$.data[?(@.name == '화장실 11')]").isEmpty)
			.andExpect(jsonPath("$.data[?(@.name == '범위 밖 화장실')]").isEmpty)
	}

	@Test
	fun `주변 공중화장실 조회 좌표 범위를 검증한다`() {
		val child = saveUser(UserRole.CHILD, "child-1", "혜린")
		val mother = saveUser(UserRole.PARENT, "parent-1", "엄마", GenderType.FEMALE)
		connectFamily(child, mother)
		saveCompletedParentProfile(mother)
		val tripId = createTrip(child, mother, futureDate(0), futureDate(0))

		mockMvc.perform(
			get("/api/v1/trips/$tripId/nearby-restrooms")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(child)}")
				.param("latitude", "91")
				.param("longitude", "126.9684817"),
		)
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.message").value("latitude는 -90 이상 90 이하여야 합니다."))
	}

	@Test
	fun `선택되지 않은 같은 가족도 마지막 날 여행 모드와 10계명 완료 여부를 조회한다`() {
		val child = saveUser(UserRole.CHILD, "child-1", "혜린")
		val mother = saveUser(UserRole.PARENT, "parent-1", "엄마", GenderType.FEMALE)
		val father = saveUser(UserRole.PARENT, "parent-2", "아빠", GenderType.MALE)
		connectFamily(child, mother, father)
		saveCompletedParentProfile(mother)
		val tripId = createTrip(child, mother, futureDate(10), futureDate(10))
		saveReviewedPledge(child, tripId)
		savePledgeSignature(child, tripId)
		savePledgeSignature(mother, tripId)
		moveTripToDates(tripId, futureDate(0), futureDate(0))

		mockMvc.perform(
			get("/api/v1/trips/$tripId/travel-mode")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(father)}"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.data.status").value("in_progress"))
			.andExpect(jsonPath("$.data.currentDayNumber").value(1))
			.andExpect(jsonPath("$.data.isLastDay").value(true))
			.andExpect(jsonPath("$.data.pledgeCompleted").value(true))
	}

	@Test
	fun `다른 가족은 여행 모드에 접근할 수 없다`() {
		val child = saveUser(UserRole.CHILD, "child-1", "혜린")
		val mother = saveUser(UserRole.PARENT, "parent-1", "엄마", GenderType.FEMALE)
		val otherChild = saveUser(UserRole.CHILD, "child-2", "다른 자녀")
		connectFamily(child, mother)
		connectFamily(otherChild)
		saveCompletedParentProfile(mother)
		val tripId = createTrip(child, mother, futureDate(0), futureDate(0))

		mockMvc.perform(
			get("/api/v1/trips/$tripId/travel-mode")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(otherChild)}"),
		)
			.andExpect(status().isForbidden)
			.andExpect(jsonPath("$.message").value("여행 조회 권한이 없습니다."))
	}

	@Test
	fun `여행 시작 전에는 여행 모드에 진입할 수 없다`() {
		val child = saveUser(UserRole.CHILD, "child-1", "혜린")
		val mother = saveUser(UserRole.PARENT, "parent-1", "엄마", GenderType.FEMALE)
		connectFamily(child, mother)
		saveCompletedParentProfile(mother)
		val tripId = createTrip(child, mother, futureDate(1), futureDate(2))

		mockMvc.perform(
			get("/api/v1/trips/$tripId/travel-mode")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(child)}"),
		)
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.message").value("여행 시작일부터 여행 모드를 이용할 수 있습니다."))
	}

	@Test
	fun `여행 종료 후에는 완료 상태로 전환되고 여행 모드에 진입할 수 없다`() {
		val child = saveUser(UserRole.CHILD, "child-1", "혜린")
		val mother = saveUser(UserRole.PARENT, "parent-1", "엄마", GenderType.FEMALE)
		connectFamily(child, mother)
		saveCompletedParentProfile(mother)
		val tripId = createTrip(child, mother, futureDate(10), futureDate(10))
		moveTripToDates(tripId, futureDate(-1), futureDate(-1))

		mockMvc.perform(
			get("/api/v1/trips/$tripId/travel-mode")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(child)}"),
		)
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.message").value("종료된 여행은 여행 모드를 이용할 수 없습니다."))

		check(tripRepository.findById(tripId.toLong()).orElseThrow().status == TripStatus.COMPLETED)

		mockMvc.perform(
			get("/api/v1/trips")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(child)}"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.data.trips[0].status").value("completed"))
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
	fun `여행 제목만 수정하면 기존 코스를 유지한다`() {
		val child = saveUser(UserRole.CHILD, "child-1", "혜린")
		val mother = saveUser(UserRole.PARENT, "parent-1", "엄마", GenderType.FEMALE)
		connectFamily(child, mother)
		saveCompletedParentProfile(mother)
		val startDate = futureDate(10)
		val tripId = createTrip(child, mother, startDate, startDate)
		saveSingleStopCourse(child, tripId)
		saveReviewedPledge(child, tripId)

		mockMvc.perform(
			put("/api/v1/trips/$tripId")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(child)}")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "title": "엄마와 경주 여행",
					  "destinationCode": "gyeongju",
					  "startDate": "$startDate",
					  "endDate": "$startDate",
					  "parentUserIds": [${requireNotNull(mother.id)}]
					}
					""".trimIndent(),
				),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.data.title").value("엄마와 경주 여행"))

		mockMvc.perform(
			get("/api/v1/trips/$tripId/course")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(child)}"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.data.days[0].stops.length()").value(1))
			.andExpect(jsonPath("$.data.days[0].stops[0].name").value("경주 산책 공원"))

		check(tripPledgeRepository.findByTripId(tripId.toLong()) != null)
	}

	@Test
	fun `코스가 있는 여행의 추천 입력 변경은 초기화 동의가 필요하다`() {
		val child = saveUser(UserRole.CHILD, "child-1", "혜린")
		val mother = saveUser(UserRole.PARENT, "parent-1", "엄마", GenderType.FEMALE)
		connectFamily(child, mother)
		saveCompletedParentProfile(mother)
		val startDate = futureDate(10)
		val tripId = createTrip(child, mother, startDate, startDate)
		saveSingleStopCourse(child, tripId)

		mockMvc.perform(
			put("/api/v1/trips/$tripId")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(child)}")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "title": "부산 여행",
					  "destinationCode": "busan",
					  "startDate": "$startDate",
					  "endDate": "$startDate",
					  "parentUserIds": [${requireNotNull(mother.id)}]
					}
					""".trimIndent(),
				),
		)
			.andExpect(status().isBadRequest)
			.andExpect(
				jsonPath("$.message")
					.value("도시, 날짜 또는 참여 부모를 변경하면 기존 코스가 삭제됩니다. 코스 초기화에 동의해주세요."),
			)
	}

	@Test
	fun `여행 추천 입력 변경에 동의하면 코스와 경로를 초기화하고 일자와 부모를 갱신한다`() {
		val child = saveUser(UserRole.CHILD, "child-1", "혜린")
		val mother = saveUser(UserRole.PARENT, "parent-1", "엄마", GenderType.FEMALE)
		val father = saveUser(UserRole.PARENT, "parent-2", "아빠", GenderType.MALE)
		connectFamily(child, mother, father)
		saveCompletedParentProfile(mother)
		saveCompletedParentProfile(father)
		val startDate = futureDate(10)
		val tripId = createTrip(child, mother, startDate, startDate)
		saveSingleStopCourse(child, tripId)
		saveReviewedPledge(child, tripId)
		savePledgeSignature(child, tripId)
		savePledgeSignature(mother, tripId)
		check(pledgeItemRepository.count() == 10L)
		check(pledgeSignatureRepository.count() == 2L)
		val trip = tripRepository.findById(tripId.toLong()).orElseThrow()
		trip.status = TripStatus.READY
		tripRepository.saveAndFlush(trip)
		val changedStartDate = startDate.plusDays(5)
		val changedEndDate = changedStartDate.plusDays(2)

		mockMvc.perform(
			put("/api/v1/trips/$tripId")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(child)}")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "title": "아빠와 부산 여행",
					  "destinationCode": "busan",
					  "startDate": "$changedStartDate",
					  "endDate": "$changedEndDate",
					  "parentUserIds": [${requireNotNull(father.id)}],
					  "courseResetConfirmed": true
					}
					""".trimIndent(),
				),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.data.title").value("아빠와 부산 여행"))
			.andExpect(jsonPath("$.data.destination.code").value("busan"))
			.andExpect(jsonPath("$.data.startDate").value(changedStartDate.toString()))
			.andExpect(jsonPath("$.data.endDate").value(changedEndDate.toString()))
			.andExpect(jsonPath("$.data.status").value("planning"))
			.andExpect(jsonPath("$.data.participants.length()").value(2))
			.andExpect(jsonPath("$.data.participants[1].userId").value(requireNotNull(father.id)))
			.andExpect(jsonPath("$.data.recommendationSnapshot.destinationCode").value("busan"))
			.andExpect(jsonPath("$.data.recommendationSnapshot.parents[0].parentUserId").value(requireNotNull(father.id)))
			.andExpect(jsonPath("$.data.days.length()").value(3))
			.andExpect(jsonPath("$.data.days[0].travelDate").value(changedStartDate.toString()))
			.andExpect(jsonPath("$.data.days[2].travelDate").value(changedEndDate.toString()))

		mockMvc.perform(
			get("/api/v1/trips/$tripId/course")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(child)}"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.data.days.length()").value(3))
			.andExpect(jsonPath("$.data.days[0].route").doesNotExist())
			.andExpect(jsonPath("$.data.days[0].stops.length()").value(0))

		check(tripPledgeRepository.findByTripId(tripId.toLong()) == null)
		check(pledgeItemRepository.count() == 0L)
		check(pledgeSignatureRepository.count() == 0L)
		mockMvc.perform(
			get("/api/v1/trips/$tripId/pledge")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(child)}"),
		)
			.andExpect(status().isNotFound)
			.andExpect(jsonPath("$.message").value("저장된 여행 10계명이 없습니다."))
	}

	@Test
	fun `여행을 만든 자녀가 아니면 여행 정보를 수정할 수 없다`() {
		val child = saveUser(UserRole.CHILD, "child-1", "혜린")
		val mother = saveUser(UserRole.PARENT, "parent-1", "엄마", GenderType.FEMALE)
		connectFamily(child, mother)
		saveCompletedParentProfile(mother)
		val startDate = futureDate(10)
		val tripId = createTrip(child, mother, startDate, startDate)

		mockMvc.perform(
			put("/api/v1/trips/$tripId")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(mother)}")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "title": "부모가 바꾼 여행",
					  "destinationCode": "gyeongju",
					  "startDate": "$startDate",
					  "endDate": "$startDate",
					  "parentUserIds": [${requireNotNull(mother.id)}]
					}
					""".trimIndent(),
				),
		)
			.andExpect(status().isForbidden)
			.andExpect(jsonPath("$.message").value("여행을 만든 자녀만 여행 정보를 수정할 수 있습니다."))
	}

	@Test
	fun `진행 중 여행은 오늘을 포함한 기간과 참여 부모를 수정할 수 있다`() {
		val child = saveUser(UserRole.CHILD, "child-1", "혜린")
		val mother = saveUser(UserRole.PARENT, "parent-1", "엄마", GenderType.FEMALE)
		val father = saveUser(UserRole.PARENT, "parent-2", "아빠", GenderType.MALE)
		connectFamily(child, mother, father)
		saveCompletedParentProfile(mother)
		saveCompletedParentProfile(father)
		val today = futureDate(0)
		val tripId = createTrip(child, mother, today, today.plusDays(1))
		saveSingleStopCourse(child, tripId)
		val changedEndDate = today.plusDays(2)

		mockMvc.perform(
			put("/api/v1/trips/$tripId")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(child)}")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "title": "경주 여행",
					  "destinationCode": "gyeongju",
					  "startDate": "$today",
					  "endDate": "$changedEndDate",
					  "parentUserIds": [${requireNotNull(father.id)}],
					  "courseResetConfirmed": true
					}
					""".trimIndent(),
				),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.data.status").value("in_progress"))
			.andExpect(jsonPath("$.data.startDate").value(today.toString()))
			.andExpect(jsonPath("$.data.endDate").value(changedEndDate.toString()))
			.andExpect(jsonPath("$.data.participants.length()").value(2))
			.andExpect(jsonPath("$.data.participants[1].userId").value(requireNotNull(father.id)))
			.andExpect(jsonPath("$.data.days.length()").value(3))
			.andExpect(jsonPath("$.data.days[2].travelDate").value(changedEndDate.toString()))

		mockMvc.perform(
			get("/api/v1/trips/$tripId/course")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(child)}"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.data.days.length()").value(3))
			.andExpect(jsonPath("$.data.days[0].stops.length()").value(0))
	}

	@Test
	fun `진행 중 여행은 제목과 도시를 수정할 수 없다`() {
		val child = saveUser(UserRole.CHILD, "child-1", "혜린")
		val mother = saveUser(UserRole.PARENT, "parent-1", "엄마", GenderType.FEMALE)
		connectFamily(child, mother)
		saveCompletedParentProfile(mother)
		val today = futureDate(0)
		val tripId = createTrip(child, mother, today, today.plusDays(1))
		val authorization = "Bearer ${tokenService.createAccessToken(child)}"

		mockMvc.perform(
			put("/api/v1/trips/$tripId")
				.header("Authorization", authorization)
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "title": "수정한 경주 여행",
					  "destinationCode": "gyeongju",
					  "startDate": "$today",
					  "endDate": "${today.plusDays(1)}",
					  "parentUserIds": [${requireNotNull(mother.id)}]
					}
					""".trimIndent(),
				),
		)
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.message").value("여행 중에는 여행 제목을 수정할 수 없습니다."))

		mockMvc.perform(
			put("/api/v1/trips/$tripId")
				.header("Authorization", authorization)
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "title": "경주 여행",
					  "destinationCode": "busan",
					  "startDate": "$today",
					  "endDate": "${today.plusDays(1)}",
					  "parentUserIds": [${requireNotNull(mother.id)}]
					}
					""".trimIndent(),
				),
		)
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.message").value("여행 중에는 여행 도시를 수정할 수 없습니다."))
	}

	@Test
	fun `진행 중 여행의 변경 기간에는 오늘이 포함되어야 한다`() {
		val child = saveUser(UserRole.CHILD, "child-1", "혜린")
		val mother = saveUser(UserRole.PARENT, "parent-1", "엄마", GenderType.FEMALE)
		connectFamily(child, mother)
		saveCompletedParentProfile(mother)
		val today = futureDate(0)
		val tripId = createTrip(child, mother, today, today.plusDays(1))

		mockMvc.perform(
			put("/api/v1/trips/$tripId")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(child)}")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "title": "경주 여행",
					  "destinationCode": "gyeongju",
					  "startDate": "${today.plusDays(1)}",
					  "endDate": "${today.plusDays(2)}",
					  "parentUserIds": [${requireNotNull(mother.id)}]
					}
					""".trimIndent(),
				),
		)
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.message").value("여행 중 변경한 기간에는 오늘이 포함되어야 합니다."))
	}

	@Test
	fun `부모는 여행 코스를 변경할 수 없다`() {
		val child = saveUser(UserRole.CHILD, "child-1", "혜린")
		val mother = saveUser(UserRole.PARENT, "parent-1", "엄마", GenderType.FEMALE)
		connectFamily(child, mother)
		saveCompletedParentProfile(mother)
		val tripId = createTrip(child, mother, futureDate(10), futureDate(10))
		val authorization = "Bearer ${tokenService.createAccessToken(mother)}"

		mockMvc.perform(
			put("/api/v1/trips/$tripId/course")
				.header("Authorization", authorization)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"days\":[]}"),
		)
			.andExpect(status().isForbidden)
			.andExpect(jsonPath("$.message").value("여행을 만든 자녀만 여행 코스를 수정할 수 있습니다."))

		mockMvc.perform(
			post("/api/v1/trips/$tripId/course/recommendation")
				.header("Authorization", authorization),
		)
			.andExpect(status().isForbidden)
			.andExpect(jsonPath("$.message").value("여행을 만든 자녀만 여행 코스를 수정할 수 있습니다."))

		mockMvc.perform(
			post("/api/v1/trips/$tripId/days/1/route-optimization")
				.header("Authorization", authorization),
		)
			.andExpect(status().isForbidden)
			.andExpect(jsonPath("$.message").value("여행을 만든 자녀만 여행 코스를 수정할 수 있습니다."))
	}

	@Test
	fun `완료된 여행은 기본 정보와 코스를 수정할 수 없다`() {
		val child = saveUser(UserRole.CHILD, "child-1", "혜린")
		val mother = saveUser(UserRole.PARENT, "parent-1", "엄마", GenderType.FEMALE)
		connectFamily(child, mother)
		saveCompletedParentProfile(mother)
		val tripId = createTrip(child, mother, futureDate(10), futureDate(10))
		val yesterday = futureDate(-1)
		moveTripToDates(tripId, yesterday, yesterday)
		val authorization = "Bearer ${tokenService.createAccessToken(child)}"

		mockMvc.perform(
			put("/api/v1/trips/$tripId")
				.header("Authorization", authorization)
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "title": "경주 여행",
					  "destinationCode": "gyeongju",
					  "startDate": "$yesterday",
					  "endDate": "$yesterday",
					  "parentUserIds": [${requireNotNull(mother.id)}]
					}
					""".trimIndent(),
				),
		)
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.message").value("완료되거나 보관된 여행은 여행 정보를 수정할 수 없습니다."))

		mockMvc.perform(
			put("/api/v1/trips/$tripId/course")
				.header("Authorization", authorization)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"days\":[]}"),
		)
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.message").value("완료되거나 보관된 여행은 여행 코스를 수정할 수 없습니다."))
	}

	@Test
	fun `다른 여행과 겹치는 날짜로 여행 정보를 수정할 수 없다`() {
		val child = saveUser(UserRole.CHILD, "child-1", "혜린")
		val mother = saveUser(UserRole.PARENT, "parent-1", "엄마", GenderType.FEMALE)
		connectFamily(child, mother)
		saveCompletedParentProfile(mother)
		val firstStartDate = futureDate(10)
		createTrip(child, mother, firstStartDate, firstStartDate.plusDays(1))
		val secondStartDate = futureDate(20)
		val secondTripId = createTrip(child, mother, secondStartDate, secondStartDate)

		mockMvc.perform(
			put("/api/v1/trips/$secondTripId")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(child)}")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "title": "겹치는 여행",
					  "destinationCode": "gyeongju",
					  "startDate": "${firstStartDate.plusDays(1)}",
					  "endDate": "${firstStartDate.plusDays(1)}",
					  "parentUserIds": [${requireNotNull(mother.id)}]
					}
					""".trimIndent(),
				),
		)
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.message").value("선택한 날짜에 이미 등록된 여행이 있습니다."))
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
	fun `여행 일자 방문지의 시작과 끝 조합을 탐색해 경로를 최적화한다`() {
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
					      "dayNumber": 1,
					      "stops": [
					        {
					          "stopType": "sightseeing",
					          "name": "오도리 공원",
					          "latitude": 35.8562,
					          "longitude": 129.2247
					        },
					        {
					          "stopType": "meal",
					          "name": "경주 한식당",
					          "latitude": 35.8500,
					          "longitude": 129.2100
					        },
					        {
					          "stopType": "cafe",
					          "name": "조용한 카페",
					          "latitude": 35.8420,
					          "longitude": 129.2050
					        }
					      ]
					    }
					  ]
					}
					""".trimIndent(),
				),
		)
			.andExpect(status().isOk)

		mockMvc.perform(
			post("/api/v1/trips/$tripId/days/1/route-optimization")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(child)}"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.data.days[0].route.provider").value("tmap"))
			.andExpect(jsonPath("$.data.days[0].route.totalDistanceMeters").value(1200))
			.andExpect(jsonPath("$.data.days[0].route.totalDurationSeconds").value(600))
			.andExpect(jsonPath("$.data.days[0].route.polyline[0].longitude").value(129.2100))
			.andExpect(jsonPath("$.data.days[0].route.sourcePayload.policyVersion").value("tmap-route-optimization-v1"))
			.andExpect(jsonPath("$.data.days[0].stops[0].name").value("경주 한식당"))
			.andExpect(jsonPath("$.data.days[0].stops[0].arrivalTime").value("10:00:00"))
			.andExpect(jsonPath("$.data.days[0].stops[0].dwellMinutes").value(60))
			.andExpect(jsonPath("$.data.days[0].stops[1].name").value("조용한 카페"))
			.andExpect(jsonPath("$.data.days[0].stops[1].arrivalTime").value("10:30:00"))
			.andExpect(jsonPath("$.data.days[0].stops[1].dwellMinutes").value(40))
			.andExpect(jsonPath("$.data.days[0].stops[2].name").value("오도리 공원"))

		check(fakeTmapRouteClient.requests.size == 6)
		check(fakeTmapRouteClient.requests.any { it.start.name == "경주 한식당" && it.end.name == "오도리 공원" })
		check(fakeTmapRouteClient.requests.all { it.startTime.toLocalTime() == LocalTime.of(10, 0) })
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
	fun `여행 도시 범위에서 방문지를 검색한다`() {
		val child = saveUser(UserRole.CHILD, "child-1", "혜린")
		val mother = saveUser(UserRole.PARENT, "parent-1", "엄마", GenderType.FEMALE)
		connectFamily(child, mother)
		saveCompletedParentProfile(mother)
		val tripId = createTrip(child, mother, futureDate(10), futureDate(10))
		fakeTourApiClient.keywordPlaces = listOf(
			tourPlace("food-1", "39", "경주 한식당", "FD"),
			tourPlace("nature-1", "12", "경주 산책 공원", "NA"),
		)

		mockMvc.perform(
			get("/api/v1/trips/$tripId/places/search")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(child)}")
				.param("keyword", "경주")
				.param("contentTypeId", "39")
				.param("page", "1")
				.param("size", "10"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.data.tripId").value(tripId))
			.andExpect(jsonPath("$.data.keyword").value("경주"))
			.andExpect(jsonPath("$.data.contentTypeId").value("39"))
			.andExpect(jsonPath("$.data.places.length()").value(1))
			.andExpect(jsonPath("$.data.places[0].externalPlaceId").value("food-1"))
			.andExpect(jsonPath("$.data.places[0].contentTypeName").value("음식점"))
			.andExpect(jsonPath("$.data.places[0].stopType").value("meal"))
	}

	@Test
	fun `방문지 상세와 무장애 정보를 조회한다`() {
		val child = saveUser(UserRole.CHILD, "child-1", "혜린")
		val mother = saveUser(UserRole.PARENT, "parent-1", "엄마", GenderType.FEMALE)
		connectFamily(child, mother)
		saveCompletedParentProfile(mother)
		val tripId = createTrip(child, mother, futureDate(10), futureDate(10))
		fakeTourApiClient.detailsByContentId["nature-1"] = TourApiPlaceDetail(
			homepage = "https://example.com/nature-1",
			overview = "산책하기 좋은 공원입니다.",
			raw = mapOf("contentid" to "nature-1", "title" to "경주 산책 공원"),
			contentId = "nature-1",
			contentTypeId = "12",
			title = "경주 산책 공원",
			address = "경상북도 경주시",
			latitude = "35.8562".toBigDecimal(),
			longitude = "129.2247".toBigDecimal(),
			tel = "054-000-0000",
			firstImage = "https://example.com/nature-1.jpg",
			lclsSystm1 = "NA",
		)
		fakeTourApiClient.accessibilityByContentId["nature-1"] = TourApiAccessibility(
			parking = "장애인 주차장 있음",
			publicTransport = "",
			route = "출입구까지 경사로 있음",
			wheelchair = "",
			exit = "휠체어 접근 가능",
			elevator = "",
			restroom = "장애인 화장실 있음",
			raw = mapOf("contentid" to "nature-1", "route" to "출입구까지 경사로 있음"),
		)

		mockMvc.perform(
			get("/api/v1/trips/$tripId/places/nature-1")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(child)}")
				.param("contentTypeId", "12"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.data.externalPlaceId").value("nature-1"))
			.andExpect(jsonPath("$.data.contentTypeName").value("관광지"))
			.andExpect(jsonPath("$.data.stopType").value("sightseeing"))
			.andExpect(jsonPath("$.data.homepageUrl").value("https://example.com/nature-1"))
			.andExpect(jsonPath("$.data.accessibility.route").value("출입구까지 경사로 있음"))
			.andExpect(jsonPath("$.data.accessibility.restroom").value("장애인 화장실 있음"))
			.andExpect(jsonPath("$.data.recommendationTags[0]").value("tour_api"))
			.andExpect(jsonPath("$.data.recommendationTags[3]").value("mobility_info"))
			.andExpect(jsonPath("$.data.sourcePayload.provider").value("tour_api"))
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

	private fun saveSingleStopCourse(user: User, tripId: Int) {
		mockMvc.perform(
			put("/api/v1/trips/$tripId/course")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(user)}")
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
					          "externalPlaceId": "nature-1",
					          "contentTypeId": "12",
					          "name": "경주 산책 공원",
					          "latitude": 35.8562,
					          "longitude": 129.2247
					        }
					      ]
					    }
					  ]
					}
					""".trimIndent(),
				),
		)
			.andExpect(status().isOk)
	}

	private fun saveReviewedPledge(child: User, tripId: Int) {
		val items = (1..10).joinToString(",") { index -> "{\"content\":\"가족 약속 $index\"}" }
		mockMvc.perform(
			put("/api/v1/trips/$tripId/pledge")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(child)}")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"items\":[$items]}"),
		)
			.andExpect(status().isOk)
	}

	private fun savePledgeSignature(user: User, tripId: Int) {
		mockMvc.perform(
			post("/api/v1/trips/$tripId/pledge/signatures/me")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(user)}")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"signatureImageBase64\":\"$PNG_BASE64\"}"),
		)
			.andExpect(status().isOk)
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

	private fun moveTripToDates(tripId: Int, startDate: LocalDate, endDate: LocalDate) {
		val trip = tripRepository.findById(tripId.toLong()).orElseThrow()
		trip.startDate = startDate
		trip.endDate = endDate
		trip.status = TripStatus.READY
		tripRepository.saveAndFlush(trip)

		val existingDays = tripDayRepository.findAllByTripIdOrderByDayNumberAsc(tripId.toLong())
		tripDayRepository.deleteAllInBatch(existingDays)
		tripDayRepository.flush()
		val dayCount = (endDate.toEpochDay() - startDate.toEpochDay() + 1).toInt()
		tripDayRepository.saveAll(
			(0 until dayCount).map { index ->
				TripDay(
					trip = trip,
					dayNumber = index + 1,
					travelDate = startDate.plusDays(index.toLong()),
				)
			},
		)
		tripDayRepository.flush()
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

	companion object {
		private const val PNG_BASE64 =
			"iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII="
	}

	@TestConfiguration
	class TourApiTestConfig {
		@Bean
		@Primary
		fun fakeTourApiClient(): FakeTourApiClient = FakeTourApiClient()

		@Bean
		@Primary
		fun fakeTmapRouteClient(): FakeTmapRouteClient = FakeTmapRouteClient()
	}

	class FakeTourApiClient : TourApiClient {
		val placesByContentType: MutableMap<String, List<TourApiPlaceSummary>> = mutableMapOf()
		val detailsByContentId: MutableMap<String, TourApiPlaceDetail> = mutableMapOf()
		val accessibilityByContentId: MutableMap<String, TourApiAccessibility> = mutableMapOf()
		var keywordPlaces: List<TourApiPlaceSummary> = emptyList()

		override fun findPlaces(search: TourApiPlaceSearch): List<TourApiPlaceSummary> =
			placesByContentType[search.contentTypeId].orEmpty()

		override fun searchPlaces(search: TourApiKeywordSearch): List<TourApiPlaceSummary> =
			keywordPlaces

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
			keywordPlaces = emptyList()
		}
	}

	class FakeTmapRouteClient : TmapRouteClient {
		val requests: MutableList<TmapRouteOptimizationRequest> = mutableListOf()

		override fun optimizeRoute(request: TmapRouteOptimizationRequest): TmapRouteOptimizationResult {
			requests.add(request)
			val orderedStopIds = listOf(request.start.stopId) + request.viaPoints.map { it.stopId } + request.end.stopId
			val isBestRoute = request.start.name == "경주 한식당" && request.end.name == "오도리 공원"
			return TmapRouteOptimizationResult(
				totalDistanceMeters = if (isBestRoute) 1200 else 9000,
				totalDurationSeconds = if (isBestRoute) 600 else 9000,
				totalFare = 0,
				orderedStopIds = orderedStopIds,
				arrivalTimes = orderedStopIds.mapIndexed { index, stopId ->
					stopId to LocalTime.of(10, 0).plusMinutes(index * 30L)
				}.toMap(),
				polyline = listOf(
					TmapRouteCoordinate(request.start.longitude, request.start.latitude),
					TmapRouteCoordinate(request.end.longitude, request.end.latitude),
				),
				rawProperties = mapOf(
					"totalDistance" to if (isBestRoute) "1200" else "9000",
					"totalTime" to if (isBestRoute) "600" else "9000",
					"totalFare" to "0",
				),
			)
		}

		fun reset() {
			requests.clear()
		}
	}
}
