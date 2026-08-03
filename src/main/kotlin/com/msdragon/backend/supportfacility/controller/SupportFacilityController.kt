package com.msdragon.backend.supportfacility.controller

import com.msdragon.backend.auth.support.AuthenticatedUser
import com.msdragon.backend.auth.support.CurrentUser
import com.msdragon.backend.common.config.BEARER_AUTH_SCHEME
import com.msdragon.backend.common.response.ApiResponse
import com.msdragon.backend.supportfacility.dto.NearbyRestroomResponse
import com.msdragon.backend.supportfacility.service.SupportFacilityService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
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
@RequestMapping("/api/v1/trips")
@Tag(name = "Support Facility", description = "여행 모드 주변 편의시설 API입니다.")
@SecurityRequirement(name = BEARER_AUTH_SCHEME)
class SupportFacilityController(
	private val supportFacilityService: SupportFacilityService,
) {
	@Operation(
		summary = "현재 위치 주변 공중화장실 조회",
		description = "여행 기간 중 같은 가족 구성원이 현재 위치 기준 직선거리 5km 이내 공중화장실을 가까운 순으로 최대 10개 조회합니다.",
	)
	@ApiResponses(
		value = [
			SwaggerApiResponse(responseCode = "200", description = "처리 완료: 조회 성공(status=200) 또는 요청·인증·정책 오류(status=400/401/403/404)"),
		],
	)
	@GetMapping("/{tripId}/nearby-restrooms")
	fun getNearbyRestrooms(
		@CurrentUser currentUser: AuthenticatedUser,
		@Parameter(description = "여행 ID", example = "1")
		@PathVariable tripId: Long,
		@Parameter(description = "현재 위치 WGS84 위도", example = "37.5758692")
		@RequestParam latitude: Double,
		@Parameter(description = "현재 위치 WGS84 경도", example = "126.9684817")
		@RequestParam longitude: Double,
	): ApiResponse<List<NearbyRestroomResponse>> =
		ApiResponse.success(
			message = "주변 공중화장실 조회 성공",
			data = supportFacilityService.getNearbyRestrooms(currentUser, tripId, latitude, longitude),
		)
}
