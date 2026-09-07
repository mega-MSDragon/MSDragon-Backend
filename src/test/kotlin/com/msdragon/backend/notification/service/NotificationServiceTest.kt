package com.msdragon.backend.notification.service

import com.msdragon.backend.auth.entity.AgeBand
import com.msdragon.backend.auth.entity.DevicePlatform
import com.msdragon.backend.auth.entity.GenderType
import com.msdragon.backend.auth.entity.OAuthProvider
import com.msdragon.backend.auth.entity.User
import com.msdragon.backend.auth.entity.UserRole
import com.msdragon.backend.auth.repository.UserRepository
import com.msdragon.backend.notification.repository.UserDeviceTokenRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.boot.test.context.TestConfiguration
import java.time.LocalDateTime

@SpringBootTest
@Import(NotificationServiceTest.PushSenderTestConfig::class)
class NotificationServiceTest {
	@Autowired
	private lateinit var notificationService: NotificationService

	@Autowired
	private lateinit var userRepository: UserRepository

	@Autowired
	private lateinit var userDeviceTokenRepository: UserDeviceTokenRepository

	@Autowired
	private lateinit var pushSender: RecordingPushSender

	@Test
	fun `알림을 끈 사용자에게는 보내지 않는다`() {
		pushSender.reset()
		val enabled = saveUser("notify-on", notificationEnabled = true)
		val disabled = saveUser("notify-off", notificationEnabled = false)
		registerToken(enabled, "token-on")
		registerToken(disabled, "token-off")

		notificationService.notifyUsers(
			userIds = listOf(requireNotNull(enabled.id), requireNotNull(disabled.id)),
			title = "제목",
			body = "본문",
			data = emptyMap(),
		)

		assertThat(pushSender.sentTokens).containsExactly("token-on")
	}

	@Test
	fun `발송이 실패해도 예외를 던지지 않는다`() {
		pushSender.reset()
		pushSender.failWith = IllegalStateException("발송 실패")
		val user = saveUser("notify-fail", notificationEnabled = true)
		registerToken(user, "token-fail")

		// 알림을 유발한 API가 알림 실패 때문에 500이 되어서는 안 된다.
		notificationService.notifyUsers(
			userIds = listOf(requireNotNull(user.id)),
			title = "제목",
			body = "본문",
			data = emptyMap(),
		)

		assertThat(userDeviceTokenRepository.findByToken("token-fail")).isNotNull()
	}

	@Test
	fun `더 이상 쓸 수 없는 토큰은 정리한다`() {
		pushSender.reset()
		pushSender.invalidTokens = listOf("token-stale")
		val user = saveUser("notify-stale", notificationEnabled = true)
		registerToken(user, "token-stale")
		registerToken(user, "token-live")

		notificationService.notifyUsers(
			userIds = listOf(requireNotNull(user.id)),
			title = "제목",
			body = "본문",
			data = emptyMap(),
		)

		assertThat(userDeviceTokenRepository.findByToken("token-stale")).isNull()
		assertThat(userDeviceTokenRepository.findByToken("token-live")).isNotNull()
	}

	@Test
	fun `기기 토큰이 없으면 발송을 시도하지 않는다`() {
		pushSender.reset()
		val user = saveUser("notify-no-token", notificationEnabled = true)

		notificationService.notifyUsers(
			userIds = listOf(requireNotNull(user.id)),
			title = "제목",
			body = "본문",
			data = emptyMap(),
		)

		assertThat(pushSender.callCount).isZero()
	}

	private fun registerToken(user: User, token: String) {
		notificationService.registerDeviceToken(requireNotNull(user.id), token, DevicePlatform.IOS)
	}

	private fun saveUser(subject: String, notificationEnabled: Boolean): User =
		userRepository.save(
			User(
				role = UserRole.PARENT,
				oauthProvider = OAuthProvider.KAKAO,
				oauthSubject = subject,
				displayName = "엄마",
				ageBand = AgeBand.AGE_60S,
				gender = GenderType.FEMALE,
				notificationEnabled = notificationEnabled,
				signupCompletedAt = LocalDateTime.now(),
			),
		)

	@TestConfiguration
	class PushSenderTestConfig {
		@Bean
		@Primary
		fun recordingPushSender(): RecordingPushSender = RecordingPushSender()
	}

	class RecordingPushSender : PushSender {
		val sentTokens: MutableList<String> = mutableListOf()
		var callCount: Int = 0
		var invalidTokens: List<String> = emptyList()
		var failWith: Exception? = null

		fun reset() {
			sentTokens.clear()
			callCount = 0
			invalidTokens = emptyList()
			failWith = null
		}

		override fun send(
			tokens: List<String>,
			title: String,
			body: String,
			data: Map<String, String>,
		): PushSendResult {
			callCount++
			failWith?.let { throw it }
			sentTokens.addAll(tokens)
			return PushSendResult(invalidTokens = invalidTokens)
		}
	}
}
