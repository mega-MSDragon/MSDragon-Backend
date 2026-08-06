package com.msdragon.backend.chat.entity

import com.msdragon.backend.common.entity.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "chat_messages")
class ChatMessage(
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "chat_session_id", nullable = false)
	val chatSession: ChatSession,

	@Column(name = "sender", nullable = false, length = 20)
	val sender: ChatSender,

	@Column(name = "content", nullable = false, columnDefinition = "text")
	val content: String,

	@Column(name = "metadata", columnDefinition = "text")
	val metadata: String? = null,
) : BaseTimeEntity() {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	var id: Long? = null
		protected set
}
