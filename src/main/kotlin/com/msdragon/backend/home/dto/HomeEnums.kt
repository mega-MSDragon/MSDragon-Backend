package com.msdragon.backend.home.dto

import com.fasterxml.jackson.annotation.JsonValue

enum class HomeTripIntensity(
	@get:JsonValue
	val value: String,
) {
	LOW("low"),
	NORMAL("normal"),
	HIGH("high"),
}
