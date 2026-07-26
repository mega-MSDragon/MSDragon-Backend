package com.msdragon.backend.feedback.repository

import com.msdragon.backend.feedback.entity.TripFeedback
import org.springframework.data.jpa.repository.JpaRepository

interface TripFeedbackRepository : JpaRepository<TripFeedback, Long> {
	fun findByTripIdAndParentUserId(tripId: Long, parentUserId: Long): TripFeedback?

	fun findAllByTripIdOrderByParentUserIdAsc(tripId: Long): List<TripFeedback>

	fun countByTripId(tripId: Long): Long
}
