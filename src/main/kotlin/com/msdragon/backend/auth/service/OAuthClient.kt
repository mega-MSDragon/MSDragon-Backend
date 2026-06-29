package com.msdragon.backend.auth.service

interface OAuthClient {
	fun verify(token: String): OAuthUserInfo
}
