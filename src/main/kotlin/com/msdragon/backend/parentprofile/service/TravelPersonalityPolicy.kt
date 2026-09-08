package com.msdragon.backend.parentprofile.service

import com.msdragon.backend.parentprofile.entity.FoodPreference
import com.msdragon.backend.parentprofile.entity.TravelPersonalityTypeCode
import com.msdragon.backend.parentprofile.entity.TravelThemeCode
import com.msdragon.backend.parentprofile.entity.WalkingPace

/**
 * 확정 시안(`여행 MBTI 6유형 네이밍 보드`)의 점수 산정 방식을 그대로 구현한다.
 *
 * `테마 점수 + 일정 점수 + 음식 점수 = 최종 유형`이며 대표 테마 +2점, 보조 테마 +1점이다.
 * 이동 도움 여부는 시안 명시대로 **성향 점수에 넣지 않고** 여행 동선 필터로만 쓴다.
 */
object TravelPersonalityPolicy {
	fun resolve(
		walkingPace: WalkingPace,
		travelThemes: Collection<TravelThemeCode>,
		foodPreference: FoodPreference,
	): TravelPersonalityTypeCode {
		val themes = travelThemes.toSet()
		val scores = TravelPersonalityTypeCode.entries.associateWith { type ->
			PersonalityScore(
				theme = themes.sumOf { themePoints(type, it) },
				pace = PACE_POINTS.getValue(walkingPace)[type] ?: 0,
				food = FOOD_POINTS.getValue(foodPreference)[type] ?: 0,
			)
		}

		// 시안: 점수가 비슷하면 일정 속도로 최종 결과를 나눈다.
		return PERSONALITY_TIE_BREAKERS.maxWith(
			compareBy<TravelPersonalityTypeCode> { scores.getValue(it).total }
				.thenBy { scores.getValue(it).pace }
				.thenBy { scores.getValue(it).theme }
				.thenBy { scores.getValue(it).food }
				.thenBy { -PERSONALITY_TIE_BREAKERS.indexOf(it) },
		)
	}

	private fun themePoints(type: TravelPersonalityTypeCode, theme: TravelThemeCode): Int =
		when (THEMES_BY_TYPE.getValue(type).indexOf(theme)) {
			-1 -> 0
			0 -> PRIMARY_THEME_POINTS
			else -> SECONDARY_THEME_POINTS
		}

	private const val PRIMARY_THEME_POINTS = 2
	private const val SECONDARY_THEME_POINTS = 1

	/**
	 * 시안 유형 카드에 나열된 테마. **첫 번째가 대표 테마, 나머지가 보조 테마다.**
	 * 결과 카드의 테마 칩도 이 순서로 표시하므로 응답에도 이 목록을 그대로 내린다.
	 */
	val THEMES_BY_TYPE: Map<TravelPersonalityTypeCode, List<TravelThemeCode>> = mapOf(
		TravelPersonalityTypeCode.URBAN_EXPLORER to
			listOf(TravelThemeCode.SHOPPING, TravelThemeCode.LANDMARK, TravelThemeCode.CULTURE_LIFE),
		TravelPersonalityTypeCode.CULTURE_STROLLER to
			listOf(TravelThemeCode.CULTURE_LIFE, TravelThemeCode.LANDMARK, TravelThemeCode.HISTORY_CULTURE),
		TravelPersonalityTypeCode.HEALING_TRAVELER to
			listOf(TravelThemeCode.NATURE_SCENERY, TravelThemeCode.HISTORY_CULTURE, TravelThemeCode.LANDMARK),
		TravelPersonalityTypeCode.HERITAGE_WALKER to
			listOf(TravelThemeCode.HISTORY_CULTURE, TravelThemeCode.NATURE_SCENERY, TravelThemeCode.LANDMARK),
		TravelPersonalityTypeCode.ACTIVE_ADVENTURER to
			listOf(TravelThemeCode.ACTIVITY, TravelThemeCode.EXPERIENCE, TravelThemeCode.NATURE_SCENERY),
		TravelPersonalityTypeCode.LOCAL_CHALLENGER to
			listOf(TravelThemeCode.EXPERIENCE, TravelThemeCode.SHOPPING, TravelThemeCode.CULTURE_LIFE),
	)

	/**
	 * 일정 속도 점수. 시안은 `유형별 +1~2점`이라고만 정하고 표를 주지 않아,
	 * 시안의 동점 처리 방향(천천히→풍경 수집가, 적당히→시간 여행자, 여러 곳→도시 탐험가)을
	 * 지키면서 여섯 유형 결과 비율이 고르게 나오도록 정했다.
	 */
	private val PACE_POINTS: Map<WalkingPace, Map<TravelPersonalityTypeCode, Int>> = mapOf(
		WalkingPace.SLOW to mapOf(
			TravelPersonalityTypeCode.HEALING_TRAVELER to 2,
			TravelPersonalityTypeCode.HERITAGE_WALKER to 2,
			TravelPersonalityTypeCode.CULTURE_STROLLER to 1,
		),
		WalkingPace.NORMAL to mapOf(
			TravelPersonalityTypeCode.CULTURE_STROLLER to 1,
			TravelPersonalityTypeCode.HERITAGE_WALKER to 1,
			TravelPersonalityTypeCode.URBAN_EXPLORER to 1,
		),
		WalkingPace.FAST to mapOf(
			TravelPersonalityTypeCode.URBAN_EXPLORER to 2,
			TravelPersonalityTypeCode.ACTIVE_ADVENTURER to 2,
			TravelPersonalityTypeCode.LOCAL_CHALLENGER to 2,
		),
	)

	/** 음식 취향 점수. 일정 속도와 같은 이유로 시안의 `+1~2점` 범위 안에서 정했다. */
	private val FOOD_POINTS: Map<FoodPreference, Map<TravelPersonalityTypeCode, Int>> = mapOf(
		FoodPreference.KOREAN to mapOf(
			TravelPersonalityTypeCode.HEALING_TRAVELER to 2,
			TravelPersonalityTypeCode.HERITAGE_WALKER to 1,
		),
		FoodPreference.FAMILIAR to mapOf(
			TravelPersonalityTypeCode.CULTURE_STROLLER to 2,
			TravelPersonalityTypeCode.URBAN_EXPLORER to 1,
		),
		FoodPreference.ADVENTUROUS to mapOf(
			TravelPersonalityTypeCode.LOCAL_CHALLENGER to 2,
			TravelPersonalityTypeCode.ACTIVE_ADVENTURER to 2,
		),
	)

	/** 모든 축이 완전히 같을 때만 쓰는 안정적인 최종 tie-breaker. */
	private val PERSONALITY_TIE_BREAKERS = listOf(
		TravelPersonalityTypeCode.CULTURE_STROLLER,
		TravelPersonalityTypeCode.URBAN_EXPLORER,
		TravelPersonalityTypeCode.HERITAGE_WALKER,
		TravelPersonalityTypeCode.LOCAL_CHALLENGER,
		TravelPersonalityTypeCode.HEALING_TRAVELER,
		TravelPersonalityTypeCode.ACTIVE_ADVENTURER,
	)
}

private data class PersonalityScore(
	val theme: Int,
	val pace: Int,
	val food: Int,
) {
	val total: Int get() = theme + pace + food
}
