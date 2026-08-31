package com.msdragon.backend.feedback.dto

import com.msdragon.backend.feedback.entity.FeedbackBodyCondition
import com.msdragon.backend.feedback.entity.FeedbackTag
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.LocalDateTime

@Schema(description = "여행 피드백 제출 현황")
data class TripFeedbackStatusResponse(
	@field:Schema(description = "여행 ID", example = "1")
	val tripId: Long,

	@field:Schema(description = "마지막 날이 시작되었거나 여행이 수동 종료되어 피드백을 작성할 수 있는지 여부", example = "true")
	val feedbackAvailable: Boolean,

	@field:Schema(description = "참여 부모 수", example = "2")
	val totalParentCount: Int,

	@field:Schema(description = "평가 요청을 받은 부모 수", example = "2")
	val requestedParentCount: Int,

	@field:Schema(description = "피드백 제출을 마친 부모 수", example = "1")
	val submittedParentCount: Int,

	@field:Schema(description = "현재 사용자가 자녀 평가 요청을 보낼 수 있는지 여부", example = "true")
	val canRequest: Boolean,

	@field:Schema(description = "현재 사용자가 본인 피드백을 제출할 수 있는지 여부", example = "false")
	val canSubmit: Boolean,

	@field:Schema(description = "모든 참여 부모가 제출해 효도 리포트를 만들 수 있는지 여부", example = "false")
	val reportReady: Boolean,

	@field:Schema(description = "참여 부모별 요청·제출 상태")
	val parents: List<TripParentFeedbackStatusResponse>,
)

@Schema(description = "참여 부모별 피드백 상태")
data class TripParentFeedbackStatusResponse(
	@field:Schema(description = "부모 사용자 ID", example = "2")
	val parentUserId: Long,

	@field:Schema(description = "부모 표시 이름", example = "길순님")
	val displayName: String,

	@field:Schema(description = "가족 관계 표시값", example = "엄마", nullable = true)
	val relationLabel: String?,

	@field:Schema(description = "평가 요청 시간", example = "2026-07-26T10:00:00", nullable = true)
	val requestedAt: LocalDateTime?,

	@field:Schema(description = "피드백 제출 시간", example = "2026-07-26T11:00:00", nullable = true)
	val submittedAt: LocalDateTime?,

	@field:Schema(
		description = "이 부모가 제출한 전체 만족도. 아직 제출하지 않았으면 null입니다. 효도 리포트 생성 전에도 제출한 부모의 별점을 표시할 수 있습니다.",
		example = "4.5",
		nullable = true,
	)
	val overallRating: BigDecimal?,
)

@Schema(description = "부모 여행 피드백")
data class TripFeedbackResponse(
	@field:Schema(description = "피드백 ID", example = "1")
	val id: Long,

	@field:Schema(description = "여행 ID", example = "1")
	val tripId: Long,

	@field:Schema(description = "작성 부모 사용자 ID", example = "2")
	val parentUserId: Long,

	@field:Schema(description = "전체 만족도", example = "4.5")
	val overallRating: BigDecimal,

	@field:Schema(
		description = "여행 후 몸 상태",
		example = "comfortable",
		allowableValues = ["comfortable", "slightly_tired", "very_tired"],
	)
	val bodyCondition: FeedbackBodyCondition,

	@field:Schema(description = "좋았던 점")
	val goodTags: List<FeedbackTag>,

	@field:Schema(description = "다음 여행에서 개선할 점")
	val improvementTags: List<FeedbackTag>,

	@field:Schema(description = "가장 좋았던 방문지")
	val bestPlace: TripFeedbackBestPlaceResponse,

	@field:Schema(description = "자유 의견", example = "다음에도 함께 여행하고 싶어요.", nullable = true)
	val freeComment: String?,

	@field:Schema(description = "제출 시간", example = "2026-07-26T11:00:00")
	val submittedAt: LocalDateTime,

	@field:Schema(description = "모든 참여 부모가 제출해 효도 리포트를 만들 수 있는지 여부", example = "false")
	val reportReady: Boolean,
)

@Schema(description = "피드백에서 선택한 베스트 장소")
data class TripFeedbackBestPlaceResponse(
	@field:Schema(description = "제출 당시 방문지 tripStopId", example = "15")
	val tripStopId: Long,

	@field:Schema(description = "제출 당시 장소명", example = "오도리 공원")
	val name: String,
)
