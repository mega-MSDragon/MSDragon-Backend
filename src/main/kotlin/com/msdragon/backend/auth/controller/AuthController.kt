package com.msdragon.backend.auth.controller

import com.msdragon.backend.auth.dto.AuthResponse
import com.msdragon.backend.auth.dto.CompleteSignupRequest
import com.msdragon.backend.auth.dto.LogoutRequest
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
			SwaggerApiResponse(responseCode = "200", description = "처리 완료: 성공(status=200) 또는 요청·소셜 토큰 오류(status=400/401)"),
			SwaggerApiResponse(responseCode = "500", description = "소셜 인증 제공자 연동 오류"),
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
		description = "소셜 로그인 후 받은 signupToken과 약관 동의, 역할, 이름, 연령대, 성별을 저장하고 서비스 토큰을 발급합니다. 성별을 생략하면 undisclosed로 저장합니다.",
	)
	@ApiResponses(
		value = [
			SwaggerApiResponse(responseCode = "200", description = "처리 완료: 회원가입 성공(status=201) 또는 정책 오류(status=400/401)"),
		],
	)
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
			SwaggerApiResponse(responseCode = "200", description = "처리 완료: 재발급 성공(status=200) 또는 요청·refresh token 오류(status=400/401)"),
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

	@Operation(
		summary = "로그아웃",
		description = "현재 세션의 refresh token을 폐기합니다. 이미 폐기되었거나 서버에 없는 토큰도 성공으로 처리하며, 클라이언트는 보관 중인 access/refresh token을 삭제해야 합니다.",
	)
	@ApiResponses(
		value = [
			SwaggerApiResponse(responseCode = "200", description = "처리 완료: 로그아웃 성공(status=200) 또는 요청 값 오류(status=400)"),
		],
	)
	@PostMapping("/logout")
	fun logout(
		@Valid @RequestBody request: LogoutRequest,
	): ApiResponse<Unit> {
		authService.logout(request)
		return ApiResponse.success(message = "로그아웃 성공")
	}
}
