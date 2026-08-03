package com.msdragon.backend.profile.controller

import com.msdragon.backend.auth.support.AuthenticatedUser
import com.msdragon.backend.auth.support.CurrentUser
import com.msdragon.backend.common.config.BEARER_AUTH_SCHEME
import com.msdragon.backend.common.response.ApiResponse
import com.msdragon.backend.profile.dto.MyProfileResponse
import com.msdragon.backend.profile.dto.UpdateMyProfileRequest
import com.msdragon.backend.profile.service.ProfileService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Profile", description = "마이페이지 내 프로필 API 입니다.")
@SecurityRequirement(name = BEARER_AUTH_SCHEME)
class ProfileController(
	private val profileService: ProfileService,
) {
	@Operation(
		summary = "내 프로필 조회",
		description = "로그인 사용자의 기본 프로필을 조회합니다. Authorization Bearer access token이 필요합니다.",
	)
	@ApiResponses(
		value = [
			SwaggerApiResponse(responseCode = "200", description = "처리 완료: 조회 성공(status=200) 또는 인증 오류(status=401)"),
		],
	)
	@GetMapping("/me")
	fun getMyProfile(
		@CurrentUser currentUser: AuthenticatedUser,
	): ApiResponse<MyProfileResponse> =
		ApiResponse.success(
			message = "내 프로필 조회 성공",
			data = profileService.getMyProfile(currentUser.id),
		)

	@Operation(
		summary = "내 프로필 수정",
		description = "로그인 사용자의 이름, 연령대, 성별을 수정합니다. 전달한 필드만 변경합니다.",
	)
	@ApiResponses(
		value = [
			SwaggerApiResponse(responseCode = "200", description = "처리 완료: 수정 성공(status=200) 또는 요청·인증·정책 오류(status=400/401)"),
		],
	)
	@PatchMapping("/me")
	fun updateMyProfile(
		@CurrentUser currentUser: AuthenticatedUser,
		@Valid @RequestBody request: UpdateMyProfileRequest,
	): ApiResponse<MyProfileResponse> =
		ApiResponse.success(
			message = "내 프로필 수정 성공",
			data = profileService.updateMyProfile(currentUser.id, request),
		)
}
