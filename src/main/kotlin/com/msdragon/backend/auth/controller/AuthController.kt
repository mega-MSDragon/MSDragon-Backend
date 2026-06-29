package com.msdragon.backend.auth.controller

import com.msdragon.backend.auth.dto.AuthResponse
import com.msdragon.backend.auth.dto.CompleteSignupRequest
import com.msdragon.backend.auth.dto.RefreshTokenRequest
import com.msdragon.backend.auth.dto.SocialLoginRequest
import com.msdragon.backend.auth.service.AuthService
import com.msdragon.backend.common.response.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "소셜 로그인과 회원가입 완료 API 입니다.")
class AuthController(
	private val authService: AuthService,
) {
	@Operation(
		summary = "소셜 로그인",
		description = "카카오 accessToken 또는 애플 identityToken을 검증하고, 가입 상태에 따라 서비스 토큰 또는 회원가입 토큰을 발급합니다.",
	)
	@ApiResponses(
		value = [
			SwaggerApiResponse(responseCode = "200", description = "소셜 로그인 처리 성공"),
			SwaggerApiResponse(responseCode = "401", description = "소셜 로그인 토큰이 유효하지 않음"),
		],
	)
	@PostMapping("/social-login")
	fun socialLogin(
		@Valid @RequestBody request: SocialLoginRequest,
	): ApiResponse<AuthResponse> =
		ApiResponse.success(
			message = "소셜 로그인 처리 성공",
			data = authService.socialLogin(request),
		)

	@Operation(
		summary = "회원가입 완료",
		description = "소셜 로그인 후 받은 signupToken과 역할/이름/연령대/성별을 저장하고 서비스 토큰을 발급합니다.",
	)
	@ApiResponses(
		value = [
			SwaggerApiResponse(responseCode = "201", description = "회원가입 완료"),
			SwaggerApiResponse(responseCode = "400", description = "회원가입 요청 값이 올바르지 않음"),
			SwaggerApiResponse(responseCode = "401", description = "회원가입 토큰이 유효하지 않음"),
		],
	)
	@ResponseStatus(HttpStatus.CREATED)
	@PostMapping("/signup/complete")
	fun completeSignup(
		@Valid @RequestBody request: CompleteSignupRequest,
	): ApiResponse<AuthResponse> =
		ApiResponse.success(
			status = HttpStatus.CREATED.value(),
			message = "회원가입 완료",
			data = authService.completeSignup(request),
		)

	@Operation(
		summary = "토큰 재발급",
		description = "refresh token을 검증하고 새 access token과 refresh token을 발급합니다.",
	)
	@ApiResponses(
		value = [
			SwaggerApiResponse(responseCode = "200", description = "토큰 재발급 성공"),
			SwaggerApiResponse(responseCode = "401", description = "refresh token이 유효하지 않음"),
		],
	)
	@PostMapping("/refresh")
	fun refresh(
		@Valid @RequestBody request: RefreshTokenRequest,
	): ApiResponse<AuthResponse> =
		ApiResponse.success(
			message = "토큰 재발급 성공",
			data = authService.refresh(request),
		)
}
