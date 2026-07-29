package com.msdragon.backend.supportfacility.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal

@Schema(description = "현재 위치 주변 공중화장실")
data class NearbyRestroomResponse(
	@field:Schema(description = "시설 ID", example = "1")
	val id: Long,

	@field:Schema(description = "화장실명", example = "사직단 공중화장실")
	val name: String,

	@field:Schema(description = "주소", example = "서울특별시 종로구 사직동 1-28", nullable = true)
	val address: String?,

	@field:Schema(description = "WGS84 위도", example = "37.5758692")
	val latitude: BigDecimal,

	@field:Schema(description = "WGS84 경도", example = "126.9684817")
	val longitude: BigDecimal,

	@field:Schema(description = "현재 위치와의 직선거리(m)", example = "320")
	val distanceMeters: Int,

	@field:Schema(description = "전화번호", example = "02-2148-2832", nullable = true)
	val phone: String?,

	@field:Schema(description = "개방시간", example = "상시", nullable = true)
	val operatingHours: String?,
)
