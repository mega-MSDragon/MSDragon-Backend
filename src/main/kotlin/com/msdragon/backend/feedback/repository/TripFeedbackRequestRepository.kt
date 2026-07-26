package com.msdragon.backend.feedback.repository

import com.msdragon.backend.feedback.entity.TripFeedbackRequest
import org.springframework.data.jpa.repository.JpaRepository

interface TripFeedbackRequestRepository : JpaRepository<TripFeedbackRequest, Long> {
	fun findAllByTripIdOrderByParentUserIdAsc(tripId: Long): List<TripFeedbackRequest>
}
