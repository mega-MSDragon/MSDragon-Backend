package com.msdragon.backend.auth.service

import com.msdragon.backend.auth.config.AuthProperties
import com.msdragon.backend.auth.entity.OAuthProvider
import com.msdragon.backend.common.exception.UnAuthorizedException
import com.nimbusds.jose.util.JSONObjectUtils
import org.springframework.stereotype.Component
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

@Component
class KakaoOAuthClient(
	private val authProperties: AuthProperties,
) : OAuthClient {
	private val httpClient: HttpClient = HttpClient.newHttpClient()

	override fun verify(token: String): OAuthUserInfo {
		val request = HttpRequest.newBuilder(URI.create(authProperties.oauth.kakao.userInfoUri))
			.header("Authorization", "Bearer $token")
			.GET()
			.build()
		val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

		if (response.statusCode() !in 200..299) {
			throw UnAuthorizedException("카카오 로그인 토큰이 유효하지 않습니다.")
		}

		val body = JSONObjectUtils.parse(response.body())
		val subject = body["id"]?.toString()
			?: throw UnAuthorizedException("카카오 사용자 식별자를 확인할 수 없습니다.")
		val displayName = extractDisplayName(body)

		return OAuthUserInfo(
			provider = OAuthProvider.KAKAO,
			subject = subject,
			displayName = displayName,
		)
	}

	@Suppress("UNCHECKED_CAST")
	private fun extractDisplayName(body: Map<String, Any>): String? {
		val kakaoAccount = body["kakao_account"] as? Map<String, Any>
		val profile = kakaoAccount?.get("profile") as? Map<String, Any>
		val accountNickname = profile?.get("nickname") as? String
		if (!accountNickname.isNullOrBlank()) {
			return accountNickname
		}

		val properties = body["properties"] as? Map<String, Any>
		return properties?.get("nickname") as? String
	}
}
