package com.msdragon.backend.family.repository

import com.msdragon.backend.family.entity.FamilyCodeUsage
import org.springframework.data.jpa.repository.JpaRepository

interface FamilyCodeUsageRepository : JpaRepository<FamilyCodeUsage, Long> {
	fun existsByFamilyCodeIdAndRequesterUserId(familyCodeId: Long, requesterUserId: Long): Boolean
}
