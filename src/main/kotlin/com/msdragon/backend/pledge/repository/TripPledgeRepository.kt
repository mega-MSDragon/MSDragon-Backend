package com.msdragon.backend.pledge.repository

import com.msdragon.backend.pledge.entity.TripPledge
import org.springframework.data.jpa.repository.JpaRepository

interface TripPledgeRepository : JpaRepository<TripPledge, Long> {
	fun findByTripId(tripId: Long): TripPledge?
}
