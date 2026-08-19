package com.msdragon.backend.trip.dto

import com.msdragon.backend.trip.entity.ExternalApiProvider
import com.msdragon.backend.trip.entity.StopType
import com.msdragon.backend.trip.entity.TripDestinationCode
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import tools.jackson.databind.JsonNode
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime

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
			"gangneung_sokcho",
			"gyeongju",
			"daegu",
			"busan",
			"seoul",
			"suwon_yongin",
			"yeosu",
			"incheon",
			"jeonju",
			"jeju",
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

	@field:Schema(description = "여행 제목. 공백을 제외하고 필수이며 최대 15자입니다.", example = "아빠와 단둘이 경주")
	@field:NotBlank(message = "여행 제목을 입력해주세요.")
	@field:Size(max = 15, message = "여행 제목은 15자 이하로 입력해주세요.")
	val title: String,
)

@Schema(description = "여행 기본정보 수정 요청")
data class UpdateTripRequest(
	@field:Schema(description = "여행 제목. 공백을 제외하고 필수이며 최대 15자입니다.", example = "부모님과 경주 여행")
	@field:NotBlank(message = "여행 제목을 입력해주세요.")
	@field:Size(max = 15, message = "여행 제목은 15자 이하로 입력해주세요.")
	val title: String,

	@field:Schema(description = "여행 시작일. 여행 중 변경하는 경우 시작일과 종료일 사이에 오늘이 포함되어야 합니다.", example = "2026-07-10")
	@field:NotNull(message = "여행 시작일을 선택해주세요.")
	val startDate: LocalDate,

	@field:Schema(description = "여행 종료일. 시작일과 같거나 이후이며, 여행 중 변경하는 경우 기간에 오늘이 포함되어야 합니다.", example = "2026-07-11")
	@field:NotNull(message = "여행 종료일을 선택해주세요.")
	val endDate: LocalDate,

	@field:Schema(description = "함께 여행할 부모 사용자 ID 목록. 같은 가족에 연결된 부모만 선택할 수 있으며, 기존 참여 부모 구성이 바뀌면 10계명과 모든 서명이 초기화됩니다.", example = "[2,3]")
	@field:NotEmpty(message = "여행 대상 부모를 선택해주세요.")
	@field:Size(max = 2, message = "부모는 최대 2명까지 선택할 수 있습니다.")
	val parentUserIds: List<Long>,
)

@Schema(description = "여행 방문지 메모 수정 요청")
data class UpdateTripStopNoteRequest(
	@field:Schema(description = "방문지 메모. null 또는 공백이면 기존 메모를 삭제합니다.", example = "부모님과 사진 찍기", nullable = true)
	@field:Size(max = 255, message = "메모는 255자 이하로 입력해주세요.")
	val note: String? = null,
)

@Schema(description = "여행 코스 전체 저장 요청")
data class SaveTripCourseRequest(
	@field:Schema(description = "저장할 일자별 코스. 포함하지 않은 일자는 빈 코스로 저장됩니다.")
	@field:Valid
	val days: List<SaveTripCourseDayRequest> = emptyList(),
)

@Schema(description = "여행 일자별 코스 저장 요청")
data class SaveTripCourseDayRequest(
	@field:Schema(description = "여행 며칠차", example = "1")
	@field:Min(value = 1, message = "여행 일자는 1 이상이어야 합니다.")
	val dayNumber: Int,

	@field:Schema(description = "해당 일자의 방문지 목록. 배열 순서대로 코스 순서가 저장됩니다.")
	@field:Valid
	@field:Size(max = 50, message = "하루 방문지는 최대 50개까지 저장할 수 있습니다.")
	val stops: List<SaveTripStopRequest> = emptyList(),
)

