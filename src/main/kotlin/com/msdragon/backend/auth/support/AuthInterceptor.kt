package com.msdragon.backend.auth.support

import com.msdragon.backend.auth.repository.UserRepository
import com.msdragon.backend.auth.service.TokenService
import com.msdragon.backend.common.exception.UnAuthorizedException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

@Component
class AuthInterceptor(
	private val tokenService: TokenService,
	private val userRepository: UserRepository,
) : HandlerInterceptor {
	override fun preHandle(
		request: HttpServletRequest,
		response: HttpServletResponse,
		handler: Any,
	): Boolean {
		val accessToken = extractBearerToken(request.getHeader(AUTHORIZATION_HEADER))
		val claims = tokenService.parseAccessToken(accessToken)
		val user = userRepository.findByIdAndDeletedAtIsNull(claims.userId)
			?: throw UnAuthorizedException("로그인할 수 없는 사용자입니다.")

		if (!user.isSignupCompleted()) {
			throw UnAuthorizedException("로그인할 수 없는 사용자입니다.")
		}

		request.setAttribute(
			AuthRequestAttributes.CURRENT_USER,
			AuthenticatedUser(
				id = requireNotNull(user.id),
				role = user.role,
			),
		)
		return true
	}

	private fun extractBearerToken(authorizationHeader: String?): String {
		if (authorizationHeader.isNullOrBlank()) {
			throw UnAuthorizedException("Authorization 헤더가 필요합니다.")
		}
		if (!authorizationHeader.startsWith(BEARER_PREFIX, ignoreCase = true)) {
			throw UnAuthorizedException("Bearer 토큰 형식이 올바르지 않습니다.")
		}

		val token = authorizationHeader.substring(BEARER_PREFIX.length).trim()
		if (token.isBlank()) {
			throw UnAuthorizedException("Bearer 토큰이 비어있습니다.")
		}
		return token
	}

	companion object {
		private const val AUTHORIZATION_HEADER = "Authorization"
		private const val BEARER_PREFIX = "Bearer "
	}
}
