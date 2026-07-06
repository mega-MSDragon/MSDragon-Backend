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
					  "activityLevel": "moderate",
					  "needsMobilityAssistance": true,
					  "themeCodes": ["nature", "history"],
					  "foodPreference": "korean_only",
					  "avoidSpicy": true,
					  "complete": true
					}
					""".trimIndent(),
				),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.data.profileExists").value(true))
			.andExpect(jsonPath("$.data.status").value("completed"))
			.andExpect(jsonPath("$.data.completionPercent").value(100))
			.andExpect(jsonPath("$.data.personalityType").value("history_walker"))
			.andExpect(jsonPath("$.data.themeCodes.length()").value(2))
	}

	@Test
	fun `자녀는 부모 프로필을 작성할 수 없다`() {
		val child = saveUser(UserRole.CHILD, "child-1", "혜린")

		mockMvc.perform(
			put("/api/v1/parent-profiles/me")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(child)}")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"activityLevel":"slow"}"""),
		)
			.andExpect(status().isBadRequest)
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
			.andExpect(jsonPath("$.data.foodPreference").value("familiar_food"))
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
			.andExpect(status().isForbidden)
			.andExpect(jsonPath("$.message").value("부모 프로필 조회 권한이 없습니다."))
	}

	@Test
	fun `여행 테마가 3개를 넘으면 거절한다`() {
		val parent = saveUser(UserRole.PARENT, "parent-1", "엄마")

		mockMvc.perform(
			put("/api/v1/parent-profiles/me")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(parent)}")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"themeCodes":["nature","history","activity","food"]}"""),
		)
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.message").value("여행 테마는 최대 3개까지 선택할 수 있습니다."))
	}

	private fun saveCompletedParentProfile(parent: User) {
		mockMvc.perform(
			put("/api/v1/parent-profiles/me")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(parent)}")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "activityLevel": "slow",
					  "needsMobilityAssistance": false,
					  "themeCodes": ["nature"],
					  "foodPreference": "familiar_food",
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
