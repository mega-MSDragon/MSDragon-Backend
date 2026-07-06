package com.msdragon.backend.trip.dto

import com.msdragon.backend.auth.entity.GenderType
import com.msdragon.backend.auth.entity.User
import com.msdragon.backend.auth.entity.UserRole
import com.msdragon.backend.family.entity.FamilyMember
import com.msdragon.backend.parentprofile.entity.FoodPreference
import com.msdragon.backend.parentprofile.entity.ParentProfile
import com.msdragon.backend.parentprofile.entity.ParentProfileStatus
import com.msdragon.backend.parentprofile.entity.TravelPersonalityTypeCode
import com.msdragon.backend.parentprofile.entity.TravelThemeCode
import com.msdragon.backend.parentprofile.entity.WalkingPace
import com.msdragon.backend.trip.entity.ExternalApiProvider
import com.msdragon.backend.trip.entity.StopType
import com.msdragon.backend.trip.entity.Trip
import com.msdragon.backend.trip.entity.TripDay
import com.msdragon.backend.trip.entity.TripDestinationCode
import com.msdragon.backend.trip.entity.TripParticipant
import com.msdragon.backend.trip.entity.TripStatus
import com.msdragon.backend.trip.entity.TripStop
import io.swagger.v3.oas.annotations.media.Schema
import tools.jackson.databind.JsonNode
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Schema(description = "여행 대상 부모 후보 목록 응답")
data class TripParentCandidatesResponse(
	@field:Schema(description = "가족 ID. 아직 가족 매칭 전이면 null입니다.", example = "1", nullable = true)
	val familyId: Long?,

	@field:Schema(description = "같은 가족에 연결된 부모 후보 목록")
	val parents: List<TripParentCandidateResponse>,
) {
	companion object {
		fun empty(): TripParentCandidatesResponse =
			TripParentCandidatesResponse(
				familyId = null,
				parents = emptyList(),
			)
	}
}

@Schema(description = "여행 대상 부모 후보 응답")
data class TripParentCandidateResponse(
	@field:Schema(description = "부모 사용자 ID", example = "2")
	val userId: Long,

	@field:Schema(description = "부모 이름 또는 닉네임", example = "김영희")
	val displayName: String,

	@field:Schema(description = "성별", example = "female", allowableValues = ["female", "male", "undisclosed"])
	val gender: String,

	@field:Schema(description = "가족 관계 표시 이름", example = "엄마", nullable = true)
	val relationLabel: String?,

	@field:Schema(description = "부모 상세 프로필 저장 여부", example = "true")
	val profileExists: Boolean,

	@field:Schema(description = "부모 상세 프로필 완료 여부", example = "true")
	val profileCompleted: Boolean,

	@field:Schema(description = "부모 상세 프로필 상태", example = "completed", allowableValues = ["draft", "completed"], nullable = true)
	val profileStatus: ParentProfileStatus?,

	@field:Schema(description = "부모 상세 프로필 작성률", example = "100")
	val profileCompletionPercent: Int,
) {
	companion object {
		fun of(member: FamilyMember, profile: ParentProfile?): TripParentCandidateResponse =
			TripParentCandidateResponse(
				userId = requireNotNull(member.user.id),
				displayName = member.user.displayName,
				gender = member.user.gender.value,
				relationLabel = relationLabelOf(member.user),
				profileExists = profile != null,
				profileCompleted = profile?.status == ParentProfileStatus.COMPLETED,
				profileStatus = profile?.status,
				profileCompletionPercent = profile?.completionPercent ?: 0,
			)
	}
}

@Schema(description = "여행 도시 응답")
data class TripDestinationResponse(
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
	val code: TripDestinationCode,

	@field:Schema(description = "화면 표시 이름", example = "경주")
	val displayName: String,

	@field:Schema(description = "화면 표시 순서", example = "3")
	val displayOrder: Int,

	@field:Schema(description = "배지 문구", example = "인기", nullable = true)
	val badgeLabel: String?,
) {
	companion object {
		fun from(code: TripDestinationCode): TripDestinationResponse =
			TripDestinationResponse(
				code = code,
				displayName = code.displayName,
				displayOrder = code.displayOrder,
				badgeLabel = code.badgeLabel,
			)
	}
}

