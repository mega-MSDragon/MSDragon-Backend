package com.msdragon.backend.profile.service

import com.msdragon.backend.auth.entity.UserProfileImage
import com.msdragon.backend.auth.entity.UserRole
import com.msdragon.backend.auth.repository.UserRefreshTokenRepository
import com.msdragon.backend.auth.repository.UserRepository
import com.msdragon.backend.auth.service.OAuthClientResolver
import com.msdragon.backend.auth.service.OAuthUnlinkTarget
import com.msdragon.backend.auth.service.UserProfilePolicy
import com.msdragon.backend.common.exception.BadRequestException
import com.msdragon.backend.common.exception.UnAuthorizedException
import com.msdragon.backend.family.repository.FamilyCodeRepository
import com.msdragon.backend.family.repository.FamilyMemberRepository
import com.msdragon.backend.profile.dto.MyProfileResponse
import com.msdragon.backend.profile.dto.UpdateMyProfileRequest
import com.msdragon.backend.trip.entity.TripStatus
import com.msdragon.backend.trip.repository.TripRepository
import org.slf4j.LoggerFactory
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
	private val oAuthClientResolver: OAuthClientResolver,
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

		// none은 지우기 요청이다. 필드를 생략하면 기존 아바타를 유지한다.
		if (request.profileImage != null) {
			user.profileImage = request.profileImage.takeIf { it != UserProfileImage.NONE }
		}

		return MyProfileResponse.from(user)
	}

	@Transactional
	fun withdraw(userId: Long) {
		val user = getLoginUser(userId)
		val withdrawnAt = currentDateTime()
		// 익명화가 oauthSubject와 refresh token을 덮어쓰므로 연결 해제 정보를 먼저 확보한다.
		val unlinkTarget = OAuthUnlinkTarget(
			provider = user.oauthProvider,
			subject = user.oauthSubject,
			oauthRefreshToken = user.oauthRefreshToken,
		)
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

		unlinkOauthQuietly(unlinkTarget)
	}

	/**
	 * provider 앱 연결을 해제한다. 애플은 App Store 심사 요구사항이지만 실패가 탈퇴를 막아서는 안 된다.
	 * 이 변경 전에 가입한 사용자는 refresh token이 없어 해제할 수 없고, 이미 사용자가 직접 연결을 끊었을 수도 있으며,
	 * 외부 장애로 탈퇴가 막히면 개인정보 삭제 요구를 거부하는 셈이 된다. 실패는 로그로 남겨 수동 후속 처리한다.
	 *
	 * ponytail: 트랜잭션 안에서 외부 호출을 한다. 타임아웃을 5초로 제한했고 탈퇴는 드문 요청이라 감수한다.
	 * 탈퇴 빈도가 올라가면 커밋 후 실행으로 분리한다.
	 */
	private fun unlinkOauthQuietly(target: OAuthUnlinkTarget) {
		try {
			oAuthClientResolver.resolve(target.provider).unlink(target)
		} catch (e: Exception) {
			log.error(
				"소셜 연결 해제에 실패했습니다. 수동 확인이 필요합니다. provider={} subject={}",
				target.provider.value,
				target.subject,
				e,
			)
		}
	}

	private fun getLoginUser(userId: Long) =
		userRepository.findByIdAndDeletedAtIsNull(userId)
			?.takeIf { it.isSignupCompleted() }
			?: throw UnAuthorizedException("로그인할 수 없는 사용자입니다.")

	private fun currentDate(): LocalDate = LocalDate.now(SERVICE_ZONE_ID)

	private fun currentDateTime(): LocalDateTime =
		LocalDateTime.now(SERVICE_ZONE_ID).truncatedTo(ChronoUnit.MICROS)

	companion object {
		private val log = LoggerFactory.getLogger(ProfileService::class.java)
		private val SERVICE_ZONE_ID: ZoneId = ZoneId.of("Asia/Seoul")
	}
}
