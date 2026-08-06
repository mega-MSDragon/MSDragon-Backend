package com.msdragon.backend.chat.repository

import com.msdragon.backend.chat.entity.ChatSession
import com.msdragon.backend.chat.entity.ChatSessionScope
import org.springframework.data.jpa.repository.JpaRepository

interface ChatSessionRepository : JpaRepository<ChatSession, Long> {
	fun findFirstByUserIdAndTripIdAndScopeAndClosedAtIsNullOrderByIdDesc(
		userId: Long,
		tripId: Long,
		scope: ChatSessionScope,
	): ChatSession?
}