@Schema(description = "내 여행 목록 응답")
data class MyTripsResponse(
	@field:Schema(description = "가족 ID. 아직 가족 매칭 전이면 null입니다.", example = "1", nullable = true)
	val familyId: Long?,

	@field:Schema(description = "내 가족 여행 목록")
	val trips: List<TripSummaryResponse>,
) {
	companion object {
		fun empty(): MyTripsResponse = MyTripsResponse(familyId = null, trips = emptyList())
	}
}

@Schema(description = "여행 요약 응답")
data class TripSummaryResponse(
	@field:Schema(description = "여행 ID", example = "1")
	val id: Long,

	@field:Schema(description = "여행 제목", example = "경주 여행")
	val title: String,

	@field:Schema(description = "여행 도시")
	val destination: TripDestinationResponse,

	@field:Schema(description = "여행 시작일", example = "2026-07-10")
	val startDate: LocalDate,

	@field:Schema(description = "여행 종료일", example = "2026-07-11")
	val endDate: LocalDate,

	@field:Schema(description = "여행 상태", example = "planning", allowableValues = ["planning", "ready", "in_progress", "completed", "archived"])
	val status: TripStatus,

	@field:Schema(description = "참여자 수", example = "2")
	val participantCount: Int,
) {
	companion object {
		fun of(trip: Trip, participantCount: Int): TripSummaryResponse =
			TripSummaryResponse(
				id = requireNotNull(trip.id),
				title = trip.title,
				destination = TripDestinationResponse.from(trip.destinationCode),
				startDate = trip.startDate,
				endDate = trip.endDate,
				status = trip.status,
				participantCount = participantCount,
			)
	}
}

@Schema(description = "여행 상세 응답")
data class TripDetailResponse(
	@field:Schema(description = "여행 ID", example = "1")
	val id: Long,

	@field:Schema(description = "가족 ID", example = "1")
	val familyId: Long,

	@field:Schema(description = "여행 제목", example = "경주 여행")
	val title: String,

	@field:Schema(description = "여행 도시")
	val destination: TripDestinationResponse,

	@field:Schema(description = "여행 시작일", example = "2026-07-10")
	val startDate: LocalDate,

	@field:Schema(description = "여행 종료일", example = "2026-07-11")
	val endDate: LocalDate,

	@field:Schema(description = "여행 상태", example = "planning", allowableValues = ["planning", "ready", "in_progress", "completed", "archived"])
	val status: TripStatus,

	@field:Schema(description = "여행 참여자")
	val participants: List<TripParticipantResponse>,

	@field:Schema(description = "여행 생성 시 확정한 추천 입력 스냅샷", nullable = true)
	val recommendationSnapshot: TripRecommendationSnapshotResponse?,

	@field:Schema(description = "여행 일자")
	val days: List<TripDayResponse>,
) {
	companion object {
		fun of(
			trip: Trip,
			participants: List<TripParticipant>,
			days: List<TripDay>,
			recommendationSnapshot: TripRecommendationSnapshotResponse?,
		): TripDetailResponse =
			TripDetailResponse(
				id = requireNotNull(trip.id),
				familyId = requireNotNull(trip.family.id),
				title = trip.title,
				destination = TripDestinationResponse.from(trip.destinationCode),
				startDate = trip.startDate,
				endDate = trip.endDate,
				status = trip.status,
				participants = participants.map(TripParticipantResponse::from),
				recommendationSnapshot = recommendationSnapshot,
				days = days.map(TripDayResponse::from),
			)
	}
}

@Schema(description = "여행 추천 입력 스냅샷")
data class TripRecommendationSnapshotResponse(
	@field:Schema(description = "추천 정책 버전", example = "parent-travel-mbti-v1")
	val policyVersion: String,

	@field:Schema(description = "스냅샷 생성 시간", example = "2026-07-06T12:00:00")
	val capturedAt: LocalDateTime,

	@field:Schema(description = "여행 도시 코드", example = "gyeongju")
	val destinationCode: TripDestinationCode,

	@field:Schema(description = "여행 시작일", example = "2026-07-10")
	val startDate: LocalDate,

	@field:Schema(description = "여행 종료일", example = "2026-07-11")
	val endDate: LocalDate,

	@field:Schema(description = "여행 대상 부모별 추천 입력값")
	val parents: List<TripParentProfileSnapshotResponse>,
)

