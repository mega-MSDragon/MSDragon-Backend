package com.msdragon.backend.notification.service

/** 푸시 발송 결과. 더 이상 쓸 수 없는 토큰은 호출자가 정리한다. */
data class PushSendResult(
	val invalidTokens: List<String>,
)

interface PushSender {
	/** 발송 설정이 되어 있는지. 테스트 응답에서 발송을 시도했는지 알려주는 데 쓴다. */
	fun isConfigured(): Boolean

	/**
	 * 여러 기기에 같은 알림을 보낸다. 설정이 없으면 아무것도 보내지 않고 빈 결과를 반환한다.
	 * 발송 실패가 이 알림을 유발한 API를 실패시켜서는 안 되므로 예외를 던지지 않는다.
	 */
	fun send(tokens: List<String>, title: String, body: String, data: Map<String, String>): PushSendResult
}
