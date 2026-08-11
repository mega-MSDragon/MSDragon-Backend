package com.msdragon.backend.parentprofile.controller

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
import com.msdragon.backend.trip.repository.TripDayRepository
import com.msdragon.backend.trip.repository.TripParticipantRepository
import com.msdragon.backend.trip.repository.TripRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime

@SpringBootTest
@AutoConfigureMockMvc
class ParentProfileControllerTest {
	@Autowired
	private lateinit var mockMvc: MockMvc

	@Autowired
	private lateinit var tokenService: TokenService

	@Autowired
	private lateinit var userRepository: UserRepository

	@Autowired
	private lateinit var userRefreshTokenRepository: UserRefreshTokenRepository

	@Autowired
	private lateinit var parentProfileRepository: ParentProfileRepository

	@Autowired
	private lateinit var familyCodeUsageRepository: FamilyCodeUsageRepository

	@Autowired
	private lateinit var familyMemberRepository: FamilyMemberRepository

	@Autowired
	private lateinit var familyCodeRepository: FamilyCodeRepository

	@Autowired
	private lateinit var familyRepository: FamilyRepository

	@Autowired
	private lateinit var tripDayRepository: TripDayRepository

	@Autowired
	private lateinit var tripParticipantRepository: TripParticipantRepository

	@Autowired
	private lateinit var tripRepository: TripRepository

	@BeforeEach
	fun setUp() {
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
	fun `부모가 본인 프로필을 작성 완료한다`() {
		val parent = saveUser(UserRole.PARENT, "parent-1", "엄마")

		mockMvc.perform(
			put("/api/v1/parent-profiles/me")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(parent)}")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "currentStep": 3,
					  "walkingPace": "normal",
					  "needsMobilityAssistance": true,
					  "travelThemes": ["nature_scenery", "history_culture"],
					  "foodPreference": "korean",
					  "complete": true
					}
					""".trimIndent(),
				),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.data.parentDisplayName").value("엄마"))
			.andExpect(jsonPath("$.data.profileExists").value(true))
			.andExpect(jsonPath("$.data.status").value("completed"))
			.andExpect(jsonPath("$.data.completionPercent").value(100))
			.andExpect(jsonPath("$.data.personalityType").value("heritage_walker"))
			.andExpect(jsonPath("$.data.personalityResult.code").value("heritage_walker"))
			.andExpect(jsonPath("$.data.personalityResult.name").value("역사 산책가형"))
			.andExpect(jsonPath("$.data.personalityResult.catchphrase").value("이야기가 있는 길을 걷는 게 좋아."))
			.andExpect(jsonPath("$.data.travelThemes.length()").value(2))
	}

	@Test
	fun `PDF 예시 입력은 유유자적 힐링러형 결과를 반환한다`() {
		val parent = saveUser(UserRole.PARENT, "parent-healing", "영희")

		mockMvc.perform(
			put("/api/v1/parent-profiles/me")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(parent)}")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "walkingPace": "slow",
					  "needsMobilityAssistance": true,
					  "travelThemes": ["nature_scenery"],
					  "foodPreference": "korean",
					  "complete": true
					}
					""".trimIndent(),
				),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.data.parentDisplayName").value("영희"))
			.andExpect(jsonPath("$.data.personalityType").value("healing_traveler"))
			.andExpect(jsonPath("$.data.personalityResult.code").value("healing_traveler"))
			.andExpect(jsonPath("$.data.personalityResult.name").value("유유자적 힐링러형"))
			.andExpect(jsonPath("$.data.personalityResult.catchphrase").value("여행은 쉬러 가는 거지."))
			.andExpect(
				jsonPath("$.data.personalityResult.description").value(
					"자연풍경, 역사, 산책을 좋아하며 천천히 둘러보는 타입이시네요. 음식도 한식처럼 편안한 선택을 선호하시는 편이에요.",
				),
			)
	}

	@Test
	fun `자녀는 부모 프로필을 작성할 수 없다`() {
		val child = saveUser(UserRole.CHILD, "child-1", "혜린")

		mockMvc.perform(
			put("/api/v1/parent-profiles/me")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(child)}")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"walkingPace":"slow"}"""),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.message").value("부모 사용자만 프로필을 작성할 수 있습니다."))
	}

	@Test
	fun `연결된 자녀는 부모 프로필을 조회한다`() {
		val child = saveUser(UserRole.CHILD, "child-1", "혜린")
		val parent = saveUser(UserRole.PARENT, "parent-1", "엄마")
		connectFamily(child, parent)
		saveCompletedParentProfile(parent)

		mockMvc.perform(
			get("/api/v1/parent-profiles/${requireNotNull(parent.id)}")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(child)}"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.data.parentUserId").value(requireNotNull(parent.id).toInt()))
			.andExpect(jsonPath("$.data.status").value("completed"))
			.andExpect(jsonPath("$.data.foodPreference").value("familiar"))
	}

	@Test
	fun `연결되지 않은 자녀의 부모 프로필 조회를 거절한다`() {
		val child = saveUser(UserRole.CHILD, "child-1", "혜린")
		val parent = saveUser(UserRole.PARENT, "parent-1", "엄마")
		saveCompletedParentProfile(parent)

		mockMvc.perform(
			get("/api/v1/parent-profiles/${requireNotNull(parent.id)}")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(child)}"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.status").value(403))
			.andExpect(jsonPath("$.message").value("부모 프로필 조회 권한이 없습니다."))
	}

	@Test
	fun `여행 테마가 3개를 넘으면 거절한다`() {
		val parent = saveUser(UserRole.PARENT, "parent-1", "엄마")

		mockMvc.perform(
			put("/api/v1/parent-profiles/me")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(parent)}")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"travelThemes":["nature_scenery","history_culture","activity","shopping"]}"""),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.message").value("여행 테마는 최대 3개까지 선택할 수 있습니다."))
	}

	@Test
	fun `프로필 완료 시 이동 도움 필요 여부는 필수다`() {
		val parent = saveUser(UserRole.PARENT, "parent-1", "엄마")

		mockMvc.perform(
			put("/api/v1/parent-profiles/me")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(parent)}")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "walkingPace": "slow",
					  "travelThemes": ["nature_scenery"],
					  "foodPreference": "korean",
					  "complete": true
					}
					""".trimIndent(),
				),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.message").value("이동 도움 필요 여부를 선택해주세요."))
	}

	private fun saveCompletedParentProfile(parent: User) {
		mockMvc.perform(
			put("/api/v1/parent-profiles/me")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(parent)}")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "walkingPace": "slow",
					  "needsMobilityAssistance": false,
					  "travelThemes": ["nature_scenery"],
					  "foodPreference": "familiar",
					  "complete": true
					}
					""".trimIndent(),
				),
		)
			.andExpect(status().isOk)
	}

	private fun connectFamily(child: User, parent: User) {
		val family = familyRepository.save(Family(ownerUser = child))
		familyMemberRepository.save(
			FamilyMember(
				family = family,
				user = child,
				memberRole = UserRole.CHILD,
			),
		)
		familyMemberRepository.save(
			FamilyMember(
				family = family,
				user = parent,
				memberRole = UserRole.PARENT,
			),
		)
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
}
