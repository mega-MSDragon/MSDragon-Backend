package com.msdragon.backend.pledge.controller

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
import com.msdragon.backend.parentprofile.repository.ParentProfileRepository
import com.msdragon.backend.pledge.repository.PledgeItemRepository
import com.msdragon.backend.pledge.repository.PledgeSignatureRepository
import com.msdragon.backend.pledge.repository.PledgeTemplateRepository
import com.msdragon.backend.pledge.repository.TripPledgeRepository
import com.msdragon.backend.trip.entity.Trip
import com.msdragon.backend.trip.entity.TripDestinationCode
import com.msdragon.backend.trip.entity.TripParticipant
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
import java.time.LocalDate
import java.time.LocalDateTime

@SpringBootTest
@AutoConfigureMockMvc
class TripPledgeControllerTest {
	@Autowired
	private lateinit var mockMvc: MockMvc

	@Autowired
	private lateinit var objectMapper: ObjectMapper

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
	private lateinit var tripRepository: TripRepository

	@Autowired
	private lateinit var tripParticipantRepository: TripParticipantRepository

	@Autowired
	private lateinit var tripDayRepository: TripDayRepository

	@Autowired
	private lateinit var tripStopRepository: TripStopRepository

	@Autowired
	private lateinit var tripPledgeRepository: TripPledgeRepository

	@Autowired
	private lateinit var pledgeItemRepository: PledgeItemRepository

	@Autowired
	private lateinit var pledgeSignatureRepository: PledgeSignatureRepository

	@Autowired
	private lateinit var pledgeTemplateRepository: PledgeTemplateRepository

	@BeforeEach
	fun setUp() {
		cleanDatabase()
	}

	@AfterEach
	fun tearDown() {
		cleanDatabase()
	}

	private fun cleanDatabase() {
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
	fun `활성 템플릿에서 중복 없는 여행 10계명 후보 10개를 조회한다`() {
		val (child, _, trip) = createFamilyTrip()

		val response = mockMvc.perform(
			get("/api/v1/trips/${requireNotNull(trip.id)}/pledge/candidates")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(child)}"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.data.tripId").value(requireNotNull(trip.id)))
			.andExpect(jsonPath("$.data.candidates.length()").value(10))
			.andReturn()
			.response
			.contentAsString

