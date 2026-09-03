package com.msdragon.backend.home.service

import com.msdragon.backend.home.dto.HomeSectionType

/**
 * 홈 축제 영역부터 아래까지의 섹션 구성과 순서. **순서를 바꾸려면 이 목록의 순서를 바꾼다.**
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
			type = HomeSectionType.FESTIVAL_COLLECTION,
			title = "지금 열리는 축제",
			subtitle = "오늘부터 30일 안에 만나요",
		),
		HomeSectionDefinition(
			key = "monthly_attractions",
			type = HomeSectionType.ATTRACTION_COLLECTION,
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
	val type: HomeSectionType,
	val title: String,
	val subtitle: String?,
)
