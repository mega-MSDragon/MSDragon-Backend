package com.msdragon.backend.auth.entity

import com.msdragon.backend.common.entity.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "user_refresh_tokens")
class UserRefreshToken(
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	val user: User,

	@Column(name = "refresh_token_hash", nullable = false, unique = true, length = 255)
	val refreshTokenHash: String,

	@Column(name = "platform", length = 20)
	val platform: DevicePlatform?,

	@Column(name = "issued_at", nullable = false)
	val issuedAt: LocalDateTime,

	@Column(name = "expires_at", nullable = false)
	val expiresAt: LocalDateTime,

	@Column(name = "last_used_at")
	var lastUsedAt: LocalDateTime? = null,

	@Column(name = "revoked_at")
	var revokedAt: LocalDateTime? = null,
) : BaseTimeEntity() {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	var id: Long? = null
		protected set

	fun markUsed() {
		lastUsedAt = LocalDateTime.now()
	}

	fun revoke() {
		revokedAt = LocalDateTime.now()
	}

	fun isActive(now: LocalDateTime = LocalDateTime.now()): Boolean =
		revokedAt == null && expiresAt.isAfter(now)
}
