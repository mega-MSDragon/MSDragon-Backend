package com.msdragon.backend.parentprofile.dto

import com.msdragon.backend.auth.entity.User
import com.msdragon.backend.parentprofile.entity.FoodPreference
import com.msdragon.backend.parentprofile.entity.ParentProfile
import com.msdragon.backend.parentprofile.entity.ParentProfileStatus
import com.msdragon.backend.parentprofile.entity.TravelPersonalityTypeCode
import com.msdragon.backend.parentprofile.entity.TravelThemeCode
import com.msdragon.backend.parentprofile.entity.WalkingPace
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

	@field:Schema(description = "하루 이동 성향", example = "normal", allowableValues = ["slow", "normal", "fast"], nullable = true)
	val walkingPace: WalkingPace?,

	@field:Schema(description = "이동 도움 필요 여부", example = "true", nullable = true)
	val needsMobilityAssistance: Boolean?,

	@field:Schema(description = "선호 여행 테마", example = "[\"nature_scenery\",\"history_culture\"]", allowableValues = ["nature_scenery", "history_culture", "shopping", "activity", "culture_life", "landmark", "experience"])
	val travelThemes: List<TravelThemeCode>,

	@field:Schema(description = "음식 취향", example = "familiar", allowableValues = ["korean", "familiar", "adventurous"], nullable = true)
	val foodPreference: FoodPreference?,

	@field:Schema(description = "추천용 부모님 여행 MBTI", example = "healing_traveler", allowableValues = ["urban_explorer", "culture_stroller", "healing_traveler", "heritage_walker", "active_adventurer", "local_challenger"], nullable = true)
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
				walkingPace = profile.walkingPace,
				needsMobilityAssistance = profile.needsMobilityAssistance,
				travelThemes = profile.travelThemes.map(TravelThemeCode::from),
				foodPreference = profile.foodPreference,
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
				walkingPace = null,
				needsMobilityAssistance = null,
				travelThemes = emptyList(),
				foodPreference = null,
				personalityType = null,
				completionPercent = 0,
				completedAt = null,
			)
	}
}
