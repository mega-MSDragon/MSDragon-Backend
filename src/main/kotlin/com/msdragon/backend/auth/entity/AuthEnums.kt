package com.msdragon.backend.auth.entity

import com.msdragon.backend.common.exception.BadRequestException
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

interface DbEnum {
	val value: String
}

enum class OAuthProvider(
	override val value: String,
) : DbEnum {
	KAKAO("kakao"),
	APPLE("apple"),
	;

	companion object {
		fun from(value: String): OAuthProvider = enumValueOf(value, entries)
	}
}

enum class UserRole(
	override val value: String,
) : DbEnum {
	CHILD("child"),
	PARENT("parent"),
	;

	companion object {
		fun from(value: String): UserRole = enumValueOf(value, entries)
	}
}

enum class AgeBand(
	override val value: String,
) : DbEnum {
	AGE_10S("10s"),
	AGE_20S("20s"),
	AGE_30S("30s"),
	AGE_40S("40s"),
	AGE_50S("50s"),
	AGE_60S("60s"),
	AGE_60S_PLUS("60s_plus"),
	AGE_70S("70s"),
	AGE_80S("80s"),
	AGE_90S_PLUS("90s_plus"),
	UNDISCLOSED("undisclosed"),
	;

	companion object {
		fun from(value: String): AgeBand = enumValueOf(value, entries)
	}
}

enum class GenderType(
	override val value: String,
) : DbEnum {
	FEMALE("female"),
	MALE("male"),
	UNDISCLOSED("undisclosed"),
	;

	companion object {
		fun from(value: String): GenderType = enumValueOf(value, entries)
	}
}

enum class DevicePlatform(
	override val value: String,
) : DbEnum {
	IOS("ios"),
	ANDROID("android"),
	WEB("web"),
	;

	companion object {
		fun from(value: String): DevicePlatform = enumValueOf(value, entries)
	}
}

private fun <T> enumValueOf(value: String, entries: Iterable<T>): T where T : Enum<T>, T : DbEnum =
	entries.firstOrNull { it.value == value.lowercase() || it.name.equals(value, ignoreCase = true) }
		?: throw BadRequestException("지원하지 않는 값입니다: $value")

abstract class DbEnumConverter<T>(
	private val entries: Iterable<T>,
) : AttributeConverter<T, String> where T : Enum<T>, T : DbEnum {
	override fun convertToDatabaseColumn(attribute: T?): String? = attribute?.value

	override fun convertToEntityAttribute(dbData: String?): T? =
		dbData?.let { enumValueOf(it, entries) }
}

@Converter(autoApply = true)
class OAuthProviderConverter : DbEnumConverter<OAuthProvider>(OAuthProvider.entries)

@Converter(autoApply = true)
class UserRoleConverter : DbEnumConverter<UserRole>(UserRole.entries)

@Converter(autoApply = true)
class AgeBandConverter : DbEnumConverter<AgeBand>(AgeBand.entries)

@Converter(autoApply = true)
class GenderTypeConverter : DbEnumConverter<GenderType>(GenderType.entries)

@Converter(autoApply = true)
class DevicePlatformConverter : DbEnumConverter<DevicePlatform>(DevicePlatform.entries)
