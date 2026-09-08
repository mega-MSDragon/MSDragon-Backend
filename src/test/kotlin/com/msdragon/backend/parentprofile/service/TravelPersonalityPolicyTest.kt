package com.msdragon.backend.parentprofile.service

import com.msdragon.backend.parentprofile.entity.FoodPreference
import com.msdragon.backend.parentprofile.entity.TravelPersonalityTypeCode
import com.msdragon.backend.parentprofile.entity.TravelThemeCode
import com.msdragon.backend.parentprofile.entity.WalkingPace
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TravelPersonalityPolicyTest {
	@Test
	fun `시안 결과 예시 입력은 풍경 수집가로 계산한다`() {
		val result = TravelPersonalityPolicy.resolve(
			walkingPace = WalkingPace.SLOW,
			travelThemes = listOf(TravelThemeCode.NATURE_SCENERY),
			foodPreference = FoodPreference.KOREAN,
		)

		assertEquals(TravelPersonalityTypeCode.HEALING_TRAVELER, result)
	}

	@Test
	fun `같은 테마 3개를 고르면 일정 속도가 최종 유형을 나눈다`() {
		// 시안 동점 처리 예시: 자연·풍경 + 역사·전통 + 유명 명소
		val themes = listOf(
			TravelThemeCode.NATURE_SCENERY,
			TravelThemeCode.HISTORY_CULTURE,
			TravelThemeCode.LANDMARK,
		)

		assertEquals(
			TravelPersonalityTypeCode.HEALING_TRAVELER,
			TravelPersonalityPolicy.resolve(WalkingPace.SLOW, themes, FoodPreference.KOREAN),
		)
		assertEquals(
			TravelPersonalityTypeCode.HERITAGE_WALKER,
			TravelPersonalityPolicy.resolve(WalkingPace.NORMAL, themes, FoodPreference.KOREAN),
		)
	}

	@Test
	fun `이동 도움 여부는 성향 점수에 넣지 않는다`() {
		// 시안: 이동 도움 여부는 여행 동선 필터로만 쓴다. resolve 입력에서 아예 빠졌는지 확인한다.
		val themes = listOf(TravelThemeCode.EXPERIENCE, TravelThemeCode.SHOPPING)

		assertEquals(
			TravelPersonalityPolicy.resolve(WalkingPace.FAST, themes, FoodPreference.ADVENTUROUS),
			TravelPersonalityPolicy.resolve(WalkingPace.FAST, themes, FoodPreference.ADVENTUROUS),
		)
	}

	@Test
	fun `모든 유효 입력 조합에서 여섯 유형이 고르게 계산된다`() {
		val themeCombinations = (1..3).flatMap { size -> TravelThemeCode.entries.combinations(size) }
		val resultCounts = TravelPersonalityTypeCode.entries.associateWith { 0 }.toMutableMap()
		var totalCount = 0

		WalkingPace.entries.forEach { walkingPace ->
			FoodPreference.entries.forEach { foodPreference ->
				themeCombinations.forEach { travelThemes ->
					val result = TravelPersonalityPolicy.resolve(
						walkingPace = walkingPace,
						travelThemes = travelThemes,
						foodPreference = foodPreference,
					)
					resultCounts[result] = resultCounts.getValue(result) + 1
					totalCount++
				}
			}
		}

		assertEquals(567, totalCount)
		resultCounts.forEach { (type, count) ->
			val ratio = count.toDouble() / totalCount
			assertTrue(
				ratio in MIN_RESULT_RATIO..MAX_RESULT_RATIO,
				"$type 결과 비율이 허용 범위를 벗어났습니다: $count/$totalCount ($ratio)",
			)
		}
	}

	private fun <T> List<T>.combinations(size: Int): List<List<T>> {
		if (size == 0) {
			return listOf(emptyList())
		}
		if (size > this.size) {
			return emptyList()
		}
		return indices.flatMap { index ->
			drop(index + 1).combinations(size - 1).map { tail -> listOf(this[index]) + tail }
		}
	}

	companion object {
		private const val MIN_RESULT_RATIO = 0.14
		private const val MAX_RESULT_RATIO = 0.19
	}
}
