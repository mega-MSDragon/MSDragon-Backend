package com.msdragon.backend.profile.controller

import com.msdragon.backend.auth.entity.AgeBand
import com.msdragon.backend.auth.entity.GenderType
import com.msdragon.backend.auth.entity.OAuthProvider
import com.msdragon.backend.auth.entity.User
import com.msdragon.backend.auth.entity.UserRole
import com.msdragon.backend.auth.repository.UserRefreshTokenRepository
import com.msdragon.backend.auth.repository.UserRepository
import com.msdragon.backend.auth.service.TokenService
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime

@SpringBootTest
@AutoConfigureMockMvc
class ProfileControllerTest {
	@Autowired
	private lateinit var mockMvc: MockMvc

	@Autowired
	private lateinit var tokenService: TokenService

	@Autowired
	private lateinit var userRepository: UserRepository

	@Autowired
	private lateinit var userRefreshTokenRepository: UserRefreshTokenRepository

	@Autowired
	private lateinit var familyCodeUsageRepository: FamilyCodeUsageRepository

	@Autowired
	private lateinit var familyMemberRepository: FamilyMemberRepository

	@Autowired
	private lateinit var familyCodeRepository: FamilyCodeRepository

	@Autowired
	private lateinit var familyRepository: FamilyRepository

	@Autowired
	private lateinit var parentProfileRepository: ParentProfileRepository

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
	fun `내 프로필을 조회한다`() {
		val child = saveUser(UserRole.CHILD, "child-1", "혜린")

		mockMvc.perform(
			get("/api/v1/users/me")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(child)}"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.data.id").value(requireNotNull(child.id).toInt()))
			.andExpect(jsonPath("$.data.role").value("child"))
			.andExpect(jsonPath("$.data.displayName").value("혜린"))
			.andExpect(jsonPath("$.data.ageBand").value("20s"))
	}

	@Test
	fun `내 프로필을 수정한다`() {
		val child = saveUser(UserRole.CHILD, "child-1", "혜린")

		mockMvc.perform(
			patch("/api/v1/users/me")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(child)}")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "displayName": "최혜린",
					  "ageBand": "30s",
					  "gender": "female"
					}
					""".trimIndent(),
				),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.data.displayName").value("최혜린"))
			.andExpect(jsonPath("$.data.ageBand").value("30s"))
			.andExpect(jsonPath("$.data.gender").value("female"))
	}

	@Test
	fun `역할에 맞지 않는 연령대로 프로필 수정을 거절한다`() {
		val parent = saveUser(UserRole.PARENT, "parent-1", "엄마")

		mockMvc.perform(
			patch("/api/v1/users/me")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(parent)}")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"ageBand":"20s"}"""),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.message").value("선택한 역할에서 사용할 수 없는 연령대입니다."))
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
