package com.msdragon.backend.family.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

@Schema(description = "가족 코드 매칭 요청")
data class MatchFamilyCodeRequest(
	@field:Schema(description = "상대방 가족 코드. 하이픈은 생략할 수 있습니다.", example = "MSH0001")
	@field:NotBlank(message = "상대방 코드를 입력해주세요.")
	@field:Pattern(regexp = "(?i)^MSH-?[0-9]{4}$", message = "가족 코드는 MSH0000 또는 MSH-0000 형식이어야 합니다.")
	val code: String,
)
