package com.msdragon.backend.auth.repository

import com.msdragon.backend.auth.entity.UserConsent
import org.springframework.data.jpa.repository.JpaRepository

interface UserConsentRepository : JpaRepository<UserConsent, Long> {
	fun findAllByUserIdOrderByIdAsc(userId: Long): List<UserConsent>
}
