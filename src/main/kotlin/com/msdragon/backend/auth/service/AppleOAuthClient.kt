package com.msdragon.backend.auth.service

import com.msdragon.backend.auth.config.AuthProperties
import com.msdragon.backend.auth.entity.OAuthProvider
import com.msdragon.backend.common.exception.InternalServerException
import com.msdragon.backend.common.exception.UnAuthorizedException
import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.ECDSASigner
import com.nimbusds.jose.jwk.source.JWKSourceBuilder
import com.nimbusds.jose.proc.JWSVerificationKeySelector
import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jose.util.JSONObjectUtils
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import com.nimbusds.jwt.proc.DefaultJWTProcessor
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.security.KeyFactory
import java.security.interfaces.ECPrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.time.Instant
import java.util.Base64
import java.util.Date

@Component
class AppleOAuthClient(
	private val authProperties: AuthProperties,
) : OAuthClient {
	private val httpClient: HttpClient = HttpClient.newBuilder()
		.connectTimeout(authProperties.oauth.requestTimeout)
		.build()

	override fun verify(token: String): OAuthUserInfo {
		val apple = authProperties.oauth.apple
		if (apple.clientId.isBlank()) {
			throw InternalServerException("Apple 로그인 client_id 설정이 완료되지 않았습니다.")
		}

		val claims = try {
			val processor = DefaultJWTProcessor<SecurityContext>()
			val jwkSource = JWKSourceBuilder.create<SecurityContext>(URI(apple.jwksUri).toURL()).build()
			processor.jwsKeySelector = JWSVerificationKeySelector(JWSAlgorithm.RS256, jwkSource)
			processor.process(token, null)
		} catch (_: Exception) {
			throw UnAuthorizedException("애플 로그인 토큰이 유효하지 않습니다.")
		}

		if (claims.issuer != apple.issuer) {
			throw UnAuthorizedException("애플 로그인 토큰 발급자가 올바르지 않습니다.")
		}
		if (!claims.audience.contains(apple.clientId)) {
			throw UnAuthorizedException("애플 로그인 토큰 대상이 올바르지 않습니다.")
		}

		val subject = claims.subject ?: throw UnAuthorizedException("애플 사용자 식별자를 확인할 수 없습니다.")
		val displayName = claims.getStringClaim("name") ?: claims.getStringClaim("email")

		return OAuthUserInfo(
			provider = OAuthProvider.APPLE,
			subject = subject,
			displayName = displayName,
		)
	}

	/**
	 * authorizationCode는 일회용이며 발급 후 5분 안에만 유효하므로 로그인 시점에 교환해 둔다.
	 * 탈퇴 시점에는 코드를 다시 받을 수 없다.
	 */
	override fun exchangeRefreshToken(authorizationCode: String): String? {
		val apple = authProperties.oauth.apple
		if (!apple.isRevokeConfigured()) {
			log.warn("Apple revoke 설정이 없어 authorizationCode 교환을 건너뜁니다.")
			return null
		}

		val response = postForm(
			uri = apple.tokenUri,
			form = mapOf(
				"grant_type" to "authorization_code",
				"code" to authorizationCode,
				"client_id" to apple.clientId,
				"client_secret" to createClientSecret(),
			),
		)
		if (response.statusCode() !in 200..299) {
			throw InternalServerException("애플 authorizationCode 교환에 실패했습니다. status=${response.statusCode()}")
		}

		val refreshToken = JSONObjectUtils.parse(response.body())["refresh_token"] as? String
		if (refreshToken.isNullOrBlank()) {
			throw InternalServerException("애플 응답에 refresh_token이 없습니다.")
		}
		return refreshToken
	}

	override fun unlink(target: OAuthUnlinkTarget) {
		val apple = authProperties.oauth.apple
		if (!apple.isRevokeConfigured()) {
			log.warn("Apple revoke 설정이 없어 연결 해제를 건너뜁니다.")
			return
		}
		if (target.oauthRefreshToken.isNullOrBlank()) {
			log.warn("저장된 애플 refresh token이 없어 연결 해제를 건너뜁니다. subject={}", target.subject)
			return
		}

		val response = postForm(
			uri = apple.revokeUri,
			form = mapOf(
				"client_id" to apple.clientId,
				"client_secret" to createClientSecret(),
				"token" to target.oauthRefreshToken,
				"token_type_hint" to "refresh_token",
			),
		)
		if (response.statusCode() !in 200..299) {
			throw InternalServerException("애플 연결 해제에 실패했습니다. status=${response.statusCode()}")
		}
	}

	/**
	 * Apple이 요구하는 client_secret. 팀 키로 서명한 ES256 JWT다.
	 * 최대 6개월까지 허용되지만 캐시 없이 매번 짧게 만든다. 서명 비용은 무시할 수준이다.
	 */
	internal fun createClientSecret(): String {
		val apple = authProperties.oauth.apple
		val now = Instant.now()
		val claims = JWTClaimsSet.Builder()
			.issuer(apple.teamId)
			.subject(apple.clientId)
			.audience(APPLE_AUDIENCE)
			.issueTime(Date.from(now))
			.expirationTime(Date.from(now.plusSeconds(CLIENT_SECRET_EXPIRES_IN_SECONDS)))
			.build()
		val header = JWSHeader.Builder(JWSAlgorithm.ES256)
			.keyID(apple.keyId)
			.type(JOSEObjectType.JWT)
			.build()

		return try {
			SignedJWT(header, claims).apply { sign(ECDSASigner(parsePrivateKey(apple.privateKey))) }.serialize()
		} catch (e: Exception) {
			throw InternalServerException("애플 client_secret 생성에 실패했습니다. 키 설정을 확인해주세요.")
		}
	}

	/** .p8 파일은 PKCS#8 PEM이다. 헤더와 모든 공백을 제거하면 개행 형태와 무관하게 읽을 수 있다. */
	private fun parsePrivateKey(privateKey: String): ECPrivateKey {
		val base64 = privateKey
			.replace("-----BEGIN PRIVATE KEY-----", "")
			.replace("-----END PRIVATE KEY-----", "")
			.replace("\\n", "")
			.filterNot { it.isWhitespace() }
		val keySpec = PKCS8EncodedKeySpec(Base64.getDecoder().decode(base64))
		return KeyFactory.getInstance("EC").generatePrivate(keySpec) as ECPrivateKey
	}

	private fun postForm(uri: String, form: Map<String, String>): HttpResponse<String> {
		val body = form.entries.joinToString("&") { (key, value) ->
			"${encode(key)}=${encode(value)}"
		}
		val request = HttpRequest.newBuilder(URI.create(uri))
			.header("Content-Type", "application/x-www-form-urlencoded")
			.timeout(authProperties.oauth.requestTimeout)
			.POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
			.build()
		return httpClient.send(request, HttpResponse.BodyHandlers.ofString())
	}

	private fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8)

	companion object {
		private val log = LoggerFactory.getLogger(AppleOAuthClient::class.java)
		private const val APPLE_AUDIENCE = "https://appleid.apple.com"
		private const val CLIENT_SECRET_EXPIRES_IN_SECONDS = 300L
	}
}
