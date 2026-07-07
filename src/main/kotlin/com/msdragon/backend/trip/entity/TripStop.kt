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
import java.math.BigDecimal
import java.time.LocalTime

@Entity
@Table(
	name = "trip_stops",
	uniqueConstraints = [
		UniqueConstraint(name = "uk_trip_stops_day_sort_order", columnNames = ["trip_day_id", "sort_order"]),
	],
)
class TripStop(
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "trip_day_id", nullable = false)
	val tripDay: TripDay,

	@Column(name = "sort_order", nullable = false)
	var sortOrder: Int,

	@Column(name = "stop_type", nullable = false, length = 30)
	val stopType: StopType = StopType.SIGHTSEEING,

	@Column(name = "source_provider", nullable = false, length = 30)
	val sourceProvider: ExternalApiProvider = ExternalApiProvider.TOUR_API,

	@Column(name = "external_place_id", length = 120)
	val externalPlaceId: String? = null,

	@Column(name = "content_type_id", length = 20)
	val contentTypeId: String? = null,

	@Column(name = "name", nullable = false, length = 120)
	val name: String,

	@Column(name = "category", length = 60)
	val category: String? = null,

	@Column(name = "address", length = 255)
	val address: String? = null,

	@Column(name = "latitude", precision = 10, scale = 7)
	val latitude: BigDecimal? = null,

	@Column(name = "longitude", precision = 10, scale = 7)
	val longitude: BigDecimal? = null,

	@Column(name = "phone", length = 30)
	val phone: String? = null,

	@Column(name = "homepage_url", length = 500)
	val homepageUrl: String? = null,

	@Column(name = "image_url", length = 500)
	val imageUrl: String? = null,

	@Column(name = "overview", columnDefinition = "text")
	val overview: String? = null,

	@Column(name = "arrival_time")
	var arrivalTime: LocalTime? = null,

	@Column(name = "dwell_minutes")
	var dwellMinutes: Int? = null,

	@Column(name = "note", length = 255)
	val note: String? = null,

	@Column(name = "recommendation_reason", length = 255)
	val recommendationReason: String? = null,

	@Column(name = "recommendation_tags", columnDefinition = "text")
	val recommendationTags: String? = null,

	@Column(name = "source_payload", columnDefinition = "text")
	val sourcePayload: String? = null,

	@Column(name = "is_manual_added", nullable = false)
	val isManualAdded: Boolean = false,
) : BaseTimeEntity() {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	var id: Long? = null
		protected set
}
