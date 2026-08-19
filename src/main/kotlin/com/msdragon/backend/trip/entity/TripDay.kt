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
import java.time.LocalDateTime

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
	var travelDate: LocalDate,
) : BaseTimeEntity() {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	var id: Long? = null
		protected set

	@Column(name = "route_provider", length = 30)
	var routeProvider: ExternalApiProvider? = null
		protected set

	@Column(name = "route_total_distance_m")
	var routeTotalDistanceMeters: Int? = null
		protected set

	@Column(name = "route_total_duration_seconds")
	var routeTotalDurationSeconds: Int? = null
		protected set

	@Column(name = "route_polyline", columnDefinition = "text")
	var routePolyline: String? = null
		protected set

	@Column(name = "route_source_payload", columnDefinition = "text")
	var routeSourcePayload: String? = null
		protected set

	@Column(name = "route_optimized_at")
	var routeOptimizedAt: LocalDateTime? = null
		protected set

	fun updateTravelDate(travelDate: LocalDate) {
		this.travelDate = travelDate
	}

	fun applyRouteOptimization(
		provider: ExternalApiProvider,
		totalDistanceMeters: Int,
		totalDurationSeconds: Int,
		polyline: String?,
		sourcePayload: String?,
		optimizedAt: LocalDateTime,
	) {
		routeProvider = provider
		routeTotalDistanceMeters = totalDistanceMeters
		routeTotalDurationSeconds = totalDurationSeconds
		routePolyline = polyline
		routeSourcePayload = sourcePayload
		routeOptimizedAt = optimizedAt
	}

	fun clearRouteOptimization() {
		routeProvider = null
		routeTotalDistanceMeters = null
		routeTotalDurationSeconds = null
		routePolyline = null
		routeSourcePayload = null
		routeOptimizedAt = null
	}
}