@Schema(description = "여행 대상 부모 프로필 추천 입력 스냅샷")
data class TripParentProfileSnapshotResponse(
	@field:Schema(description = "부모 사용자 ID", example = "2")
	val parentUserId: Long,

	@field:Schema(description = "부모 프로필 ID", example = "1")
	val parentProfileId: Long,

	@field:Schema(description = "부모 이름 또는 닉네임", example = "김영희")
	val displayName: String,

	@field:Schema(description = "부모 성별 기반 가족 관계 표시 이름", example = "엄마", nullable = true)
	val relationLabel: String?,

	@field:Schema(description = "하루 이동 성향", example = "slow", allowableValues = ["slow", "normal", "fast"])
	val walkingPace: WalkingPace,

	@field:Schema(description = "이동 도움 필요 여부", example = "false")
	val needsMobilityAssistance: Boolean,

	@field:Schema(description = "선호 여행 테마", example = "[\"nature_scenery\",\"history_culture\"]")
	val travelThemes: List<TravelThemeCode>,

	@field:Schema(description = "음식 취향", example = "familiar", allowableValues = ["korean", "familiar", "adventurous"])
	val foodPreference: FoodPreference,

	@field:Schema(description = "추천용 부모님 여행 MBTI", example = "healing_traveler", allowableValues = ["urban_explorer", "culture_stroller", "healing_traveler", "heritage_walker", "active_adventurer", "local_challenger"])
	val personalityType: TravelPersonalityTypeCode,

	@field:Schema(description = "프로필 작성 완료 시간", example = "2026-07-01T12:00:00", nullable = true)
	val profileCompletedAt: LocalDateTime?,
)

@Schema(description = "여행 참여자 응답")
data class TripParticipantResponse(
	@field:Schema(description = "사용자 ID", example = "1")
	val userId: Long,

	@field:Schema(description = "역할", example = "parent", allowableValues = ["child", "parent"])
	val role: String,

	@field:Schema(description = "이름 또는 닉네임", example = "엄마")
	val displayName: String,

	@field:Schema(description = "성별", example = "female", allowableValues = ["female", "male", "undisclosed"])
	val gender: String,

	@field:Schema(description = "부모 성별 기반 가족 관계 표시 이름", example = "엄마", nullable = true)
	val relationLabel: String?,
) {
	companion object {
		fun from(participant: TripParticipant): TripParticipantResponse =
			TripParticipantResponse(
				userId = requireNotNull(participant.user.id),
				role = participant.user.role.value,
				displayName = participant.user.displayName,
				gender = participant.user.gender.value,
				relationLabel = relationLabelOf(participant.user),
			)
	}
}

@Schema(description = "여행 일자 응답")
data class TripDayResponse(
	@field:Schema(description = "여행 일자 ID", example = "1")
	val id: Long,

	@field:Schema(description = "여행 며칠차", example = "1")
	val dayNumber: Int,

	@field:Schema(description = "날짜", example = "2026-07-10")
	val travelDate: LocalDate,
) {
	companion object {
		fun from(day: TripDay): TripDayResponse =
			TripDayResponse(
				id = requireNotNull(day.id),
				dayNumber = day.dayNumber,
				travelDate = day.travelDate,
			)
	}
}

@Schema(description = "여행 코스 응답")
data class TripCourseResponse(
	@field:Schema(description = "여행 ID", example = "1")
	val tripId: Long,

	@field:Schema(description = "여행 제목", example = "경주 여행")
	val title: String,

	@field:Schema(description = "여행 도시")
	val destination: TripDestinationResponse,

	@field:Schema(description = "여행 상태", example = "planning", allowableValues = ["planning", "ready", "in_progress", "completed", "archived"])
	val status: TripStatus,

	@field:Schema(description = "일자별 코스")
	val days: List<TripCourseDayResponse>,
)

@Schema(description = "여행 일자별 코스 응답")
data class TripCourseDayResponse(
	@field:Schema(description = "여행 일자 ID", example = "1")
	val tripDayId: Long,

	@field:Schema(description = "여행 며칠차", example = "1")
	val dayNumber: Int,

	@field:Schema(description = "날짜", example = "2026-07-10")
	val travelDate: LocalDate,

	@field:Schema(description = "방문지 목록")
	val stops: List<TripStopResponse>,
)

