package com.msdragon.backend.trip.entity

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
import java.time.LocalDate

@Entity
@Table(
	name = "trip_days",
	uniqueConstraints = [
		UniqueConstraint(name = "uk_trip_days_trip_day_number", columnNames = ["trip_id", "day_number"]),
	],
)
class TripDay(
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "trip_id", nullable = false)
	val trip: Trip,

	@Column(name = "day_number", nullable = false)
	val dayNumber: Int,

	@Column(name = "travel_date", nullable = false)
	val travelDate: LocalDate,
) : BaseTimeEntity() {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	var id: Long? = null
		protected set
}
