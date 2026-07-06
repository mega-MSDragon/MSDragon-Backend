package com.msdragon.backend.parentprofile.entity

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import com.msdragon.backend.auth.entity.DbEnum
import com.msdragon.backend.common.exception.BadRequestException
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

enum class ParentProfileStatus(
	@get:JsonValue
	override val value: String,
) : DbEnum {
	DRAFT("draft"),
	COMPLETED("completed"),
	;

	companion object {
		@JvmStatic
		@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
		fun from(value: String): ParentProfileStatus = enumValueOf(value, entries)
	}
}

enum class WalkingPace(
	@get:JsonValue
	override val value: String,
) : DbEnum {
	SLOW("slow"),
	NORMAL("normal"),
	FAST("fast"),
	;

	companion object {
		@JvmStatic
		@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
		fun from(value: String): WalkingPace = enumValueOf(value, entries)
	}
}

enum class FoodPreference(
	@get:JsonValue
	override val value: String,
) : DbEnum {
	KOREAN("korean"),
	FAMILIAR("familiar"),
	ADVENTUROUS("adventurous"),
	;

	companion object {
		@JvmStatic
		@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
		fun from(value: String): FoodPreference = enumValueOf(value, entries)
	}
}

enum class TravelThemeCode(
	@get:JsonValue
	override val value: String,
) : DbEnum {
	NATURE_SCENERY("nature_scenery"),
	HISTORY_CULTURE("history_culture"),
	SHOPPING("shopping"),
	ACTIVITY("activity"),
	CULTURE_LIFE("culture_life"),
	LANDMARK("landmark"),
	EXPERIENCE("experience"),
	;

	companion object {
		@JvmStatic
		@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
		fun from(value: String): TravelThemeCode = enumValueOf(value, entries)
	}
}

enum class TravelPersonalityTypeCode(
	@get:JsonValue
	override val value: String,
) : DbEnum {
	URBAN_EXPLORER("urban_explorer"),
	CULTURE_STROLLER("culture_stroller"),
	HEALING_TRAVELER("healing_traveler"),
	HERITAGE_WALKER("heritage_walker"),
	ACTIVE_ADVENTURER("active_adventurer"),
	LOCAL_CHALLENGER("local_challenger"),
	;

	companion object {
		@JvmStatic
		@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
		fun from(value: String): TravelPersonalityTypeCode = enumValueOf(value, entries)
	}
}

private fun <T> enumValueOf(value: String, entries: Iterable<T>): T where T : Enum<T>, T : DbEnum =
	entries.firstOrNull { it.value == value.lowercase() || it.name.equals(value, ignoreCase = true) }
		?: throw BadRequestException("지원하지 않는 값입니다: $value")

abstract class ParentProfileDbEnumConverter<T>(
	private val entries: Iterable<T>,
) : AttributeConverter<T, String> where T : Enum<T>, T : DbEnum {
	override fun convertToDatabaseColumn(attribute: T?): String? = attribute?.value

	override fun convertToEntityAttribute(dbData: String?): T? =
		dbData?.let { enumValueOf(it, entries) }
}

@Converter(autoApply = true)
class ParentProfileStatusConverter : ParentProfileDbEnumConverter<ParentProfileStatus>(ParentProfileStatus.entries)

@Converter(autoApply = true)
class WalkingPaceConverter : ParentProfileDbEnumConverter<WalkingPace>(WalkingPace.entries)

@Converter(autoApply = true)
class FoodPreferenceConverter : ParentProfileDbEnumConverter<FoodPreference>(FoodPreference.entries)

@Converter(autoApply = true)
class TravelPersonalityTypeCodeConverter :
	ParentProfileDbEnumConverter<TravelPersonalityTypeCode>(TravelPersonalityTypeCode.entries)
