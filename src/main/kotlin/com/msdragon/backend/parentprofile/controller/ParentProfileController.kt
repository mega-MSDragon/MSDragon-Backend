package com.msdragon.backend.parentprofile.controller

import com.msdragon.backend.auth.support.AuthenticatedUser
import com.msdragon.backend.auth.support.CurrentUser
import com.msdragon.backend.common.config.BEARER_AUTH_SCHEME
import com.msdragon.backend.common.response.ApiResponse
import com.msdragon.backend.parentprofile.dto.ParentProfileResponse
import com.msdragon.backend.parentprofile.dto.UpsertParentProfileRequest
import com.msdragon.backend.parentprofile.service.ParentProfileService
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
@RequestMapping("/api/v1/parent-profiles")
@Tag(name = "ParentProfile", description = "부모님 상세 프로필 API 입니다.")
@SecurityRequirement(name = BEARER_AUTH_SCHEME)
class ParentProfileController(
	private val parentProfileService: ParentProfileService,
) {
	@Operation(
		summary = "내 부모님 프로필 조회",
		description = "부모 사용자가 본인의 상세 프로필을 조회합니다. 완료된 프로필은 여행 MBTI 결과 화면용 이름과 문구를 함께 반환하며, 아직 저장된 프로필이 없으면 빈 draft 응답을 반환합니다.",
	)
	@ApiResponses(
		value = [
			SwaggerApiResponse(responseCode = "200", description = "처리 완료: 조회 성공(status=200) 또는 인증·정책 오류(status=400/401)"),
		],
	)
	@GetMapping("/me")
	fun getMyParentProfile(
		@CurrentUser currentUser: AuthenticatedUser,
	): ApiResponse<ParentProfileResponse> =
		ApiResponse.success(
			message = "내 부모님 프로필 조회 성공",
			data = parentProfileService.getMyParentProfile(currentUser),
		)

	@Operation(
		summary = "부모님 프로필 조회",
		description = "자녀가 같은 가족으로 연결된 부모의 상세 프로필과 여행 MBTI 결과 화면용 정보를 조회합니다. 부모 본인도 자기 프로필을 조회할 수 있습니다.",
	)
	@ApiResponses(
		value = [
			SwaggerApiResponse(responseCode = "200", description = "처리 완료: 조회 성공(status=200) 또는 인증·정책 오류(status=401/403/404)"),
		],
	)
	@GetMapping("/{parentUserId}")
	fun getParentProfile(
		@CurrentUser currentUser: AuthenticatedUser,
		@Parameter(description = "조회할 부모 사용자 ID", example = "2")
		@PathVariable parentUserId: Long,
	): ApiResponse<ParentProfileResponse> =
		ApiResponse.success(
			message = "부모님 프로필 조회 성공",
			data = parentProfileService.getParentProfile(currentUser, parentUserId),
		)

	@Operation(
		summary = "내 부모님 프로필 저장",
		description = "부모 사용자가 본인의 상세 프로필을 작성하거나 수정합니다. complete=true이면 여행 MBTI를 계산하고 결과 화면용 이름과 문구를 반환합니다. 자녀는 호출할 수 없습니다.",
	)
	@ApiResponses(
		value = [
			SwaggerApiResponse(responseCode = "200", description = "처리 완료: 저장 성공(status=200) 또는 요청·인증·정책 오류(status=400/401)"),
		],
	)
	@PutMapping("/me")
	fun upsertMyParentProfile(
		@CurrentUser currentUser: AuthenticatedUser,
		@Valid @RequestBody request: UpsertParentProfileRequest,
	): ApiResponse<ParentProfileResponse> =
		ApiResponse.success(
			message = "내 부모님 프로필 저장 성공",
			data = parentProfileService.upsertMyParentProfile(currentUser, request),
		)
}
