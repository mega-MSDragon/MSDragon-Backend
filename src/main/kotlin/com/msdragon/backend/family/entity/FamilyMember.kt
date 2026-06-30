package com.msdragon.backend.family.entity

import com.msdragon.backend.auth.entity.User
import com.msdragon.backend.auth.entity.UserRole
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
	name = "family_members",
	uniqueConstraints = [
		UniqueConstraint(name = "uk_family_members_user", columnNames = ["user_id"]),
	],
)
class FamilyMember(
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "family_id", nullable = false)
	val family: Family,

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	val user: User,

	@Column(name = "member_role", nullable = false, length = 20)
	val memberRole: UserRole,

	@Column(name = "relation_label", length = 20)
	var relationLabel: String? = null,

	@Column(name = "joined_at", nullable = false)
	val joinedAt: LocalDateTime = LocalDateTime.now(),
) : BaseTimeEntity() {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	var id: Long? = null
		protected set
}
