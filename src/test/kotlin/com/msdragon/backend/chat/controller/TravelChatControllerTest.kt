package com.msdragon.backend.chat.controller

import com.msdragon.backend.auth.entity.OAuthProvider
import com.msdragon.backend.auth.entity.User
import com.msdragon.backend.auth.entity.UserRole
import com.msdragon.backend.auth.repository.UserRefreshTokenRepository
import com.msdragon.backend.auth.repository.UserRepository
import com.msdragon.backend.auth.service.TokenService
import com.msdragon.backend.chat.entity.ChatSender
import com.msdragon.backend.chat.openai.OpenAiChatRequest
import com.msdragon.backend.chat.openai.OpenAiChatResult
import com.msdragon.backend.chat.openai.OpenAiResponsesClient
import com.msdragon.backend.chat.openai.OpenAiToolCall
import com.msdragon.backend.chat.repository.ChatMessageRepository
import com.msdragon.backend.chat.repository.ChatSessionRepository
import com.msdragon.backend.common.exception.InternalServerException
import com.msdragon.backend.family.entity.Family
import com.msdragon.backend.family.entity.FamilyMember
import com.msdragon.backend.family.repository.FamilyMemberRepository
import com.msdragon.backend.family.repository.FamilyRepository
import com.msdragon.backend.trip.entity.Trip
import com.msdragon.backend.trip.entity.TripDay
import com.msdragon.backend.trip.entity.TripDestinationCode
import com.msdragon.backend.trip.entity.TripStatus
import com.msdragon.backend.trip.entity.TripStop
import com.msdragon.backend.trip.repository.TripDayRepository
import com.msdragon.backend.trip.repository.TripRepository
import com.msdragon.backend.trip.repository.TripStopRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest
@AutoConfigureMockMvc
@Import(TravelChatControllerTest.OpenAiTestConfig::class)
class TravelChatControllerTest {
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
	private lateinit var tripRepository: TripRepository

	@Autowired
	private lateinit var tripDayRepository: TripDayRepository

	@Autowired
	private lateinit var tripStopRepository: TripStopRepository

	@Autowired
	private lateinit var chatSessionRepository: ChatSessionRepository

	@Autowired
	private lateinit var chatMessageRepository: ChatMessageRepository

	@Autowired
	private lateinit var fakeOpenAiResponsesClient: FakeOpenAiResponsesClient

	@BeforeEach
	fun setUp() {
		fakeOpenAiResponsesClient.reset()
		cleanDatabase()
	}

	@AfterEach
	fun tearDown() {
		cleanDatabase()
	}

	@Test
	fun `여행 중 질문하면 사용자별 세션과 질문 답변을 저장한다`() {
		val child = saveUser("child-1")
		val tripId = createTravelModeTrip(child)
		val token = tokenService.createAccessToken(child)

		mockMvc.perform(
			post("/api/v1/trips/$tripId/chat/messages")
				.header("Authorization", "Bearer $token")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"message":"오늘 첫 장소가 어디야?"}"""),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.status").value(200))
			.andExpect(jsonPath("$.data.sessionId").isNumber)
			.andExpect(jsonPath("$.data.userMessage.sender").value("user"))
			.andExpect(jsonPath("$.data.userMessage.content").value("오늘 첫 장소가 어디야?"))
			.andExpect(jsonPath("$.data.assistantMessage.sender").value("assistant"))
			.andExpect(jsonPath("$.data.assistantMessage.content").value("첫 장소는 첨성대입니다."))

		mockMvc.perform(
			get("/api/v1/trips/$tripId/chat/messages")
				.header("Authorization", "Bearer $token"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.data.messages.length()").value(2))
			.andExpect(jsonPath("$.data.messages[0].sender").value("user"))
			.andExpect(jsonPath("$.data.messages[1].sender").value("assistant"))

		assertEquals(1, fakeOpenAiResponsesClient.requests.size)
		val aiRequest = fakeOpenAiResponsesClient.requests.single()
		assertTrue(aiRequest.context.contains("경주 가족 여행"))
		assertTrue(aiRequest.context.contains("첨성대"))
		assertEquals(ChatSender.USER, aiRequest.messages.single().role)
		assertEquals(64, aiRequest.safetyIdentifier.length)
		assertEquals(
			listOf("get_trip_schedule", "get_place_detail", "find_nearby_facilities"),
			aiRequest.tools.map { it.name },
		)
	}

	@Test
	fun `다음 질문에는 최근 대화 이력을 함께 전달한다`() {
		val child = saveUser("child-1")
		val tripId = createTravelModeTrip(child)
		val token = tokenService.createAccessToken(child)

		sendMessage(tripId, token, "첫 장소는 어디야?")
		sendMessage(tripId, token, "그 다음은 어디야?")

		assertEquals(2, fakeOpenAiResponsesClient.requests.size)
		val secondRequest = fakeOpenAiResponsesClient.requests[1]
		assertEquals(listOf(ChatSender.USER, ChatSender.ASSISTANT, ChatSender.USER), secondRequest.messages.map { it.role })
		assertEquals("그 다음은 어디야?", secondRequest.messages.last().content)
	}

	@Test
	fun `여행 시작 전에는 챗봇을 사용할 수 없다`() {
		val child = saveUser("child-1")
		val tripId = createTrip(child, LocalDate.now(SEOUL_ZONE).plusDays(1))

		mockMvc.perform(
			post("/api/v1/trips/$tripId/chat/messages")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(child)}")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"message":"내일 일정 알려줘"}"""),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.success").value(false))

