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
	fun `PDF 예시 입력은 유유자적 힐링러형으로 계산한다`() {
		val result = TravelPersonalityPolicy.resolve(
			walkingPace = WalkingPace.SLOW,
			needsMobilityAssistance = true,
			travelThemes = listOf(TravelThemeCode.NATURE_SCENERY),
			foodPreference = FoodPreference.KOREAN,
		)

		assertEquals(TravelPersonalityTypeCode.HEALING_TRAVELER, result)
	}

	@Test
	fun `모든 유효 입력 조합에서 여섯 유형이 고르게 계산된다`() {
		val themeCombinations = (1..3).flatMap { size -> TravelThemeCode.entries.combinations(size) }
		val resultCounts = TravelPersonalityTypeCode.entries.associateWith { 0 }.toMutableMap()
		var totalCount = 0

		WalkingPace.entries.forEach { walkingPace ->
			listOf(true, false).forEach { needsMobilityAssistance ->
				FoodPreference.entries.forEach { foodPreference ->
					themeCombinations.forEach { travelThemes ->
						val result = TravelPersonalityPolicy.resolve(
							walkingPace = walkingPace,
							needsMobilityAssistance = needsMobilityAssistance,
							travelThemes = travelThemes,
							foodPreference = foodPreference,
						)
						resultCounts[result] = resultCounts.getValue(result) + 1
						totalCount++
					}
				}
			}
		}

		assertEquals(1_134, totalCount)
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
		private const val MIN_RESULT_RATIO = 0.15
		private const val MAX_RESULT_RATIO = 0.18
	}
}
