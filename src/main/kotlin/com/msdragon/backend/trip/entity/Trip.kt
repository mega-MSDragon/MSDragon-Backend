package com.msdragon.backend.trip.entity

import com.msdragon.backend.auth.entity.User
import com.msdragon.backend.common.entity.BaseTimeEntity
import com.msdragon.backend.family.entity.Family
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "trips")
class Trip(
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "family_id", nullable = false)
	val family: Family,

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "created_by_user_id", nullable = false)
	val createdByUser: User,

	@Column(name = "destination_code", nullable = false, length = 60)
	val destinationCode: TripDestinationCode,

	@Column(name = "title", nullable = false, length = 80)
	var title: String,

	@Column(name = "start_date", nullable = false)
	val startDate: LocalDate,

	@Column(name = "end_date", nullable = false)
	val endDate: LocalDate,

	@Column(name = "status", nullable = false, length = 30)
	var status: TripStatus = TripStatus.PLANNING,

	@Column(name = "recommendation_snapshot", columnDefinition = "text")
	var recommendationSnapshot: String? = null,

	@Column(name = "deleted_at")
	var deletedAt: LocalDateTime? = null,
) : BaseTimeEntity() {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	var id: Long? = null
		protected set
}
