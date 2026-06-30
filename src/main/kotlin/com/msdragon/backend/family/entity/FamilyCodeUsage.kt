package com.msdragon.backend.family.entity

import com.msdragon.backend.auth.entity.User
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
	name = "family_code_usages",
	uniqueConstraints = [
		UniqueConstraint(name = "uk_family_code_usage_requester", columnNames = ["family_code_id", "requester_user_id"]),
	],
)
class FamilyCodeUsage(
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "family_code_id", nullable = false)
	val familyCode: FamilyCode,

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "requester_user_id", nullable = false)
	val requesterUser: User,

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "family_id", nullable = false)
	val family: Family,

	@Column(name = "matched_at", nullable = false)
	val matchedAt: LocalDateTime = LocalDateTime.now(),
) {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	var id: Long? = null
		protected set
}
