package com.msdragon.backend.notification.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.notification")
data class NotificationProperties(
	/**
	 * Firebase 서비스 계정 키(JSON) 원문. 비어 있으면 푸시 발송을 건너뛴다.
	 * 키가 없어도 앱이 기동하고 알림을 유발하는 API가 정상 동작해야 한다.
	 */
	val firebaseCredentials: String = "",
) {
	fun isConfigured(): Boolean = firebaseCredentials.isNotBlank()
}
