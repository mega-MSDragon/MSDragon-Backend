package com.msdragon.backend.auth.support

import com.msdragon.backend.auth.entity.UserRole
import com.msdragon.backend.auth.entity.DevicePlatform

data class AuthenticatedUser(
	val id: Long,
	val role: UserRole,
	val platform: DevicePlatform? = null,
)
