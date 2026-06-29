package com.msdragon.backend.auth.service

import com.msdragon.backend.auth.entity.OAuthProvider
import org.springframework.stereotype.Component

@Component
class OAuthClientResolver(
	private val kakaoOAuthClient: KakaoOAuthClient,
	private val appleOAuthClient: AppleOAuthClient,
) {
	fun resolve(provider: OAuthProvider): OAuthClient =
		when (provider) {
			OAuthProvider.KAKAO -> kakaoOAuthClient
			OAuthProvider.APPLE -> appleOAuthClient
		}
}
