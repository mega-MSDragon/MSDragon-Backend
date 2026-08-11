package com.msdragon.backend.home.controller

import com.msdragon.backend.auth.support.AuthenticatedUser
import com.msdragon.backend.auth.support.CurrentUser
import com.msdragon.backend.common.config.BEARER_AUTH_SCHEME
import com.msdragon.backend.common.response.ApiResponse
import com.msdragon.backend.home.dto.HomeResponse
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
@Tag(name = "Home", description = "홈 화면 집계 API 입니다.")
@SecurityRequirement(name = BEARER_AUTH_SCHEME)
class HomeController(
	private val homeService: HomeService,
) {
	@Operation(
		summary = "홈 조회",
		description = "로그인 사용자의 역할, 부모님 프로필 안내, 진행·예정 여행, 월별 추천 도시와 축제를 한 번에 조회합니다. 완료·보관 여행은 제외하며 TourAPI 장애 시 추천 이미지는 null, 축제는 빈 목록 또는 직전 캐시로 반환합니다.",
	)
	@ApiResponses(
		value = [
			SwaggerApiResponse(responseCode = "200", description = "처리 완료: 조회 성공(status=200) 또는 인증 오류(status=401)"),
		],
	)
	@GetMapping
	fun getHome(
		@CurrentUser currentUser: AuthenticatedUser,
	): ApiResponse<HomeResponse> =
		ApiResponse.success(
			message = "홈 조회 성공",
			data = homeService.getHome(currentUser),
		)
}
