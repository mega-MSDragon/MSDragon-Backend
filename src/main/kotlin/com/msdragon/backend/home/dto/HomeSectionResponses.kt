package com.msdragon.backend.home.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(
	description = "홈 동적 섹션 목록. 배열 순서가 화면 노출 순서이며 서버가 결정합니다. " +
		"모든 섹션은 같은 카드 형태로 그려지므로 클라이언트는 순서대로 렌더링만 하면 됩니다. " +
		"섹션을 추가·삭제·재정렬하거나 항목 내용을 바꾸는 일은 모두 서버에서 끝납니다.",
)
data class HomeSectionsResponse(
	@field:Schema(description = "노출 순서대로 정렬된 섹션 목록")
	val sections: List<HomeSectionResponse>,
)

@Schema(description = "홈 섹션")
data class HomeSectionResponse(
	@field:Schema(description = "섹션 식별자. 화면 상태 관리와 로깅에 사용합니다.", example = "monthly_attractions")
	val key: String,

	@field:Schema(description = "섹션 제목", example = "이번 달 가볼 만한 곳")
	val title: String,

	@field:Schema(description = "섹션 부제. 없으면 null입니다.", example = "경주 · 전주 · 제주", nullable = true)
	val subtitle: String?,

	@field:Schema(description = "섹션 항목. 외부 API 장애 시 빈 배열일 수 있습니다.")
	val items: List<HomeSectionItemResponse>,
)

@Schema(
	description = "홈 섹션 항목. 모든 섹션이 같은 카드로 그려지므로 항목 형태도 하나입니다. " +
		"섹션마다 다른 부가 정보는 caption에 서버가 문장으로 만들어 담습니다.",
)
data class HomeSectionItemResponse(
	@field:Schema(description = "TourAPI 콘텐츠 ID. 상세 조회에 사용합니다.", example = "126508")
	val contentId: String,

	@field:Schema(
		description = "TourAPI 콘텐츠 타입 ID. `GET /api/v1/places/{contentId}` 호출에 그대로 넘깁니다. " +
			"클라이언트가 섹션 종류를 몰라도 상세로 이동할 수 있습니다.",
		example = "12",
	)
	val contentTypeId: String,

	@field:Schema(description = "항목명", example = "불국사")
	val title: String,

	@field:Schema(
		description = "카드에 표시할 부가 문장. 축제는 기간, 그 밖에는 지역처럼 섹션에 맞는 값을 서버가 만듭니다. 없으면 null입니다.",
		example = "2026.08.01 - 08.31",
		nullable = true,
	)
	val caption: String?,

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
)
