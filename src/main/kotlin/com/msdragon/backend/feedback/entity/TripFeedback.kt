package com.msdragon.backend.feedback.entity

import com.msdragon.backend.auth.entity.User
import com.msdragon.backend.common.entity.BaseTimeEntity
import com.msdragon.backend.trip.entity.Trip
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
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
import java.time.LocalDateTime

@Entity
@Table(
	name = "trip_feedbacks",
	uniqueConstraints = [
		UniqueConstraint(name = "uk_trip_feedbacks_trip_parent", columnNames = ["trip_id", "parent_user_id"]),
	],
)
class TripFeedback(
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "trip_id", nullable = false)
	val trip: Trip,

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "parent_user_id", nullable = false)
	val parentUser: User,

	@Column(name = "overall_rating", nullable = false, precision = 2, scale = 1)
	val overallRating: BigDecimal,

	@Column(name = "body_condition", nullable = false, length = 30)
	val bodyCondition: FeedbackBodyCondition,

	@Column(name = "best_trip_stop_id", nullable = false)
	val bestTripStopId: Long,

	@Column(name = "best_place_name_snapshot", nullable = false, length = 120)
	val bestPlaceNameSnapshot: String,

	@Column(name = "free_comment", length = 200)
	val freeComment: String?,

	@ElementCollection(fetch = FetchType.LAZY)
	@CollectionTable(
		name = "trip_feedback_tags",
		joinColumns = [JoinColumn(name = "trip_feedback_id")],
		uniqueConstraints = [
			UniqueConstraint(name = "uk_trip_feedback_tags", columnNames = ["trip_feedback_id", "tag"]),
		],
	)
	@Column(name = "tag", nullable = false, length = 40)
	val tags: MutableSet<String> = mutableSetOf(),

	@Column(name = "submitted_at", nullable = false)
	val submittedAt: LocalDateTime,
) : BaseTimeEntity() {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	var id: Long? = null
		protected set
}
