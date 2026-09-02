package com.msdragon.backend.profile.dto

import com.msdragon.backend.auth.entity.AgeBand
import com.msdragon.backend.auth.entity.GenderType
import com.msdragon.backend.auth.entity.UserProfileImage
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size

@Schema(description = "내 프로필 수정 요청")
data class UpdateMyProfileRequest(
	@field:Schema(description = "이름 또는 닉네임", example = "최혜린", nullable = true)
	@field:Size(max = 50, message = "이름은 50자 이하로 입력해주세요.")
	val displayName: String? = null,

	@field:Schema(description = "연령대. 역할별 허용 범위가 다르며 서버에서 검증합니다.", example = "20s", allowableValues = ["10s", "20s", "30s", "40s", "50s", "60s", "60s_plus", "70s", "80s", "90s_plus", "undisclosed"], nullable = true)
	val ageBand: AgeBand? = null,

	@field:Schema(description = "성별", example = "female", allowableValues = ["female", "male", "undisclosed"], nullable = true)
	val gender: GenderType? = null,

	@field:Schema(
		description = "프리셋 프로필 이미지 식별자. 필드를 생략하면 변경하지 않고, `none`을 보내면 아바타를 지워 기본 실루엣으로 되돌립니다.",
		example = "coral",
		allowableValues = ["green", "coral", "yellow", "blue", "none"],
		nullable = true,
	)
	val profileImage: UserProfileImage? = null,
)
