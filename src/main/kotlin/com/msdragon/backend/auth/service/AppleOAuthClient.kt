package com.msdragon.backend.auth.service

import com.msdragon.backend.auth.config.AuthProperties
import com.msdragon.backend.auth.entity.OAuthProvider
import com.msdragon.backend.common.exception.InternalServerException
import com.msdragon.backend.common.exception.UnAuthorizedException
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.jwk.source.RemoteJWKSet
import com.nimbusds.jose.proc.JWSVerificationKeySelector
import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jwt.proc.DefaultJWTProcessor
import org.springframework.stereotype.Component
import java.net.URL

@Component
class AppleOAuthClient(
	private val authProperties: AuthProperties,
) : OAuthClient {
	override fun verify(token: String): OAuthUserInfo {
		val apple = authProperties.oauth.apple
		if (apple.clientId.isBlank()) {
			throw InternalServerException("Apple 로그인 client_id 설정이 완료되지 않았습니다.")
		}

		val claims = try {
			val processor = DefaultJWTProcessor<SecurityContext>()
			val jwkSource = RemoteJWKSet<SecurityContext>(URL(apple.jwksUri))
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
}
