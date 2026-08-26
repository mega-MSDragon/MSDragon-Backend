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
	var oauthSubject: String,

	@Column(name = "display_name", nullable = false, length = 50)
	var displayName: String,

	@Column(name = "age_band", nullable = false, length = 20)
	var ageBand: AgeBand = AgeBand.UNDISCLOSED,

	@Column(name = "gender", nullable = false, length = 20)
	var gender: GenderType = GenderType.UNDISCLOSED,

	/**
	 * 탈퇴 시 provider 연결 해제(revoke)에 사용하는 provider refresh token.
	 * 자격증명이므로 로그에 남기지 않는다. 현재는 애플만 사용한다.
	 */
	@Column(name = "oauth_refresh_token", length = 512)
	var oauthRefreshToken: String? = null,

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

	/** 재로그인 시 코드 교환이 없었으면 기존 값을 지우지 않는다. */
	fun updateOauthRefreshToken(oauthRefreshToken: String?) {
		if (!oauthRefreshToken.isNullOrBlank()) {
			this.oauthRefreshToken = oauthRefreshToken
		}
	}

	fun withdraw(withdrawnAt: LocalDateTime, withdrawnOauthSubject: String) {
		oauthSubject = withdrawnOauthSubject
		displayName = WITHDRAWN_DISPLAY_NAME
		ageBand = AgeBand.UNDISCLOSED
		gender = GenderType.UNDISCLOSED
		signupCompletedAt = null
		lastLoginAt = null
		oauthRefreshToken = null
		deletedAt = withdrawnAt
	}

	fun isSignupCompleted(): Boolean = signupCompletedAt != null

	companion object {
		private const val WITHDRAWN_DISPLAY_NAME = "탈퇴한 사용자"
	}
}