@Schema(description = "여행 방문지 저장 요청")
data class SaveTripStopRequest(
	@field:Schema(description = "방문지 유형", example = "sightseeing", allowableValues = ["sightseeing", "meal", "rest", "cafe"])
	@field:NotNull(message = "방문지 유형을 선택해주세요.")
	val stopType: StopType = StopType.SIGHTSEEING,

	@field:Schema(
		description = "장소 원천 provider",
		example = "tour_api",
		allowableValues = ["tour_api", "tmap", "kakao_map", "public_data", "local_excel", "internal"],
	)
	@field:NotNull(message = "장소 원천 provider를 선택해주세요.")
	val sourceProvider: ExternalApiProvider = ExternalApiProvider.TOUR_API,

	@field:Schema(description = "외부 장소 ID. TourAPI contentId, 지도 API 장소 ID 등을 저장합니다.", example = "988449", nullable = true)
	@field:Size(max = 120, message = "외부 장소 ID는 120자 이하로 입력해주세요.")
	val externalPlaceId: String? = null,

	@field:Schema(description = "TourAPI contentTypeId", example = "12", nullable = true)
	@field:Size(max = 20, message = "콘텐츠 타입 ID는 20자 이하로 입력해주세요.")
	val contentTypeId: String? = null,

	@field:Schema(description = "장소명", example = "오도리 공원")
	@field:NotBlank(message = "장소명을 입력해주세요.")
	@field:Size(max = 120, message = "장소명은 120자 이하로 입력해주세요.")
	val name: String,

	@field:Schema(description = "장소 카테고리", example = "관광지", nullable = true)
	@field:Size(max = 60, message = "카테고리는 60자 이하로 입력해주세요.")
	val category: String? = null,

	@field:Schema(description = "주소", example = "대구광역시 동구 효목동", nullable = true)
	@field:Size(max = 255, message = "주소는 255자 이하로 입력해주세요.")
	val address: String? = null,

	@field:Schema(description = "위도", example = "35.8821234", nullable = true)
	@field:DecimalMin(value = "-90.0", message = "위도는 -90 이상이어야 합니다.")
	@field:DecimalMax(value = "90.0", message = "위도는 90 이하여야 합니다.")
	val latitude: BigDecimal? = null,

	@field:Schema(description = "경도", example = "128.6212345", nullable = true)
	@field:DecimalMin(value = "-180.0", message = "경도는 -180 이상이어야 합니다.")
	@field:DecimalMax(value = "180.0", message = "경도는 180 이하여야 합니다.")
	val longitude: BigDecimal? = null,

	@field:Schema(description = "전화번호", example = "053-123-4567", nullable = true)
	@field:Size(max = 30, message = "전화번호는 30자 이하로 입력해주세요.")
	val phone: String? = null,

	@field:Schema(description = "홈페이지 URL", example = "https://example.com", nullable = true)
	@field:Size(max = 500, message = "홈페이지 URL은 500자 이하로 입력해주세요.")
	val homepageUrl: String? = null,

	@field:Schema(description = "대표 이미지 URL", example = "https://example.com/image.jpg", nullable = true)
	@field:Size(max = 500, message = "대표 이미지 URL은 500자 이하로 입력해주세요.")
	val imageUrl: String? = null,

	@field:Schema(description = "장소 소개", example = "짧은 산책을 즐기기 좋은 공원입니다.", nullable = true)
	@field:Size(max = 2000, message = "장소 소개는 2000자 이하로 입력해주세요.")
	val overview: String? = null,

	@field:Schema(description = "도착 예정 시간", example = "10:30", nullable = true)
	val arrivalTime: LocalTime? = null,

	@field:Schema(description = "예상 체류 시간(분)", example = "60", nullable = true)
	@field:Min(value = 1, message = "체류 시간은 1분 이상이어야 합니다.")
	@field:Max(value = 1440, message = "체류 시간은 1440분 이하여야 합니다.")
	val dwellMinutes: Int? = null,

	@field:Schema(description = "방문지 메모", example = "부모님과 사진 찍기", nullable = true)
	@field:Size(max = 255, message = "메모는 255자 이하로 입력해주세요.")
	val note: String? = null,

	@field:Schema(description = "추천 이유", example = "짧은 산책과 휴식에 적합합니다.", nullable = true)
	@field:Size(max = 255, message = "추천 이유는 255자 이하로 입력해주세요.")
	val recommendationReason: String? = null,

	@field:Schema(description = "추천 태그", example = "[\"nature_scenery\",\"low_slope\"]")
	@field:Size(max = 20, message = "추천 태그는 최대 20개까지 저장할 수 있습니다.")
	val recommendationTags: List<@Size(max = 40, message = "추천 태그는 40자 이하로 입력해주세요.") String> = emptyList(),

	@field:Schema(description = "외부 API 원본 응답 일부. 정규 필드로 승격하기 전까지 JSON 객체로 보관합니다.", nullable = true)
	val sourcePayload: JsonNode? = null,

	@field:Schema(description = "사용자가 직접 추가한 장소 여부", example = "false")
	val isManualAdded: Boolean = false,
)
