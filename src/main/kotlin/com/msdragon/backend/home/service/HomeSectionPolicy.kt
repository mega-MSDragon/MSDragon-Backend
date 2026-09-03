package com.msdragon.backend.home.service

/**
 * 홈 축제 영역부터 아래까지의 섹션 구성과 순서. **순서를 바꾸려면 이 목록의 순서를 바꾼다.**
 *
 * 모든 섹션은 클라이언트에서 같은 카드로 그려진다. 그래서 섹션을 추가·삭제·재정렬하거나
 * 제목과 항목 내용을 바꾸는 일이 **서버에서만** 끝난다. 응답에는 섹션 종류를 내리지 않는다.
 * [HomeSectionSource]는 항목을 어디서 채우는지 고르기 위한 서버 내부 값이다.
 *
 * 지금은 코드로 고정한다. 월별 추천 도시([HomeRecommendationPolicy])와 같은 방식이며,
 * 어드민 도구가 없는 상태에서 DB로 관리하면 SQL을 직접 쳐야 하므로 실익이 없다.
 * 운영 중 비개발자가 섹션을 바꿔야 하는 시점에 응답 계약을 유지한 채 데이터 출처만 DB로 옮긴다.
 */
object HomeSectionPolicy {
	/** 노출 순서대로 정렬한 섹션 정의. */
	val sections: List<HomeSectionDefinition> = listOf(
		HomeSectionDefinition(
			key = "festivals",
			source = HomeSectionSource.FESTIVAL,
			title = "지금 열리는 축제",
			subtitle = "오늘부터 30일 안에 만나요",
		),
		HomeSectionDefinition(
			key = "monthly_attractions",
			source = HomeSectionSource.MONTHLY_ATTRACTION,
			title = "이번 달 가볼 만한 곳",
			subtitle = null,
		),
	)

	/** 추천 도시 하나당 가져올 관광지 수. 도시 5개 기준 최대 10개다. */
	const val ATTRACTIONS_PER_DESTINATION = 2

	const val ATTRACTION_TAG = "관광지"
}

data class HomeSectionDefinition(
	val key: String,
	val source: HomeSectionSource,
	val title: String,
	val subtitle: String?,
)

/** 섹션 항목을 채우는 데이터 출처. 서버 내부 값이며 응답에 나가지 않는다. */
enum class HomeSectionSource {
	FESTIVAL,
	MONTHLY_ATTRACTION,
}
