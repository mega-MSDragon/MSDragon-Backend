package com.msdragon.backend.parentprofile.dto

import com.msdragon.backend.auth.entity.User
import com.msdragon.backend.parentprofile.entity.FoodPreference
import com.msdragon.backend.parentprofile.entity.ParentProfile
import com.msdragon.backend.parentprofile.entity.ParentProfileStatus
import com.msdragon.backend.parentprofile.entity.TravelPersonalityTypeCode
import com.msdragon.backend.parentprofile.service.TravelPersonalityPolicy
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

	@field:Schema(description = "부모 이름 또는 닉네임", example = "영희")
	val parentDisplayName: String,

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

	@field:Schema(description = "여행 MBTI 결과 화면 표시 정보. 프로필 완료 전이면 null입니다.", nullable = true)
	val personalityResult: TravelPersonalityResultResponse?,

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
				parentDisplayName = profile.user.displayName,
				profileExists = true,
				status = profile.status,
				currentStep = profile.currentStep,
				walkingPace = profile.walkingPace,
				needsMobilityAssistance = profile.needsMobilityAssistance,
				travelThemes = profile.travelThemes.map(TravelThemeCode::from),
				foodPreference = profile.foodPreference,
				personalityType = profile.personalityType,
				personalityResult = profile.personalityType?.let(TravelPersonalityResultResponse::from),
				completionPercent = profile.completionPercent,
				completedAt = profile.completedAt,
			)

		fun empty(parent: User): ParentProfileResponse =
			ParentProfileResponse(
				id = null,
				parentUserId = requireNotNull(parent.id),
				parentDisplayName = parent.displayName,
				profileExists = false,
				status = ParentProfileStatus.DRAFT,
				currentStep = 1,
				walkingPace = null,
				needsMobilityAssistance = null,
				travelThemes = emptyList(),
				foodPreference = null,
				personalityType = null,
				personalityResult = null,
				completionPercent = 0,
				completedAt = null,
			)
	}
}

@Schema(description = "부모님 여행 MBTI 결과 표시 정보")
data class TravelPersonalityResultResponse(
	@field:Schema(description = "여행 MBTI 코드", example = "healing_traveler")
	val code: TravelPersonalityTypeCode,

	@field:Schema(description = "결과 화면 유형 보조명", example = "풍경 수집가")
	val name: String,

	@field:Schema(description = "결과 화면 유형 별명", example = "쉬엄쉬엄이 최고!")
	val catchphrase: String,

	@field:Schema(
		description = "결과 화면 유형 설명",
		example = "아름다운 풍경 속에서 여유롭게 쉬며 편안하게 둘러보는 여행이 잘 맞아요.",
	)
	val description: String,

	@field:Schema(
		description = "결과 카드 테마 칩. 첫 번째가 대표 테마이며 표시 순서대로 내려줍니다.",
		example = "[\"nature_scenery\",\"history_culture\",\"landmark\"]",
	)
	val themes: List<TravelThemeCode>,
) {
	companion object {
		fun from(type: TravelPersonalityTypeCode): TravelPersonalityResultResponse =
			when (type) {
				TravelPersonalityTypeCode.URBAN_EXPLORER -> TravelPersonalityResultResponse(
					code = type,
					name = "도시 탐험가",
					catchphrase = "볼 건 다 봐야지!",
					description = "대표 명소와 쇼핑을 놓치지 않고, 도시의 볼거리를 알차게 즐기는 여행이 잘 맞아요.",
					themes = TravelPersonalityPolicy.THEMES_BY_TYPE.getValue(type),
				)
				TravelPersonalityTypeCode.CULTURE_STROLLER -> TravelPersonalityResultResponse(
					code = type,
					name = "문화 산책가",
					catchphrase = "분위기가 반이지!",
					description = "공연과 전시, 분위기 좋은 장소를 천천히 감상하며 여행하는 걸 좋아해요.",
					themes = TravelPersonalityPolicy.THEMES_BY_TYPE.getValue(type),
				)
				TravelPersonalityTypeCode.HEALING_TRAVELER -> TravelPersonalityResultResponse(
					code = type,
					name = "풍경 수집가",
					catchphrase = "쉬엄쉬엄이 최고!",
					description = "아름다운 풍경 속에서 여유롭게 쉬며 편안하게 둘러보는 여행이 잘 맞아요.",
					themes = TravelPersonalityPolicy.THEMES_BY_TYPE.getValue(type),
				)
				TravelPersonalityTypeCode.HERITAGE_WALKER -> TravelPersonalityResultResponse(
					code = type,
					name = "시간 여행자",
					catchphrase = "아는 만큼 보인다!",
					description = "유적지와 박물관을 산책하듯 둘러보며 지역의 이야기를 알아가는 걸 좋아해요.",
					themes = TravelPersonalityPolicy.THEMES_BY_TYPE.getValue(type),
				)
				TravelPersonalityTypeCode.ACTIVE_ADVENTURER -> TravelPersonalityResultResponse(
					code = type,
					name = "체험 대장",
					catchphrase = "해봐야 제맛이지!",
					description = "직접 움직이고 참여하는 활동을 즐기며 하루를 활기차게 보내는 편이에요.",
					themes = TravelPersonalityPolicy.THEMES_BY_TYPE.getValue(type),
				)
				TravelPersonalityTypeCode.LOCAL_CHALLENGER -> TravelPersonalityResultResponse(
					code = type,
					name = "골목 탐험가",
					catchphrase = "현지인처럼 즐기자!",
					description = "시장과 골목, 음식과 생활문화를 통해 그 지역만의 매력을 발견하는 걸 좋아해요.",
					themes = TravelPersonalityPolicy.THEMES_BY_TYPE.getValue(type),
				)
			}
	}
}