		val candidateIds: List<Int> = JsonPath.read(response, "$.data.candidates[*].id")
		check(candidateIds.distinct().size == 10)
	}

	@Test
	fun `수정한 문구 10개를 여행별 확정본으로 저장하고 조회한다`() {
		val (child, _, trip) = createFamilyTrip()
		val templates = pledgeTemplateRepository.findAllByIsActiveTrueOrderByIdAsc().take(10)
		val items = templates.mapIndexed { index, template ->
			mapOf(
				"templateId" to requireNotNull(template.id),
				"content" to if (index == 0) "서로 재촉하지 않기" else template.content,
			)
		}

		mockMvc.perform(
			put("/api/v1/trips/${requireNotNull(trip.id)}/pledge")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(child)}")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(mapOf("items" to items))),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.data.status").value("reviewed"))
			.andExpect(jsonPath("$.data.title").value("가족 여행 10계명"))
			.andExpect(jsonPath("$.data.items.length()").value(10))
			.andExpect(jsonPath("$.data.items[0].sortOrder").value(1))
			.andExpect(jsonPath("$.data.items[0].content").value("서로 재촉하지 않기"))
			.andExpect(jsonPath("$.data.items[0].isFromTemplate").value(false))
			.andExpect(jsonPath("$.data.items[1].isFromTemplate").value(true))
			.andExpect(jsonPath("$.data.reviewedAt").isString)

		mockMvc.perform(
			get("/api/v1/trips/${requireNotNull(trip.id)}/pledge")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(child)}"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.data.items.length()").value(10))
			.andExpect(jsonPath("$.data.items[0].templateId").value(requireNotNull(templates[0].id)))
	}

	@Test
	fun `여행 10계명 항목이 10개가 아니면 저장할 수 없다`() {
		val (child, _, trip) = createFamilyTrip()
		val items = (1..9).map { index -> mapOf("content" to "약속 $index") }

		mockMvc.perform(
			put("/api/v1/trips/${requireNotNull(trip.id)}/pledge")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(child)}")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(mapOf("items" to items))),
		)
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.message").value("여행 10계명은 정확히 10개여야 합니다."))
	}

	@Test
	fun `같은 템플릿을 중복해 확정본에 저장할 수 없다`() {
		val (child, _, trip) = createFamilyTrip()
		val templateId = requireNotNull(pledgeTemplateRepository.findAllByIsActiveTrueOrderByIdAsc().first().id)
		val items = (1..10).map { index ->
			mapOf(
				"templateId" to templateId,
				"content" to "약속 $index",
			)
		}

		mockMvc.perform(
			put("/api/v1/trips/${requireNotNull(trip.id)}/pledge")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(child)}")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(mapOf("items" to items))),
		)
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.message").value("같은 여행 10계명 템플릿을 중복 사용할 수 없습니다."))
	}

	@Test
	fun `여행을 만든 자녀가 아니면 여행 10계명을 작성할 수 없다`() {
		val (_, parent, trip) = createFamilyTrip()

		mockMvc.perform(
			get("/api/v1/trips/${requireNotNull(trip.id)}/pledge/candidates")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(parent)}"),
		)
			.andExpect(status().isForbidden)
			.andExpect(jsonPath("$.message").value("여행을 만든 자녀만 여행 10계명을 작성할 수 있습니다."))
	}

	@Test
	fun `자녀가 먼저 서명하면 부모 서명 요청 상태로 변경된다`() {
		val (child, _, trip) = createFamilyTrip()
		saveReviewedPledge(child, trip)

		submitSignature(child, trip)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.data.status").value("signature_requested"))
			.andExpect(jsonPath("$.data.signatures.length()").value(1))
			.andExpect(jsonPath("$.data.signatures[0].userId").value(requireNotNull(child.id)))
			.andExpect(jsonPath("$.data.signatures[0].role").value("child"))
			.andExpect(jsonPath("$.data.signatures[0].signatureImageMimeType").value("image/png"))
			.andExpect(jsonPath("$.data.signatures[0].signatureImageBase64").value(PNG_BASE64))
			.andExpect(jsonPath("$.data.canSign").value(false))
			.andExpect(jsonPath("$.data.requestedAt").isString)
	}

	@Test
	fun `부모는 자녀보다 먼저 서명할 수 없다`() {
		val (child, parent, trip) = createFamilyTrip()
		saveReviewedPledge(child, trip)

		submitSignature(parent, trip)
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.message").value("자녀가 먼저 여행 10계명에 서명해야 합니다."))
	}

	@Test
	fun `부모 한 명이 서명하면 완료되고 모든 참여자가 같은 전체 서명을 조회한다`() {
		val (child, parent, trip) = createFamilyTrip()
		saveReviewedPledge(child, trip)
		submitSignature(child, trip).andExpect(status().isOk)

		submitSignature(parent, trip)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.data.status").value("completed"))
			.andExpect(jsonPath("$.data.signatures.length()").value(2))
			.andExpect(jsonPath("$.data.signatures[0].userId").value(requireNotNull(child.id)))
			.andExpect(jsonPath("$.data.signatures[1].userId").value(requireNotNull(parent.id)))
			.andExpect(jsonPath("$.data.completedAt").isString)

		listOf(child, parent).forEach { participant ->
			mockMvc.perform(
				get("/api/v1/trips/${requireNotNull(trip.id)}/pledge")
					.header("Authorization", "Bearer ${tokenService.createAccessToken(participant)}"),
			)
				.andExpect(status().isOk)
				.andExpect(jsonPath("$.data.signatures.length()").value(2))
				.andExpect(jsonPath("$.data.signatures[0].userId").value(requireNotNull(child.id)))
				.andExpect(jsonPath("$.data.signatures[1].userId").value(requireNotNull(parent.id)))
		}
	}

	@Test
	fun `완료 후에도 아직 서명하지 않은 다른 부모가 추가로 서명할 수 있다`() {
		val (child, firstParent, trip) = createFamilyTrip()
		val secondParent = saveUser(UserRole.PARENT, "parent-2", "아빠")
		familyMemberRepository.save(
			FamilyMember(family = trip.family, user = secondParent, memberRole = UserRole.PARENT),
		)
		tripParticipantRepository.save(TripParticipant(trip = trip, user = secondParent))
		saveReviewedPledge(child, trip)
		submitSignature(child, trip).andExpect(status().isOk)

		val completedAt: String = JsonPath.read(
			submitSignature(firstParent, trip)
				.andExpect(status().isOk)
				.andReturn()
				.response
				.contentAsString,
			"$.data.completedAt",
		)
		check(LocalDateTime.parse(completedAt).nano % 1_000 == 0)

		submitSignature(secondParent, trip)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.data.status").value("completed"))
			.andExpect(jsonPath("$.data.completedAt").value(completedAt))
			.andExpect(jsonPath("$.data.signatures.length()").value(3))
			.andExpect(jsonPath("$.data.signatures[2].userId").value(requireNotNull(secondParent.id)))
	}

	@Test
	fun `제출한 서명은 다시 저장할 수 없다`() {
		val (child, _, trip) = createFamilyTrip()
		saveReviewedPledge(child, trip)
		submitSignature(child, trip).andExpect(status().isOk)

		submitSignature(child, trip)
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.message").value("이미 여행 10계명에 서명했습니다."))
	}

	@Test
	fun `PNG가 아닌 Base64 데이터는 서명으로 저장할 수 없다`() {
		val (child, _, trip) = createFamilyTrip()
		saveReviewedPledge(child, trip)

		submitSignature(child, trip, "bm90LXBuZw==")
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.message").value("PNG 형식의 서명 이미지만 사용할 수 있습니다."))
	}

	@Test
	fun `여행에 참여하지 않은 부모는 서명할 수 없다`() {
		val (child, _, trip) = createFamilyTrip()
		val otherParent = saveUser(UserRole.PARENT, "parent-other", "다른 부모")
		saveReviewedPledge(child, trip)
		submitSignature(child, trip).andExpect(status().isOk)

		submitSignature(otherParent, trip)
			.andExpect(status().isForbidden)
			.andExpect(jsonPath("$.message").value("여행 참여자만 여행 10계명에 서명할 수 있습니다."))
	}

	private fun saveReviewedPledge(child: User, trip: Trip) {
		val items = (1..10).map { index -> mapOf("content" to "가족 약속 $index") }
		mockMvc.perform(
			put("/api/v1/trips/${requireNotNull(trip.id)}/pledge")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(child)}")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(mapOf("items" to items))),
		)
			.andExpect(status().isOk)
	}

	private fun submitSignature(user: User, trip: Trip, imageBase64: String = PNG_BASE64) =
		mockMvc.perform(
			post("/api/v1/trips/${requireNotNull(trip.id)}/pledge/signatures/me")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(user)}")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(mapOf("signatureImageBase64" to imageBase64))),
		)

	private fun createFamilyTrip(): Triple<User, User, Trip> {
		val child = saveUser(UserRole.CHILD, "child-1", "혜린")
		val parent = saveUser(UserRole.PARENT, "parent-1", "엄마")
		val family = familyRepository.save(Family(ownerUser = child))
		familyMemberRepository.save(FamilyMember(family = family, user = child, memberRole = UserRole.CHILD))
		familyMemberRepository.save(FamilyMember(family = family, user = parent, memberRole = UserRole.PARENT))
		val trip = tripRepository.save(
			Trip(
				family = family,
				createdByUser = child,
				destinationCode = TripDestinationCode.GYEONGJU,
				title = "경주 여행",
				startDate = LocalDate.now().plusDays(10),
				endDate = LocalDate.now().plusDays(10),
			),
		)
		tripParticipantRepository.save(TripParticipant(trip = trip, user = child))
		tripParticipantRepository.save(TripParticipant(trip = trip, user = parent))
		return Triple(child, parent, trip)
	}

	private fun saveUser(role: UserRole, subject: String, displayName: String): User =
		userRepository.save(
			User(
				role = role,
				oauthProvider = OAuthProvider.KAKAO,
				oauthSubject = subject,
				displayName = displayName,
				ageBand = if (role == UserRole.CHILD) AgeBand.AGE_20S else AgeBand.AGE_60S,
				gender = GenderType.UNDISCLOSED,
				signupCompletedAt = LocalDateTime.now(),
			),
		)

	companion object {
		private const val PNG_BASE64 =
			"iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII="
	}
}
