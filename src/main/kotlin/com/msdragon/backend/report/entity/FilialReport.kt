package com.msdragon.backend.report.entity

import com.msdragon.backend.common.entity.BaseTimeEntity
import com.msdragon.backend.trip.entity.Trip
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(
	name = "filial_reports",
	uniqueConstraints = [
		UniqueConstraint(name = "uk_filial_reports_trip", columnNames = ["trip_id"]),
	],
)
class FilialReport(
	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "trip_id", nullable = false)
	val trip: Trip,

	@Column(name = "total_score")
	val totalScore: Int? = null,

	@Column(name = "satisfaction_score")
	val satisfactionScore: Int? = null,

	@Column(name = "leg_comfort_score")
	val legComfortScore: Int? = null,

	@Column(name = "nagging_prevention_score")
	val naggingPreventionScore: Int? = null,

	@Column(name = "meal_satisfaction_score")
	val mealSatisfactionScore: Int? = null,

	@Column(name = "restroom_safety_score")
	val restroomSafetyScore: Int? = null,

	@Column(name = "award_title", length = 80)
	val awardTitle: String? = null,

	@Column(name = "summary", length = 255)
	val summary: String? = null,

	@Column(name = "cover_image_url", length = 500)
	var coverImageUrl: String?,

	@Column(name = "best_place_id")
	val bestPlaceId: Long? = null,

	@Column(name = "best_comment", length = 500)
	val bestComment: String? = null,

	@Column(name = "parent_comment", length = 500)
	val parentComment: String? = null,

	@Column(name = "total_place_count", nullable = false)
	var totalPlaceCount: Int,

	@Column(name = "average_rating", nullable = false, precision = 2, scale = 1)
	var averageRating: BigDecimal,

	@Column(name = "total_distance_km", precision = 6, scale = 2)
	var totalDistanceKm: BigDecimal?,

	@Column(name = "total_step_count")
	val totalStepCount: Int? = null,

	@Column(name = "share_image_url", length = 500)
	val shareImageUrl: String? = null,

	@Column(name = "generated_at", nullable = false)
	val generatedAt: LocalDateTime,
) : BaseTimeEntity() {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	var id: Long? = null
		protected set

	fun refreshCourseSummary(
		coverImageUrl: String?,
		totalPlaceCount: Int,
		averageRating: BigDecimal,
		totalDistanceKm: BigDecimal?,
	) {
		this.coverImageUrl = coverImageUrl
		this.totalPlaceCount = totalPlaceCount
		this.averageRating = averageRating
		this.totalDistanceKm = totalDistanceKm
	}
}
