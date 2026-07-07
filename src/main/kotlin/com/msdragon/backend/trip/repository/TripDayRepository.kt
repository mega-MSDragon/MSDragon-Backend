package com.msdragon.backend.trip.repository

import com.msdragon.backend.trip.entity.TripDay
import org.springframework.data.jpa.repository.JpaRepository

interface TripDayRepository : JpaRepository<TripDay, Long> {
	fun findAllByTripIdOrderByDayNumberAsc(tripId: Long): List<TripDay>

	fun findByTripIdAndDayNumber(tripId: Long, dayNumber: Int): TripDay?
}
