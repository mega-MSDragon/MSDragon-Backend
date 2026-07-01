package com.msdragon.backend.parentprofile.dto

import com.msdragon.backend.parentprofile.entity.ActivityLevel
import com.msdragon.backend.parentprofile.entity.FoodPreference
import com.msdragon.backend.parentprofile.entity.TravelThemeCode
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size

@Schema(description = "부모님 프로필 저장 요청")
data class UpsertParentProfileRequest(
	@field:Schema(description = "현재 작성 단계. 부분 저장 시 클라이언트가 머문 단계를 저장합니다.", example = "2", nullable = true)
	@field:Min(1, message = "현재 작성 단계는 1 이상이어야 합니다.")
	@field:Max(3, message = "현재 작성 단계는 3 이하이어야 합니다.")
	val currentStep: Int? = null,

	@field:Schema(description = "체력 수준", example = "moderate", allowableValues = ["slow", "moderate", "active"], nullable = true)
	val activityLevel: ActivityLevel? = null,

	@field:Schema(description = "이동 도움 필요 여부. true이면 무장애 정보 추천 가중치로 사용합니다.", example = "true", nullable = true)
	val needsMobilityAssistance: Boolean? = null,

	@field:Schema(description = "선호 여행 테마. 최대 3개까지 선택할 수 있습니다.", example = "[\"nature\",\"history\"]", allowableValues = ["nature", "history", "activity", "food", "culture", "landmark"], nullable = true)
	@field:Size(max = 3, message = "여행 테마는 최대 3개까지 선택할 수 있습니다.")
	val themeCodes: List<TravelThemeCode>? = null,

	@field:Schema(description = "음식 취향", example = "korean_only", allowableValues = ["korean_only", "familiar_food", "open_minded"], nullable = true)
	val foodPreference: FoodPreference? = null,

	@field:Schema(description = "매운 음식 회피 여부", example = "true", nullable = true)
	val avoidSpicy: Boolean? = null,

	@field:Schema(description = "프로필 작성 완료 처리 여부. true이면 추천용 여행 MBTI를 계산합니다.", example = "true")
	val complete: Boolean = false,
)
