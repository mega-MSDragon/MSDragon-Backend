package com.msdragon.backend.profile.controller

import com.msdragon.backend.auth.support.AuthenticatedUser
import com.msdragon.backend.auth.support.CurrentUser
import com.msdragon.backend.common.response.ApiResponse
import com.msdragon.backend.profile.dto.MyProfileResponse
import com.msdragon.backend.profile.dto.UpdateMyProfileRequest
import com.msdragon.backend.profile.service.ProfileService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
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
class ProfileController(
	private val profileService: ProfileService,
) {
	@Operation(
		summary = "내 프로필 조회",
		description = "로그인 사용자의 기본 프로필을 조회합니다. Authorization Bearer access token이 필요합니다.",
	)
	@ApiResponses(
		value = [
			SwaggerApiResponse(responseCode = "200", description = "내 프로필 조회 성공"),
			SwaggerApiResponse(responseCode = "401", description = "인증 실패"),
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
			SwaggerApiResponse(responseCode = "200", description = "내 프로필 수정 성공"),
			SwaggerApiResponse(responseCode = "400", description = "수정 요청 값이 올바르지 않음"),
			SwaggerApiResponse(responseCode = "401", description = "인증 실패"),
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
