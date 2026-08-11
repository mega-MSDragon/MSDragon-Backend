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

	@field:Schema(description = "결과 화면 유형명", example = "유유자적 힐링러형")
	val name: String,

	@field:Schema(description = "결과 화면 한 줄 문구", example = "여행은 쉬러 가는 거지.")
	val catchphrase: String,

	@field:Schema(
		description = "결과 화면 유형 설명",
		example = "자연풍경, 역사, 산책을 좋아하며 천천히 둘러보는 타입이시네요. 음식도 한식처럼 편안한 선택을 선호하시는 편이에요.",
	)
	val description: String,
) {
	companion object {
		fun from(type: TravelPersonalityTypeCode): TravelPersonalityResultResponse =
			when (type) {
				TravelPersonalityTypeCode.URBAN_EXPLORER -> TravelPersonalityResultResponse(
					code = type,
					name = "도시 취향 탐험가형",
					catchphrase = "유명한 곳은 알차게 둘러봐야지.",
					description = "쇼핑, 문화생활, 랜드마크를 좋아하며 도시의 볼거리를 알차게 즐기는 타입이시네요. 적당히 움직이고 익숙한 음식을 편안하게 즐기시는 편이에요.",
				)
				TravelPersonalityTypeCode.CULTURE_STROLLER -> TravelPersonalityResultResponse(
					code = type,
					name = "감성 문화 산책가형",
					catchphrase = "좋은 곳에서는 천천히 쉬어가도 괜찮아.",
					description = "문화생활과 랜드마크를 좋아하며 여유 있게 도시를 둘러보는 타입이시네요. 중간중간 카페나 맛집에서 쉬어가는 일정을 선호하시는 편이에요.",
				)
				TravelPersonalityTypeCode.HEALING_TRAVELER -> TravelPersonalityResultResponse(
					code = type,
					name = "유유자적 힐링러형",
					catchphrase = "여행은 쉬러 가는 거지.",
					description = "자연풍경, 역사, 산책을 좋아하며 천천히 둘러보는 타입이시네요. 음식도 한식처럼 편안한 선택을 선호하시는 편이에요.",
				)
				TravelPersonalityTypeCode.HERITAGE_WALKER -> TravelPersonalityResultResponse(
					code = type,
					name = "역사 산책가형",
					catchphrase = "이야기가 있는 길을 걷는 게 좋아.",
					description = "역사적인 장소와 자연풍경을 좋아하며 적당히 걸으면서 여유를 챙기는 타입이시네요. 익숙한 음식 안에서 지역의 특색도 함께 즐기시는 편이에요.",
				)
				TravelPersonalityTypeCode.ACTIVE_ADVENTURER -> TravelPersonalityResultResponse(
					code = type,
					name = "액티비티 열정가형",
					catchphrase = "가만히 있기엔 여행 시간이 아까워.",
					description = "액티비티와 체험, 이동이 많은 일정을 좋아하는 타입이시네요. 여러 장소를 둘러보고 새로운 음식에도 적극적으로 도전하시는 편이에요.",
				)
				TravelPersonalityTypeCode.LOCAL_CHALLENGER -> TravelPersonalityResultResponse(
					code = type,
					name = "로컬 도전가형",
					catchphrase = "여행은 직접 해보고 먹어봐야지.",
					description = "체험형 여행과 새로운 음식, 현지 분위기를 좋아하는 타입이시네요. 유명 관광지만 보기보다 직접 경험하고 맛보는 데서 여행의 재미를 찾으시는 편이에요.",
				)
			}
	}
}
