package com.msdragon.backend.pledge.entity

import com.msdragon.backend.common.entity.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "pledge_templates")
class PledgeTemplate(
	@Column(name = "content", nullable = false, length = 255)
	val content: String,

	@Column(name = "is_active", nullable = false)
	var isActive: Boolean = true,
) : BaseTimeEntity() {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	var id: Long? = null
		protected set
}
