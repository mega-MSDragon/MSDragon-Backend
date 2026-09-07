package com.msdragon.backend.notification.controller

import com.msdragon.backend.auth.entity.AgeBand
import com.msdragon.backend.auth.entity.GenderType
import com.msdragon.backend.auth.entity.OAuthProvider
import com.msdragon.backend.auth.entity.User
import com.msdragon.backend.auth.entity.UserRole
import com.msdragon.backend.auth.repository.UserRepository
import com.msdragon.backend.auth.service.TokenService
import com.msdragon.backend.notification.repository.UserDeviceTokenRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime

@SpringBootTest
@AutoConfigureMockMvc
class DeviceTokenControllerTest {
	@Autowired
	private lateinit var mockMvc: MockMvc

	@Autowired
	private lateinit var tokenService: TokenService

	@Autowired
	private lateinit var userRepository: UserRepository

	@Autowired
	private lateinit var userDeviceTokenRepository: UserDeviceTokenRepository

	@Test
	fun `기기 토큰을 등록하고 같은 토큰을 다시 보내도 한 건만 유지한다`() {
		val user = saveUser(UserRole.PARENT, "token-parent-1", "엄마")

		repeat(2) {
			mockMvc.perform(
				post("/api/v1/users/me/device-tokens")
					.header("Authorization", bearer(user))
					.contentType(MediaType.APPLICATION_JSON)
					.content("""{"token":"device-token-1","platform":"ios"}"""),
			)
				.andExpect(status().isOk)
				.andExpect(jsonPath("$.status").value(200))
		}

		assertThat(userDeviceTokenRepository.findAllByUserId(requireNotNull(user.id))).hasSize(1)
	}

	@Test
	fun `같은 기기에 다른 사용자가 로그인하면 토큰 소유자를 옮긴다`() {
		val first = saveUser(UserRole.PARENT, "token-parent-2", "엄마")
		val second = saveUser(UserRole.CHILD, "token-child-2", "혜린")

		listOf(first, second).forEach { user ->
			mockMvc.perform(
				post("/api/v1/users/me/device-tokens")
					.header("Authorization", bearer(user))
					.contentType(MediaType.APPLICATION_JSON)
					.content("""{"token":"shared-device","platform":"android"}"""),
			)
				.andExpect(status().isOk)
		}

		// 이전 사용자에게 갈 알림이 새 사용자 기기로 가면 안 된다.
		assertThat(userDeviceTokenRepository.findAllByUserId(requireNotNull(first.id))).isEmpty()
		assertThat(userDeviceTokenRepository.findAllByUserId(requireNotNull(second.id))).hasSize(1)
	}

	@Test
	fun `기기 토큰을 해제하고 없는 토큰도 성공으로 처리한다`() {
		val user = saveUser(UserRole.PARENT, "token-parent-3", "엄마")
		mockMvc.perform(
			post("/api/v1/users/me/device-tokens")
				.header("Authorization", bearer(user))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"token":"device-token-3","platform":"ios"}"""),
		)
			.andExpect(status().isOk)

		repeat(2) {
			mockMvc.perform(
				delete("/api/v1/users/me/device-tokens")
					.header("Authorization", bearer(user))
					.contentType(MediaType.APPLICATION_JSON)
					.content("""{"token":"device-token-3"}"""),
			)
				.andExpect(status().isOk)
				.andExpect(jsonPath("$.status").value(200))
		}

		assertThat(userDeviceTokenRepository.findAllByUserId(requireNotNull(user.id))).isEmpty()
	}

	@Test
	fun `기기 토큰 API는 인증이 필요하고 빈 토큰을 거절한다`() {
		val user = saveUser(UserRole.PARENT, "token-parent-4", "엄마")

		mockMvc.perform(
			post("/api/v1/users/me/device-tokens")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"token":"device-token-4","platform":"ios"}"""),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.status").value(401))

		mockMvc.perform(
			post("/api/v1/users/me/device-tokens")
				.header("Authorization", bearer(user))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"token":"   ","platform":"ios"}"""),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.status").value(400))
	}

	@Test
	fun `탈퇴하면 기기 토큰을 지운다`() {
		val user = saveUser(UserRole.PARENT, "token-parent-5", "엄마")
		mockMvc.perform(
			post("/api/v1/users/me/device-tokens")
				.header("Authorization", bearer(user))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"token":"device-token-5","platform":"ios"}"""),
		)
			.andExpect(status().isOk)

		mockMvc.perform(
			delete("/api/v1/users/me").header("Authorization", bearer(user)),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.status").value(200))

		// 토큰을 남기면 탈퇴 후에도 그 기기로 알림이 갈 수 있다.
		assertThat(userDeviceTokenRepository.findByToken("device-token-5")).isNull()
	}

	private fun bearer(user: User) = "Bearer ${tokenService.createAccessToken(user)}"

	private fun saveUser(role: UserRole, subject: String, displayName: String): User =
		userRepository.save(
			User(
				role = role,
				oauthProvider = OAuthProvider.KAKAO,
				oauthSubject = subject,
				displayName = displayName,
				ageBand = if (role == UserRole.CHILD) AgeBand.AGE_20S else AgeBand.AGE_60S,
				gender = GenderType.FEMALE,
				signupCompletedAt = LocalDateTime.now(),
			),
		)
}
