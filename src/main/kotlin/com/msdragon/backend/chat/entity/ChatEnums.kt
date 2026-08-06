package com.msdragon.backend.chat.entity

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import com.msdragon.backend.auth.entity.DbEnum
import com.msdragon.backend.auth.entity.DbEnumConverter
import com.msdragon.backend.common.exception.BadRequestException
import jakarta.persistence.Converter

enum class ChatSessionScope(
	@get:JsonValue
	override val value: String,
) : DbEnum {
	TRAVEL_MODE("travel_mode"),
	PLACE_DETAIL("place_detail"),
	GENERAL("general"),
	;

	companion object {
		@JvmStatic
		@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
		fun from(value: String): ChatSessionScope =
			entries.firstOrNull { it.value == value.lowercase() || it.name.equals(value, ignoreCase = true) }
				?: throw BadRequestException("지원하지 않는 채팅 범위입니다: $value")
	}
}

enum class ChatSender(
	@get:JsonValue
	override val value: String,
) : DbEnum {
	USER("user"),
	ASSISTANT("assistant"),
	;

	companion object {
		@JvmStatic
		@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
		fun from(value: String): ChatSender =
			entries.firstOrNull { it.value == value.lowercase() || it.name.equals(value, ignoreCase = true) }
				?: throw BadRequestException("지원하지 않는 채팅 발신자입니다: $value")
	}
}

@Converter(autoApply = true)
class ChatSessionScopeConverter : DbEnumConverter<ChatSessionScope>(ChatSessionScope.entries)

@Converter(autoApply = true)
class ChatSenderConverter : DbEnumConverter<ChatSender>(ChatSender.entries)
