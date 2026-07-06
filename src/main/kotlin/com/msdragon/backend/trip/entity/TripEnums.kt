package com.msdragon.backend.trip.entity

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import com.msdragon.backend.auth.entity.DbEnum
import com.msdragon.backend.common.exception.BadRequestException
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

enum class TripStatus(
	@get:JsonValue
	override val value: String,
) : DbEnum {
	PLANNING("planning"),
	READY("ready"),
	IN_PROGRESS("in_progress"),
	COMPLETED("completed"),
	ARCHIVED("archived"),
	;

	companion object {
		@JvmStatic
		@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
		fun from(value: String): TripStatus = enumValueOf(value, entries)
	}
}

enum class TripDestinationCode(
	@get:JsonValue
	override val value: String,
	val displayName: String,
	val displayOrder: Int,
	val badgeLabel: String? = null,
) : DbEnum {
	DAEGU("daegu", "대구", 1),
	GANGNEUNG_SOKCHO("gangneung_sokcho", "강릉·속초", 2),
	GYEONGJU("gyeongju", "경주", 3, "인기"),
	BUSAN("busan", "부산", 4),
	YEOSU("yeosu", "여수", 5, "Hot!"),
	INCHEON("incheon", "인천", 6),
	JEONJU("jeonju", "전주", 7),
	JEJU("jeju", "제주", 8),
	SEOUL("seoul", "서울", 9),
	SUWON_YONGIN("suwon_yongin", "수원·용인", 10),
	TONGYEONG_GEOJE_NAMHAE("tongyeong_geoje_namhae", "통영·거제·남해", 11),
	POHANG_ANDONG("pohang_andong", "포항·안동", 12),
	;

	companion object {
		@JvmStatic
		@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
		fun from(value: String): TripDestinationCode = enumValueOf(value, entries)
	}
}

enum class StopType(
	@get:JsonValue
	override val value: String,
) : DbEnum {
	SIGHTSEEING("sightseeing"),
	MEAL("meal"),
	REST("rest"),
	CAFE("cafe"),
	;

	companion object {
		@JvmStatic
		@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
		fun from(value: String): StopType = enumValueOf(value, entries)
	}
}

enum class ExternalApiProvider(
	@get:JsonValue
	override val value: String,
) : DbEnum {
	TOUR_API("tour_api"),
	TMAP("tmap"),
	KAKAO_MAP("kakao_map"),
	PUBLIC_DATA("public_data"),
	LOCAL_EXCEL("local_excel"),
	INTERNAL("internal"),
	;

	companion object {
		@JvmStatic
		@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
		fun from(value: String): ExternalApiProvider = enumValueOf(value, entries)
	}
}

private fun <T> enumValueOf(value: String, entries: Iterable<T>): T where T : Enum<T>, T : DbEnum =
	entries.firstOrNull { it.value == value.lowercase() || it.name.equals(value, ignoreCase = true) }
		?: throw BadRequestException("지원하지 않는 값입니다: $value")

abstract class TripDbEnumConverter<T>(
	private val entries: Iterable<T>,
) : AttributeConverter<T, String> where T : Enum<T>, T : DbEnum {
	override fun convertToDatabaseColumn(attribute: T?): String? = attribute?.value

	override fun convertToEntityAttribute(dbData: String?): T? =
		dbData?.let { enumValueOf(it, entries) }
}

@Converter(autoApply = true)
class TripStatusConverter : TripDbEnumConverter<TripStatus>(TripStatus.entries)

@Converter(autoApply = true)
class TripDestinationCodeConverter : TripDbEnumConverter<TripDestinationCode>(TripDestinationCode.entries)

@Converter(autoApply = true)
class StopTypeConverter : TripDbEnumConverter<StopType>(StopType.entries)

@Converter(autoApply = true)
class ExternalApiProviderConverter : TripDbEnumConverter<ExternalApiProvider>(ExternalApiProvider.entries)
