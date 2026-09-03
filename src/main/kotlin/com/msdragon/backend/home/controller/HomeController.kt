package com.msdragon.backend.home.controller

import com.msdragon.backend.auth.support.AuthenticatedUser
import com.msdragon.backend.auth.support.CurrentUser
import com.msdragon.backend.common.config.BEARER_AUTH_SCHEME
import com.msdragon.backend.common.response.ApiResponse
import com.msdragon.backend.home.dto.HomeFestivalsResponse
import com.msdragon.backend.home.dto.HomeSectionsResponse
import com.msdragon.backend.home.dto.HomeMonthlyRecommendationsResponse
import com.msdragon.backend.home.dto.HomeMyTripsResponse
import com.msdragon.backend.home.service.HomeService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/home")
@Tag(name = "Home", description = "홈 화면 섹션별 조회 API 입니다.")
@SecurityRequirement(name = BEARER_AUTH_SCHEME)
class HomeController(
	private val homeService: HomeService,
) {
	@Operation(summary = "홈 나의 여행 조회", description = "사용자 역할, 부모별 프로필 완성 여부와 진행·완료·예정 여행을 조회합니다. 완료 여행에는 제출된 부모별 별점이 포함됩니다.")
	@ApiResponses(
		value = [
			SwaggerApiResponse(responseCode = "200", description = "처리 완료: 조회 성공(status=200) 또는 인증 오류(status=401)"),
		],
	)
	@GetMapping("/my-trips")
	fun getMyTrips(
		@CurrentUser currentUser: AuthenticatedUser,
	): ApiResponse<HomeMyTripsResponse> =
		ApiResponse.success(
			message = "나의 여행 조회 성공",
			data = homeService.getMyTrips(currentUser),
		)

	@Operation(summary = "홈 월별 추천 여행 조회", description = "서울 기준 현재 월과 해당 월의 고정 추천 도시 5개를 조회합니다.")
	@ApiResponses(
		value = [
			SwaggerApiResponse(responseCode = "200", description = "처리 완료: 조회 성공(status=200) 또는 인증 오류(status=401)"),
		],
	)
	@GetMapping("/monthly-recommendations")
	fun getMonthlyRecommendations(): ApiResponse<HomeMonthlyRecommendationsResponse> =
		ApiResponse.success(
			message = "월별 추천 여행 조회 성공",
			data = homeService.getMonthlyRecommendations(),
		)

	@Operation(
		summary = "홈 축제 조회 (deprecated)",
		description = "현재 진행 중이거나 30일 이내 시작하는 축제를 조회합니다. " +
			"`GET /api/v1/home/sections`의 `festivals` 섹션으로 대체되었습니다. " +
			"기존 앱 버전 호환을 위해 유지하며, 해당 버전이 사용되지 않게 되면 제거합니다.",
		deprecated = true,
	)
	@ApiResponses(
		value = [
			SwaggerApiResponse(responseCode = "200", description = "처리 완료: 조회 성공(status=200) 또는 인증 오류(status=401)"),
		],
	)
	@Deprecated("GET /api/v1/home/sections의 festivals 섹션을 사용한다.")
	@GetMapping("/festivals")
	fun getFestivals(): ApiResponse<HomeFestivalsResponse> =
		ApiResponse.success(
			message = "축제 조회 성공",
			data = homeService.getFestivals(),
		)

	@Operation(
		summary = "홈 동적 섹션 조회",
		description = "홈 축제 영역부터 아래까지를 섹션 목록으로 조회합니다. " +
			"배열 순서가 화면 노출 순서이며 서버가 결정합니다. " +
			"모든 섹션은 같은 카드로 그려지므로 클라이언트는 순서대로 렌더링만 하면 됩니다. " +
			"섹션을 추가·삭제·재정렬하거나 항목 내용을 바꾸는 일은 모두 서버에서 끝나며 앱 수정이 필요하지 않습니다. " +
			"항목의 `contentTypeId`를 `GET /api/v1/places/{contentId}`에 넘겨 상세로 이동합니다.",
	)
	@ApiResponses(
		value = [
			SwaggerApiResponse(responseCode = "200", description = "처리 완료: 조회 성공(status=200) 또는 인증 오류(status=401)"),
		],
	)
	@GetMapping("/sections")
	fun getSections(): ApiResponse<HomeSectionsResponse> =
		ApiResponse.success(
			message = "홈 섹션 조회 성공",
			data = homeService.getSections(),
		)
}
