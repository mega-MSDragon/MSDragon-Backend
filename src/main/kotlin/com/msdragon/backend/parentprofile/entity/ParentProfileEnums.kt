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

enum class ActivityLevel(
	@get:JsonValue
	override val value: String,
) : DbEnum {
	SLOW("slow"),
	MODERATE("moderate"),
	ACTIVE("active"),
	;

	companion object {
		@JvmStatic
		@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
		fun from(value: String): ActivityLevel = enumValueOf(value, entries)
	}
}

enum class FoodPreference(
	@get:JsonValue
	override val value: String,
) : DbEnum {
	KOREAN_ONLY("korean_only"),
	FAMILIAR_FOOD("familiar_food"),
	OPEN_MINDED("open_minded"),
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
	NATURE("nature"),
	HISTORY("history"),
	ACTIVITY("activity"),
	FOOD("food"),
	CULTURE("culture"),
	LANDMARK("landmark"),
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
	CITY_TASTER("city_taster"),
	SENSITIVE_CULTURE("sensitive_culture"),
	RELAXED_EXPLORER("relaxed_explorer"),
	HISTORY_WALKER("history_walker"),
	ACTIVE_EXPERIENCER("active_experiencer"),
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
class ActivityLevelConverter : ParentProfileDbEnumConverter<ActivityLevel>(ActivityLevel.entries)

@Converter(autoApply = true)
class FoodPreferenceConverter : ParentProfileDbEnumConverter<FoodPreference>(FoodPreference.entries)

@Converter(autoApply = true)
class TravelPersonalityTypeCodeConverter :
	ParentProfileDbEnumConverter<TravelPersonalityTypeCode>(TravelPersonalityTypeCode.entries)
