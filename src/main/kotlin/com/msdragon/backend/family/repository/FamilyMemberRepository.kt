package com.msdragon.backend.family.repository

import com.msdragon.backend.auth.entity.UserRole
import com.msdragon.backend.family.entity.FamilyMember
import org.springframework.data.jpa.repository.JpaRepository

interface FamilyMemberRepository : JpaRepository<FamilyMember, Long> {
	fun findByUserId(userId: Long): FamilyMember?

	fun findAllByFamilyIdOrderByJoinedAtAsc(familyId: Long): List<FamilyMember>

	fun countByFamilyIdAndMemberRole(familyId: Long, memberRole: UserRole): Long
}
