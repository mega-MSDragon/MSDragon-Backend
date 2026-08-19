package com.msdragon.backend.report.controller

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
import com.msdragon.backend.feedback.repository.TripFeedbackRequestRepository
import com.msdragon.backend.report.entity.FilialReport
import com.msdragon.backend.report.repository.FilialReportRepository
import com.msdragon.backend.trip.entity.ExternalApiProvider
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
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

@SpringBootTest
@AutoConfigureMockMvc
class TripRecordControllerTest {
	@Autowired
	private lateinit var mockMvc: MockMvc

	@Autowired
	private lateinit var tokenService: TokenService

	@Autowired
	private lateinit var userRepository: UserRepository

	@Autowired
	private lateinit var familyRepository: FamilyRepository

	@Autowired
	private lateinit var familyMemberRepository: FamilyMemberRepository

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

	@Test
	fun `완료와 중단 여행을 최신순으로 조회하고 완료 여행만 기록 통계에 반영한다`() {
		val child = saveUser(UserRole.CHILD, "record-child", "혜린", GenderType.FEMALE)
		val mother = saveUser(UserRole.PARENT, "record-mother", "길순", GenderType.FEMALE)
		val father = saveUser(UserRole.PARENT, "record-father", "철수", GenderType.MALE)
		val family = saveFamily(child, mother, father)
		val participants = listOf(child, mother, father)

		val readyTrip = saveTrip(
			family = family,
			child = child,
			participants = participants,
			title = "경주 가족여행",
			destinationCode = TripDestinationCode.GYEONGJU,
			startDate = today().minusDays(5),
			endDate = today().minusDays(4),
			stopNames = listOf("오도리 공원", "한식 점심"),
			routeDistanceMeters = 7_500,
		)
		saveFeedback(readyTrip, mother, BigDecimal("4.0"), readyTrip.stops[0])
		saveFeedback(readyTrip, father, BigDecimal("5.0"), readyTrip.stops[1])
		filialReportRepository.save(
			FilialReport(
				trip = readyTrip.trip,
				coverImageUrl = "https://example.com/report-cover.jpg",
				totalPlaceCount = 2,
				averageRating = BigDecimal("4.5"),
				totalDistanceKm = BigDecimal("7.50"),
				generatedAt = LocalDateTime.now(),
			),
		)

		val pendingTrip = saveTrip(
			family = family,
			child = child,
			participants = participants,
			title = "부산 온천 여행",
			destinationCode = TripDestinationCode.BUSAN,
			startDate = today().minusDays(2),
			endDate = today().minusDays(1),
			stopNames = listOf("해운대 산책로"),
			routeDistanceMeters = 2_500,
		)
		saveFeedback(pendingTrip, mother, BigDecimal("3.5"), pendingTrip.stops[0])

		val lastDayTrip = saveTrip(
			family = family,
			child = child,
			participants = participants,
			title = "오늘 여행",
			destinationCode = TripDestinationCode.SEOUL,
			startDate = today(),
			endDate = today(),
			stopNames = listOf("서울 공원"),
			routeDistanceMeters = 1_000,
		)
		val stoppedTrip = saveTrip(
			family = family,
			child = child,
			participants = participants,
			title = "중단한 여행",
			destinationCode = TripDestinationCode.JEONJU,
			startDate = today(),
			endDate = today().plusDays(1),
			stopNames = listOf("한옥마을"),
			routeDistanceMeters = 1_000,
		)
		stoppedTrip.trip.status = TripStatus.STOPPED
		tripRepository.saveAndFlush(stoppedTrip.trip)

		mockMvc.perform(
			get("/api/v1/records")
				.header("Authorization", authorization(child)),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.data.familyId").value(requireNotNull(family.id)))
			.andExpect(jsonPath("$.data.statistics.completedTripCount").value(2))
			.andExpect(jsonPath("$.data.statistics.averageRating").value(4.0))
			.andExpect(jsonPath("$.data.statistics.totalPlaceCount").value(3))
			.andExpect(jsonPath("$.data.statistics.totalDistanceKm").value(10.0))
			.andExpect(jsonPath("$.data.records.length()").value(3))
			.andExpect(jsonPath("$.data.records[0].tripId").value(requireNotNull(stoppedTrip.trip.id)))
			.andExpect(jsonPath("$.data.records[0].status").value("stopped"))
			.andExpect(jsonPath("$.data.records[0].reportReady").value(false))
			.andExpect(jsonPath("$.data.records[1].tripId").value(requireNotNull(pendingTrip.trip.id)))
			.andExpect(jsonPath("$.data.records[1].title").value("부산 온천 여행"))
			.andExpect(jsonPath("$.data.records[1].status").value("completed"))
			.andExpect(jsonPath("$.data.records[1].coverImageUrl").value("https://example.com/해운대 산책로.jpg"))
			.andExpect(jsonPath("$.data.records[1].averageRating").value(3.5))
			.andExpect(jsonPath("$.data.records[1].reportReady").value(false))
			.andExpect(jsonPath("$.data.records[1].participants.length()").value(3))
			.andExpect(jsonPath("$.data.records[2].tripId").value(requireNotNull(readyTrip.trip.id)))
			.andExpect(jsonPath("$.data.records[2].coverImageUrl").value("https://example.com/report-cover.jpg"))
			.andExpect(jsonPath("$.data.records[2].averageRating").value(4.5))
			.andExpect(jsonPath("$.data.records[2].reportReady").value(true))

		check(tripRepository.findById(requireNotNull(readyTrip.trip.id)).orElseThrow().status == TripStatus.COMPLETED)
		check(tripRepository.findById(requireNotNull(pendingTrip.trip.id)).orElseThrow().status == TripStatus.COMPLETED)
		check(tripRepository.findById(requireNotNull(lastDayTrip.trip.id)).orElseThrow().status == TripStatus.IN_PROGRESS)
		check(tripRepository.findById(requireNotNull(stoppedTrip.trip.id)).orElseThrow().status == TripStatus.STOPPED)

		mockMvc.perform(
			get("/api/v1/records")
				.header("Authorization", authorization(mother)),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.data.records.length()").value(3))
	}

	@Test
	fun `가족 매칭 전에는 빈 기록과 통계를 반환한다`() {
		val child = saveUser(UserRole.CHILD, "record-unmatched", "미매칭", GenderType.FEMALE)

		mockMvc.perform(
			get("/api/v1/records")
				.header("Authorization", authorization(child)),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.data.familyId").doesNotExist())
			.andExpect(jsonPath("$.data.statistics.completedTripCount").value(0))
			.andExpect(jsonPath("$.data.statistics.averageRating").doesNotExist())
			.andExpect(jsonPath("$.data.statistics.totalPlaceCount").value(0))
			.andExpect(jsonPath("$.data.statistics.totalDistanceKm").doesNotExist())
			.andExpect(jsonPath("$.data.records").isEmpty)
	}

	private fun saveFamily(child: User, mother: User, father: User): Family {
		val family = familyRepository.save(Family(ownerUser = child))
		familyMemberRepository.saveAll(
			listOf(
				FamilyMember(family = family, user = child, memberRole = UserRole.CHILD),
				FamilyMember(family = family, user = mother, memberRole = UserRole.PARENT),
				FamilyMember(family = family, user = father, memberRole = UserRole.PARENT),
			),
		)
		return family
	}

	private fun saveTrip(
		family: Family,
		child: User,
		participants: List<User>,
		title: String,
		destinationCode: TripDestinationCode,
		startDate: LocalDate,
		endDate: LocalDate,
		stopNames: List<String>,
		routeDistanceMeters: Int,
	): TripFixture {
		val trip = tripRepository.save(
			Trip(
				family = family,
				createdByUser = child,
				destinationCode = destinationCode,
				title = title,
				startDate = startDate,
				endDate = endDate,
			),
		)
		tripParticipantRepository.saveAll(participants.map { TripParticipant(trip = trip, user = it) })
		val day = tripDayRepository.save(
			TripDay(
				trip = trip,
				dayNumber = 1,
				travelDate = endDate,
			),
		)
		day.applyRouteOptimization(
			provider = ExternalApiProvider.TMAP,
			totalDistanceMeters = routeDistanceMeters,
			totalDurationSeconds = 900,
			polyline = null,
			sourcePayload = null,
			optimizedAt = LocalDateTime.now(),
		)
		tripDayRepository.saveAndFlush(day)
		val stops = tripStopRepository.saveAll(
			stopNames.mapIndexed { index, name ->
				TripStop(
					tripDay = day,
					sortOrder = index + 1,
					name = name,
					imageUrl = "https://example.com/$name.jpg",
				)
			},
		)
		return TripFixture(trip, stops)
	}

	private fun saveFeedback(fixture: TripFixture, parent: User, rating: BigDecimal, bestStop: TripStop) {
		tripFeedbackRepository.save(
			TripFeedback(
				trip = fixture.trip,
				parentUser = parent,
				overallRating = rating,
				bodyCondition = FeedbackBodyCondition.COMFORTABLE,
				bestTripStopId = requireNotNull(bestStop.id),
				bestPlaceNameSnapshot = bestStop.name,
				freeComment = null,
				submittedAt = LocalDateTime.now(),
			),
		)
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
				ageBand = if (role == UserRole.CHILD) AgeBand.AGE_20S else AgeBand.AGE_60S,
				gender = gender,
				signupCompletedAt = LocalDateTime.now(),
			),
		)

	private fun authorization(user: User): String =
		"Bearer ${tokenService.createAccessToken(user)}"

	private fun today(): LocalDate = LocalDate.now(ZoneId.of("Asia/Seoul"))

	private fun cleanDatabase() {
		filialReportRepository.deleteAll()
		tripFeedbackRepository.deleteAll()
		tripFeedbackRequestRepository.deleteAll()
		tripStopRepository.deleteAll()
		tripDayRepository.deleteAll()
		tripParticipantRepository.deleteAll()
		tripRepository.deleteAll()
		familyMemberRepository.deleteAll()
		familyRepository.deleteAll()
		userRepository.deleteAll()
	}

	private data class TripFixture(
		val trip: Trip,
		val stops: List<TripStop>,
	)
}
