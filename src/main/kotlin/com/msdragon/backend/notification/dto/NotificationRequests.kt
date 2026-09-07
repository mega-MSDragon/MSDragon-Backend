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
