package com.msdragon.backend.family.controller

import com.msdragon.backend.auth.support.AuthenticatedUser
import com.msdragon.backend.auth.support.CurrentUser
import com.msdragon.backend.common.config.BEARER_AUTH_SCHEME
import com.msdragon.backend.common.response.ApiResponse
import com.msdragon.backend.family.dto.FamilyCodeResponse
import com.msdragon.backend.family.dto.FamilyMatchResponse
import com.msdragon.backend.family.dto.MatchFamilyCodeRequest
import com.msdragon.backend.family.dto.MyFamilyResponse
import com.msdragon.backend.family.service.FamilyService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/family")
@Tag(name = "Family", description = "가족 코드 발급과 가족 매칭 API 입니다.")
@SecurityRequirement(name = BEARER_AUTH_SCHEME)
class FamilyController(
	private val familyService: FamilyService,
) {
	@Operation(
		summary = "내 가족 조회",
		description = "로그인 사용자의 가족과 구성원을 조회합니다. 아직 매칭되지 않았으면 familyId는 null이고 members는 빈 배열입니다.",
	)
	@ApiResponses(
		value = [
			SwaggerApiResponse(responseCode = "200", description = "내 가족 조회 성공"),
			SwaggerApiResponse(responseCode = "401", description = "인증 실패"),
		],
	)
	@GetMapping
	fun getMyFamily(
		@CurrentUser currentUser: AuthenticatedUser,
	): ApiResponse<MyFamilyResponse> =
		ApiResponse.success(
			message = "내 가족 조회 성공",
			data = familyService.getMyFamily(currentUser.id),
		)

	@Operation(
		summary = "내 가족 코드 발급/조회",
		description = "로그인 사용자의 고정 가족 코드를 발급하거나 기존 코드를 조회합니다. Authorization Bearer access token이 필요합니다.",
	)
	@ApiResponses(
		value = [
			SwaggerApiResponse(responseCode = "200", description = "가족 코드 조회 성공"),
			SwaggerApiResponse(responseCode = "401", description = "인증 실패"),
		],
	)
	@PostMapping("/code")
	fun issueMyCode(
		@CurrentUser currentUser: AuthenticatedUser,
	): ApiResponse<FamilyCodeResponse> =
		ApiResponse.success(
			message = "가족 코드 조회 성공",
			data = familyService.issueMyCode(currentUser.id),
		)

	@Operation(
		summary = "상대방 코드로 가족 매칭",
		description = "상대방 가족 코드를 입력해 부모-자녀 가족 관계를 생성합니다. 자녀 1명, 부모 최대 2명까지만 연결됩니다.",
	)
	@ApiResponses(
		value = [
			SwaggerApiResponse(responseCode = "200", description = "가족 매칭 성공"),
			SwaggerApiResponse(responseCode = "400", description = "매칭할 수 없는 요청"),
			SwaggerApiResponse(responseCode = "401", description = "인증 실패"),
			SwaggerApiResponse(responseCode = "404", description = "가족 코드를 찾을 수 없음"),
		],
	)
	@PostMapping("/matches")
	fun matchByCode(
		@CurrentUser currentUser: AuthenticatedUser,
		@Valid @RequestBody request: MatchFamilyCodeRequest,
	): ApiResponse<FamilyMatchResponse> =
		ApiResponse.success(
			message = "가족 매칭 성공",
			data = familyService.matchByCode(currentUser.id, request),
		)
}
