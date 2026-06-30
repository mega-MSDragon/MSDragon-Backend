package com.msdragon.backend.auth.support

import com.msdragon.backend.auth.entity.UserRole

data class AuthenticatedUser(
	val id: Long,
	val role: UserRole,
)
