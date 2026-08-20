package com.msdragon.backend.home.dto

import com.msdragon.backend.auth.entity.UserRole
import com.msdragon.backend.parentprofile.entity.TravelThemeCode
import com.msdragon.backend.trip.dto.TripDestinationResponse
import com.msdragon.backend.trip.entity.TripDestinationCode
import com.msdragon.backend.trip.entity.TripStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "홈 나의 여행 응답")
data class HomeMyTripsResponse(
	@field:Schema(description = "가족 ID. 가족 매칭 전이면 null입니다.", example = "1", nullable = true)
	val familyId: Long?,

	@field:Schema(description = "로그인 사용자 역할", example = "child", allowableValues = ["child", "parent"])
	val userRole: UserRole,

	@field:Schema(description = "화면에서 확인할 부모별 프로필 완성 상태")
	val parentProfiles: List<HomeParentProfileResponse>,

	@field:Schema(description = "진행 중이거나 예정된 가족 여행 목록")
	val trips: List<HomeTripSummaryResponse>,
)

@Schema(description = "홈 월별 추천 여행 응답")
data class HomeMonthlyRecommendationsResponse(
	@field:Schema(description = "추천 도시 기준 월", example = "5", minimum = "1", maximum = "12")
	val recommendationMonth: Int,

	@field:Schema(description = "해당 월 추천 도시 3개")
	val recommendedCities: List<HomeRecommendedCityResponse>,
)

@Schema(description = "홈 축제 응답")
data class HomeFestivalsResponse(
	@field:Schema(description = "현재 진행 중이거나 30일 이내 시작하는 추천 축제. TourAPI 장애 시 빈 목록일 수 있습니다.")
	val festivals: List<HomeFestivalResponse>,
)

@Schema(description = "홈 부모별 프로필 상태")
data class HomeParentProfileResponse(
	@field:Schema(description = "부모 사용자 ID", example = "2")
	val userId: Long,

	@field:Schema(description = "부모 이름 또는 닉네임", example = "김영희")
	val displayName: String,

	@field:Schema(description = "성별 기반 관계 이름", example = "엄마", nullable = true)
	val relationLabel: String?,

	@field:Schema(description = "부모 상세 프로필 완성 여부", example = "true")
	val profileCompleted: Boolean,
)

@Schema(description = "홈 여행 카드")
data class HomeTripSummaryResponse(
	@field:Schema(description = "여행 ID", example = "1")
	val id: Long,

	@field:Schema(description = "여행 제목", example = "경주 가족 여행")
	val title: String,

	@field:Schema(description = "여행 도시")
	val destination: TripDestinationResponse,

	@field:Schema(description = "여행 시작일", example = "2026-07-27")
	val startDate: LocalDate,

	@field:Schema(description = "여행 종료일", example = "2026-07-28")
	val endDate: LocalDate,

	@field:Schema(
		description = "홈에 노출되는 여행 상태",
		example = "in_progress",
		allowableValues = ["planning", "ready", "in_progress"],
	)
	val status: TripStatus,

	@field:Schema(description = "여행 시작까지 남은 일수. 진행 중이면 null입니다.", example = "48", nullable = true)
	val dDay: Int?,

	@field:Schema(
		description = "참여 부모의 선택 빈도가 가장 높은 대표 여행 테마. 스냅샷이 없으면 null입니다.",
		example = "history_culture",
		allowableValues = ["nature_scenery", "history_culture", "shopping", "activity", "culture_life", "landmark", "experience"],
		nullable = true,
	)
	val primaryTheme: TravelThemeCode?,

	@field:Schema(
		description = "가장 천천히 걷는 참여 부모 기준 여행 강도. 스냅샷이 없으면 null입니다.",
		example = "low",
		allowableValues = ["low", "normal", "high"],
		nullable = true,
	)
	val intensity: HomeTripIntensity?,
)

@Schema(description = "홈 월별 추천 도시")
data class HomeRecommendedCityResponse(
	@field:Schema(description = "여행 도시 코드", example = "gyeongju")
	val code: TripDestinationCode,

	@field:Schema(description = "화면 표시 이름", example = "경주")
	val displayName: String,

	@field:Schema(description = "TourAPI 대표 이미지 URL. 조회하지 못하면 null입니다.", example = "https://example.com/gyeongju.jpg", nullable = true)
	val imageUrl: String?,
)

@Schema(description = "홈 추천 축제")
data class HomeFestivalResponse(
	@field:Schema(description = "TourAPI 콘텐츠 ID", example = "250119")
	val contentId: String,

	@field:Schema(description = "축제명", example = "안동 선유줄불놀이")
	val title: String,

	@field:Schema(description = "축제 소개 요약. 상세 정보가 없으면 null입니다.", example = "낙동강 위로 불꽃이 이어지는 전통 축제입니다.", nullable = true)
	val summary: String?,

	@field:Schema(description = "대표 이미지 URL", example = "https://example.com/festival.jpg", nullable = true)
	val imageUrl: String?,

	@field:Schema(description = "축제 주소", example = "경상북도 안동시 풍천면", nullable = true)
	val address: String?,

	@field:Schema(description = "화면 표시용 지역 이름", example = "안동", nullable = true)
	val regionName: String?,

	@field:Schema(description = "축제 시작일", example = "2026-08-01")
	val eventStartDate: LocalDate,

	@field:Schema(description = "축제 종료일", example = "2026-08-31")
	val eventEndDate: LocalDate,

	@field:Schema(description = "화면 표시용 태그. 현재 지역과 축제 분류를 내려줍니다.", example = "[\"안동\",\"축제\"]")
	val tags: List<String>,
)
