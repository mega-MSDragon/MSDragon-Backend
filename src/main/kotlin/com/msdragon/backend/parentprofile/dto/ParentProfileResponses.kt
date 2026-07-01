package com.msdragon.backend.parentprofile.dto

import com.msdragon.backend.auth.entity.User
import com.msdragon.backend.parentprofile.entity.ActivityLevel
import com.msdragon.backend.parentprofile.entity.FoodPreference
import com.msdragon.backend.parentprofile.entity.ParentProfile
import com.msdragon.backend.parentprofile.entity.ParentProfileStatus
import com.msdragon.backend.parentprofile.entity.TravelPersonalityTypeCode
import com.msdragon.backend.parentprofile.entity.TravelThemeCode
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "부모님 프로필 응답")
data class ParentProfileResponse(
	@field:Schema(description = "부모님 프로필 ID. 아직 저장된 프로필이 없으면 null입니다.", example = "1", nullable = true)
	val id: Long?,

	@field:Schema(description = "부모 사용자 ID", example = "2")
	val parentUserId: Long,

	@field:Schema(description = "프로필 저장 여부", example = "true")
	val profileExists: Boolean,

	@field:Schema(description = "프로필 상태", example = "completed", allowableValues = ["draft", "completed"])
	val status: ParentProfileStatus,

	@field:Schema(description = "현재 작성 단계", example = "3")
	val currentStep: Int,

	@field:Schema(description = "체력 수준", example = "moderate", allowableValues = ["slow", "moderate", "active"], nullable = true)
	val activityLevel: ActivityLevel?,

	@field:Schema(description = "이동 도움 필요 여부", example = "true", nullable = true)
	val needsMobilityAssistance: Boolean?,

	@field:Schema(description = "선호 여행 테마", example = "[\"nature\",\"history\"]", allowableValues = ["nature", "history", "activity", "food", "culture", "landmark"])
	val themeCodes: List<TravelThemeCode>,

	@field:Schema(description = "음식 취향", example = "korean_only", allowableValues = ["korean_only", "familiar_food", "open_minded"], nullable = true)
	val foodPreference: FoodPreference?,

	@field:Schema(description = "매운 음식 회피 여부", example = "true")
	val avoidSpicy: Boolean,

	@field:Schema(description = "추천용 부모님 여행 MBTI", example = "relaxed_explorer", allowableValues = ["city_taster", "sensitive_culture", "relaxed_explorer", "history_walker", "active_experiencer", "local_challenger"], nullable = true)
	val personalityType: TravelPersonalityTypeCode?,

	@field:Schema(description = "프로필 작성률", example = "100")
	val completionPercent: Int,

	@field:Schema(description = "프로필 작성 완료 시간", example = "2026-07-01T12:00:00", nullable = true)
	val completedAt: LocalDateTime?,
) {
	companion object {
		fun from(profile: ParentProfile): ParentProfileResponse =
			ParentProfileResponse(
				id = profile.id,
				parentUserId = requireNotNull(profile.user.id),
				profileExists = true,
				status = profile.status,
				currentStep = profile.currentStep,
				activityLevel = profile.activityLevel,
				needsMobilityAssistance = profile.needsMobilityAssistance,
				themeCodes = profile.themeCodes.map(TravelThemeCode::from),
				foodPreference = profile.foodPreference,
				avoidSpicy = profile.avoidSpicy,
				personalityType = profile.personalityType,
				completionPercent = profile.completionPercent,
				completedAt = profile.completedAt,
			)

		fun empty(parent: User): ParentProfileResponse =
			ParentProfileResponse(
				id = null,
				parentUserId = requireNotNull(parent.id),
				profileExists = false,
				status = ParentProfileStatus.DRAFT,
				currentStep = 1,
				activityLevel = null,
				needsMobilityAssistance = null,
				themeCodes = emptyList(),
				foodPreference = null,
				avoidSpicy = false,
				personalityType = null,
				completionPercent = 0,
				completedAt = null,
			)
	}
}
