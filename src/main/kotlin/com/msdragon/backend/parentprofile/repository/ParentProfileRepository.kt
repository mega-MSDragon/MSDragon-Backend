package com.msdragon.backend.parentprofile.repository

import com.msdragon.backend.parentprofile.entity.ParentProfile
import org.springframework.data.jpa.repository.JpaRepository

interface ParentProfileRepository : JpaRepository<ParentProfile, Long> {
	fun findByUserId(userId: Long): ParentProfile?
}
