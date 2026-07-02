package com.msdragon.backend.family.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "가족 코드 매칭 요청")
data class MatchFamilyCodeRequest(
	@field:Schema(description = "상대방 가족 코드", example = "MSH-2405")
	@field:NotBlank(message = "상대방 코드를 입력해주세요.")
	val code: String,
)
