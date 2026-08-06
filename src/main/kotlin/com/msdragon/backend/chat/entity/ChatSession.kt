package com.msdragon.backend.chat.entity

import com.msdragon.backend.auth.entity.User
import com.msdragon.backend.common.entity.BaseTimeEntity
import com.msdragon.backend.trip.entity.Trip
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "chat_sessions")
class ChatSession(
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	val user: User,

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "trip_id", nullable = false)
	val trip: Trip,

	@Column(name = "context_place_id")
	val contextPlaceId: Long? = null,

	@Column(name = "scope", nullable = false, length = 30)
	val scope: ChatSessionScope = ChatSessionScope.TRAVEL_MODE,

	@Column(name = "system_prompt_version", length = 30)
	var systemPromptVersion: String,

	@Column(name = "model_name", length = 60)
	var modelName: String,

	@Column(name = "personalization_enabled", nullable = false)
	val personalizationEnabled: Boolean = false,

	@Column(name = "context_snapshot", columnDefinition = "text")
	var contextSnapshot: String,

	@Column(name = "closed_at")
	var closedAt: LocalDateTime? = null,
) : BaseTimeEntity() {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	var id: Long? = null
		protected set

	fun refreshContext(systemPromptVersion: String, modelName: String, contextSnapshot: String) {
		this.systemPromptVersion = systemPromptVersion
		this.modelName = modelName
		this.contextSnapshot = contextSnapshot
	}
}
