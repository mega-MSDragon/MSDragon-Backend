package com.msdragon.backend.pledge.entity

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import com.msdragon.backend.auth.entity.DbEnum
import com.msdragon.backend.common.exception.BadRequestException
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

enum class TripPledgeStatus(
	@get:JsonValue
	override val value: String,
) : DbEnum {
	DRAFT("draft"),
	REVIEWED("reviewed"),
	SIGNATURE_REQUESTED("signature_requested"),
	COMPLETED("completed"),
	;

	companion object {
		@JvmStatic
		@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
		fun from(value: String): TripPledgeStatus =
			entries.firstOrNull { it.value == value.lowercase() || it.name.equals(value, ignoreCase = true) }
				?: throw BadRequestException("지원하지 않는 값입니다: $value")
	}
}

enum class TripPledgeProgressStatus(
	@get:JsonValue
	val value: String,
) {
	NOT_CREATED("not_created"),
	AWAITING_CHILD_SIGNATURE("awaiting_child_signature"),
	AWAITING_PARENT_SIGNATURE("awaiting_parent_signature"),
	COMPLETED("completed"),
}

@Converter(autoApply = true)
class TripPledgeStatusConverter : AttributeConverter<TripPledgeStatus, String> {
	override fun convertToDatabaseColumn(attribute: TripPledgeStatus?): String? = attribute?.value

	override fun convertToEntityAttribute(dbData: String?): TripPledgeStatus? = dbData?.let(TripPledgeStatus::from)
}
