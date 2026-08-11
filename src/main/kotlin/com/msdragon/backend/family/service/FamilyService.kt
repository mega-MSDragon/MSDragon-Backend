package com.msdragon.backend.family.service

import com.msdragon.backend.auth.entity.User
import com.msdragon.backend.auth.entity.UserRole
import com.msdragon.backend.auth.repository.UserRepository
import com.msdragon.backend.common.exception.BadRequestException
import com.msdragon.backend.common.exception.InternalServerException
import com.msdragon.backend.common.exception.NotFoundException
import com.msdragon.backend.common.exception.UnAuthorizedException
import com.msdragon.backend.family.dto.FamilyCodeResponse
import com.msdragon.backend.family.dto.FamilyMatchResponse
import com.msdragon.backend.family.dto.MyFamilyResponse
import com.msdragon.backend.family.dto.MatchFamilyCodeRequest
import com.msdragon.backend.family.entity.Family
import com.msdragon.backend.family.entity.FamilyCode
import com.msdragon.backend.family.entity.FamilyCodeUsage
import com.msdragon.backend.family.entity.FamilyMember
import com.msdragon.backend.family.repository.FamilyCodeRepository
import com.msdragon.backend.family.repository.FamilyCodeUsageRepository
import com.msdragon.backend.family.repository.FamilyMemberRepository
import com.msdragon.backend.family.repository.FamilyRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom

@Service
class FamilyService(
	private val userRepository: UserRepository,
	private val familyRepository: FamilyRepository,
	private val familyMemberRepository: FamilyMemberRepository,
	private val familyCodeRepository: FamilyCodeRepository,
	private val familyCodeUsageRepository: FamilyCodeUsageRepository,
) {
	private val random = SecureRandom()

	@Transactional
	fun issueMyCode(userId: Long): FamilyCodeResponse {
		val user = getLoginUser(userId)
		val familyCode = familyCodeRepository.findByUserId(userId)
			?: familyCodeRepository.save(FamilyCode(user = user, code = generateUniqueCode()))

		return FamilyCodeResponse(code = familyCode.code)
	}

	@Transactional(readOnly = true)
	fun getMyFamily(userId: Long): MyFamilyResponse {
		getLoginUser(userId)
		val myCode = familyCodeRepository.findByUserId(userId)?.code
		val myMember = familyMemberRepository.findByUserId(userId)
			?: return MyFamilyResponse.empty(myCode)
		return MyFamilyResponse.of(
			family = myMember.family,
			myCode = myCode,
			members = familyMembers(myMember.family),
		)
	}

	@Transactional
	fun matchByCode(userId: Long, request: MatchFamilyCodeRequest): FamilyMatchResponse {
		val requester = getLoginUser(userId)
		val targetCode = familyCodeRepository.findByCodeAndIsActiveTrue(normalizeCode(request.code))
			?: throw NotFoundException("가족 코드를 찾을 수 없습니다.")
		val targetUser = targetCode.user

		if (targetUser.deletedAt != null || !targetUser.isSignupCompleted()) {
			throw NotFoundException("가족 코드를 찾을 수 없습니다.")
		}
		if (requester.id == targetUser.id) {
			throw BadRequestException("내 코드는 입력할 수 없습니다.")
		}
		if (requester.role == targetUser.role) {
			throw BadRequestException("부모와 자녀만 가족으로 연결할 수 있습니다.")
		}

		val child = if (requester.role == UserRole.CHILD) requester else targetUser
		val parent = if (requester.role == UserRole.PARENT) requester else targetUser
		val childMember = familyMemberRepository.findByUserId(requireNotNull(child.id))
		val parentMember = familyMemberRepository.findByUserId(requireNotNull(parent.id))

		if (parentMember != null && childMember != null && parentMember.family.id != childMember.family.id) {
			throw BadRequestException("이미 다른 가족과 연결된 사용자입니다.")
		}
		if (parentMember != null && childMember == null) {
			throw BadRequestException("이미 다른 가족과 연결된 부모입니다.")
		}

		val family = childMember?.family ?: createChildFamily(child)
		if (parentMember != null) {
			recordCodeUsageIfNeeded(targetCode, requester, family)
			return FamilyMatchResponse.of(family, targetUser, familyMembers(family))
		}

		val familyId = requireNotNull(family.id)
		if (familyMemberRepository.countByFamilyIdAndMemberRole(familyId, UserRole.PARENT) >= MAX_PARENT_COUNT) {
			throw BadRequestException("가족에는 부모를 최대 2명까지만 연결할 수 있습니다.")
		}

		familyMemberRepository.save(
			FamilyMember(
				family = family,
				user = parent,
				memberRole = UserRole.PARENT,
			),
		)
		recordCodeUsageIfNeeded(targetCode, requester, family)

		return FamilyMatchResponse.of(family, targetUser, familyMembers(family))
	}

	private fun getLoginUser(userId: Long): User =
		userRepository.findByIdAndDeletedAtIsNull(userId)
			?.takeIf { it.isSignupCompleted() }
			?: throw UnAuthorizedException("로그인할 수 없는 사용자입니다.")

	private fun createChildFamily(child: User): Family {
		val family = familyRepository.save(Family(ownerUser = child))
		familyMemberRepository.save(
			FamilyMember(
				family = family,
				user = child,
				memberRole = UserRole.CHILD,
			),
		)
		return family
	}

	private fun recordCodeUsageIfNeeded(
		familyCode: FamilyCode,
		requester: User,
		family: Family,
	) {
		val familyCodeId = requireNotNull(familyCode.id)
		val requesterUserId = requireNotNull(requester.id)
		if (!familyCodeUsageRepository.existsByFamilyCodeIdAndRequesterUserId(familyCodeId, requesterUserId)) {
			familyCodeUsageRepository.save(
				FamilyCodeUsage(
					familyCode = familyCode,
					requesterUser = requester,
					family = family,
				),
			)
		}
	}

	private fun familyMembers(family: Family): List<FamilyMember> =
		familyMemberRepository.findAllByFamilyIdOrderByJoinedAtAsc(requireNotNull(family.id))

	private fun generateUniqueCode(): String {
		repeat(CODE_GENERATION_MAX_ATTEMPTS) {
			val code = "MSH-%04d".format(random.nextInt(CODE_NUMBER_BOUND))
			if (!familyCodeRepository.existsByCode(code)) {
				return code
			}
		}
		throw InternalServerException("가족 코드를 생성할 수 없습니다.")
	}

	private fun normalizeCode(code: String): String {
		val compactCode = code.uppercase().replace("-", "")
		return "${compactCode.take(3)}-${compactCode.drop(3)}"
	}

	companion object {
		private const val MAX_PARENT_COUNT = 2L
		private const val CODE_GENERATION_MAX_ATTEMPTS = 30
		private const val CODE_NUMBER_BOUND = 10_000
	}
}
