package com.msdragon.backend.feedback.controller

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
import com.msdragon.backend.feedback.repository.TripFeedbackRepository
import com.msdragon.backend.feedback.repository.TripFeedbackRequestRepository
import com.msdragon.backend.parentprofile.entity.FoodPreference
import com.msdragon.backend.parentprofile.entity.ParentProfile
import com.msdragon.backend.parentprofile.entity.ParentProfileStatus
import com.msdragon.backend.parentprofile.entity.TravelPersonalityTypeCode
import com.msdragon.backend.parentprofile.entity.TravelThemeCode
import com.msdragon.backend.parentprofile.entity.WalkingPace
import com.msdragon.backend.parentprofile.repository.ParentProfileRepository
import com.msdragon.backend.report.repository.FilialReportRepository
import com.msdragon.backend.trip.entity.ExternalApiProvider
import com.msdragon.backend.trip.entity.Trip
import com.msdragon.backend.trip.entity.TripDay
import com.msdragon.backend.trip.entity.TripDestinationCode
import com.msdragon.backend.trip.entity.TripParticipant
import com.msdragon.backend.trip.entity.TripStop
import com.msdragon.backend.trip.repository.TripDayRepository
import com.msdragon.backend.trip.repository.TripParticipantRepository
import com.msdragon.backend.trip.repository.TripRepository
import com.msdragon.backend.trip.repository.TripStopRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.ObjectMapper
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

@SpringBootTest
@AutoConfigureMockMvc
class TripFeedbackControllerTest {
	@Autowired
	private lateinit var mockMvc: MockMvc

	@Autowired
	private lateinit var objectMapper: ObjectMapper

	@Autowired
	private lateinit var tokenService: TokenService

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
	private lateinit var tripParticipantRepository: TripParticipantRepository

	@Autowired
	private lateinit var tripDayRepository: TripDayRepository

	@Autowired
	private lateinit var tripStopRepository: TripStopRepository

	@Autowired
	private lateinit var tripFeedbackRequestRepository: TripFeedbackRequestRepository

	@Autowired
	private lateinit var tripFeedbackRepository: TripFeedbackRepository

	@Autowired
	private lateinit var filialReportRepository: FilialReportRepository

	@BeforeEach
	fun setUp() {
		cleanDatabase()
	}

	@AfterEach
	fun tearDown() {
		cleanDatabase()
	}

	private fun cleanDatabase() {
		filialReportRepository.deleteAll()
		tripFeedbackRepository.deleteAll()
		tripFeedbackRequestRepository.deleteAll()
		tripStopRepository.deleteAll()
		tripDayRepository.deleteAll()
		tripParticipantRepository.deleteAll()
		tripRepository.deleteAll()
		parentProfileRepository.deleteAll()
		familyMemberRepository.deleteAll()
		familyRepository.deleteAll()
		userRepository.deleteAll()
	}

	@Test
	fun `마지막 날 자녀가 미제출 부모 모두에게 평가를 중복 없이 요청한다`() {
		val fixture = createTripFixture(endDate = today())
		val tripId = requireNotNull(fixture.trip.id)
		val authorization = authorization(fixture.child)

		repeat(2) {
			mockMvc.perform(
				post("/api/v1/trips/$tripId/feedback/requests")
					.header("Authorization", authorization),
			)
				.andExpect(status().isOk)
				.andExpect(jsonPath("$.data.feedbackAvailable").value(true))
				.andExpect(jsonPath("$.data.totalParentCount").value(2))
				.andExpect(jsonPath("$.data.requestedParentCount").value(2))
				.andExpect(jsonPath("$.data.submittedParentCount").value(0))
				.andExpect(jsonPath("$.data.canRequest").value(true))
				.andExpect(jsonPath("$.data.reportReady").value(false))
				.andExpect(jsonPath("$.data.parents[0].requestedAt").isString)
		}

		check(tripFeedbackRequestRepository.count() == 2L)
	}

