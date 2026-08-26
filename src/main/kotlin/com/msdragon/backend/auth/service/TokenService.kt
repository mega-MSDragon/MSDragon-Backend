package com.msdragon.backend.auth.service

import com.msdragon.backend.auth.config.AuthProperties
import com.msdragon.backend.auth.entity.OAuthProvider
import com.msdragon.backend.auth.entity.User
import com.msdragon.backend.common.exception.InternalServerException
import com.msdragon.backend.common.exception.UnAuthorizedException
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.MACSigner
import com.nimbusds.jose.crypto.MACVerifier
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Duration
import java.time.Instant
import java.util.Base64
import java.util.Date

@Service
class TokenService(
	private val authProperties: AuthProperties,
) {
	private val random = SecureRandom()
	private val secretBytes: ByteArray by lazy {
		authProperties.jwt.secret.toByteArray(Charsets.UTF_8).also {
			if (it.size < 32) {
				throw InternalServerException("JWT secret은 32바이트 이상이어야 합니다.")
			}
		}
	}

	fun createAccessToken(user: User): String {
		val userId = user.id ?: throw InternalServerException("사용자 식별자가 없습니다.")
		return createJwt(
			subject = userId.toString(),
			expiresIn = authProperties.jwt.accessTokenExpiration,
			claims = mapOf(
				"token_type" to ACCESS_TOKEN_TYPE,
				"role" to user.role.value,
			),
		)
	}

	/**
	 * 신규 가입자는 아직 users row가 없어 provider refresh token을 저장할 곳이 없다.
	 * 가입 완료까지 서버 서명 토큰에 실어 전달한다.
	 *
	 * ponytail: signup token은 서명만 하고 암호화하지 않으므로 클라이언트가 claim을 읽을 수 있다.
	 * 앱은 이미 같은 authorizationCode를 가지고 있었고 토큰 수명이 30분이라 MVP에서는 감수한다.
	 * 서버 보관이 필요해지면 미가입자용 임시 저장소를 도입한다.
	 */
	fun createSignupToken(oAuthUserInfo: OAuthUserInfo, oauthRefreshToken: String? = null): String =
		createJwt(
			subject = oAuthUserInfo.subject,
			expiresIn = authProperties.jwt.signupTokenExpiration,
			claims = mapOf(
				"token_type" to SIGNUP_TOKEN_TYPE,
				"provider" to oAuthUserInfo.provider.value,
				"display_name" to oAuthUserInfo.displayName,
				"oauth_refresh_token" to oauthRefreshToken,
			),
		)

	fun parseAccessToken(token: String): AccessTokenClaims {
		val claims = parseJwt(token, ACCESS_TOKEN_TYPE)
		val userId = claims.subject.toLongOrNull() ?: throw UnAuthorizedException("인증 토큰의 사용자 식별자가 올바르지 않습니다.")
		return AccessTokenClaims(userId = userId)
	}

	fun parseSignupToken(token: String): SignupTokenClaims {
		val claims = parseJwt(token, SIGNUP_TOKEN_TYPE)
		val providerValue = claims.getStringClaim("provider")
			?: throw UnAuthorizedException("회원가입 토큰의 provider가 없습니다.")
		return SignupTokenClaims(
			provider = OAuthProvider.from(providerValue),
			subject = claims.subject,
			displayName = claims.getStringClaim("display_name"),
			oauthRefreshToken = claims.getStringClaim("oauth_refresh_token"),
		)
	}

	fun generateRefreshToken(): String {
		val bytes = ByteArray(32)
		random.nextBytes(bytes)
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
	}

	fun hashRefreshToken(refreshToken: String): String {
		val digest = MessageDigest.getInstance("SHA-256").digest(refreshToken.toByteArray(Charsets.UTF_8))
		return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
	}

	fun accessTokenExpiresInSeconds(): Long = authProperties.jwt.accessTokenExpiration.seconds

	fun refreshTokenExpiresInSeconds(): Long = authProperties.jwt.refreshTokenExpiration.seconds

	fun refreshTokenExpiration(): Duration = authProperties.jwt.refreshTokenExpiration

	private fun createJwt(
		subject: String,
		expiresIn: Duration,
		claims: Map<String, Any?>,
	): String {
		val now = Instant.now()
		val builder = JWTClaimsSet.Builder()
			.issuer(authProperties.jwt.issuer)
			.subject(subject)
			.issueTime(Date.from(now))
			.expirationTime(Date.from(now.plus(expiresIn)))

		claims.forEach { (key, value) ->
			if (value != null) {
				builder.claim(key, value)
			}
		}

		return SignedJWT(JWSHeader(JWSAlgorithm.HS256), builder.build())
			.apply { sign(MACSigner(secretBytes)) }
			.serialize()
	}

	private fun parseJwt(token: String, expectedType: String): JWTClaimsSet {
		val signedJwt = try {
			SignedJWT.parse(token)
		} catch (_: Exception) {
			throw UnAuthorizedException("인증 토큰 형식이 올바르지 않습니다.")
		}

		val verified = try {
			signedJwt.verify(MACVerifier(secretBytes))
		} catch (_: Exception) {
			false
		}
		if (!verified) {
			throw UnAuthorizedException("인증 토큰 서명이 올바르지 않습니다.")
		}

		val claims = signedJwt.jwtClaimsSet
		if (claims.issuer != authProperties.jwt.issuer) {
			throw UnAuthorizedException("인증 토큰 발급자가 올바르지 않습니다.")
		}
		if (claims.expirationTime.before(Date.from(Instant.now()))) {
			throw UnAuthorizedException("인증 토큰이 만료되었습니다.")
		}
		if (claims.getStringClaim("token_type") != expectedType) {
			throw UnAuthorizedException("인증 토큰 유형이 올바르지 않습니다.")
		}
		return claims
	}

	companion object {
		private const val ACCESS_TOKEN_TYPE = "access"
		private const val SIGNUP_TOKEN_TYPE = "signup"
	}
}

data class AccessTokenClaims(
	val userId: Long,
)

data class SignupTokenClaims(
	val provider: OAuthProvider,
	val subject: String,
	val displayName: String?,
	val oauthRefreshToken: String? = null,
)
