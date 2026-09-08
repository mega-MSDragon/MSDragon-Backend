package com.msdragon.backend.notification.dto

import com.msdragon.backend.auth.entity.DevicePlatform
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

@Schema(description = "푸시 기기 토큰 등록 요청")
data class RegisterDeviceTokenRequest(
	@field:Schema(description = "FCM 기기 토큰", example = "fcm-device-token")
	@field:NotBlank(message = "기기 토큰을 입력해주세요.")
	val token: String,

	@field:Schema(description = "기기 플랫폼", example = "ios", allowableValues = ["ios", "android", "web"])
	@field:NotNull(message = "기기 플랫폼을 선택해주세요.")
	val platform: DevicePlatform,
)

@Schema(description = "푸시 기기 토큰 해제 요청")
data class UnregisterDeviceTokenRequest(
	@field:Schema(description = "해제할 FCM 기기 토큰", example = "fcm-device-token")
	@field:NotBlank(message = "기기 토큰을 입력해주세요.")
	val token: String,
)

@Schema(description = "테스트 알림 발송 요청. 모든 필드가 선택이며 클라이언트 라우팅 확인에 사용합니다.")
data class SendTestNotificationRequest(
	@field:Schema(
		description = "`data.type`에 넣을 값. 생략하면 `test`를 보내 앱의 '모르는 type은 홈' 처리를 확인할 수 있습니다.",
		example = "trip_feedback_request",
		nullable = true,
	)
	val type: String? = null,

	@field:Schema(
		description = "`data.tripId`에 넣을 값. 생략하면 넣지 않습니다.",
		example = "12",
		nullable = true,
	)
	val tripId: String? = null,
)
