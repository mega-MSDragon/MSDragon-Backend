package com.msdragon.backend.report.controller

import com.msdragon.backend.auth.support.AuthenticatedUser
import com.msdragon.backend.auth.support.CurrentUser
import com.msdragon.backend.common.config.BEARER_AUTH_SCHEME
import com.msdragon.backend.common.response.ApiResponse
import com.msdragon.backend.report.dto.TripRecordsResponse
import com.msdragon.backend.report.service.FilialReportService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/records")
@Tag(name = "Record", description = "완료·중단 여행 기록 목록과 완료 여행 통계 API입니다.")
@SecurityRequirement(name = BEARER_AUTH_SCHEME)
class TripRecordController(
	private val filialReportService: FilialReportService,
) {
	@Operation(
		summary = "기록 탭 조회",
		description = "같은 가족의 completed·stopped 여행을 종료일 최신순으로 조회합니다. 상단 통계는 completed 여행만 계산합니다.",
	)
	@ApiResponses(
		value = [
			SwaggerApiResponse(responseCode = "200", description = "처리 완료: 조회 성공(status=200) 또는 인증 오류(status=401)"),
		],
	)
	@GetMapping
	fun getRecords(
		@Parameter(hidden = true) @CurrentUser currentUser: AuthenticatedUser,
	): ApiResponse<TripRecordsResponse> =
		ApiResponse.success(
			message = "기록 탭 조회 성공",
			data = filialReportService.getRecords(currentUser),
		)
}
