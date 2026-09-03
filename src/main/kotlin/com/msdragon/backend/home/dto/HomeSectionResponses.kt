package com.msdragon.backend.home.dto

import com.fasterxml.jackson.annotation.JsonValue
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(
	description = "홈 동적 섹션 목록. 배열 순서가 화면 노출 순서이며 서버가 결정합니다. " +
		"클라이언트는 순서를 그대로 유지하고, 모르는 type의 섹션은 건너뜁니다.",
)
data class HomeSectionsResponse(
	@field:Schema(description = "노출 순서대로 정렬된 섹션 목록")
	val sections: List<HomeSectionResponse>,
)

@Schema(description = "홈 섹션")
data class HomeSectionResponse(
	@field:Schema(description = "섹션 식별자. 같은 type을 여러 섹션에서 쓸 수 있으므로 화면 상태 관리에 사용합니다.", example = "monthly_attractions")
	val key: String,

	@field:Schema(
		description = "섹션 종류. 클라이언트는 이 값으로 렌더러를 고르고, 모르는 값이면 섹션을 건너뜁니다.",
		example = "attraction_collection",
		allowableValues = ["festival_collection", "attraction_collection"],
	)
	val type: HomeSectionType,

	@field:Schema(description = "섹션 제목", example = "이번 달 가볼 만한 곳")
	val title: String,

	@field:Schema(description = "섹션 부제. 없으면 null입니다.", example = "경주 · 전주 · 제주", nullable = true)
	val subtitle: String?,

	@field:Schema(description = "섹션 항목. 외부 API 장애 시 빈 배열일 수 있습니다.")
	val items: List<HomeSectionItemResponse>,
)

@Schema(
	description = "홈 섹션 항목. 모든 섹션이 같은 형태를 사용하며 섹션 종류에 따라 일부 필드가 null입니다.",
)
data class HomeSectionItemResponse(
	@field:Schema(description = "TourAPI 콘텐츠 ID", example = "126508")
	val contentId: String,

	@field:Schema(description = "항목명", example = "불국사")
	val title: String,

	@field:Schema(description = "소개 요약. 상세 정보가 없으면 null입니다.", example = "신라 불교 예술의 정수", nullable = true)
	val summary: String?,

	@field:Schema(description = "대표 이미지 URL", example = "https://example.com/bulguksa.jpg", nullable = true)
	val imageUrl: String?,

	@field:Schema(description = "주소", example = "경상북도 경주시 진현동", nullable = true)
	val address: String?,

	@field:Schema(description = "화면 표시용 지역 이름", example = "경주", nullable = true)
	val regionName: String?,

	@field:Schema(description = "화면 표시용 태그", example = "[\"경주\",\"관광지\"]")
	val tags: List<String>,

	@field:Schema(description = "행사 시작일. `festival_collection`만 값이 있고 나머지는 null입니다.", example = "2026-08-01", nullable = true)
	val eventStartDate: LocalDate?,

	@field:Schema(description = "행사 종료일. `festival_collection`만 값이 있고 나머지는 null입니다.", example = "2026-08-31", nullable = true)
	val eventEndDate: LocalDate?,
)

enum class HomeSectionType(
	@get:JsonValue
	val value: String,
) {
	FESTIVAL_COLLECTION("festival_collection"),
	ATTRACTION_COLLECTION("attraction_collection"),
}
