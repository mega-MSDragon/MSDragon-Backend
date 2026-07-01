package com.msdragon.backend.parentprofile.service

import com.msdragon.backend.auth.entity.User
import com.msdragon.backend.auth.entity.UserRole
import com.msdragon.backend.auth.repository.UserRepository
import com.msdragon.backend.auth.support.AuthenticatedUser
import com.msdragon.backend.common.exception.BadRequestException
import com.msdragon.backend.common.exception.ForbiddenException
import com.msdragon.backend.common.exception.NotFoundException
import com.msdragon.backend.common.exception.UnAuthorizedException
import com.msdragon.backend.family.repository.FamilyMemberRepository
import com.msdragon.backend.parentprofile.dto.ParentProfileResponse
import com.msdragon.backend.parentprofile.dto.UpsertParentProfileRequest
import com.msdragon.backend.parentprofile.entity.ActivityLevel
import com.msdragon.backend.parentprofile.entity.FoodPreference
import com.msdragon.backend.parentprofile.entity.ParentProfile
import com.msdragon.backend.parentprofile.entity.ParentProfileStatus
import com.msdragon.backend.parentprofile.entity.TravelPersonalityTypeCode
import com.msdragon.backend.parentprofile.entity.TravelThemeCode
import com.msdragon.backend.parentprofile.repository.ParentProfileRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class ParentProfileService(
	private val userRepository: UserRepository,
	private val familyMemberRepository: FamilyMemberRepository,
	private val parentProfileRepository: ParentProfileRepository,
) {
	@Transactional(readOnly = true)
	fun getMyParentProfile(currentUser: AuthenticatedUser): ParentProfileResponse {
		val parent = getLoginUser(currentUser.id)
		validateParent(parent)

		return parentProfileRepository.findByUserId(requireNotNull(parent.id))
			?.let(ParentProfileResponse::from)
			?: ParentProfileResponse.empty(parent)
	}

	@Transactional(readOnly = true)
	fun getParentProfile(currentUser: AuthenticatedUser, parentUserId: Long): ParentProfileResponse {
		val requester = getLoginUser(currentUser.id)
		val parent = getParentUser(parentUserId)
		validateParentProfileReadable(requester, parent)

		return parentProfileRepository.findByUserId(parentUserId)
			?.let(ParentProfileResponse::from)
			?: ParentProfileResponse.empty(parent)
	}

	@Transactional
	fun upsertMyParentProfile(
		currentUser: AuthenticatedUser,
		request: UpsertParentProfileRequest,
	): ParentProfileResponse {
		val parent = getLoginUser(currentUser.id)
		validateParent(parent)

		val profile = parentProfileRepository.findByUserId(requireNotNull(parent.id))
			?: parentProfileRepository.save(ParentProfile(user = parent))
		applyRequest(profile, request)

		val shouldComplete = request.complete || profile.status == ParentProfileStatus.COMPLETED
		if (shouldComplete) {
			validateCompletable(profile)
			profile.status = ParentProfileStatus.COMPLETED
			profile.currentStep = COMPLETED_STEP
			profile.completionPercent = 100
			profile.completedAt = profile.completedAt ?: LocalDateTime.now()
			profile.personalityType = resolvePersonalityType(profile)
		} else {
			profile.status = ParentProfileStatus.DRAFT
			profile.completionPercent = calculateDraftCompletionPercent(profile)
		}

		return ParentProfileResponse.from(profile)
	}

	private fun applyRequest(profile: ParentProfile, request: UpsertParentProfileRequest) {
		if (request.currentStep != null) {
			profile.currentStep = request.currentStep
		}
		if (request.activityLevel != null) {
			profile.activityLevel = request.activityLevel
		}
		if (request.needsMobilityAssistance != null) {
			profile.needsMobilityAssistance = request.needsMobilityAssistance
		}
		if (request.themeCodes != null) {
			val distinctThemeCodes = request.themeCodes.distinct()
			if (distinctThemeCodes.size != request.themeCodes.size) {
				throw BadRequestException("여행 테마는 중복 없이 선택해주세요.")
			}
			if (distinctThemeCodes.size > MAX_THEME_COUNT) {
				throw BadRequestException("여행 테마는 최대 3개까지 선택할 수 있습니다.")
			}
			profile.themeCodes.clear()
			profile.themeCodes.addAll(distinctThemeCodes.map { it.value })
		}
		if (request.foodPreference != null) {
			profile.foodPreference = request.foodPreference
		}
		if (request.avoidSpicy != null) {
			profile.avoidSpicy = request.avoidSpicy
		}
	}

	private fun validateCompletable(profile: ParentProfile) {
		if (profile.activityLevel == null) {
			throw BadRequestException("체력 수준을 선택해주세요.")
		}
		if (profile.needsMobilityAssistance == null) {
			throw BadRequestException("이동 도움 필요 여부를 선택해주세요.")
		}
		if (profile.foodPreference == null) {
			throw BadRequestException("음식 취향을 선택해주세요.")
		}
	}

	private fun calculateDraftCompletionPercent(profile: ParentProfile): Int {
		val completedStepCount = listOf(
			profile.activityLevel != null && profile.needsMobilityAssistance != null,
			profile.themeCodes.isNotEmpty(),
			profile.foodPreference != null,
		).count { it }

		return when (completedStepCount) {
			0 -> 0
			1 -> 33
			2 -> 66
			else -> 100
		}
	}

	private fun resolvePersonalityType(profile: ParentProfile): TravelPersonalityTypeCode {
		val themeCodes = profile.themeCodes.map(TravelThemeCode::from).toSet()
		return when {
			profile.activityLevel == ActivityLevel.ACTIVE -> TravelPersonalityTypeCode.ACTIVE_EXPERIENCER
			TravelThemeCode.HISTORY in themeCodes -> TravelPersonalityTypeCode.HISTORY_WALKER
			TravelThemeCode.CULTURE in themeCodes -> TravelPersonalityTypeCode.SENSITIVE_CULTURE
			TravelThemeCode.FOOD in themeCodes || profile.foodPreference == FoodPreference.KOREAN_ONLY -> TravelPersonalityTypeCode.CITY_TASTER
			profile.foodPreference == FoodPreference.OPEN_MINDED -> TravelPersonalityTypeCode.LOCAL_CHALLENGER
			else -> TravelPersonalityTypeCode.RELAXED_EXPLORER
		}
	}

	private fun validateParentProfileReadable(requester: User, parent: User) {
		val requesterId = requireNotNull(requester.id)
		val parentId = requireNotNull(parent.id)
		if (requester.role == UserRole.PARENT && requesterId == parentId) {
			return
		}
		if (requester.role != UserRole.CHILD) {
			throw ForbiddenException("부모 프로필 조회 권한이 없습니다.")
		}

		val requesterMember = familyMemberRepository.findByUserId(requesterId)
			?: throw ForbiddenException("부모 프로필 조회 권한이 없습니다.")
		val parentMember = familyMemberRepository.findByUserId(parentId)
			?: throw ForbiddenException("부모 프로필 조회 권한이 없습니다.")

		if (parentMember.memberRole != UserRole.PARENT || requesterMember.family.id != parentMember.family.id) {
			throw ForbiddenException("부모 프로필 조회 권한이 없습니다.")
		}
	}

	private fun validateParent(user: User) {
		if (user.role != UserRole.PARENT) {
			throw BadRequestException("부모 사용자만 프로필을 작성할 수 있습니다.")
		}
	}

	private fun getParentUser(parentUserId: Long): User =
		userRepository.findByIdAndDeletedAtIsNull(parentUserId)
			?.takeIf { it.isSignupCompleted() && it.role == UserRole.PARENT }
			?: throw NotFoundException("부모 사용자를 찾을 수 없습니다.")

	private fun getLoginUser(userId: Long): User =
		userRepository.findByIdAndDeletedAtIsNull(userId)
			?.takeIf { it.isSignupCompleted() }
			?: throw UnAuthorizedException("로그인할 수 없는 사용자입니다.")

	companion object {
		private const val COMPLETED_STEP = 3
		private const val MAX_THEME_COUNT = 3
	}
}
