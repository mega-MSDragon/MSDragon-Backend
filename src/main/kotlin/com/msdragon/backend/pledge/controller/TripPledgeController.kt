package com.msdragon.backend.pledge.controller

import com.msdragon.backend.auth.support.AuthenticatedUser
import com.msdragon.backend.auth.support.CurrentUser
import com.msdragon.backend.common.config.BEARER_AUTH_SCHEME
import com.msdragon.backend.common.response.ApiResponse
import com.msdragon.backend.pledge.dto.SavePledgeSignatureRequest
import com.msdragon.backend.pledge.dto.SaveTripPledgeRequest
import com.msdragon.backend.pledge.dto.TripPledgeCandidatesResponse
import com.msdragon.backend.pledge.dto.TripPledgeResponse
import com.msdragon.backend.pledge.service.TripPledgeService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
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
			SwaggerApiResponse(responseCode = "200", description = "처리 완료: 조회 성공(status=200) 또는 인증·정책 오류(status=400/401/403/404)"),
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
		description = "여행별 확정 문구와 현재까지 제출된 전체 참여자 서명을 조회합니다. 부모는 자녀 서명 요청 후 조회할 수 있습니다.",
	)
	@ApiResponses(
		value = [
			SwaggerApiResponse(responseCode = "200", description = "처리 완료: 조회 성공(status=200) 또는 인증·정책 오류(status=401/403/404)"),
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
		summary = "여행 10계명 PDF 조회",
		description = "완료된 여행 10계명의 확정 문구와 현재까지 제출된 전체 서명을 HTML 템플릿에 합성해 PDF 원본으로 반환합니다.",
	)
	@ApiResponses(
		value = [
			SwaggerApiResponse(
				responseCode = "200",
				description = "처리 완료: PDF 생성 성공 또는 JSON 인증·정책 오류(status=400/401/403/404)",
				content = [
					Content(mediaType = MediaType.APPLICATION_PDF_VALUE, schema = Schema(type = "string", format = "binary")),
					Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = Schema(implementation = ApiResponse::class)),
				],
			),
			SwaggerApiResponse(responseCode = "500", description = "PDF 생성 실패"),
		],
	)
	@GetMapping("/pdf", produces = [MediaType.APPLICATION_PDF_VALUE])
	fun getPledgePdf(
		@Parameter(hidden = true) @CurrentUser currentUser: AuthenticatedUser,
		@Parameter(description = "여행 ID", example = "1") @PathVariable tripId: Long,
	): ResponseEntity<ByteArray> {
		val pdf = tripPledgeService.generatePdf(currentUser, tripId)
		return ResponseEntity.ok()
			.contentType(MediaType.APPLICATION_PDF)
			.contentLength(pdf.content.size.toLong())
			.header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename(pdf.fileName).build().toString())
			.header(HttpHeaders.CACHE_CONTROL, "private, no-store")
			.body(pdf.content)
	}

	@Operation(
		summary = "여행 10계명 확정본 저장",
		description = "본인 서명 화면으로 이동하기 직전에 수정 완료한 문구 10개를 여행별 확정본으로 저장합니다. 배열 순서가 표시 순서가 됩니다.",
	)
	@ApiResponses(
		value = [
			SwaggerApiResponse(responseCode = "200", description = "처리 완료: 저장 성공(status=200) 또는 요청·인증·정책 오류(status=400/401/403/404)"),
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

	@Operation(
		summary = "여행 10계명 본인 서명 저장",
		description = "현재 사용자의 PNG 서명을 저장합니다. 자녀가 먼저 서명해야 하며, 참여 부모는 전체 완료 후에도 본인 서명을 추가할 수 있습니다.",
	)
	@ApiResponses(
		value = [
			SwaggerApiResponse(responseCode = "200", description = "처리 완료: 저장 성공(status=200) 또는 요청·인증·정책 오류(status=400/401/403/404)"),
		],
	)
	@PostMapping("/signatures/me")
	fun saveSignature(
		@Parameter(hidden = true) @CurrentUser currentUser: AuthenticatedUser,
		@Parameter(description = "여행 ID", example = "1") @PathVariable tripId: Long,
		@Valid @RequestBody request: SavePledgeSignatureRequest,
	): ApiResponse<TripPledgeResponse> =
		ApiResponse.success(
			message = "여행 10계명 서명 저장 성공",
			data = tripPledgeService.saveSignature(currentUser, tripId, request),
		)
}
