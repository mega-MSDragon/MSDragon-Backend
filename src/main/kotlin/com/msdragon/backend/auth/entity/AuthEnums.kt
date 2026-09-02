package com.msdragon.backend.auth.entity

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import com.msdragon.backend.common.exception.BadRequestException
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

interface DbEnum {
	val value: String
}

enum class OAuthProvider(
	@get:JsonValue
	override val value: String,
) : DbEnum {
	KAKAO("kakao"),
	APPLE("apple"),
	;

	companion object {
		@JvmStatic
		@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
		fun from(value: String): OAuthProvider = enumValueOf(value, entries)
	}
}

enum class UserRole(
	@get:JsonValue
	override val value: String,
) : DbEnum {
	CHILD("child"),
	PARENT("parent"),
	;

	companion object {
		@JvmStatic
		@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
		fun from(value: String): UserRole = enumValueOf(value, entries)
	}
}

enum class AgeBand(
	@get:JsonValue
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
		@JvmStatic
		@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
		fun from(value: String): AgeBand = enumValueOf(value, entries)
	}
}

enum class GenderType(
	@get:JsonValue
	override val value: String,
) : DbEnum {
	FEMALE("female"),
	MALE("male"),
	UNDISCLOSED("undisclosed"),
	;

	companion object {
		@JvmStatic
		@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
		fun from(value: String): GenderType = enumValueOf(value, entries)
	}
}

enum class DevicePlatform(
	@get:JsonValue
	override val value: String,
) : DbEnum {
	IOS("ios"),
	ANDROID("android"),
	WEB("web"),
	;

	companion object {
		@JvmStatic
		@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
		fun from(value: String): DevicePlatform = enumValueOf(value, entries)
	}
}

enum class UserConsentType(
	@get:JsonValue
	override val value: String,
) : DbEnum {
	PRIVACY_COLLECTION("privacy_collection"),
	LOCATION_BASED_FACILITY("location_based_facility"),
	;

	companion object {
		@JvmStatic
		@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
		fun from(value: String): UserConsentType = enumValueOf(value, entries)
	}
}

/**
 * 마이페이지 프로필 이미지. 서버가 식별자를 정하고 클라이언트가 같은 값에 에셋을 맞춘다.
 * 업로드가 아닌 프리셋 선택이므로 이미지 파일을 저장하지 않는다.
 *
 * 시안의 배경색 기준으로 이름을 정했다. 어떤 아바타인지는 `docs/policy/mypage.md`의 매핑 표가 기준이며,
 * 배경색이 리터치되어도 식별자는 그대로 두고 표만 갱신한다. 식별자를 바꾸면 클라이언트와 저장된 값이 함께 깨진다.
 *
 * `none`은 요청 전용 값으로 "아바타 지우기"를 뜻한다. 저장은 `null`이며 응답에도 `null`로 나간다.
 * 프로필 수정은 필드를 생략하면 변경하지 않는 규칙이라 `null`로 지우기를 표현할 수 없어 별도 값을 둔다.
 */
enum class UserProfileImage(
	@get:JsonValue
	override val value: String,
) : DbEnum {
	GREEN("green"),
	CORAL("coral"),
	YELLOW("yellow"),
	BLUE("blue"),
	NONE("none"),
	;

	companion object {
		@JvmStatic
		@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
		fun from(value: String): UserProfileImage = enumValueOf(value, entries)
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

@Converter(autoApply = true)
class UserConsentTypeConverter : DbEnumConverter<UserConsentType>(UserConsentType.entries)

@Converter(autoApply = true)
class UserProfileImageConverter : DbEnumConverter<UserProfileImage>(UserProfileImage.entries)