	@Test
	fun `부모는 요청 없이 0점 피드백을 제출하고 모든 부모 제출 시 리포트 준비가 완료된다`() {
		val fixture = createTripFixture(endDate = today())
		val tripId = requireNotNull(fixture.trip.id)
		val stopId = requireNotNull(fixture.stop.id)
		fixture.days.last().applyRouteOptimization(
			provider = ExternalApiProvider.TMAP,
			totalDistanceMeters = 7_500,
			totalDurationSeconds = 900,
			polyline = null,
			sourcePayload = null,
			optimizedAt = LocalDateTime.now(),
		)
		tripDayRepository.saveAndFlush(fixture.days.last())

		mockMvc.perform(
			post("/api/v1/trips/$tripId/feedback/me")
				.header("Authorization", authorization(fixture.mother))
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					feedbackBody(
						stopId = stopId,
						rating = BigDecimal("0.0"),
						goodTags = listOf("walking_comfortable", "food_good"),
						improvementTags = listOf("more_rest_needed"),
						freeComment = "  다음에도 함께 가고 싶어요.  ",
					),
				),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.data.overallRating").value(0.0))
			.andExpect(jsonPath("$.data.bodyCondition").value("comfortable"))
			.andExpect(jsonPath("$.data.goodTags[0]").value("walking_comfortable"))
			.andExpect(jsonPath("$.data.goodTags[1]").value("food_good"))
			.andExpect(jsonPath("$.data.improvementTags[0]").value("more_rest_needed"))
			.andExpect(jsonPath("$.data.bestPlace.tripStopId").value(stopId))
			.andExpect(jsonPath("$.data.bestPlace.name").value("오도리 공원"))
			.andExpect(jsonPath("$.data.freeComment").value("다음에도 함께 가고 싶어요."))
			.andExpect(jsonPath("$.data.reportReady").value(false))

		mockMvc.perform(
			get("/api/v1/trips/$tripId/feedback/me")
				.header("Authorization", authorization(fixture.mother)),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.data.parentUserId").value(requireNotNull(fixture.mother.id)))

		mockMvc.perform(
			post("/api/v1/trips/$tripId/feedback/me")
				.header("Authorization", authorization(fixture.father))
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					feedbackBody(
						stopId = requireNotNull(fixture.secondStop.id),
						rating = BigDecimal("5.0"),
						goodTags = listOf("walking_comfortable", "scenery_good"),
						freeComment = "풍경이 좋았어요.",
					),
				),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.data.reportReady").value(true))

		check(filialReportRepository.count() == 1L)

		mockMvc.perform(
			get("/api/v1/trips/$tripId/feedback/status")
				.header("Authorization", authorization(fixture.child)),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.data.submittedParentCount").value(2))
			.andExpect(jsonPath("$.data.canRequest").value(false))
			.andExpect(jsonPath("$.data.reportReady").value(true))

		mockMvc.perform(
			get("/api/v1/trips/$tripId/filial-report")
				.header("Authorization", authorization(fixture.child)),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.data.tripId").value(tripId))
			.andExpect(jsonPath("$.data.coverImageUrl").value("https://example.com/park.jpg"))
			.andExpect(jsonPath("$.data.totalPlaceCount").value(2))
			.andExpect(jsonPath("$.data.averageRating").value(2.5))
			.andExpect(jsonPath("$.data.totalDistanceKm").value(7.5))
			.andExpect(jsonPath("$.data.totalScore").doesNotExist())
			.andExpect(jsonPath("$.data.goodTags.length()").value(3))
			.andExpect(jsonPath("$.data.goodTags[0]").value("walking_comfortable"))
			.andExpect(jsonPath("$.data.goodTags[1]").value("scenery_good"))
			.andExpect(jsonPath("$.data.goodTags[2]").value("food_good"))
			.andExpect(jsonPath("$.data.improvementTags[0]").value("more_rest_needed"))
			.andExpect(jsonPath("$.data.parentFeedbacks.length()").value(2))
			.andExpect(jsonPath("$.data.parentFeedbacks[0].bestPlace.tripStopId").value(stopId))
			.andExpect(
				jsonPath("$.data.parentFeedbacks[1].bestPlace.tripStopId")
					.value(requireNotNull(fixture.secondStop.id)),
			)
			.andExpect(jsonPath("$.data.stops.length()").value(2))
			.andExpect(jsonPath("$.data.generatedAt").isString)

		mockMvc.perform(
			post("/api/v1/trips/$tripId/feedback/me")
				.header("Authorization", authorization(fixture.mother))
				.contentType(MediaType.APPLICATION_JSON)
				.content(feedbackBody(stopId = stopId)),
		)
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.message").value("이미 여행 피드백을 제출했습니다."))
	}

	@Test
	fun `효도 리포트 생성 API는 모든 부모 제출 후 멱등하게 동작하고 다른 가족은 조회할 수 없다`() {
		val fixture = createTripFixture(endDate = today())
		val tripId = requireNotNull(fixture.trip.id)

		mockMvc.perform(
			post("/api/v1/trips/$tripId/filial-report")
				.header("Authorization", authorization(fixture.child)),
		)
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.message").value("모든 참여 부모가 피드백을 제출한 후 효도 리포트를 생성할 수 있습니다."))

		listOf(fixture.mother to fixture.stop, fixture.father to fixture.secondStop).forEach { (parent, stop) ->
			mockMvc.perform(
				post("/api/v1/trips/$tripId/feedback/me")
					.header("Authorization", authorization(parent))
					.contentType(MediaType.APPLICATION_JSON)
					.content(feedbackBody(stopId = requireNotNull(stop.id))),
			)
				.andExpect(status().isOk)
		}

		repeat(2) {
			mockMvc.perform(
				post("/api/v1/trips/$tripId/filial-report")
					.header("Authorization", authorization(fixture.mother)),
			)
				.andExpect(status().isOk)
				.andExpect(jsonPath("$.data.id").isNumber)
		}
		check(filialReportRepository.count() == 1L)

		val outsider = saveUser(UserRole.CHILD, "child-outside-report", "다른 자녀", GenderType.FEMALE)
		mockMvc.perform(
			get("/api/v1/trips/$tripId/filial-report")
				.header("Authorization", authorization(outsider)),
		)
			.andExpect(status().isForbidden)
			.andExpect(jsonPath("$.message").value("효도 리포트 조회 권한이 없습니다."))
	}

	@Test
	fun `마지막 날 전에는 요청하거나 제출할 수 없고 여행 종료 후에는 제출할 수 있다`() {
		val fixture = createTripFixture(endDate = today().plusDays(1))
		val tripId = requireNotNull(fixture.trip.id)

		mockMvc.perform(
			post("/api/v1/trips/$tripId/feedback/requests")
				.header("Authorization", authorization(fixture.child)),
		)
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.message").value("여행 마지막 날부터 피드백을 작성할 수 있습니다."))

		mockMvc.perform(
			post("/api/v1/trips/$tripId/feedback/me")
				.header("Authorization", authorization(fixture.mother))
				.contentType(MediaType.APPLICATION_JSON)
				.content(feedbackBody(stopId = requireNotNull(fixture.stop.id))),
		)
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.message").value("여행 마지막 날부터 피드백을 작성할 수 있습니다."))

		fixture.trip.startDate = today().minusDays(1)
		fixture.trip.endDate = today().minusDays(1)
		tripRepository.saveAndFlush(fixture.trip)

		mockMvc.perform(
			post("/api/v1/trips/$tripId/feedback/me")
				.header("Authorization", authorization(fixture.mother))
				.contentType(MediaType.APPLICATION_JSON)
				.content(feedbackBody(stopId = requireNotNull(fixture.stop.id))),
		)
			.andExpect(status().isOk)
	}

	@Test
	fun `별점 단위와 태그 분류와 베스트 장소를 검증한다`() {
		val fixture = createTripFixture(endDate = today())
		val tripId = requireNotNull(fixture.trip.id)
		val authorization = authorization(fixture.mother)

		mockMvc.perform(
			post("/api/v1/trips/$tripId/feedback/me")
				.header("Authorization", authorization)
				.contentType(MediaType.APPLICATION_JSON)
				.content(feedbackBody(stopId = requireNotNull(fixture.stop.id), rating = BigDecimal("5.5"))),
		)
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.message").value("전체 만족도는 5.0 이하여야 합니다."))

		mockMvc.perform(
			post("/api/v1/trips/$tripId/feedback/me")
				.header("Authorization", authorization)
				.contentType(MediaType.APPLICATION_JSON)
				.content(feedbackBody(stopId = requireNotNull(fixture.stop.id), rating = BigDecimal("0.3"))),
		)
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.message").value("전체 만족도는 0.5 단위로 입력해주세요."))

		mockMvc.perform(
			post("/api/v1/trips/$tripId/feedback/me")
				.header("Authorization", authorization)
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					feedbackBody(
						stopId = requireNotNull(fixture.stop.id),
						goodTags = listOf("more_rest_needed"),
					),
				),
		)
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.message").value("좋았던 점에 사용할 수 없는 태그가 포함되어 있습니다."))

		mockMvc.perform(
			post("/api/v1/trips/$tripId/feedback/me")
				.header("Authorization", authorization)
				.contentType(MediaType.APPLICATION_JSON)
				.content(feedbackBody(stopId = Long.MAX_VALUE)),
		)
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.message").value("해당 여행에 포함된 방문지만 베스트 장소로 선택할 수 있습니다."))
	}

	@Test
	fun `여행 참여 부모만 피드백을 제출하고 현황을 조회할 수 있다`() {
		val fixture = createTripFixture(endDate = today())
		val tripId = requireNotNull(fixture.trip.id)
		val outsider = saveUser(UserRole.PARENT, "parent-outside", "다른 부모", GenderType.FEMALE)

		mockMvc.perform(
			post("/api/v1/trips/$tripId/feedback/requests")
				.header("Authorization", authorization(fixture.mother)),
		)
			.andExpect(status().isForbidden)
			.andExpect(jsonPath("$.message").value("여행을 만든 자녀만 부모 평가를 요청할 수 있습니다."))

		mockMvc.perform(
			post("/api/v1/trips/$tripId/feedback/me")
				.header("Authorization", authorization(fixture.child))
				.contentType(MediaType.APPLICATION_JSON)
				.content(feedbackBody(stopId = requireNotNull(fixture.stop.id))),
		)
			.andExpect(status().isForbidden)
			.andExpect(jsonPath("$.message").value("여행에 참여한 부모만 피드백을 작성할 수 있습니다."))

		mockMvc.perform(
			get("/api/v1/trips/$tripId/feedback/status")
				.header("Authorization", authorization(outsider)),
		)
			.andExpect(status().isForbidden)
			.andExpect(jsonPath("$.message").value("여행 참여자만 피드백 현황을 조회할 수 있습니다."))
	}

	@Test
	fun `여행 기간을 바꾸면 기존 피드백과 요청을 초기화한다`() {
		val fixture = createTripFixture(endDate = today())
		saveCompletedParentProfile(fixture.mother)
		saveCompletedParentProfile(fixture.father)
		val tripId = requireNotNull(fixture.trip.id)
		val parentIds = listOf(requireNotNull(fixture.mother.id), requireNotNull(fixture.father.id))

		mockMvc.perform(
			post("/api/v1/trips/$tripId/feedback/requests")
				.header("Authorization", authorization(fixture.child)),
		)
			.andExpect(status().isOk)
		mockMvc.perform(
			post("/api/v1/trips/$tripId/feedback/me")
				.header("Authorization", authorization(fixture.mother))
				.contentType(MediaType.APPLICATION_JSON)
				.content(feedbackBody(stopId = requireNotNull(fixture.stop.id))),
		)
			.andExpect(status().isOk)
		mockMvc.perform(
			post("/api/v1/trips/$tripId/feedback/me")
				.header("Authorization", authorization(fixture.father))
				.contentType(MediaType.APPLICATION_JSON)
				.content(feedbackBody(stopId = requireNotNull(fixture.secondStop.id))),
		)
			.andExpect(status().isOk)
		check(filialReportRepository.count() == 1L)

		mockMvc.perform(
			put("/api/v1/trips/$tripId")
				.header("Authorization", authorization(fixture.child))
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					objectMapper.writeValueAsString(
						mapOf(
							"title" to "경주 여행",
							"destinationCode" to "gyeongju",
							"startDate" to fixture.trip.startDate.toString(),
							"endDate" to today().plusDays(1).toString(),
							"parentUserIds" to parentIds,
							"courseResetConfirmed" to true,
						),
					),
				),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.data.status").value("in_progress"))

		check(tripFeedbackRepository.count() == 0L)
		check(tripFeedbackRequestRepository.count() == 0L)
		check(filialReportRepository.count() == 0L)
	}

	@Test
	fun `참여 부모를 바꾸면 기존 피드백과 요청을 초기화한다`() {
		val fixture = createTripFixture(endDate = today())
		saveCompletedParentProfile(fixture.mother)
		val tripId = requireNotNull(fixture.trip.id)

		mockMvc.perform(
			post("/api/v1/trips/$tripId/feedback/requests")
				.header("Authorization", authorization(fixture.child)),
		)
			.andExpect(status().isOk)
		mockMvc.perform(
			post("/api/v1/trips/$tripId/feedback/me")
				.header("Authorization", authorization(fixture.mother))
				.contentType(MediaType.APPLICATION_JSON)
				.content(feedbackBody(stopId = requireNotNull(fixture.stop.id))),
		)
			.andExpect(status().isOk)

		mockMvc.perform(
			put("/api/v1/trips/$tripId")
				.header("Authorization", authorization(fixture.child))
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					objectMapper.writeValueAsString(
						mapOf(
							"title" to "경주 여행",
							"destinationCode" to "gyeongju",
							"startDate" to fixture.trip.startDate.toString(),
							"endDate" to fixture.trip.endDate.toString(),
							"parentUserIds" to listOf(requireNotNull(fixture.mother.id)),
							"courseResetConfirmed" to true,
						),
					),
				),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.data.participants.length()").value(2))

		check(tripFeedbackRepository.count() == 0L)
		check(tripFeedbackRequestRepository.count() == 0L)
	}

	private fun createTripFixture(endDate: LocalDate): TripFixture {
		val child = saveUser(UserRole.CHILD, "child-1", "혜린", GenderType.FEMALE)
		val mother = saveUser(UserRole.PARENT, "parent-1", "길순", GenderType.FEMALE)
		val father = saveUser(UserRole.PARENT, "parent-2", "철수", GenderType.MALE)
		val family = familyRepository.save(Family(ownerUser = child))
		familyMemberRepository.saveAll(
			listOf(
				FamilyMember(family = family, user = child, memberRole = UserRole.CHILD),
				FamilyMember(family = family, user = mother, memberRole = UserRole.PARENT),
				FamilyMember(family = family, user = father, memberRole = UserRole.PARENT),
			),
		)
		val startDate = endDate.minusDays(1)
		val trip = tripRepository.save(
			Trip(
				family = family,
				createdByUser = child,
				destinationCode = TripDestinationCode.GYEONGJU,
				title = "경주 여행",
				startDate = startDate,
				endDate = endDate,
			),
		)
		tripParticipantRepository.saveAll(
			listOf(
				TripParticipant(trip = trip, user = child),
				TripParticipant(trip = trip, user = mother),
				TripParticipant(trip = trip, user = father),
			),
		)
		val days = listOf(
			tripDayRepository.save(TripDay(trip = trip, dayNumber = 1, travelDate = startDate)),
			tripDayRepository.save(TripDay(trip = trip, dayNumber = 2, travelDate = endDate)),
		)
		val stop = tripStopRepository.save(
			TripStop(
				tripDay = days.last(),
				sortOrder = 1,
				name = "오도리 공원",
				imageUrl = "https://example.com/park.jpg",
			),
		)
		val secondStop = tripStopRepository.save(
			TripStop(
				tripDay = days.last(),
				sortOrder = 2,
				name = "소프카레 점심",
				category = "식당",
				imageUrl = "https://example.com/curry.jpg",
			),
		)
		return TripFixture(child, mother, father, trip, days, stop, secondStop)
	}

	private fun saveCompletedParentProfile(parent: User) {
		parentProfileRepository.save(
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
			),
		)
	}

	private fun feedbackBody(
		stopId: Long,
		rating: BigDecimal = BigDecimal("4.5"),
		goodTags: List<String> = emptyList(),
		improvementTags: List<String> = emptyList(),
		freeComment: String? = null,
	): String =
		objectMapper.writeValueAsString(
			mapOf(
				"overallRating" to rating,
				"bodyCondition" to "comfortable",
				"goodTags" to goodTags,
				"improvementTags" to improvementTags,
				"bestTripStopId" to stopId,
				"freeComment" to freeComment,
			),
		)

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
				ageBand = if (role == UserRole.CHILD) AgeBand.AGE_20S else AgeBand.AGE_60S,
				gender = gender,
				signupCompletedAt = LocalDateTime.now(),
			),
		)

	private fun authorization(user: User): String =
		"Bearer ${tokenService.createAccessToken(user)}"

	private fun today(): LocalDate = LocalDate.now(ZoneId.of("Asia/Seoul"))

	private data class TripFixture(
		val child: User,
		val mother: User,
		val father: User,
		val trip: Trip,
		val days: List<TripDay>,
		val stop: TripStop,
		val secondStop: TripStop,
	)
}
