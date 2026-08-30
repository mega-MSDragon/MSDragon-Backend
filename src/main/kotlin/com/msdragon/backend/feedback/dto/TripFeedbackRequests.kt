package com.msdragon.backend.feedback.dto

import com.msdragon.backend.feedback.entity.FeedbackBodyCondition
import com.msdragon.backend.feedback.entity.FeedbackTag
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.math.BigDecimal

@Schema(description = "부모 여행 피드백 제출 요청")
data class SubmitTripFeedbackRequest(
	@field:Schema(description = "전체 만족도. 0.0부터 5.0까지 0.5 단위로 입력합니다.", example = "4.5")
	@field:NotNull(message = "전체 만족도를 입력해주세요.")
	@field:DecimalMin(value = "0.0", message = "전체 만족도는 0.0 이상이어야 합니다.")
	@field:DecimalMax(value = "5.0", message = "전체 만족도는 5.0 이하여야 합니다.")
	val overallRating: BigDecimal,

	@field:Schema(
		description = "여행 후 몸 상태",
		example = "comfortable",
		allowableValues = ["comfortable", "slightly_tired", "very_tired"],
	)
	@field:NotNull(message = "여행 후 몸 상태를 선택해주세요.")
	val bodyCondition: FeedbackBodyCondition,

	@field:Schema(
		description = "좋았던 점. 최대 3개까지 선택할 수 있습니다.",
		example = "[\"walking_comfortable\",\"rest_time_good\"]",
		allowableValues = [
			"walking_comfortable",
			"rest_time_good",
			"scenery_good",
			"transport_comfortable",
			"food_good",
			"seating_sufficient",
		],
	)
	@field:Size(max = 3, message = "좋았던 점은 최대 3개까지 선택할 수 있습니다.")
	val goodTags: List<FeedbackTag> = emptyList(),

	@field:Schema(
		description = "다음 여행에서 개선할 점. 여러 개 선택할 수 있습니다.",
		example = "[\"more_rest_needed\",\"long_travel_time\"]",
		allowableValues = [
			"more_rest_needed",
			"many_stairs_or_slopes",
			"long_travel_time",
			"crowded",
		],
	)
	@field:Size(max = 4, message = "개선할 점은 최대 4개까지 선택할 수 있습니다.")
	val improvementTags: List<FeedbackTag> = emptyList(),

	@field:Schema(description = "이번 여행에서 가장 좋았던 방문지의 tripStopId", example = "15")
	@field:NotNull(message = "가장 좋았던 방문지를 선택해주세요.")
	val bestTripStopId: Long,

	@field:Schema(
		description = "자유 의견. 앞뒤 공백을 제거한 뒤 30자까지 저장하며, 공백만 입력하면 저장하지 않습니다. 이모지는 1자로 셉니다.",
		example = "여행 계획 짜느라 고생 많았어, 우리 딸",
		nullable = true,
	)
	val freeComment: String? = null,
)
