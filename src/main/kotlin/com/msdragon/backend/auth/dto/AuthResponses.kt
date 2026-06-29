package com.msdragon.backend.auth.dto

import com.msdragon.backend.auth.entity.User
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "인증 응답")
data class AuthResponse(
	@field:Schema(description = "회원가입 필요 여부", example = "false")
	val signupRequired: Boolean,

	@field:Schema(description = "회원가입 토큰. 회원가입 필요 시에만 내려갑니다.", nullable = true)
	val signupToken: String? = null,

	@field:Schema(description = "Access Token", nullable = true)
	val accessToken: String? = null,

	@field:Schema(description = "Refresh Token", nullable = true)
	val refreshToken: String? = null,

	@field:Schema(description = "토큰 타입", example = "Bearer")
	val tokenType: String = "Bearer",

	@field:Schema(description = "Access Token 만료까지 남은 초", example = "3600", nullable = true)
	val accessTokenExpiresInSeconds: Long? = null,

	@field:Schema(description = "Refresh Token 만료까지 남은 초", example = "1209600", nullable = true)
	val refreshTokenExpiresInSeconds: Long? = null,

	@field:Schema(description = "로그인 사용자 정보", nullable = true)
	val user: AuthUserResponse? = null,
)

@Schema(description = "로그인 사용자 정보")
data class AuthUserResponse(
	@field:Schema(description = "사용자 ID", example = "1")
	val id: Long,

	@field:Schema(description = "역할", example = "child")
	val role: String,

	@field:Schema(description = "이름 또는 닉네임", example = "최혜린")
	val displayName: String,

	@field:Schema(description = "연령대", example = "20s")
	val ageBand: String,

	@field:Schema(description = "성별", example = "female")
	val gender: String,

	@field:Schema(description = "회원가입 완료 여부", example = "true")
	val signupCompleted: Boolean,
) {
	companion object {
		fun from(user: User): AuthUserResponse =
			AuthUserResponse(
				id = requireNotNull(user.id),
				role = user.role.value,
				displayName = user.displayName,
				ageBand = user.ageBand.value,
				gender = user.gender.value,
				signupCompleted = user.isSignupCompleted(),
			)
	}
}
