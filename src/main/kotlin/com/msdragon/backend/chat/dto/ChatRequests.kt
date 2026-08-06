package com.msdragon.backend.chat.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "여행 모드 AI 챗봇 질문 요청")
data class SendChatMessageRequest(
	@field:NotBlank(message = "질문을 입력해 주세요.")
	@field:Size(max = 500, message = "질문은 500자 이내로 입력해 주세요.")
	@field:Schema(description = "여행 또는 여행지에 관한 질문", example = "오늘 첫 번째 방문지는 어디야?", maxLength = 500)
	val message: String,
)
