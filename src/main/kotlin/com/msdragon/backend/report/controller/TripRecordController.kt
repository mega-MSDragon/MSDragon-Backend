package com.msdragon.backend.report.controller

import com.msdragon.backend.auth.support.AuthenticatedUser
import com.msdragon.backend.auth.support.CurrentUser
import com.msdragon.backend.common.config.BEARER_AUTH_SCHEME
import com.msdragon.backend.common.response.ApiResponse
import com.msdragon.backend.report.dto.TripRecordDetailResponse
import com.msdragon.backend.report.dto.TripRecordsResponse
import com.msdragon.backend.report.service.FilialReportService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
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

	@Operation(
		summary = "기록 상세 조회",
		description = "기록 카드를 눌렀을 때 필요한 여행 정보, 요약 통계, 일차별 코스, 여행 10계명·효도 리포트 진행 상태를 한 번에 조회합니다. " +
			"효도 리포트 본문은 포함하지 않으며 `보러가기`에서 기존 리포트 API를 호출합니다.",
	)
	@ApiResponses(
		value = [
			SwaggerApiResponse(
				responseCode = "200",
				description = "처리 완료: 조회 성공(status=200), 인증 오류(status=401), 참여하지 않은 여행(status=403), 없는 여행(status=404)",
			),
		],
	)
	@GetMapping("/{tripId}")
	fun getRecordDetail(
		@Parameter(hidden = true) @CurrentUser currentUser: AuthenticatedUser,
		@Parameter(description = "여행 ID", example = "1") @PathVariable tripId: Long,
	): ApiResponse<TripRecordDetailResponse> =
		ApiResponse.success(
			message = "기록 상세 조회 성공",
			data = filialReportService.getRecordDetail(currentUser, tripId),
		)
}