		assertTrue(fakeOpenAiResponsesClient.requests.isEmpty())
	}

	@Test
	fun `OpenAI 호출 실패는 실제 HTTP 500이고 질문도 저장하지 않는다`() {
		val child = saveUser("child-1")
		val tripId = createTravelModeTrip(child)
		fakeOpenAiResponsesClient.failure = InternalServerException("AI 답변 생성에 실패했습니다.")

		mockMvc.perform(
			post("/api/v1/trips/$tripId/chat/messages")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(child)}")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"message":"오늘 일정 알려줘"}"""),
		)
			.andExpect(status().isInternalServerError)
			.andExpect(jsonPath("$.status").value(500))

		assertEquals(0, chatSessionRepository.count())
		assertEquals(0, chatMessageRepository.count())
	}

	@Test
	fun `빈 질문은 HTTP 200 본문 status 400으로 거절한다`() {
		val child = saveUser("child-1")
		val tripId = createTravelModeTrip(child)

		mockMvc.perform(
			post("/api/v1/trips/$tripId/chat/messages")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(child)}")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"message":"   "}"""),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.message").value("질문을 입력해 주세요."))
	}

	@Test
	fun `현재 위치는 위도와 경도를 함께 입력해야 한다`() {
		val child = saveUser("child-1")
		val tripId = createTravelModeTrip(child)

		mockMvc.perform(
			post("/api/v1/trips/$tripId/chat/messages")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(child)}")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"message":"가까운 화장실 알려줘","latitude":35.1587}"""),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.message").value("현재 위치는 latitude와 longitude를 함께 입력해 주세요."))

		assertTrue(fakeOpenAiResponsesClient.requests.isEmpty())
	}

	private fun sendMessage(tripId: Long, token: String, message: String) {
		mockMvc.perform(
			post("/api/v1/trips/$tripId/chat/messages")
				.header("Authorization", "Bearer $token")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"message":"$message"}"""),
		)
			.andExpect(status().isOk)
	}

	private fun saveUser(subject: String): User =
		userRepository.save(
			User(
				role = UserRole.CHILD,
				oauthProvider = OAuthProvider.KAKAO,
				oauthSubject = subject,
				displayName = "혜린",
				signupCompletedAt = LocalDateTime.now(),
			),
		)

	private fun createTravelModeTrip(child: User): Long = createTrip(child, LocalDate.now(SEOUL_ZONE))

	private fun createTrip(child: User, travelDate: LocalDate): Long {
		val family = familyRepository.save(Family(ownerUser = child))
		familyMemberRepository.save(FamilyMember(family = family, user = child, memberRole = UserRole.CHILD))
		val trip = tripRepository.save(
			Trip(
				family = family,
				createdByUser = child,
				destinationCode = TripDestinationCode.GYEONGJU,
				title = "경주 가족 여행",
				startDate = travelDate,
				endDate = travelDate,
				status = TripStatus.PLANNING,
			),
		)
		val day = tripDayRepository.save(TripDay(trip = trip, dayNumber = 1, travelDate = travelDate))
		tripStopRepository.save(
			TripStop(
				tripDay = day,
				sortOrder = 1,
				name = "첨성대",
				category = "역사 관광지",
				address = "경상북도 경주시 첨성로 140-25",
				overview = "신라 시대의 천문 관측 시설입니다.",
			),
		)
		return requireNotNull(trip.id)
	}

	private fun cleanDatabase() {
		chatMessageRepository.deleteAll()
		chatSessionRepository.deleteAll()
		tripStopRepository.deleteAll()
		tripDayRepository.deleteAll()
		tripRepository.deleteAll()
		familyMemberRepository.deleteAll()
		familyRepository.deleteAll()
		userRefreshTokenRepository.deleteAll()
		userRepository.deleteAll()
	}

	@TestConfiguration
	class OpenAiTestConfig {
		@Bean
		@Primary
		fun fakeOpenAiResponsesClient(): FakeOpenAiResponsesClient = FakeOpenAiResponsesClient()
	}

	class FakeOpenAiResponsesClient : OpenAiResponsesClient {
		val requests = mutableListOf<OpenAiChatRequest>()
		var failure: RuntimeException? = null

		override fun generate(
			request: OpenAiChatRequest,
			toolExecutor: (OpenAiToolCall) -> String,
		): OpenAiChatResult {
			requests += request
			failure?.let { throw it }
			return OpenAiChatResult(
				responseId = "resp_test",
				content = "첫 장소는 첨성대입니다.",
				usage = mapOf("input_tokens" to 10, "output_tokens" to 5),
			)
		}

		fun reset() {
			requests.clear()
			failure = null
		}
	}

	companion object {
		private val SEOUL_ZONE: ZoneId = ZoneId.of("Asia/Seoul")
	}
}
