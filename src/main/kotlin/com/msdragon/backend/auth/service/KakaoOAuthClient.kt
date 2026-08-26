package com.msdragon.backend.auth.service

import com.msdragon.backend.auth.config.AuthProperties
import com.msdragon.backend.auth.entity.OAuthProvider
import com.msdragon.backend.common.exception.UnAuthorizedException
import com.msdragon.backend.common.exception.InternalServerException
import com.nimbusds.jose.util.JSONObjectUtils
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets

@Component
class KakaoOAuthClient(
	private val authProperties: AuthProperties,
) : OAuthClient {
	private val httpClient: HttpClient = HttpClient.newBuilder()
		.connectTimeout(authProperties.oauth.requestTimeout)
		.build()

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

	/**
	 * 카카오 연결 끊기. 어드민 키로 서버가 단독 처리하므로 사용자 access token을 보관하지 않아도 된다.
	 * target_id는 로그인 시 저장한 카카오 회원번호(oauth_subject)를 사용한다.
	 */
	override fun unlink(target: OAuthUnlinkTarget) {
		val kakao = authProperties.oauth.kakao
		if (kakao.adminKey.isBlank()) {
			log.warn("카카오 어드민 키 설정이 없어 연결 끊기를 건너뜁니다.")
			return
		}

		val body = "target_id_type=user_id&target_id=${URLEncoder.encode(target.subject, StandardCharsets.UTF_8)}"
		val request = HttpRequest.newBuilder(URI.create(kakao.unlinkUri))
			.header("Authorization", "KakaoAK ${kakao.adminKey}")
			.header("Content-Type", "application/x-www-form-urlencoded")
			.timeout(authProperties.oauth.requestTimeout)
			.POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
			.build()
		val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

		if (response.statusCode() !in 200..299) {
			throw InternalServerException("카카오 연결 끊기에 실패했습니다. status=${response.statusCode()}")
		}
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

	companion object {
		private val log = LoggerFactory.getLogger(KakaoOAuthClient::class.java)
	}
}
