package com.msdragon.backend.report.controller

import com.msdragon.backend.auth.support.AuthenticatedUser
import com.msdragon.backend.auth.support.CurrentUser
import com.msdragon.backend.common.config.BEARER_AUTH_SCHEME
import com.msdragon.backend.common.response.ApiResponse
import com.msdragon.backend.report.dto.FilialReportResponse
import com.msdragon.backend.report.service.FilialReportService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/trips/{tripId}/filial-report")
@Tag(name = "Filial Report", description = "여행별 효도 리포트 생성·조회 API입니다.")
@SecurityRequirement(name = BEARER_AUTH_SCHEME)
class FilialReportController(
	private val filialReportService: FilialReportService,
) {
	@Operation(
		summary = "효도 리포트 생성",
		description = "모든 참여 부모가 피드백을 제출한 여행의 효도 리포트를 생성합니다. 이미 생성된 경우 현재 코스 집계값을 반영해 같은 리포트를 반환합니다.",
	)
	@ApiResponses(
		value = [
			SwaggerApiResponse(responseCode = "200", description = "처리 완료: 생성 성공(status=200) 또는 인증·정책 오류(status=400/401/403/404)"),
		],
	)
	@PostMapping
	fun createReport(
		@Parameter(hidden = true) @CurrentUser currentUser: AuthenticatedUser,
		@Parameter(description = "여행 ID", example = "1") @PathVariable tripId: Long,
	): ApiResponse<FilialReportResponse> =
		ApiResponse.success(
			message = "효도 리포트 생성 성공",
			data = filialReportService.createReport(currentUser, tripId),
		)

	@Operation(
		summary = "효도 리포트 조회",
		description = "같은 가족 구성원이 생성된 효도 리포트를 조회합니다. 코스 기반 대표 이미지, 방문지 수, 이동거리는 현재 저장된 코스로 갱신합니다.",
	)
	@ApiResponses(
		value = [
			SwaggerApiResponse(responseCode = "200", description = "처리 완료: 조회 성공(status=200) 또는 인증·정책 오류(status=400/401/403/404)"),
		],
	)
	@GetMapping
	fun getReport(
		@Parameter(hidden = true) @CurrentUser currentUser: AuthenticatedUser,
		@Parameter(description = "여행 ID", example = "1") @PathVariable tripId: Long,
	): ApiResponse<FilialReportResponse> =
		ApiResponse.success(
			message = "효도 리포트 조회 성공",
			data = filialReportService.getReport(currentUser, tripId),
		)
}
