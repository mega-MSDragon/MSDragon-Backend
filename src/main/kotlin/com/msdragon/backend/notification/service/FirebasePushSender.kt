package com.msdragon.backend.notification.service

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.MessagingErrorCode
import com.google.firebase.messaging.Notification
import com.msdragon.backend.notification.config.NotificationProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

@Component
class FirebasePushSender(
	private val notificationProperties: NotificationProperties,
) : PushSender {
	/**
	 * Firebase 앱은 한 번만 초기화한다. 서비스 계정 키가 없으면 초기화하지 않아
	 * 키 없이도 애플리케이션이 기동하고 알림을 유발하는 API가 정상 동작한다.
	 */
	private val messaging: FirebaseMessaging? by lazy {
		if (!notificationProperties.isConfigured()) {
			log.warn("Firebase 서비스 계정 키 설정이 없어 푸시 발송을 건너뜁니다.")
			return@lazy null
		}
		try {
			val credentials = ByteArrayInputStream(
				notificationProperties.firebaseCredentials.toByteArray(StandardCharsets.UTF_8),
			).use(GoogleCredentials::fromStream)
			val app = FirebaseApp.getApps().firstOrNull { it.name == FirebaseApp.DEFAULT_APP_NAME }
				?: FirebaseApp.initializeApp(
					FirebaseOptions.builder().setCredentials(credentials).build(),
				)
			FirebaseMessaging.getInstance(app)
		} catch (exception: Exception) {
			log.error("Firebase 초기화에 실패했습니다. 서비스 계정 키 설정을 확인해주세요.", exception)
			null
		}
	}

	override fun isConfigured(): Boolean = notificationProperties.isConfigured()

	override fun send(
		tokens: List<String>,
		title: String,
		body: String,
		data: Map<String, String>,
	): PushSendResult {
		val messaging = messaging ?: return PushSendResult(invalidTokens = emptyList())
		val invalidTokens = mutableListOf<String>()

		tokens.distinct().forEach { token ->
			try {
				messaging.send(
					Message.builder()
						.setToken(token)
						.setNotification(Notification.builder().setTitle(title).setBody(body).build())
						.putAllData(data)
						.build(),
				)
			} catch (exception: FirebaseMessagingException) {
				// 삭제된 앱이나 만료된 토큰이다. 다음 발송에서 빼기 위해 정리 대상으로 알린다.
				if (exception.messagingErrorCode == MessagingErrorCode.UNREGISTERED ||
					exception.messagingErrorCode == MessagingErrorCode.INVALID_ARGUMENT
				) {
					invalidTokens.add(token)
				} else {
					log.warn("푸시 발송에 실패했습니다. errorCode={}", exception.messagingErrorCode, exception)
				}
			} catch (exception: Exception) {
				log.warn("푸시 발송 중 오류가 발생했습니다.", exception)
			}
		}
		return PushSendResult(invalidTokens = invalidTokens)
	}

	companion object {
		private val log = LoggerFactory.getLogger(FirebasePushSender::class.java)
	}
}
