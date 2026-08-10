package com.msdragon.backend.auth.repository

import com.msdragon.backend.auth.entity.UserRefreshToken
import org.springframework.data.jpa.repository.JpaRepository

interface UserRefreshTokenRepository : JpaRepository<UserRefreshToken, Long> {
	fun findByRefreshTokenHash(refreshTokenHash: String): UserRefreshToken?

	fun findAllByUserIdAndRevokedAtIsNull(userId: Long): List<UserRefreshToken>
}
