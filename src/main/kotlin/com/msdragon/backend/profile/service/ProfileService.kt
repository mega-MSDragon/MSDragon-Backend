package com.msdragon.backend.profile.service

import com.msdragon.backend.auth.entity.UserRole
import com.msdragon.backend.auth.repository.UserRefreshTokenRepository
import com.msdragon.backend.auth.repository.UserRepository
import com.msdragon.backend.auth.service.UserProfilePolicy
import com.msdragon.backend.common.exception.BadRequestException
import com.msdragon.backend.common.exception.UnAuthorizedException
import com.msdragon.backend.family.repository.FamilyCodeRepository
import com.msdragon.backend.family.repository.FamilyMemberRepository
import com.msdragon.backend.profile.dto.MyProfileResponse
import com.msdragon.backend.profile.dto.UpdateMyProfileRequest
import com.msdragon.backend.trip.entity.TripStatus
import com.msdragon.backend.trip.repository.TripRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.UUID

@Service
class ProfileService(
	private val userRepository: UserRepository,
	private val userRefreshTokenRepository: UserRefreshTokenRepository,
	private val userProfilePolicy: UserProfilePolicy,
	private val familyMemberRepository: FamilyMemberRepository,
	private val familyCodeRepository: FamilyCodeRepository,
	private val tripRepository: TripRepository,
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

	@Transactional
	fun withdraw(userId: Long) {
		val user = getLoginUser(userId)
		val withdrawnAt = currentDateTime()
		familyCodeRepository.findByUserId(userId)?.deactivate()
		userRefreshTokenRepository.findAllByUserIdAndRevokedAtIsNull(userId)
			.forEach { it.revoke() }

		val member = familyMemberRepository.findByUserId(userId)
		if (member != null && member.memberRole == UserRole.CHILD) {
			val family = member.family
			family.deactivate()
			tripRepository.findAllByFamilyIdAndDeletedAtIsNullOrderByStartDateAscIdAsc(requireNotNull(family.id))
				.forEach { trip ->
					trip.synchronizeStatus(currentDate())
					if (trip.status != TripStatus.COMPLETED) {
						trip.archive()
					}
				}
			familyMemberRepository.deleteAll(
				familyMemberRepository.findAllByFamilyIdOrderByJoinedAtAsc(requireNotNull(family.id)),
			)
		} else if (member != null) {
			familyMemberRepository.delete(member)
		}

		user.withdraw(
			withdrawnAt = withdrawnAt,
			withdrawnOauthSubject = "withdrawn:${requireNotNull(user.id)}:${UUID.randomUUID()}",
		)
	}

	private fun getLoginUser(userId: Long) =
		userRepository.findByIdAndDeletedAtIsNull(userId)
			?.takeIf { it.isSignupCompleted() }
			?: throw UnAuthorizedException("로그인할 수 없는 사용자입니다.")

	private fun currentDate(): LocalDate = LocalDate.now(SERVICE_ZONE_ID)

	private fun currentDateTime(): LocalDateTime =
		LocalDateTime.now(SERVICE_ZONE_ID).truncatedTo(ChronoUnit.MICROS)

	companion object {
		private val SERVICE_ZONE_ID: ZoneId = ZoneId.of("Asia/Seoul")
	}
}
