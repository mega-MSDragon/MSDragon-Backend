package com.msdragon.backend.pledge.repository

import com.msdragon.backend.pledge.entity.PledgeItem
import org.springframework.data.jpa.repository.JpaRepository

interface PledgeItemRepository : JpaRepository<PledgeItem, Long> {
	fun findAllByTripPledgeIdOrderBySortOrderAsc(tripPledgeId: Long): List<PledgeItem>
}
