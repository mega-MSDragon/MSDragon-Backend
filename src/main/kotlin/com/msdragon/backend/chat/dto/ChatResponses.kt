package com.msdragon.backend.chat.dto

import com.msdragon.backend.chat.entity.ChatMessage
import com.msdragon.backend.chat.entity.ChatSender
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "채팅 메시지")
data class ChatMessageResponse(
	@field:Schema(description = "메시지 ID", example = "1")
	val id: Long,

	@field:Schema(description = "발신자", example = "assistant", allowableValues = ["user", "assistant"])
	val sender: ChatSender,

	@field:Schema(description = "메시지 내용", example = "오늘 첫 번째 방문지는 오도리 공원입니다.")
	val content: String,

	@field:Schema(description = "생성 시간", example = "2026-08-06T11:30:00")
	val createdAt: LocalDateTime,
) {
	companion object {
		fun from(message: ChatMessage): ChatMessageResponse =
			ChatMessageResponse(
				id = requireNotNull(message.id),
				sender = message.sender,
				content = message.content,
				createdAt = requireNotNull(message.createdAt),
			)
	}
}

@Schema(description = "여행 모드 AI 챗봇 대화 이력")
data class ChatConversationResponse(
	@field:Schema(description = "채팅 세션 ID. 아직 질문하지 않았으면 null입니다.", example = "1", nullable = true)
	val sessionId: Long?,

	@field:Schema(description = "시간순 메시지 목록")
	val messages: List<ChatMessageResponse>,

	@field:Schema(
		description = "현재 화면에 표시할 추천 질문 목록",
		example = "[\"오늘 일정 알려줘\", \"첫 방문지는 어떤 곳이야?\", \"가까운 화장실 어디야?\"]",
	)
	val suggestedQuestions: List<String>,
) {
	companion object {
		fun empty(suggestedQuestions: List<String>): ChatConversationResponse =
			ChatConversationResponse(sessionId = null, messages = emptyList(), suggestedQuestions = suggestedQuestions)
	}
}

@Schema(description = "여행 모드 AI 챗봇 질문과 답변")
data class ChatMessageExchangeResponse(
	@field:Schema(description = "채팅 세션 ID", example = "1")
	val sessionId: Long,

	@field:Schema(description = "저장된 사용자 질문")
	val userMessage: ChatMessageResponse,

	@field:Schema(description = "AI 답변")
	val assistantMessage: ChatMessageResponse,

	@field:Schema(
		description = "AI 답변 맥락에 맞춘 다음 추천 질문 2~3개",
		example = "[\"첨성대 관람 시간은 얼마나 걸려?\", \"그다음 방문지는 어디야?\"]",
	)
	val suggestedQuestions: List<String>,
)
