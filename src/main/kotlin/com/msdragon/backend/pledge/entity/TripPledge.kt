package com.msdragon.backend.pledge.entity

import com.msdragon.backend.auth.entity.User
import com.msdragon.backend.common.entity.BaseTimeEntity
import com.msdragon.backend.trip.entity.Trip
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "trip_pledges")
class TripPledge(
	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "trip_id", nullable = false, unique = true)
	val trip: Trip,

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "created_by_user_id", nullable = false)
	val createdByUser: User,

	@Column(name = "status", nullable = false, length = 30)
	var status: TripPledgeStatus = TripPledgeStatus.DRAFT,

	@Column(name = "title", length = 80)
	var title: String? = null,

	@Column(name = "rendered_image_url", length = 500)
	var renderedImageUrl: String? = null,

	@Column(name = "pdf_url", length = 500)
	var pdfUrl: String? = null,

	@Column(name = "reviewed_at")
	var reviewedAt: LocalDateTime? = null,

	@Column(name = "requested_at")
	var requestedAt: LocalDateTime? = null,

	@Column(name = "completed_at")
	var completedAt: LocalDateTime? = null,

	@Column(name = "shared_at")
	var sharedAt: LocalDateTime? = null,
) : BaseTimeEntity() {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	var id: Long? = null
		protected set

	fun review(title: String, reviewedAt: LocalDateTime) {
		this.title = title
		this.reviewedAt = reviewedAt
		status = TripPledgeStatus.REVIEWED
	}
}
