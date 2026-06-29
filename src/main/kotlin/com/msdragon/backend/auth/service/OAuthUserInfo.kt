package com.msdragon.backend.auth.service

import com.msdragon.backend.auth.entity.OAuthProvider

data class OAuthUserInfo(
	val provider: OAuthProvider,
	val subject: String,
	val displayName: String?,
)
