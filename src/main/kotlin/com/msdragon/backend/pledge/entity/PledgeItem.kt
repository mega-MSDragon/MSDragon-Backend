package com.msdragon.backend.pledge.entity

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
	name = "pledge_items",
	uniqueConstraints = [
		UniqueConstraint(name = "uk_pledge_items_pledge_sort_order", columnNames = ["trip_pledge_id", "sort_order"]),
	],
)
class PledgeItem(
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "trip_pledge_id", nullable = false)
	val tripPledge: TripPledge,

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "pledge_template_id")
	val pledgeTemplate: PledgeTemplate? = null,

	@Column(name = "sort_order", nullable = false)
	val sortOrder: Int,

	@Column(name = "content", nullable = false, length = 255)
	val content: String,

	@Column(name = "is_from_template", nullable = false)
	val isFromTemplate: Boolean,
) : BaseTimeEntity() {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	var id: Long? = null
		protected set
}
