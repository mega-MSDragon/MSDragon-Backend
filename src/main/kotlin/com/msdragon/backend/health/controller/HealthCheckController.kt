package com.msdragon.backend.health.controller

import com.msdragon.backend.health.dto.HealthCheckResponse
import com.msdragon.backend.health.service.HealthCheckService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@Tag(name = "Health", description = "서버 상태 확인 API 입니다.")
class HealthCheckController(
	private val healthCheckService: HealthCheckService,
) {
	@Operation(summary = "Health Check API", description = "서버가 정상적으로 실행 중인지 확인합니다.")
	@ApiResponses(
		value = [
			SwaggerApiResponse(responseCode = "200", description = "서버 상태 조회 성공"),
		],
	)
	@GetMapping("/health")
	fun getHealth(): HealthCheckResponse = healthCheckService.getHealth()
}
