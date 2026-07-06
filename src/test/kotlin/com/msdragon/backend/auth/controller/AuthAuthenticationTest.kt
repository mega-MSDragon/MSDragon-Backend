package com.msdragon.backend.auth.controller

import com.msdragon.backend.auth.entity.AgeBand
import com.msdragon.backend.auth.entity.GenderType
import com.msdragon.backend.auth.entity.OAuthProvider
import com.msdragon.backend.auth.entity.User
import com.msdragon.backend.auth.entity.UserRole
import com.msdragon.backend.auth.repository.UserRefreshTokenRepository
import com.msdragon.backend.auth.repository.UserRepository
import com.msdragon.backend.auth.service.TokenService
import com.msdragon.backend.auth.support.AuthenticatedUser
import com.msdragon.backend.auth.support.CurrentUser
import com.msdragon.backend.common.response.ApiResponse
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
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@SpringBootTest
@AutoConfigureMockMvc
@Import(AuthAuthenticationTest.TestControllerConfig::class)
class AuthAuthenticationTest {
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
	fun `Bearer access token으로 보호 API에 접근한다`() {
		val user = saveUser()
		val accessToken = tokenService.createAccessToken(user)

		mockMvc.perform(
			get("/api/v1/probe/current-user")
				.header("Authorization", "Bearer $accessToken"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.data.id").value(requireNotNull(user.id).toInt()))
			.andExpect(jsonPath("$.data.role").value("child"))
	}

	@Test
	fun `Authorization 헤더가 없으면 보호 API 접근을 거절한다`() {
		mockMvc.perform(get("/api/v1/probe/current-user"))
			.andExpect(status().isUnauthorized)
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.message").value("Authorization 헤더가 필요합니다."))
	}

	@Test
	fun `Bearer 형식이 아니면 보호 API 접근을 거절한다`() {
		mockMvc.perform(
			get("/api/v1/probe/current-user")
				.header("Authorization", "Basic invalid"),
		)
			.andExpect(status().isUnauthorized)
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.message").value("Bearer 토큰 형식이 올바르지 않습니다."))
	}

	private fun saveUser(): User =
		userRepository.save(
			User(
				role = UserRole.CHILD,
				oauthProvider = OAuthProvider.KAKAO,
				oauthSubject = "12345",
				displayName = "최혜린",
				ageBand = AgeBand.AGE_20S,
				gender = GenderType.FEMALE,
				signupCompletedAt = LocalDateTime.now(),
			),
		)

	@TestConfiguration
	class TestControllerConfig {
		@Bean
		fun authProbeController(): AuthProbeController = AuthProbeController()
	}
}

@RestController
class AuthProbeController {
	@GetMapping("/api/v1/probe/current-user")
	fun currentUser(
		@CurrentUser currentUser: AuthenticatedUser,
	): ApiResponse<AuthProbeResponse> =
		ApiResponse.success(data = AuthProbeResponse.from(currentUser))
}

data class AuthProbeResponse(
	val id: Long,
	val role: String,
) {
	companion object {
		fun from(currentUser: AuthenticatedUser): AuthProbeResponse =
			AuthProbeResponse(
				id = currentUser.id,
				role = currentUser.role.value,
			)
	}
}
