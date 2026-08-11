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
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime

@Entity
@Table(
	name = "user_consents",
	uniqueConstraints = [
		UniqueConstraint(
			name = "uk_user_consents_user_type_version",
			columnNames = ["user_id", "consent_type", "terms_version"],
		),
	],
)
class UserConsent(
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	val user: User,

	@Column(name = "consent_type", nullable = false, length = 40)
	val consentType: UserConsentType,

	@Column(name = "terms_version", nullable = false, length = 20)
	val termsVersion: String,

	@Column(name = "agreed", nullable = false)
	val agreed: Boolean,

	@Column(name = "decided_at", nullable = false)
	val decidedAt: LocalDateTime,
) : BaseTimeEntity() {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	var id: Long? = null
		protected set
}
