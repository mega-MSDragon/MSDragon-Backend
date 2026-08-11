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
	)

	data class Kakao(
		val userInfoUri: String = "https://kapi.kakao.com/v2/user/me",
	)

	data class Apple(
		val issuer: String = "https://appleid.apple.com",
		val jwksUri: String = "https://appleid.apple.com/auth/keys",
		val clientId: String = "",
	)
}
