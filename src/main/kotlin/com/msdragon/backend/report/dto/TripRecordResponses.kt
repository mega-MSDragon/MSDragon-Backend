package com.msdragon.backend.report.dto

import com.msdragon.backend.trip.dto.TripDestinationResponse
import com.msdragon.backend.trip.dto.TripParticipantResponse
import com.msdragon.backend.trip.entity.TripStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.LocalDate

@Schema(description = "기록 탭 여행 목록과 상단 통계")
data class TripRecordsResponse(
	@field:Schema(description = "가족 ID. 아직 가족 매칭 전이면 null입니다.", example = "1", nullable = true)
	val familyId: Long?,

	@field:Schema(description = "완료 여행 통계")
	val statistics: TripRecordStatisticsResponse,

	@field:Schema(description = "종료일 최신순 완료·중단 여행 목록")
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

	@field:Schema(description = "기록 상태", example = "completed", allowableValues = ["completed", "stopped"])
	val status: TripStatus,

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

@Schema(description = "기록 상세 화면 전체 데이터")
data class TripRecordDetailResponse(
	@field:Schema(description = "여행 ID", example = "1")
	val tripId: Long,

	@field:Schema(description = "여행 제목", example = "부산 온천 가족여행")
	val title: String,

	@field:Schema(description = "상단 배경 이미지 URL", nullable = true)
	val coverImageUrl: String?,

	@field:Schema(description = "기록 상태", example = "completed", allowableValues = ["completed", "stopped"])
	val status: TripStatus,

	@field:Schema(description = "여행 도시")
	val destination: TripDestinationResponse,

	@field:Schema(description = "여행 시작일", example = "2026-09-12")
	val startDate: LocalDate,

	@field:Schema(description = "여행 종료일", example = "2026-09-14")
	val endDate: LocalDate,

	@field:Schema(description = "동행. `동행` 줄에 표시합니다. 본인은 클라이언트가 `나`로 바꿔 표시합니다.")
	val participants: List<TripParticipantResponse>,

	@field:Schema(description = "`이번 여행을 한눈에` 카드")
	val summary: TripRecordDetailSummaryResponse,

	@field:Schema(description = "일차별 여행 코스. 일차 탭과 지도 보기에 함께 사용합니다.")
	val days: List<TripRecordDayResponse>,

	@field:Schema(description = "여행 10계명 카드")
	val pledge: TripRecordPledgeResponse,

	@field:Schema(description = "효도 리포트 카드")
	val report: TripRecordReportResponse,

	@field:Schema(description = "이 여행을 삭제할 수 있는지 여부. false면 휴지통을 숨깁니다.", example = "true")
	val canDelete: Boolean,
)

@Schema(description = "기록 상세 `이번 여행을 한눈에`")
data class TripRecordDetailSummaryResponse(
	@field:Schema(description = "함께 걸은 길(km). Tmap 경로가 없으면 null입니다.", example = "120.5", nullable = true)
	val totalDistanceKm: BigDecimal?,

	@field:Schema(description = "함께 방문한 장소 수", example = "5")
	val totalPlaceCount: Int,

	@field:Schema(description = "방문지 카테고리별 개수. `관광지 3 · 음식점 2`에 사용하며 방문 순서대로 정렬합니다.")
	val placeCounts: List<TripRecordPlaceCountResponse>,

	@field:Schema(description = "부모님 만족도 평균. 제출된 피드백이 없으면 null이며 화면에는 `-`를 표시합니다.", example = "4.5", nullable = true)
	val averageRating: BigDecimal?,

	@field:Schema(description = "제출된 부모별 별점. `엄마 4.0 · 아빠 5.0`에 사용합니다.")
	val parentRatings: List<TripRecordParentRatingResponse>,
)

@Schema(description = "방문지 카테고리별 개수")
data class TripRecordPlaceCountResponse(
	@field:Schema(description = "카테고리 이름. 코스 저장 시 받은 값을 그대로 씁니다.", example = "관광지")
	val category: String,

	@field:Schema(description = "개수", example = "3")
	val count: Int,
)

@Schema(description = "부모별 제출 별점")
data class TripRecordParentRatingResponse(
	@field:Schema(description = "부모 사용자 ID", example = "2")
	val parentUserId: Long,

	@field:Schema(description = "부모 이름 또는 닉네임", example = "김영희")
	val displayName: String,

	@field:Schema(description = "가족 관계 표시 이름", example = "엄마", nullable = true)
	val relationLabel: String?,

	@field:Schema(description = "제출한 별점", example = "4.0")
	val overallRating: BigDecimal,
)

@Schema(description = "기록 상세 일차별 코스")
data class TripRecordDayResponse(
	@field:Schema(description = "일차", example = "1")
	val dayNumber: Int,

	@field:Schema(description = "일차 날짜. 탭에 `09.12`로 표시합니다.", example = "2026-09-12")
	val travelDate: LocalDate,

	@field:Schema(description = "방문 순서대로 정렬한 방문지")
	val stops: List<TripRecordStopResponse>,
)

@Schema(description = "기록 상세 방문지")
data class TripRecordStopResponse(
	@field:Schema(description = "방문지 ID", example = "10")
	val tripStopId: Long,

	@field:Schema(description = "방문 순서. 지도 핀 번호에도 사용합니다.", example = "1")
	val sortOrder: Int,

	@field:Schema(description = "방문지 이름", example = "대릉원")
	val name: String,

	@field:Schema(description = "카테고리", example = "관광지", nullable = true)
	val category: String?,

	@field:Schema(description = "여행 중 작성한 메모. 없으면 메모 영역을 숨깁니다.", nullable = true)
	val note: String?,

	@field:Schema(description = "위도. 지도 보기에 사용하며 좌표가 없으면 핀을 표시하지 않습니다.", example = "35.8383", nullable = true)
	val latitude: BigDecimal?,

	@field:Schema(description = "경도", example = "129.2126", nullable = true)
	val longitude: BigDecimal?,
)

@Schema(description = "기록 상세 여행 10계명 카드")
data class TripRecordPledgeResponse(
	@field:Schema(description = "10계명을 작성한 적이 있는지 여부. false면 카드를 숨깁니다.", example = "true")
	val exists: Boolean,

	@field:Schema(description = "참여자가 모두 서명했는지 여부", example = "true")
	val allSigned: Boolean,

	@field:Schema(description = "서명한 참여자")
	val signedParticipants: List<TripParticipantResponse>,

	@field:Schema(description = "아직 서명하지 않은 참여자")
	val pendingParticipants: List<TripParticipantResponse>,
)

@Schema(description = "기록 상세 효도 리포트 카드")
data class TripRecordReportResponse(
	@field:Schema(description = "효도 리포트 생성 완료 여부. true면 `보러가기`를 표시합니다.", example = "false")
	val ready: Boolean,

	@field:Schema(description = "별점을 제출한 참여 부모 수", example = "1")
	val submittedParentCount: Int,

	@field:Schema(description = "참여 부모 수", example = "2")
	val totalParentCount: Int,

	@field:Schema(description = "별점을 제출한 부모")
	val submittedParents: List<TripParticipantResponse>,

	@field:Schema(description = "아직 제출하지 않은 부모. 조회자가 여기에 있으면 `별점 남기기`, 아니면 `별점 부탁드리기`를 표시합니다.")
	val pendingParents: List<TripParticipantResponse>,
)
