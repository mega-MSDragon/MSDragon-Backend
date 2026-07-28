package com.msdragon.backend.report.dto

import com.msdragon.backend.feedback.entity.FeedbackBodyCondition
import com.msdragon.backend.feedback.entity.FeedbackTag
import com.msdragon.backend.trip.dto.TripDestinationResponse
import com.msdragon.backend.trip.dto.TripParticipantResponse
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Schema(description = "효도 리포트")
data class FilialReportResponse(
	@field:Schema(description = "효도 리포트 ID", example = "1")
	val id: Long,

	@field:Schema(description = "여행 ID", example = "1")
	val tripId: Long,

	@field:Schema(description = "여행 제목", example = "부산 온천 가족여행")
	val title: String,

	@field:Schema(description = "여행 도시")
	val destination: TripDestinationResponse,

	@field:Schema(description = "여행 시작일", example = "2026-07-10")
	val startDate: LocalDate,

	@field:Schema(description = "여행 종료일", example = "2026-07-11")
	val endDate: LocalDate,

	@field:Schema(description = "여행 참여자")
	val participants: List<TripParticipantResponse>,

	@field:Schema(description = "대표 이미지 URL", nullable = true)
	val coverImageUrl: String?,

	@field:Schema(description = "방문지 수", example = "5")
	val totalPlaceCount: Int,

	@field:Schema(description = "부모 전체 만족도 평균", example = "4.8")
	val averageRating: BigDecimal,

	@field:Schema(description = "Tmap 일자별 경로를 합산한 대략적인 이동거리(km)", example = "7.50", nullable = true)
	val totalDistanceKm: BigDecimal?,

	@field:Schema(description = "효도 지수. 산식 확정 전에는 null입니다.", nullable = true)
	val totalScore: Int?,

	@field:Schema(description = "만족도 점수. 산식 확정 전에는 null입니다.", nullable = true)
	val satisfactionScore: Int?,

	@field:Schema(description = "다리 편안함 점수. 산식 확정 전에는 null입니다.", nullable = true)
	val legComfortScore: Int?,

	@field:Schema(description = "잔소리 방지 점수. 산식 확정 전에는 null입니다.", nullable = true)
	val naggingPreventionScore: Int?,

	@field:Schema(description = "식사 만족 점수. 산식 확정 전에는 null입니다.", nullable = true)
	val mealSatisfactionScore: Int?,

	@field:Schema(description = "화장실 안심 점수. 산식 확정 전에는 null입니다.", nullable = true)
	val restroomSafetyScore: Int?,

	@field:Schema(description = "리포트 수상 문구. 확정 전에는 null입니다.", nullable = true)
	val awardTitle: String?,

	@field:Schema(description = "리포트 요약. 확정 전에는 null입니다.", nullable = true)
	val summary: String?,

	@field:Schema(description = "좋았던 점 태그. 부모가 중복 선택해도 한 번만 반환합니다.")
	val goodTags: List<FeedbackTag>,

	@field:Schema(description = "개선할 점 태그. 부모가 중복 선택해도 한 번만 반환합니다.")
	val improvementTags: List<FeedbackTag>,

	@field:Schema(description = "부모별 피드백")
	val parentFeedbacks: List<FilialReportParentFeedbackResponse>,

	@field:Schema(description = "일차와 순서대로 정렬한 방문지 목록")
	val stops: List<FilialReportStopResponse>,

	@field:Schema(description = "걸음 수. 측정 기준 확정 전에는 null입니다.", nullable = true)
	val totalStepCount: Int?,

	@field:Schema(description = "공유 이미지 URL. 공유 디자인 구현 전에는 null입니다.", nullable = true)
	val shareImageUrl: String?,

	@field:Schema(description = "최초 리포트 생성 시간", example = "2026-07-28T12:00:00")
	val generatedAt: LocalDateTime,
)

@Schema(description = "효도 리포트의 부모별 피드백")
data class FilialReportParentFeedbackResponse(
	@field:Schema(description = "부모 사용자 ID", example = "2")
	val parentUserId: Long,

	@field:Schema(description = "부모 표시 이름", example = "길순님")
	val displayName: String,

	@field:Schema(description = "가족 관계 표시값", example = "엄마", nullable = true)
	val relationLabel: String?,

	@field:Schema(description = "전체 만족도", example = "4.5")
	val overallRating: BigDecimal,

	@field:Schema(
		description = "여행 후 몸 상태",
		example = "comfortable",
		allowableValues = ["comfortable", "slightly_tired", "very_tired"],
	)
	val bodyCondition: FeedbackBodyCondition,

	@field:Schema(description = "부모가 선택한 베스트 장소")
	val bestPlace: FilialReportBestPlaceResponse,

	@field:Schema(description = "부모 자유 의견", nullable = true)
	val freeComment: String?,

	@field:Schema(description = "피드백 제출 시간")
	val submittedAt: LocalDateTime,
)

@Schema(description = "부모가 선택한 베스트 장소")
data class FilialReportBestPlaceResponse(
	@field:Schema(description = "제출 당시 방문지 ID", example = "15")
	val tripStopId: Long,

	@field:Schema(description = "제출 당시 장소명", example = "오도리 공원")
	val name: String,

	@field:Schema(description = "현재 코스에 장소가 남아 있을 때의 이미지 URL", nullable = true)
	val imageUrl: String?,
)

@Schema(description = "효도 리포트 방문지")
data class FilialReportStopResponse(
	@field:Schema(description = "방문지 ID", example = "15")
	val tripStopId: Long,

	@field:Schema(description = "여행 일차", example = "1")
	val dayNumber: Int,

	@field:Schema(description = "일차 내 방문 순서", example = "2")
	val sortOrder: Int,

	@field:Schema(description = "장소명", example = "해운대 해수욕장")
	val name: String,

	@field:Schema(description = "장소 카테고리", example = "관광지", nullable = true)
	val category: String?,

	@field:Schema(description = "장소 이미지 URL", nullable = true)
	val imageUrl: String?,
)
