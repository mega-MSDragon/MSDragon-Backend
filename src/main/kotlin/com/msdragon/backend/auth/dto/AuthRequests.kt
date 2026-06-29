package com.msdragon.backend.auth.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "소셜 로그인 요청")
data class SocialLoginRequest(
	@field:Schema(description = "소셜 로그인 provider", example = "kakao", allowableValues = ["kakao", "apple"])
	@field:NotBlank(message = "provider를 입력해주세요.")
	val provider: String,

	@field:Schema(description = "카카오 accessToken 또는 애플 identityToken", example = "social-token")
	@field:NotBlank(message = "소셜 로그인 토큰을 입력해주세요.")
	val token: String,

	@field:Schema(description = "기기 식별자", example = "ios-device-1", nullable = true)
	val deviceId: String? = null,

	@field:Schema(description = "기기 플랫폼", example = "ios", allowableValues = ["ios", "android", "web"], nullable = true)
	val platform: String? = null,
)

@Schema(description = "회원가입 완료 요청")
data class CompleteSignupRequest(
	@field:Schema(description = "소셜 로그인 후 발급받은 회원가입 토큰", example = "signup-token")
	@field:NotBlank(message = "회원가입 토큰을 입력해주세요.")
	val signupToken: String,

	@field:Schema(description = "사용자 역할", example = "child", allowableValues = ["child", "parent"])
	@field:NotBlank(message = "역할을 선택해주세요.")
	val role: String,

	@field:Schema(description = "이름 또는 닉네임", example = "최혜린")
	@field:NotBlank(message = "이름을 입력해주세요.")
	@field:Size(max = 50, message = "이름은 50자 이하로 입력해주세요.")
	val displayName: String,

	@field:Schema(description = "연령대", example = "20s")
	@field:NotBlank(message = "연령대를 선택해주세요.")
	val ageBand: String,

	@field:Schema(description = "성별", example = "female", allowableValues = ["female", "male", "undisclosed"])
	@field:NotBlank(message = "성별을 선택해주세요.")
	val gender: String,

	@field:Schema(description = "기기 식별자", example = "ios-device-1", nullable = true)
	val deviceId: String? = null,

	@field:Schema(description = "기기 플랫폼", example = "ios", allowableValues = ["ios", "android", "web"], nullable = true)
	val platform: String? = null,
)

@Schema(description = "토큰 재발급 요청")
data class RefreshTokenRequest(
	@field:Schema(description = "Refresh Token", example = "refresh-token")
	@field:NotBlank(message = "refresh token을 입력해주세요.")
	val refreshToken: String,
)
