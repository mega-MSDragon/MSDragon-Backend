package com.msdragon.backend.health.service

import com.msdragon.backend.health.dto.HealthCheckResponse
import org.springframework.stereotype.Service

@Service
class HealthCheckService {
	fun getHealth(): HealthCheckResponse = HealthCheckResponse(status = "UP")
}
