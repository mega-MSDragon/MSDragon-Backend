package com.msdragon.backend.notification.entity

import com.msdragon.backend.auth.entity.DevicePlatform
import com.msdragon.backend.auth.entity.User
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
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime

/**
 * 푸시 발송 대상 기기. 토큰은 기기마다 하나이므로 전역 unique다.
 * 같은 기기에 다른 사용자가 로그인하면 소유자를 옮겨, 이전 사용자에게 갈 알림이 새 사용자에게 가지 않게 한다.
 */
@Entity
@Table(
	name = "user_device_tokens",
	uniqueConstraints = [
		UniqueConstraint(name = "uk_user_device_tokens_token", columnNames = ["token"]),
	],
)
class UserDeviceToken(
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	var user: User,

	@Column(name = "token", nullable = false, length = 512)
	val token: String,

	@Column(name = "platform", nullable = false, length = 20)
	var platform: DevicePlatform,

	@Column(name = "last_used_at", nullable = false)
	var lastUsedAt: LocalDateTime,
) : BaseTimeEntity() {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	var id: Long? = null
		protected set

	fun reassign(user: User, platform: DevicePlatform, usedAt: LocalDateTime) {
		this.user = user
		this.platform = platform
		this.lastUsedAt = usedAt
	}
}
