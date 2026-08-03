package com.msdragon.backend.feedback.controller

import com.msdragon.backend.auth.support.AuthenticatedUser
import com.msdragon.backend.auth.support.CurrentUser
import com.msdragon.backend.common.config.BEARER_AUTH_SCHEME
import com.msdragon.backend.common.response.ApiResponse
import com.msdragon.backend.feedback.dto.SubmitTripFeedbackRequest
import com.msdragon.backend.feedback.dto.TripFeedbackResponse
import com.msdragon.backend.feedback.dto.TripFeedbackStatusResponse
import com.msdragon.backend.feedback.service.TripFeedbackService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/trips/{tripId}/feedback")
@Tag(name = "Trip Feedback", description = "여행 마지막 날 이후 부모 피드백 API입니다.")
@SecurityRequirement(name = BEARER_AUTH_SCHEME)
class TripFeedbackController(
	private val tripFeedbackService: TripFeedbackService,
) {
	@Operation(
		summary = "부모 평가 요청",
		description = "여행을 만든 자녀가 마지막 날부터 아직 피드백을 제출하지 않은 참여 부모 모두에게 평가를 요청합니다. 중복 호출해도 요청 이력은 부모별 한 건만 유지합니다.",
	)
	@ApiResponses(
		value = [
			SwaggerApiResponse(responseCode = "200", description = "처리 완료: 요청 성공(status=200) 또는 인증·정책 오류(status=400/401/403/404)"),
		],
	)
	@PostMapping("/requests")
	fun requestFeedback(
		@Parameter(hidden = true) @CurrentUser currentUser: AuthenticatedUser,
		@Parameter(description = "여행 ID", example = "1") @PathVariable tripId: Long,
	): ApiResponse<TripFeedbackStatusResponse> =
		ApiResponse.success(
			message = "부모 평가 요청 성공",
			data = tripFeedbackService.requestFeedback(currentUser, tripId),
		)

	@Operation(
		summary = "피드백 제출 현황 조회",
		description = "여행을 만든 자녀와 참여 부모가 부모별 평가 요청·제출 현황과 효도 리포트 생성 가능 여부를 조회합니다.",
	)
	@ApiResponses(
		value = [
			SwaggerApiResponse(responseCode = "200", description = "처리 완료: 조회 성공(status=200) 또는 인증·정책 오류(status=401/403/404)"),
		],
	)
	@GetMapping("/status")
	fun getStatus(
		@Parameter(hidden = true) @CurrentUser currentUser: AuthenticatedUser,
		@Parameter(description = "여행 ID", example = "1") @PathVariable tripId: Long,
	): ApiResponse<TripFeedbackStatusResponse> =
		ApiResponse.success(
			message = "피드백 제출 현황 조회 성공",
			data = tripFeedbackService.getStatus(currentUser, tripId),
		)

	@Operation(
		summary = "내 여행 피드백 제출",
		description = "여행에 참여한 부모가 마지막 날부터 본인 피드백을 한 번 제출합니다. 자녀의 평가 요청을 받지 않았어도 제출할 수 있으며 제출 후에는 수정할 수 없습니다.",
	)
	@ApiResponses(
		value = [
			SwaggerApiResponse(responseCode = "200", description = "처리 완료: 제출 성공(status=200) 또는 요청·인증·정책 오류(status=400/401/403/404)"),
		],
	)
	@PostMapping("/me")
	fun submitFeedback(
		@Parameter(hidden = true) @CurrentUser currentUser: AuthenticatedUser,
		@Parameter(description = "여행 ID", example = "1") @PathVariable tripId: Long,
		@Valid @RequestBody request: SubmitTripFeedbackRequest,
	): ApiResponse<TripFeedbackResponse> =
		ApiResponse.success(
			message = "여행 피드백 제출 성공",
			data = tripFeedbackService.submitFeedback(currentUser, tripId, request),
		)

	@Operation(
		summary = "내 여행 피드백 조회",
		description = "여행에 참여한 부모가 본인이 제출한 피드백을 조회합니다.",
	)
	@ApiResponses(
		value = [
			SwaggerApiResponse(responseCode = "200", description = "처리 완료: 조회 성공(status=200) 또는 인증·정책 오류(status=401/403/404)"),
		],
	)
	@GetMapping("/me")
	fun getMyFeedback(
		@Parameter(hidden = true) @CurrentUser currentUser: AuthenticatedUser,
		@Parameter(description = "여행 ID", example = "1") @PathVariable tripId: Long,
	): ApiResponse<TripFeedbackResponse> =
		ApiResponse.success(
			message = "내 여행 피드백 조회 성공",
			data = tripFeedbackService.getMyFeedback(currentUser, tripId),
		)
}
