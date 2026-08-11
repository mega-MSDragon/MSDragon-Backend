package com.msdragon.backend.home.dto

import com.fasterxml.jackson.annotation.JsonValue

enum class HomeProfileGuideType(
	@get:JsonValue
	val value: String,
) {
	COMPLETE_MY_PROFILE("complete_my_profile"),
	REQUEST_PARENT_PROFILE("request_parent_profile"),
}

enum class HomeTripIntensity(
	@get:JsonValue
	val value: String,
) {
	LOW("low"),
	NORMAL("normal"),
	HIGH("high"),
}
