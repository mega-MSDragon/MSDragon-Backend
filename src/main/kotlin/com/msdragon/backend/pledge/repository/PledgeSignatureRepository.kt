package com.msdragon.backend.pledge.repository

import com.msdragon.backend.pledge.entity.PledgeSignature
import org.springframework.data.jpa.repository.JpaRepository

interface PledgeSignatureRepository : JpaRepository<PledgeSignature, Long> {
	fun findAllByTripPledgeIdOrderBySignedAtAsc(tripPledgeId: Long): List<PledgeSignature>

	fun findByTripPledgeIdAndUserId(tripPledgeId: Long, userId: Long): PledgeSignature?
}
