package com.msdragon.backend.auth.dto

import com.msdragon.backend.auth.entity.AgeBand
import com.msdragon.backend.auth.entity.DevicePlatform
import com.msdragon.backend.auth.entity.GenderType
import com.msdragon.backend.auth.entity.OAuthProvider
import com.msdragon.backend.auth.entity.UserRole
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

@Schema(description = "소셜 로그인 요청")
data class SocialLoginRequest(
	@field:Schema(description = "소셜 로그인 provider. 카카오는 accessToken, 애플은 identityToken을 token에 전달합니다.", example = "kakao", allowableValues = ["kakao", "apple"])
	@field:NotNull(message = "provider를 입력해주세요.")
	val provider: OAuthProvider,

	@field:Schema(description = "카카오 accessToken 또는 애플 identityToken", example = "social-token")
	@field:NotBlank(message = "소셜 로그인 토큰을 입력해주세요.")
	val token: String,

	@field:Schema(description = "요청이 발생한 앱 플랫폼. 통계/디버깅용 선택 값입니다.", example = "ios", allowableValues = ["ios", "android", "web"], nullable = true)
	val platform: DevicePlatform? = null,
)

@Schema(description = "회원가입 완료 요청")
data class CompleteSignupRequest(
	@field:Schema(description = "소셜 로그인 후 발급받은 회원가입 토큰", example = "signup-token")
	@field:NotBlank(message = "회원가입 토큰을 입력해주세요.")
	val signupToken: String,

	@field:Schema(description = "사용자 역할", example = "child", allowableValues = ["child", "parent"])
	@field:NotNull(message = "역할을 선택해주세요.")
	val role: UserRole,

	@field:Schema(description = "이름 또는 닉네임", example = "최혜린")
	@field:NotBlank(message = "이름을 입력해주세요.")
	@field:Size(max = 50, message = "이름은 50자 이하로 입력해주세요.")
	val displayName: String,

	@field:Schema(description = "연령대. 역할별 허용 범위가 다르며 서버에서 검증합니다.", example = "20s", allowableValues = ["10s", "20s", "30s", "40s", "50s", "60s", "60s_plus", "70s", "80s", "90s_plus", "undisclosed"])
	@field:NotNull(message = "연령대를 선택해주세요.")
	val ageBand: AgeBand,

	@field:Schema(description = "성별. 생략하면 undisclosed로 저장합니다.", example = "female", allowableValues = ["female", "male", "undisclosed"], nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
	val gender: GenderType? = null,

	@field:Schema(description = "개인정보 수집 및 이용 필수 약관 동의 여부. 반드시 true여야 합니다.", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
	@field:AssertTrue(message = "개인정보 수집 및 이용에 동의해주세요.")
	val privacyConsentAgreed: Boolean,

	@field:Schema(description = "위치 기반 편의시설 안내 선택 약관 동의 여부", example = "false", defaultValue = "false", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
	val locationBasedFacilityConsentAgreed: Boolean = false,

	@field:Schema(description = "요청이 발생한 앱 플랫폼. 통계/디버깅용 선택 값입니다.", example = "ios", allowableValues = ["ios", "android", "web"], nullable = true)
	val platform: DevicePlatform? = null,
)

@Schema(description = "토큰 재발급 요청")
data class RefreshTokenRequest(
	@field:Schema(description = "Refresh Token", example = "refresh-token")
	@field:NotBlank(message = "refresh token을 입력해주세요.")
	val refreshToken: String,
)

@Schema(description = "로그아웃 요청")
data class LogoutRequest(
	@field:Schema(description = "폐기할 Refresh Token", example = "refresh-token")
	@field:NotBlank(message = "refresh token을 입력해주세요.")
	val refreshToken: String,
)
