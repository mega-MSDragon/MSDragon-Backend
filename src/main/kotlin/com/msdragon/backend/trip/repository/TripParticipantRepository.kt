package com.msdragon.backend.trip.repository

import com.msdragon.backend.trip.entity.TripParticipant
import org.springframework.data.jpa.repository.JpaRepository

interface TripParticipantRepository : JpaRepository<TripParticipant, Long> {
	fun findAllByTripIdOrderByIdAsc(tripId: Long): List<TripParticipant>
}
