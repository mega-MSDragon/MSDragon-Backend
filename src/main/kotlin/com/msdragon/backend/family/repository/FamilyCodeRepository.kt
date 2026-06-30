package com.msdragon.backend.family.repository

import com.msdragon.backend.family.entity.FamilyCode
import org.springframework.data.jpa.repository.JpaRepository

interface FamilyCodeRepository : JpaRepository<FamilyCode, Long> {
	fun findByUserId(userId: Long): FamilyCode?

	fun findByCodeAndIsActiveTrue(code: String): FamilyCode?

	fun existsByCode(code: String): Boolean
}
