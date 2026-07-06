package com.msdragon.backend.trip.dto

import com.msdragon.backend.trip.entity.TripDestinationCode
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDate

@Schema(description = "여행 생성 요청")
data class CreateTripRequest(
	@field:Schema(description = "여행 대상 부모 사용자 ID 목록. 같은 가족에 연결된 부모만 선택할 수 있습니다.", example = "[2,3]")
	@field:NotEmpty(message = "여행 대상 부모를 선택해주세요.")
	@field:Size(max = 2, message = "부모는 최대 2명까지 선택할 수 있습니다.")
	val parentUserIds: List<Long>,

	@field:Schema(
		description = "여행 도시 코드",
		example = "gyeongju",
		allowableValues = [
			"daegu",
			"gangneung_sokcho",
			"gyeongju",
			"busan",
			"yeosu",
			"incheon",
			"jeonju",
			"jeju",
			"seoul",
			"suwon_yongin",
			"tongyeong_geoje_namhae",
			"pohang_andong",
		],
	)
	@field:NotNull(message = "여행 도시를 선택해주세요.")
	val destinationCode: TripDestinationCode,

	@field:Schema(description = "여행 시작일", example = "2026-07-10")
	@field:NotNull(message = "여행 시작일을 선택해주세요.")
	val startDate: LocalDate,

	@field:Schema(description = "여행 종료일. 시작일과 같거나 이후 날짜를 선택할 수 있습니다.", example = "2026-07-11")
	@field:NotNull(message = "여행 종료일을 선택해주세요.")
	val endDate: LocalDate,

	@field:Schema(description = "여행 제목. 입력하지 않으면 '{도시명} 여행'으로 생성합니다.", example = "경주 가족 여행", nullable = true)
	@field:Size(max = 80, message = "여행 제목은 80자 이하로 입력해주세요.")
	val title: String? = null,
)
