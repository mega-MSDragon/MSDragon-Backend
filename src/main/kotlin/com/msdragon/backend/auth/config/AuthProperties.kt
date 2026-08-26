package com.msdragon.backend.auth.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "app.auth")
data class AuthProperties(
	val jwt: Jwt = Jwt(),
	val oauth: OAuth = OAuth(),
) {
	data class Jwt(
		val issuer: String = "msdragon",
		val secret: String = "local-dev-secret-for-msdragon-auth-token-must-be-over-32-bytes",
		val accessTokenExpiration: Duration = Duration.ofDays(365),
		val refreshTokenExpiration: Duration = Duration.ofDays(14),
		val signupTokenExpiration: Duration = Duration.ofMinutes(30),
	)

	data class OAuth(
		val kakao: Kakao = Kakao(),
		val apple: Apple = Apple(),
		val requestTimeout: Duration = Duration.ofSeconds(5),
	)

	data class Kakao(
		val userInfoUri: String = "https://kapi.kakao.com/v2/user/me",
		val unlinkUri: String = "https://kapi.kakao.com/v1/user/unlink",
		/** 카카오 연결 끊기용 어드민 키. 비어 있으면 탈퇴 시 연결 끊기를 건너뛴다. */
		val adminKey: String = "",
	)

	data class Apple(
		val issuer: String = "https://appleid.apple.com",
		val jwksUri: String = "https://appleid.apple.com/auth/keys",
		val clientId: String = "",
		val tokenUri: String = "https://appleid.apple.com/auth/token",
		val revokeUri: String = "https://appleid.apple.com/auth/revoke",
		/** Apple Developer 팀 ID. client_secret JWT의 iss. */
		val teamId: String = "",
		/** Sign in with Apple 키의 Key ID. client_secret JWT의 kid. */
		val keyId: String = "",
		/** Sign in with Apple .p8 개인키. PEM 헤더와 개행 포함 여부는 상관없다. */
		val privateKey: String = "",
	) {
		/** revoke에 필요한 설정이 모두 있는지. 하나라도 비면 탈퇴 시 revoke를 건너뛴다. */
		fun isRevokeConfigured(): Boolean =
			clientId.isNotBlank() && teamId.isNotBlank() && keyId.isNotBlank() && privateKey.isNotBlank()
	}
}
