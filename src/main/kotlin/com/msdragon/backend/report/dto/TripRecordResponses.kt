package com.msdragon.backend.report.dto

import com.msdragon.backend.trip.dto.TripDestinationResponse
import com.msdragon.backend.trip.dto.TripParticipantResponse
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.LocalDate

@Schema(description = "기록 탭 여행 목록과 상단 통계")
data class TripRecordsResponse(
	@field:Schema(description = "가족 ID. 아직 가족 매칭 전이면 null입니다.", example = "1", nullable = true)
	val familyId: Long?,

	@field:Schema(description = "완료 여행 통계")
	val statistics: TripRecordStatisticsResponse,

	@field:Schema(description = "종료일 최신순 완료 여행 목록")
	val records: List<TripRecordSummaryResponse>,
) {
	companion object {
		fun empty(familyId: Long? = null): TripRecordsResponse =
			TripRecordsResponse(
				familyId = familyId,
				statistics = TripRecordStatisticsResponse(
					completedTripCount = 0,
					averageRating = null,
					totalPlaceCount = 0,
					totalDistanceKm = null,
				),
				records = emptyList(),
			)
	}
}

@Schema(description = "기록 탭 상단 완료 여행 통계")
data class TripRecordStatisticsResponse(
	@field:Schema(description = "완료 여행 수", example = "3")
	val completedTripCount: Int,

	@field:Schema(description = "여행별 만족도 평균의 평균. 제출된 피드백이 없으면 null입니다.", example = "4.7", nullable = true)
	val averageRating: BigDecimal?,

	@field:Schema(description = "완료 여행 전체 방문지 수", example = "14")
	val totalPlaceCount: Int,

	@field:Schema(description = "Tmap 경로가 있는 완료 여행의 대략적인 이동거리 합계(km)", example = "107.50", nullable = true)
	val totalDistanceKm: BigDecimal?,
)

@Schema(description = "기록 탭 여행 카드")
data class TripRecordSummaryResponse(
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

	@field:Schema(description = "현재 제출된 부모 피드백 평균. 제출된 피드백이 없으면 null입니다.", example = "4.8", nullable = true)
	val averageRating: BigDecimal?,

	@field:Schema(description = "효도 리포트 생성 완료 여부", example = "true")
	val reportReady: Boolean,
)
