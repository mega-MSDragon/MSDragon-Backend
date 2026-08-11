package com.msdragon.backend.parentprofile.service

import com.msdragon.backend.parentprofile.entity.FoodPreference
import com.msdragon.backend.parentprofile.entity.TravelPersonalityTypeCode
import com.msdragon.backend.parentprofile.entity.TravelThemeCode
import com.msdragon.backend.parentprofile.entity.WalkingPace

object TravelPersonalityPolicy {
	fun resolve(
		walkingPace: WalkingPace,
		needsMobilityAssistance: Boolean,
		travelThemes: Collection<TravelThemeCode>,
		foodPreference: FoodPreference,
	): TravelPersonalityTypeCode {
		val scores = TravelPersonalityTypeCode.entries
			.associateWith { PersonalityScore() }
			.toMutableMap()

		fun addScore(
			type: TravelPersonalityTypeCode,
			points: Int,
			axis: ScoreAxis,
		) {
			scores[type] = scores.getValue(type).plus(points, axis)
		}

		when (walkingPace) {
			WalkingPace.SLOW -> {
				addScore(TravelPersonalityTypeCode.HEALING_TRAVELER, 4, ScoreAxis.MOBILITY)
				addScore(TravelPersonalityTypeCode.CULTURE_STROLLER, 1, ScoreAxis.MOBILITY)
				addScore(TravelPersonalityTypeCode.HERITAGE_WALKER, 1, ScoreAxis.MOBILITY)
			}
			WalkingPace.NORMAL -> {
				addScore(TravelPersonalityTypeCode.CULTURE_STROLLER, 2, ScoreAxis.MOBILITY)
				addScore(TravelPersonalityTypeCode.HERITAGE_WALKER, 2, ScoreAxis.MOBILITY)
				addScore(TravelPersonalityTypeCode.URBAN_EXPLORER, 1, ScoreAxis.MOBILITY)
			}
			WalkingPace.FAST -> {
				addScore(TravelPersonalityTypeCode.ACTIVE_ADVENTURER, 5, ScoreAxis.MOBILITY)
				addScore(TravelPersonalityTypeCode.LOCAL_CHALLENGER, 3, ScoreAxis.MOBILITY)
				addScore(TravelPersonalityTypeCode.URBAN_EXPLORER, 2, ScoreAxis.MOBILITY)
			}
		}

		if (needsMobilityAssistance) {
			addScore(TravelPersonalityTypeCode.HEALING_TRAVELER, 2, ScoreAxis.MOBILITY)
			addScore(TravelPersonalityTypeCode.CULTURE_STROLLER, 1, ScoreAxis.MOBILITY)
			addScore(TravelPersonalityTypeCode.HERITAGE_WALKER, 1, ScoreAxis.MOBILITY)
		} else {
			addScore(TravelPersonalityTypeCode.URBAN_EXPLORER, 1, ScoreAxis.MOBILITY)
			addScore(TravelPersonalityTypeCode.ACTIVE_ADVENTURER, 1, ScoreAxis.MOBILITY)
			addScore(TravelPersonalityTypeCode.LOCAL_CHALLENGER, 1, ScoreAxis.MOBILITY)
		}

		travelThemes.forEach { theme ->
			when (theme) {
				TravelThemeCode.NATURE_SCENERY -> {
					addScore(TravelPersonalityTypeCode.HEALING_TRAVELER, 4, ScoreAxis.THEME)
					addScore(TravelPersonalityTypeCode.HERITAGE_WALKER, 3, ScoreAxis.THEME)
				}
				TravelThemeCode.HISTORY_CULTURE -> {
					addScore(TravelPersonalityTypeCode.HERITAGE_WALKER, 4, ScoreAxis.THEME)
					addScore(TravelPersonalityTypeCode.HEALING_TRAVELER, 1, ScoreAxis.THEME)
					addScore(TravelPersonalityTypeCode.CULTURE_STROLLER, 1, ScoreAxis.THEME)
				}
				TravelThemeCode.SHOPPING -> {
					addScore(TravelPersonalityTypeCode.URBAN_EXPLORER, 4, ScoreAxis.THEME)
					addScore(TravelPersonalityTypeCode.CULTURE_STROLLER, 3, ScoreAxis.THEME)
				}
				TravelThemeCode.ACTIVITY -> {
					addScore(TravelPersonalityTypeCode.ACTIVE_ADVENTURER, 4, ScoreAxis.THEME)
					addScore(TravelPersonalityTypeCode.LOCAL_CHALLENGER, 2, ScoreAxis.THEME)
				}
				TravelThemeCode.CULTURE_LIFE -> {
					addScore(TravelPersonalityTypeCode.CULTURE_STROLLER, 4, ScoreAxis.THEME)
					addScore(TravelPersonalityTypeCode.URBAN_EXPLORER, 2, ScoreAxis.THEME)
				}
				TravelThemeCode.LANDMARK -> {
					addScore(TravelPersonalityTypeCode.URBAN_EXPLORER, 5, ScoreAxis.THEME)
					addScore(TravelPersonalityTypeCode.CULTURE_STROLLER, 3, ScoreAxis.THEME)
					addScore(TravelPersonalityTypeCode.HERITAGE_WALKER, 2, ScoreAxis.THEME)
				}
				TravelThemeCode.EXPERIENCE -> {
					addScore(TravelPersonalityTypeCode.LOCAL_CHALLENGER, 5, ScoreAxis.THEME)
					addScore(TravelPersonalityTypeCode.ACTIVE_ADVENTURER, 3, ScoreAxis.THEME)
				}
			}
		}

		when (foodPreference) {
			FoodPreference.KOREAN -> {
				addScore(TravelPersonalityTypeCode.HEALING_TRAVELER, 4, ScoreAxis.FOOD)
				addScore(TravelPersonalityTypeCode.HERITAGE_WALKER, 3, ScoreAxis.FOOD)
			}
			FoodPreference.FAMILIAR -> {
				addScore(TravelPersonalityTypeCode.URBAN_EXPLORER, 2, ScoreAxis.FOOD)
				addScore(TravelPersonalityTypeCode.CULTURE_STROLLER, 2, ScoreAxis.FOOD)
				addScore(TravelPersonalityTypeCode.HERITAGE_WALKER, 2, ScoreAxis.FOOD)
			}
			FoodPreference.ADVENTUROUS -> {
				addScore(TravelPersonalityTypeCode.LOCAL_CHALLENGER, 5, ScoreAxis.FOOD)
				addScore(TravelPersonalityTypeCode.ACTIVE_ADVENTURER, 4, ScoreAxis.FOOD)
			}
		}

		return PERSONALITY_TIE_BREAKERS.maxWith(
			compareBy<TravelPersonalityTypeCode> { scores.getValue(it).total }
				.thenBy { scores.getValue(it).theme }
				.thenBy { scores.getValue(it).mobility }
				.thenBy { scores.getValue(it).food }
				.thenBy { -PERSONALITY_TIE_BREAKERS.indexOf(it) },
		)
	}

	private val PERSONALITY_TIE_BREAKERS = listOf(
		TravelPersonalityTypeCode.CULTURE_STROLLER,
		TravelPersonalityTypeCode.URBAN_EXPLORER,
		TravelPersonalityTypeCode.HERITAGE_WALKER,
		TravelPersonalityTypeCode.LOCAL_CHALLENGER,
		TravelPersonalityTypeCode.HEALING_TRAVELER,
		TravelPersonalityTypeCode.ACTIVE_ADVENTURER,
	)
}

private enum class ScoreAxis {
	THEME,
	MOBILITY,
	FOOD,
}

private data class PersonalityScore(
	val total: Int = 0,
	val theme: Int = 0,
	val mobility: Int = 0,
	val food: Int = 0,
) {
	fun plus(points: Int, axis: ScoreAxis): PersonalityScore =
		when (axis) {
			ScoreAxis.THEME -> copy(total = total + points, theme = theme + points)
			ScoreAxis.MOBILITY -> copy(total = total + points, mobility = mobility + points)
			ScoreAxis.FOOD -> copy(total = total + points, food = food + points)
		}
}
