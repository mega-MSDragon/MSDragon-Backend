package com.msdragon.backend.profile.service

import com.msdragon.backend.auth.repository.UserRepository
import com.msdragon.backend.auth.service.UserProfilePolicy
import com.msdragon.backend.common.exception.BadRequestException
import com.msdragon.backend.common.exception.UnAuthorizedException
import com.msdragon.backend.profile.dto.MyProfileResponse
import com.msdragon.backend.profile.dto.UpdateMyProfileRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ProfileService(
	private val userRepository: UserRepository,
	private val userProfilePolicy: UserProfilePolicy,
) {
	@Transactional(readOnly = true)
	fun getMyProfile(userId: Long): MyProfileResponse =
		MyProfileResponse.from(getLoginUser(userId))

	@Transactional
	fun updateMyProfile(userId: Long, request: UpdateMyProfileRequest): MyProfileResponse {
		val user = getLoginUser(userId)
		val displayName = request.displayName?.trim()
		if (displayName != null) {
			if (displayName.isBlank()) {
				throw BadRequestException("이름을 입력해주세요.")
			}
			user.displayName = displayName
		}

		if (request.ageBand != null) {
			userProfilePolicy.validateAgeBand(user.role, request.ageBand)
			user.ageBand = request.ageBand
		}

		if (request.gender != null) {
			user.gender = request.gender
		}

		return MyProfileResponse.from(user)
	}

	private fun getLoginUser(userId: Long) =
		userRepository.findByIdAndDeletedAtIsNull(userId)
			?.takeIf { it.isSignupCompleted() }
			?: throw UnAuthorizedException("로그인할 수 없는 사용자입니다.")
}
