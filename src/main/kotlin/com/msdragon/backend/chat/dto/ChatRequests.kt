package com.msdragon.backend.chat.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "여행 모드 AI 챗봇 질문 요청")
data class SendChatMessageRequest(
	@field:NotBlank(message = "질문을 입력해 주세요.")
	@field:Size(max = 500, message = "질문은 500자 이내로 입력해 주세요.")
	@field:Schema(description = "여행 또는 여행지에 관한 질문", example = "오늘 첫 번째 방문지는 어디야?", maxLength = 500)
	val message: String,

	@field:DecimalMin(value = "-90.0", message = "latitude는 -90 이상이어야 합니다.")
	@field:DecimalMax(value = "90.0", message = "latitude는 90 이하여야 합니다.")
	@field:Schema(description = "주변 시설 질문에 사용할 현재 위치 WGS84 위도. longitude와 함께 전달합니다.", example = "35.1587", nullable = true)
	val latitude: Double? = null,

	@field:DecimalMin(value = "-180.0", message = "longitude는 -180 이상이어야 합니다.")
	@field:DecimalMax(value = "180.0", message = "longitude는 180 이하여야 합니다.")
	@field:Schema(description = "주변 시설 질문에 사용할 현재 위치 WGS84 경도. latitude와 함께 전달합니다.", example = "129.1604", nullable = true)
	val longitude: Double? = null,
)
