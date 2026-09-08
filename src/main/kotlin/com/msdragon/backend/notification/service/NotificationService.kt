package com.msdragon.backend.notification.service

import com.msdragon.backend.auth.entity.DevicePlatform
import com.msdragon.backend.auth.entity.User
import com.msdragon.backend.auth.repository.UserRepository
import com.msdragon.backend.common.exception.BadRequestException
import com.msdragon.backend.common.exception.UnAuthorizedException
import com.msdragon.backend.notification.dto.TestNotificationResponse
import com.msdragon.backend.notification.entity.UserDeviceToken
import com.msdragon.backend.notification.repository.UserDeviceTokenRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class NotificationService(
	private val userRepository: UserRepository,
	private val userDeviceTokenRepository: UserDeviceTokenRepository,
	private val pushSender: PushSender,
) {
	@Transactional
	fun registerDeviceToken(userId: Long, token: String, platform: DevicePlatform) {
		val user = getLoginUser(userId)
		val normalizedToken = token.trim()
		if (normalizedToken.isBlank()) {
			throw BadRequestException("기기 토큰을 입력해주세요.")
		}
		if (normalizedToken.length > MAX_TOKEN_LENGTH) {
			throw BadRequestException("기기 토큰 형식이 올바르지 않습니다.")
		}

		val now = LocalDateTime.now()
		val existing = userDeviceTokenRepository.findByToken(normalizedToken)
		if (existing != null) {
			// 같은 기기에 다른 사용자가 로그인한 경우 소유자를 옮긴다.
			existing.reassign(user, platform, now)
			return
		}
		userDeviceTokenRepository.save(
			UserDeviceToken(user = user, token = normalizedToken, platform = platform, lastUsedAt = now),
		)
	}

	@Transactional
	fun unregisterDeviceToken(userId: Long, token: String) {
		getLoginUser(userId)
		val normalizedToken = token.trim().takeIf { it.isNotEmpty() } ?: return
		// 이미 없는 토큰도 성공으로 처리한다. 로그아웃이 토큰 상태 때문에 실패해서는 안 된다.
		userDeviceTokenRepository.findByToken(normalizedToken)
			?.takeIf { it.user.id == userId }
			?.let(userDeviceTokenRepository::delete)
	}

	/**
	 * 내 기기로만 테스트 알림을 보낸다. **다른 사용자를 대상으로 지정할 수 없으므로** 남용 여지가 없다.
	 *
	 * 알림이 오지 않을 때 원인을 좁힐 수 있도록 결과를 응답에 담는다.
	 * 실제 발송 경로와 같은 형태의 페이로드를 보내 클라이언트가 라우팅을 확인할 수 있다.
	 */
	@Transactional
	fun sendTestNotification(userId: Long, type: String?, tripId: String?): TestNotificationResponse {
		val user = getLoginUser(userId)
		val deviceTokens = userDeviceTokenRepository.findAllByUserId(userId)
		val attempted = pushSender.isConfigured() && user.notificationEnabled && deviceTokens.isNotEmpty()

		if (attempted) {
			val data = buildMap {
				put("type", type?.trim()?.takeIf { it.isNotEmpty() } ?: TEST_NOTIFICATION_TYPE)
				tripId?.trim()?.takeIf { it.isNotEmpty() }?.let { put("tripId", it) }
			}
			val result = pushSender.send(
				tokens = deviceTokens.map(UserDeviceToken::token),
				title = "모셔용 테스트 알림",
				body = "이 알림이 보이면 푸시 연결이 정상이에요.",
				data = data,
			)
			if (result.invalidTokens.isNotEmpty()) {
				userDeviceTokenRepository.deleteAll(
					deviceTokens.filter { it.token in result.invalidTokens },
				)
			}
		}

		return TestNotificationResponse(
			deviceCount = deviceTokens.size,
			pushConfigured = pushSender.isConfigured(),
			notificationEnabled = user.notificationEnabled,
			attempted = attempted,
		)
	}

	/**
	 * 알림을 유발한 API를 실패시키지 않는다. 발송 실패는 로그로만 남긴다.
	 * 알림을 끈 사용자는 대상에서 제외하며, 더 이상 쓸 수 없는 토큰은 정리한다.
	 */
	@Transactional
	fun notifyUsers(userIds: List<Long>, title: String, body: String, data: Map<String, String>) {
		if (userIds.isEmpty()) {
			return
		}
		try {
			val allowedUserIds = userRepository.findAllById(userIds)
				.filter { it.deletedAt == null && it.notificationEnabled }
				.mapNotNull(User::id)
			if (allowedUserIds.isEmpty()) {
				return
			}

			val deviceTokens = userDeviceTokenRepository.findAllByUserIdIn(allowedUserIds)
			if (deviceTokens.isEmpty()) {
				return
			}

			val result = pushSender.send(deviceTokens.map(UserDeviceToken::token), title, body, data)
			if (result.invalidTokens.isNotEmpty()) {
				userDeviceTokenRepository.deleteAll(
					deviceTokens.filter { it.token in result.invalidTokens },
				)
				log.info("사용할 수 없는 기기 토큰 {}건을 정리했습니다.", result.invalidTokens.size)
			}
		} catch (exception: Exception) {
			log.error("알림 발송 처리에 실패했습니다. userIds={}", userIds, exception)
		}
	}

	private fun getLoginUser(userId: Long): User =
		userRepository.findByIdAndDeletedAtIsNull(userId)
			?.takeIf { it.isSignupCompleted() }
			?: throw UnAuthorizedException("로그인할 수 없는 사용자입니다.")

	companion object {
		private val log = LoggerFactory.getLogger(NotificationService::class.java)
		private const val MAX_TOKEN_LENGTH = 512

		/** 앱의 '모르는 type은 홈으로' 처리를 확인할 수 있는 기본 테스트 타입. */
		private const val TEST_NOTIFICATION_TYPE = "test"
	}
}
