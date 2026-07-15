package com.msdragon.backend.pledge.controller

import com.msdragon.backend.auth.support.AuthenticatedUser
import com.msdragon.backend.auth.support.CurrentUser
import com.msdragon.backend.common.config.BEARER_AUTH_SCHEME
import com.msdragon.backend.common.response.ApiResponse
import com.msdragon.backend.pledge.dto.SaveTripPledgeRequest
import com.msdragon.backend.pledge.dto.TripPledgeCandidatesResponse
import com.msdragon.backend.pledge.dto.TripPledgeResponse
import com.msdragon.backend.pledge.service.TripPledgeService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/trips/{tripId}/pledge")
@Tag(name = "Trip Pledge", description = "여행 10계명 작성 API 입니다.")
@SecurityRequirement(name = BEARER_AUTH_SCHEME)
class TripPledgeController(
	private val tripPledgeService: TripPledgeService,
) {
	@Operation(
		summary = "여행 10계명 후보 조회",
		description = "여행을 만든 자녀에게 활성 템플릿 중 중복 없는 무작위 후보 10개를 내려줍니다. 조회 결과 자체는 저장하지 않습니다.",
	)
	@ApiResponses(
		value = [
			SwaggerApiResponse(responseCode = "200", description = "여행 10계명 후보 조회 성공"),
			SwaggerApiResponse(responseCode = "400", description = "이미 확정본이 있거나 여행 상태가 올바르지 않음"),
			SwaggerApiResponse(responseCode = "401", description = "인증 실패"),
			SwaggerApiResponse(responseCode = "403", description = "작성 권한 없음"),
			SwaggerApiResponse(responseCode = "404", description = "여행을 찾을 수 없음"),
		],
	)
	@GetMapping("/candidates")
	fun getCandidates(
		@Parameter(hidden = true) @CurrentUser currentUser: AuthenticatedUser,
		@Parameter(description = "여행 ID", example = "1") @PathVariable tripId: Long,
	): ApiResponse<TripPledgeCandidatesResponse> =
		ApiResponse.success(
			message = "여행 10계명 후보 조회 성공",
			data = tripPledgeService.getCandidates(currentUser, tripId),
		)

	@Operation(
		summary = "여행 10계명 확정본 조회",
		description = "여행을 만든 자녀가 저장한 여행별 10계명 확정본을 조회합니다.",
	)
	@ApiResponses(
		value = [
			SwaggerApiResponse(responseCode = "200", description = "여행 10계명 조회 성공"),
			SwaggerApiResponse(responseCode = "401", description = "인증 실패"),
			SwaggerApiResponse(responseCode = "403", description = "조회 권한 없음"),
			SwaggerApiResponse(responseCode = "404", description = "여행 또는 저장된 10계명을 찾을 수 없음"),
		],
	)
	@GetMapping
	fun getPledge(
		@Parameter(hidden = true) @CurrentUser currentUser: AuthenticatedUser,
		@Parameter(description = "여행 ID", example = "1") @PathVariable tripId: Long,
	): ApiResponse<TripPledgeResponse> =
		ApiResponse.success(
			message = "여행 10계명 조회 성공",
			data = tripPledgeService.getPledge(currentUser, tripId),
		)

	@Operation(
		summary = "여행 10계명 확정본 저장",
		description = "본인 서명 화면으로 이동하기 직전에 수정 완료한 문구 10개를 여행별 확정본으로 저장합니다. 배열 순서가 표시 순서가 됩니다.",
	)
	@ApiResponses(
		value = [
			SwaggerApiResponse(responseCode = "200", description = "여행 10계명 저장 성공"),
			SwaggerApiResponse(responseCode = "400", description = "항목 수, 템플릿, 여행 상태 또는 10계명 상태가 올바르지 않음"),
			SwaggerApiResponse(responseCode = "401", description = "인증 실패"),
			SwaggerApiResponse(responseCode = "403", description = "작성 권한 없음"),
			SwaggerApiResponse(responseCode = "404", description = "여행을 찾을 수 없음"),
		],
	)
	@PutMapping
	fun savePledge(
		@Parameter(hidden = true) @CurrentUser currentUser: AuthenticatedUser,
		@Parameter(description = "여행 ID", example = "1") @PathVariable tripId: Long,
		@Valid @RequestBody request: SaveTripPledgeRequest,
	): ApiResponse<TripPledgeResponse> =
		ApiResponse.success(
			message = "여행 10계명 저장 성공",
			data = tripPledgeService.saveReviewedPledge(currentUser, tripId, request),
		)
}
