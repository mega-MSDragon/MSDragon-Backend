package com.msdragon.backend.family.entity

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

@Entity
@Table(
	name = "family_codes",
	uniqueConstraints = [
		UniqueConstraint(name = "uk_family_codes_user", columnNames = ["user_id"]),
		UniqueConstraint(name = "uk_family_codes_code", columnNames = ["code"]),
	],
)
class FamilyCode(
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	val user: User,

	@Column(name = "code", nullable = false, length = 20)
	val code: String,

	@Column(name = "is_active", nullable = false)
	var isActive: Boolean = true,
) : BaseTimeEntity() {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	var id: Long? = null
		protected set
}
