package com.msdragon.backend.family.dto

import com.msdragon.backend.auth.entity.User
import com.msdragon.backend.family.entity.Family
import com.msdragon.backend.family.entity.FamilyMember
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "가족 코드 응답")
data class FamilyCodeResponse(
	@field:Schema(description = "내 가족 초대 코드", example = "MSH-2405")
	val code: String,
)

@Schema(description = "가족 코드 매칭 응답")
data class FamilyMatchResponse(
	@field:Schema(description = "가족 ID", example = "1")
	val familyId: Long,

	@field:Schema(description = "연결된 상대방")
	val matchedUser: FamilyUserResponse,

	@field:Schema(description = "가족 구성원 목록")
	val members: List<FamilyMemberResponse>,
) {
	companion object {
		fun of(
			family: Family,
			matchedUser: User,
			members: List<FamilyMember>,
		): FamilyMatchResponse =
			FamilyMatchResponse(
				familyId = requireNotNull(family.id),
				matchedUser = FamilyUserResponse.from(matchedUser),
				members = members.map(FamilyMemberResponse::from),
			)
	}
}

@Schema(description = "가족 사용자 응답")
data class FamilyUserResponse(
	@field:Schema(description = "사용자 ID", example = "1")
	val id: Long,

	@field:Schema(description = "역할", example = "parent", allowableValues = ["child", "parent"])
	val role: String,

	@field:Schema(description = "이름 또는 닉네임", example = "엄마")
	val displayName: String,
) {
	companion object {
		fun from(user: User): FamilyUserResponse =
			FamilyUserResponse(
				id = requireNotNull(user.id),
				role = user.role.value,
				displayName = user.displayName,
			)
	}
}

@Schema(description = "가족 구성원 응답")
data class FamilyMemberResponse(
	@field:Schema(description = "사용자 ID", example = "1")
	val userId: Long,

	@field:Schema(description = "역할", example = "child", allowableValues = ["child", "parent"])
	val role: String,

	@field:Schema(description = "이름 또는 닉네임", example = "최혜린")
	val displayName: String,

	@field:Schema(description = "가족 관계 표시 이름", example = "엄마", nullable = true)
	val relationLabel: String?,
) {
	companion object {
		fun from(member: FamilyMember): FamilyMemberResponse =
			FamilyMemberResponse(
				userId = requireNotNull(member.user.id),
				role = member.memberRole.value,
				displayName = member.user.displayName,
				relationLabel = member.relationLabel,
			)
	}
}
