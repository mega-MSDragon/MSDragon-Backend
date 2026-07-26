package com.msdragon.backend.feedback.entity

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import com.msdragon.backend.auth.entity.DbEnum
import com.msdragon.backend.common.exception.BadRequestException
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

enum class FeedbackBodyCondition(
	@get:JsonValue
	override val value: String,
) : DbEnum {
	COMFORTABLE("comfortable"),
	SLIGHTLY_TIRED("slightly_tired"),
	VERY_TIRED("very_tired"),
	;

	companion object {
		@JvmStatic
		@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
		fun from(value: String): FeedbackBodyCondition = enumValueOf(value, entries)
	}
}

enum class FeedbackTagCategory {
	GOOD,
	IMPROVEMENT,
}

enum class FeedbackTag(
	@get:JsonValue
	override val value: String,
	val category: FeedbackTagCategory,
) : DbEnum {
	WALKING_COMFORTABLE("walking_comfortable", FeedbackTagCategory.GOOD),
	REST_TIME_GOOD("rest_time_good", FeedbackTagCategory.GOOD),
	SCENERY_GOOD("scenery_good", FeedbackTagCategory.GOOD),
	TRANSPORT_COMFORTABLE("transport_comfortable", FeedbackTagCategory.GOOD),
	FOOD_GOOD("food_good", FeedbackTagCategory.GOOD),
	SEATING_SUFFICIENT("seating_sufficient", FeedbackTagCategory.GOOD),
	MORE_REST_NEEDED("more_rest_needed", FeedbackTagCategory.IMPROVEMENT),
	MANY_STAIRS_OR_SLOPES("many_stairs_or_slopes", FeedbackTagCategory.IMPROVEMENT),
	LONG_TRAVEL_TIME("long_travel_time", FeedbackTagCategory.IMPROVEMENT),
	CROWDED("crowded", FeedbackTagCategory.IMPROVEMENT),
	;

	companion object {
		@JvmStatic
		@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
		fun from(value: String): FeedbackTag = enumValueOf(value, entries)
	}
}

private fun <T> enumValueOf(value: String, entries: Iterable<T>): T where T : Enum<T>, T : DbEnum =
	entries.firstOrNull { it.value == value.lowercase() || it.name.equals(value, ignoreCase = true) }
		?: throw BadRequestException("지원하지 않는 값입니다: $value")

@Converter(autoApply = true)
class FeedbackBodyConditionConverter : AttributeConverter<FeedbackBodyCondition, String> {
	override fun convertToDatabaseColumn(attribute: FeedbackBodyCondition?): String? = attribute?.value

	override fun convertToEntityAttribute(dbData: String?): FeedbackBodyCondition? =
		dbData?.let(FeedbackBodyCondition::from)
}
