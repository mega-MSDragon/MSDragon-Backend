package com.msdragon.backend.trip.controller

import com.msdragon.backend.auth.support.AuthenticatedUser
import com.msdragon.backend.auth.support.CurrentUser
import com.msdragon.backend.common.config.BEARER_AUTH_SCHEME
import com.msdragon.backend.common.response.ApiResponse
import com.msdragon.backend.trip.dto.TripPlaceDetailResponse
import com.msdragon.backend.trip.service.TripPlaceService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/places")
@Tag(name = "Place", description = "여행과 무관하게 장소·축제 상세를 조회하는 API 입니다.")
@SecurityRequirement(name = BEARER_AUTH_SCHEME)
class PlaceController(
	private val tripPlaceService: TripPlaceService,
) {
	@Operation(
		summary = "장소 상세 조회",
		description = "홈 섹션 카드에서 진입하는 장소 상세를 조회합니다. " +
			"여행 코스의 방문지 상세(`GET /api/v1/trips/{tripId}/places/{contentId}`)와 **같은 응답 형태**이므로 " +
			"클라이언트는 상세 화면 하나를 두 진입점에서 재사용할 수 있습니다. " +
			"축제도 TourAPI 콘텐츠이므로 `contentTypeId=15`로 이 API에서 조회합니다.",
	)
	@ApiResponses(
		value = [
			SwaggerApiResponse(
				responseCode = "200",
				description = "처리 완료: 조회 성공(status=200), 잘못된 요청(status=400), 인증 오류(status=401), 장소 없음(status=404)",
			),
			SwaggerApiResponse(responseCode = "500", description = "TourAPI 호출 실패 등 서버 오류"),
		],
	)
	@GetMapping("/{contentId}")
	fun getPlaceDetail(
		@CurrentUser currentUser: AuthenticatedUser,
		@Parameter(description = "TourAPI contentId", example = "126508")
		@PathVariable contentId: String,
		@Parameter(
			description = "TourAPI 콘텐츠 타입 ID. 홈 섹션 항목은 관광지 `12`, 축제 `15`입니다. 모르면 생략할 수 있습니다.",
			example = "12",
			schema = Schema(allowableValues = ["12", "14", "15", "28", "38", "39"]),
		)
		@RequestParam(required = false) contentTypeId: String?,
	): ApiResponse<TripPlaceDetailResponse> =
		ApiResponse.success(
			message = "장소 상세 조회 성공",
			data = tripPlaceService.getPublicPlaceDetail(currentUser, contentId, contentTypeId),
		)
}
