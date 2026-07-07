package com.msdragon.backend.trip.repository

import com.msdragon.backend.trip.entity.TripStop
import org.springframework.data.jpa.repository.JpaRepository

interface TripStopRepository : JpaRepository<TripStop, Long> {
	fun findAllByTripDayTripIdOrderByTripDayDayNumberAscSortOrderAsc(tripId: Long): List<TripStop>

	fun findAllByTripDayIdOrderBySortOrderAsc(tripDayId: Long): List<TripStop>
}
