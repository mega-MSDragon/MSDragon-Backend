package com.msdragon.backend.family.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "가족 코드 매칭 요청")
data class MatchFamilyCodeRequest(
	@field:Schema(description = "상대방 가족 코드", example = "MSH-2405")
	@field:NotBlank(message = "상대방 코드를 입력해주세요.")
	val code: String,

	@field:Schema(description = "가족 관계 표시 이름. 예: 엄마, 아빠", example = "엄마", nullable = true)
	@field:Size(max = 20, message = "관계 표시는 20자 이하로 입력해주세요.")
	val relationLabel: String? = null,
)
