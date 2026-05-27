package com.msdragon.backend.health.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "서버 상태 응답")
data class HealthCheckResponse(
	@field:Schema(description = "서버 상태", example = "UP")
	val status: String,
)
