package com.msdragon.backend.auth.service

import com.msdragon.backend.auth.dto.AuthResponse
import com.msdragon.backend.auth.dto.AuthUserResponse
import com.msdragon.backend.auth.dto.CompleteSignupRequest
import com.msdragon.backend.auth.dto.LogoutRequest
import com.msdragon.backend.auth.dto.RefreshTokenRequest
import com.msdragon.backend.auth.dto.SocialLoginRequest
import com.msdragon.backend.auth.entity.DevicePlatform
import com.msdragon.backend.auth.entity.User
import com.msdragon.backend.auth.entity.UserRefreshToken
import com.msdragon.backend.auth.repository.UserRefreshTokenRepository
import com.msdragon.backend.auth.repository.UserRepository
import com.msdragon.backend.common.exception.BadRequestException
import com.msdragon.backend.common.exception.UnAuthorizedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class AuthService(
	private val userRepository: UserRepository,
	private val userRefreshTokenRepository: UserRefreshTokenRepository,
	private val oAuthClientResolver: OAuthClientResolver,
	private val tokenService: TokenService,
	private val userProfilePolicy: UserProfilePolicy,
) {
	@Transactional
	fun socialLogin(request: SocialLoginRequest): AuthResponse {
		val oAuthUserInfo = oAuthClientResolver.resolve(request.provider).verify(request.token)
		val user = userRepository.findByOauthProviderAndOauthSubjectAndDeletedAtIsNull(
			oauthProvider = oAuthUserInfo.provider,
			oauthSubject = oAuthUserInfo.subject,
		)

		if (user == null) {
			return AuthResponse(
				signupRequired = true,
				signupToken = tokenService.createSignupToken(oAuthUserInfo),
			)
		}

		user.updateLastLogin()
		return createLoginResponse(user, request.platform)
	}

	@Transactional
	fun completeSignup(request: CompleteSignupRequest): AuthResponse {
		val signupClaims = tokenService.parseSignupToken(request.signupToken)

		userProfilePolicy.validateAgeBand(request.role, request.ageBand)

		val existingUser = userRepository.findByOauthProviderAndOauthSubjectAndDeletedAtIsNull(
			oauthProvider = signupClaims.provider,
			oauthSubject = signupClaims.subject,
		)
		if (existingUser != null) {
			if (existingUser.isSignupCompleted()) {
				throw BadRequestException("이미 가입 완료된 사용자입니다.")
			}
			existingUser.completeSignup(request.role, request.displayName.trim(), request.ageBand, request.gender)
			existingUser.updateLastLogin()
			return createLoginResponse(existingUser, request.platform)
		}

		val user = User(
			role = request.role,
			oauthProvider = signupClaims.provider,
			oauthSubject = signupClaims.subject,
			displayName = request.displayName.trim(),
			ageBand = request.ageBand,
			gender = request.gender,
			signupCompletedAt = LocalDateTime.now(),
			lastLoginAt = LocalDateTime.now(),
		)
		val savedUser = userRepository.save(user)
		return createLoginResponse(savedUser, request.platform)
	}

	@Transactional
	fun refresh(request: RefreshTokenRequest): AuthResponse {
		val refreshTokenHash = tokenService.hashRefreshToken(request.refreshToken)
		val savedRefreshToken = userRefreshTokenRepository.findByRefreshTokenHash(refreshTokenHash)
			?: throw UnAuthorizedException("refresh token이 유효하지 않습니다.")

		if (!savedRefreshToken.isActive()) {
			throw UnAuthorizedException("refresh token이 만료되었거나 폐기되었습니다.")
		}

		savedRefreshToken.markUsed()
		savedRefreshToken.revoke()

		val user = savedRefreshToken.user
		if (user.deletedAt != null || !user.isSignupCompleted()) {
			throw UnAuthorizedException("로그인할 수 없는 사용자입니다.")
		}

		return createLoginResponse(user, savedRefreshToken.platform)
	}

	@Transactional
	fun logout(request: LogoutRequest) {
		val refreshTokenHash = tokenService.hashRefreshToken(request.refreshToken)
		val savedRefreshToken = userRefreshTokenRepository.findByRefreshTokenHash(refreshTokenHash) ?: return

		if (savedRefreshToken.revokedAt == null) {
			savedRefreshToken.revoke()
		}
	}

	private fun createLoginResponse(
		user: User,
		platform: DevicePlatform?,
	): AuthResponse {
		val accessToken = tokenService.createAccessToken(user)
		val refreshToken = tokenService.generateRefreshToken()
		val issuedAt = LocalDateTime.now()
		userRefreshTokenRepository.save(
			UserRefreshToken(
				user = user,
				refreshTokenHash = tokenService.hashRefreshToken(refreshToken),
				platform = platform,
				issuedAt = issuedAt,
				expiresAt = issuedAt.plus(tokenService.refreshTokenExpiration()),
			),
		)

		return AuthResponse(
			signupRequired = false,
			accessToken = accessToken,
			refreshToken = refreshToken,
			accessTokenExpiresInSeconds = tokenService.accessTokenExpiresInSeconds(),
			refreshTokenExpiresInSeconds = tokenService.refreshTokenExpiresInSeconds(),
			user = AuthUserResponse.from(user),
		)
	}

}
