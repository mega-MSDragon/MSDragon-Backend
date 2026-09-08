package com.msdragon.backend.notification.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "테스트 알림 발송 결과. 알림이 오지 않을 때 원인을 좁히는 데 사용합니다.")
data class TestNotificationResponse(
	@field:Schema(description = "발송을 시도한 내 기기 수. 0이면 기기 토큰을 먼저 등록해야 합니다.", example = "1")
	val deviceCount: Int,

	@field:Schema(description = "서버에 Firebase 키가 설정되어 있는지. false면 발송을 시도하지 않습니다.", example = "true")
	val pushConfigured: Boolean,

	@field:Schema(description = "내 알림 설정. false면 발송 대상에서 제외됩니다.", example = "true")
	val notificationEnabled: Boolean,

	@field:Schema(description = "실제로 발송을 시도했는지. 위 세 조건이 모두 충족되어야 true입니다.", example = "true")
	val attempted: Boolean,
)