@Schema(description = "여행 방문지 응답")
data class TripStopResponse(
	@field:Schema(description = "방문지 ID", example = "1")
	val id: Long,

	@field:Schema(description = "방문 순서", example = "1")
	val sortOrder: Int,

	@field:Schema(description = "방문지 유형", example = "sightseeing", allowableValues = ["sightseeing", "meal", "rest", "cafe"])
	val stopType: StopType,

	@field:Schema(
		description = "장소 원천 provider",
		example = "tour_api",
		allowableValues = ["tour_api", "tmap", "kakao_map", "public_data", "local_excel", "internal"],
	)
	val sourceProvider: ExternalApiProvider,

	@field:Schema(description = "외부 장소 ID", example = "988449", nullable = true)
	val externalPlaceId: String?,

	@field:Schema(description = "TourAPI contentTypeId", example = "12", nullable = true)
	val contentTypeId: String?,

	@field:Schema(description = "장소명", example = "오도리 공원")
	val name: String,

	@field:Schema(description = "장소 카테고리", example = "관광지", nullable = true)
	val category: String?,

	@field:Schema(description = "주소", example = "대구광역시 동구 효목동", nullable = true)
	val address: String?,

	@field:Schema(description = "위도", example = "35.8821234", nullable = true)
	val latitude: BigDecimal?,

	@field:Schema(description = "경도", example = "128.6212345", nullable = true)
	val longitude: BigDecimal?,

	@field:Schema(description = "전화번호", example = "053-123-4567", nullable = true)
	val phone: String?,

	@field:Schema(description = "홈페이지 URL", example = "https://example.com", nullable = true)
	val homepageUrl: String?,

	@field:Schema(description = "대표 이미지 URL", example = "https://example.com/image.jpg", nullable = true)
	val imageUrl: String?,

	@field:Schema(description = "장소 소개", example = "짧은 산책을 즐기기 좋은 공원입니다.", nullable = true)
	val overview: String?,

	@field:Schema(description = "도착 예정 시간", example = "10:30", nullable = true)
	val arrivalTime: LocalTime?,

	@field:Schema(description = "예상 체류 시간(분)", example = "60", nullable = true)
	val dwellMinutes: Int?,

	@field:Schema(description = "방문지 메모", example = "부모님과 사진 찍기", nullable = true)
	val note: String?,

	@field:Schema(description = "추천 이유", example = "짧은 산책과 휴식에 적합합니다.", nullable = true)
	val recommendationReason: String?,

	@field:Schema(description = "추천 태그", example = "[\"nature_scenery\",\"low_slope\"]")
	val recommendationTags: List<String>,

	@field:Schema(description = "외부 API 원본 응답 일부", nullable = true)
	val sourcePayload: JsonNode?,

	@field:Schema(description = "사용자가 직접 추가한 장소 여부", example = "false")
	val isManualAdded: Boolean,
) {
	companion object {
		fun of(stop: TripStop, recommendationTags: List<String>, sourcePayload: JsonNode?): TripStopResponse =
			TripStopResponse(
				id = requireNotNull(stop.id),
				sortOrder = stop.sortOrder,
				stopType = stop.stopType,
				sourceProvider = stop.sourceProvider,
				externalPlaceId = stop.externalPlaceId,
				contentTypeId = stop.contentTypeId,
				name = stop.name,
				category = stop.category,
				address = stop.address,
				latitude = stop.latitude,
				longitude = stop.longitude,
				phone = stop.phone,
				homepageUrl = stop.homepageUrl,
				imageUrl = stop.imageUrl,
				overview = stop.overview,
				arrivalTime = stop.arrivalTime,
				dwellMinutes = stop.dwellMinutes,
				note = stop.note,
				recommendationReason = stop.recommendationReason,
				recommendationTags = recommendationTags,
				sourcePayload = sourcePayload,
				isManualAdded = stop.isManualAdded,
			)
	}
}

fun relationLabelOf(user: User): String? {
	if (user.role != UserRole.PARENT) {
		return null
	}
	return when (user.gender) {
		GenderType.FEMALE -> "엄마"
		GenderType.MALE -> "아빠"
		GenderType.UNDISCLOSED -> null
	}
}
