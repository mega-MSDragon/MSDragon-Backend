package com.msdragon.backend.auth.entity

import com.msdragon.backend.common.entity.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime

@Entity
@Table(
	name = "users",
	uniqueConstraints = [
		UniqueConstraint(name = "uk_users_oauth", columnNames = ["oauth_provider", "oauth_subject"]),
	],
)
class User(
	@Column(name = "role", nullable = false, length = 20)
	var role: UserRole,

	@Column(name = "oauth_provider", nullable = false, length = 20)
	val oauthProvider: OAuthProvider,

	@Column(name = "oauth_subject", nullable = false, length = 255)
	val oauthSubject: String,

	@Column(name = "display_name", nullable = false, length = 50)
	var displayName: String,

	@Column(name = "age_band", nullable = false, length = 20)
	var ageBand: AgeBand = AgeBand.UNDISCLOSED,

	@Column(name = "gender", nullable = false, length = 20)
	var gender: GenderType = GenderType.UNDISCLOSED,

	@Column(name = "signup_completed_at")
	var signupCompletedAt: LocalDateTime? = null,

	@Column(name = "last_login_at")
	var lastLoginAt: LocalDateTime? = null,

	@Column(name = "deleted_at")
	var deletedAt: LocalDateTime? = null,
) : BaseTimeEntity() {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	var id: Long? = null
		protected set

	fun completeSignup(
		role: UserRole,
		displayName: String,
		ageBand: AgeBand,
		gender: GenderType,
	) {
		this.role = role
		this.displayName = displayName
		this.ageBand = ageBand
		this.gender = gender
		signupCompletedAt = LocalDateTime.now()
	}

	fun updateLastLogin() {
		lastLoginAt = LocalDateTime.now()
	}

	fun isSignupCompleted(): Boolean = signupCompletedAt != null
}
