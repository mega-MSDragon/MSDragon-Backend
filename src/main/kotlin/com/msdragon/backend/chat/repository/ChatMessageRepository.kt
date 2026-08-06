package com.msdragon.backend.chat.repository

import com.msdragon.backend.chat.entity.ChatMessage
import org.springframework.data.jpa.repository.JpaRepository

interface ChatMessageRepository : JpaRepository<ChatMessage, Long> {
	fun findAllByChatSessionIdOrderByIdAsc(chatSessionId: Long): List<ChatMessage>

	fun findTop20ByChatSessionIdOrderByIdDesc(chatSessionId: Long): List<ChatMessage>
}
