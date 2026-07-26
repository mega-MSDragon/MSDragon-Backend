package com.msdragon.backend.feedback.entity

import com.msdragon.backend.auth.entity.User
import com.msdragon.backend.trip.entity.Trip
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
	name = "trip_feedback_requests",
	uniqueConstraints = [
		UniqueConstraint(name = "uk_trip_feedback_requests_trip_parent", columnNames = ["trip_id", "parent_user_id"]),
	],
)
class TripFeedbackRequest(
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "trip_id", nullable = false)
	val trip: Trip,

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "requested_by_user_id", nullable = false)
	val requestedByUser: User,

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "parent_user_id", nullable = false)
	val parentUser: User,

	@Column(name = "requested_at", nullable = false)
	val requestedAt: LocalDateTime,
) {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	var id: Long? = null
		protected set
}
