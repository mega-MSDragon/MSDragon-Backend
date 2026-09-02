package com.msdragon.backend.profile.dto

import com.msdragon.backend.auth.entity.User
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "내 프로필 응답")
data class MyProfileResponse(
	@field:Schema(description = "사용자 ID", example = "1")
	val id: Long,

	@field:Schema(description = "역할", example = "child", allowableValues = ["child", "parent"])
	val role: String,

	@field:Schema(description = "이름 또는 닉네임", example = "최혜린")
	val displayName: String,

	@field:Schema(description = "연령대", example = "20s")
	val ageBand: String,

	@field:Schema(description = "성별", example = "female", allowableValues = ["female", "male", "undisclosed"])
	val gender: String,

	@field:Schema(
		description = "프리셋 프로필 이미지 식별자. 선택하지 않았으면 null이며 클라이언트는 기본 실루엣을 표시합니다.",
		example = "coral",
		allowableValues = ["green", "coral", "yellow", "blue"],
		nullable = true,
	)
	val profileImage: String?,
) {
	companion object {
		fun from(user: User): MyProfileResponse =
			MyProfileResponse(
				id = requireNotNull(user.id),
				role = user.role.value,
				displayName = user.displayName,
				ageBand = user.ageBand.value,
				gender = user.gender.value,
				profileImage = user.profileImage?.value,
			)
	}
}
