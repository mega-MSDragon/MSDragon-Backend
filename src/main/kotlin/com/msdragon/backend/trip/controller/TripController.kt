package com.msdragon.backend.trip.controller

import com.msdragon.backend.auth.support.AuthenticatedUser
import com.msdragon.backend.auth.support.CurrentUser
import com.msdragon.backend.common.config.BEARER_AUTH_SCHEME
import com.msdragon.backend.common.response.ApiResponse
import com.msdragon.backend.trip.dto.CreateTripRequest
import com.msdragon.backend.trip.dto.MyTripsResponse
import com.msdragon.backend.trip.dto.SaveTripCourseRequest
import com.msdragon.backend.trip.dto.TripCourseResponse
import com.msdragon.backend.trip.dto.TripDestinationResponse
import com.msdragon.backend.trip.dto.TripDetailResponse
import com.msdragon.backend.trip.dto.TripParentCandidatesResponse
import com.msdragon.backend.trip.dto.TripPlaceDetailResponse
import com.msdragon.backend.trip.dto.TripPlaceRecommendationsResponse
import com.msdragon.backend.trip.dto.TripPlaceSearchResponse
import com.msdragon.backend.trip.dto.TripStopResponse
import com.msdragon.backend.trip.dto.TripTravelModeResponse
import com.msdragon.backend.trip.dto.UpdateTripRequest
import com.msdragon.backend.trip.dto.UpdateTripStopNoteRequest
import com.msdragon.backend.trip.entity.TripPlaceCategory
import com.msdragon.backend.trip.service.TripCourseRecommendationService
import com.msdragon.backend.trip.service.TripPlaceService
import com.msdragon.backend.trip.service.TripRouteOptimizationService
import com.msdragon.backend.trip.service.TripService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/trips")
@Tag(name = "Trip", description = "여행 생성 기본 API 입니다.")
@SecurityRequirement(name = BEARER_AUTH_SCHEME)
class TripController(
	private val tripService: TripService,
	private val tripCourseRecommendationService: TripCourseRecommendationService,
	private val tripPlaceService: TripPlaceService,
	private val tripRouteOptimizationService: TripRouteOptimizationService,
) {
	@Operation(
		summary = "여행 대상 부모 후보 조회",
		description = "자녀 사용자가 같은 가족에 연결된 부모 목록, 상세 프로필 작성 단계와 여행 MBTI 결과를 조회합니다.",
	)
	@ApiResponses(
		value = [
			SwaggerApiResponse(responseCode = "200", description = "처리 완료: 조회 성공(status=200) 또는 인증·정책 오류(status=400/401)"),
		],
	)
	@GetMapping("/parent-candidates")
	fun getParentCandidates(
		@CurrentUser currentUser: AuthenticatedUser,
	): ApiResponse<TripParentCandidatesResponse> =
		ApiResponse.success(
			message = "여행 대상 부모 후보 조회 성공",
			data = tripService.getParentCandidates(currentUser),
		)

	@Operation(
		summary = "여행 도시 목록 조회",
		description = "도시 선택 화면에 표시할 서버 고정 여행 도시 목록을 조회합니다.",
	)
	@ApiResponses(
		value = [
			SwaggerApiResponse(responseCode = "200", description = "처리 완료: 조회 성공(status=200) 또는 인증 오류(status=401)"),
		],
	)
	@GetMapping("/destinations")
	fun getDestinations(
		@CurrentUser currentUser: AuthenticatedUser,
	): ApiResponse<List<TripDestinationResponse>> =
		ApiResponse.success(
			message = "여행 도시 목록 조회 성공",
			data = tripService.getDestinations(),
		)

	@Operation(
		summary = "내 가족 여행 목록 조회",
		description = "로그인 사용자가 속한 가족의 여행 목록을 조회합니다. 가족 매칭 전이면 빈 목록을 반환합니다.",
	)
	@ApiResponses(
		value = [
			SwaggerApiResponse(responseCode = "200", description = "처리 완료: 조회 성공(status=200) 또는 인증 오류(status=401)"),
		],
	)
	@GetMapping
	fun getMyTrips(
		@CurrentUser currentUser: AuthenticatedUser,
	): ApiResponse<MyTripsResponse> =
		ApiResponse.success(
			message = "내 가족 여행 목록 조회 성공",
			data = tripService.getMyTrips(currentUser),
		)

	@Operation(
		summary = "여행 상세 조회",
		description = "같은 가족 구성원이 여행 기본 정보, 참여자, 여행 일자를 조회합니다.",
	)
	@ApiResponses(
		value = [
			SwaggerApiResponse(responseCode = "200", description = "처리 완료: 조회 성공(status=200) 또는 인증·정책 오류(status=401/403/404)"),
		],
	)
	@GetMapping("/{tripId}")
	fun getTrip(
		@CurrentUser currentUser: AuthenticatedUser,
		@Parameter(description = "여행 ID", example = "1")
		@PathVariable tripId: Long,
	): ApiResponse<TripDetailResponse> =
		ApiResponse.success(
			message = "여행 상세 조회 성공",
			data = tripService.getTrip(currentUser, tripId),
		)

	@Operation(
		summary = "여행 코스 조회",
		description = "같은 가족 구성원이 일자별 방문지 코스를 조회합니다. 아직 저장된 방문지가 없으면 일자별 빈 목록을 반환합니다.",
	)
	@ApiResponses(
		value = [
			SwaggerApiResponse(responseCode = "200", description = "처리 완료: 조회 성공(status=200) 또는 인증·정책 오류(status=401/403/404)"),
		],
	)
	@GetMapping("/{tripId}/course")
	fun getTripCourse(
		@CurrentUser currentUser: AuthenticatedUser,
		@Parameter(description = "여행 ID", example = "1")
		@PathVariable tripId: Long,
	): ApiResponse<TripCourseResponse> =
		ApiResponse.success(
			message = "여행 코스 조회 성공",
			data = tripService.getTripCourse(currentUser, tripId),
		)

	@Operation(
		summary = "여행 모드 조회",
		description = "같은 가족 구성원이 여행 시작일부터 종료일까지 현재 일차와 전체 일자별 코스를 조회합니다. 조회 시 서울 날짜를 기준으로 여행 상태를 진행 중으로 동기화합니다.",
	)
	@ApiResponses(
		value = [
			SwaggerApiResponse(responseCode = "200", description = "처리 완료: 조회 성공(status=200) 또는 인증·정책 오류(status=400/401/403/404)"),
		],
	)
	@GetMapping("/{tripId}/travel-mode")
	fun getTravelMode(
		@CurrentUser currentUser: AuthenticatedUser,
		@Parameter(description = "여행 ID", example = "1")
		@PathVariable tripId: Long,
	): ApiResponse<TripTravelModeResponse> =
		ApiResponse.success(
			message = "여행 모드 조회 성공",
			data = tripService.getTravelMode(currentUser, tripId),
		)

	@Operation(
		summary = "여행 추천 코스 생성",
		description = "여행을 만든 자녀가 부모님 프로필 스냅샷과 TourAPI 장소/무장애 정보를 기반으로 일자별 추천 코스를 생성해 저장합니다. 기존 코스가 있으면 추천 결과로 덮어쓰며 완료·중단·보관 여행은 수정할 수 없습니다.",
	)
	@ApiResponses(
		value = [
			SwaggerApiResponse(responseCode = "200", description = "처리 완료: 생성 성공(status=200) 또는 인증·정책 오류(status=400/401/403/404)"),
			SwaggerApiResponse(responseCode = "500", description = "TourAPI 설정 또는 호출 실패"),
		],
	)
	@PostMapping("/{tripId}/course/recommendation")
	fun recommendTripCourse(
		@CurrentUser currentUser: AuthenticatedUser,
		@Parameter(description = "여행 ID", example = "1")
		@PathVariable tripId: Long,
	): ApiResponse<TripCourseResponse> =
		ApiResponse.success(
			message = "여행 추천 코스 생성 성공",
			data = tripCourseRecommendationService.recommendCourse(currentUser, tripId),
		)

	@Operation(
		summary = "여행 일자 경로 최적화",
		description = "여행을 만든 자녀가 저장된 일자별 방문지 좌표를 기준으로 모든 시작/도착 조합을 Tmap API에 조회하고, 가장 짧은 경로로 방문 순서와 경로 캐시를 갱신합니다. 완료·중단·보관 여행은 수정할 수 없습니다.",
	)
	@ApiResponses(
		value = [
			SwaggerApiResponse(responseCode = "200", description = "처리 완료: 최적화 성공(status=200) 또는 인증·정책 오류(status=400/401/403/404)"),
			SwaggerApiResponse(responseCode = "500", description = "Tmap 설정 또는 호출 실패"),
		],
	)
	@PostMapping("/{tripId}/days/{dayNumber}/route-optimization")
	fun optimizeTripDayRoute(
		@CurrentUser currentUser: AuthenticatedUser,
		@Parameter(description = "여행 ID", example = "1")
		@PathVariable tripId: Long,
		@Parameter(description = "여행 며칠차", example = "1")
		@PathVariable dayNumber: Int,
	): ApiResponse<TripCourseResponse> =
		ApiResponse.success(
			message = "여행 일자 경로 최적화 성공",
			data = tripRouteOptimizationService.optimizeDayRoute(currentUser, tripId, dayNumber),
		)

	@Operation(
		summary = "방문지 추천",
		description = "코스 편집 화면 진입 시 여행 도시와 부모 프로필 추천 스냅샷을 기준으로 현재 코스에 없는 맛집 또는 관광지를 추천합니다.",
	)
	@ApiResponses(
		value = [
			SwaggerApiResponse(responseCode = "200", description = "처리 완료: 추천 성공(status=200) 또는 요청·인증·정책 오류(status=400/401/403/404)"),
			SwaggerApiResponse(responseCode = "500", description = "TourAPI 설정 또는 호출 실패"),
		],
	)
	@GetMapping("/{tripId}/places/recommendations")
	fun recommendTripPlaces(
		@CurrentUser currentUser: AuthenticatedUser,
		@Parameter(description = "여행 ID", example = "1")
		@PathVariable tripId: Long,
		@Parameter(description = "추천 장소 구분", example = "attraction", schema = Schema(allowableValues = ["restaurant", "attraction"]))
		@RequestParam category: String,
		@Parameter(description = "추천 장소 수. 최대 50", example = "20")
		@RequestParam(defaultValue = "20") size: Int,
	): ApiResponse<TripPlaceRecommendationsResponse> =
		ApiResponse.success(
			message = "방문지 추천 성공",
			data = tripCourseRecommendationService.recommendPlaces(currentUser, tripId, TripPlaceCategory.from(category), size),
		)

	@Operation(
		summary = "방문지 검색",
		description = "코스 편집 화면에서 여행 도시 범위 안의 TourAPI 맛집 또는 관광지를 키워드로 검색합니다. 숙박은 검색 결과에서 제외합니다.",
	)
	@ApiResponses(
		value = [
			SwaggerApiResponse(responseCode = "200", description = "처리 완료: 검색 성공(status=200) 또는 요청·인증·정책 오류(status=400/401/403/404)"),
			SwaggerApiResponse(responseCode = "500", description = "TourAPI 설정 또는 호출 실패"),
		],
	)
	@GetMapping("/{tripId}/places/search")
	fun searchTripPlaces(
		@CurrentUser currentUser: AuthenticatedUser,
		@Parameter(description = "여행 ID", example = "1")
		@PathVariable tripId: Long,
		@Parameter(description = "검색어", example = "경주 맛집")
		@RequestParam keyword: String,
		@Parameter(description = "검색 결과 구분", example = "restaurant", schema = Schema(allowableValues = ["restaurant", "attraction"]))
		@RequestParam category: String,
		@Parameter(description = "페이지 번호", example = "1")
		@RequestParam(defaultValue = "1") page: Int,
		@Parameter(description = "페이지 크기. 최대 50", example = "20")
		@RequestParam(defaultValue = "20") size: Int,
	): ApiResponse<TripPlaceSearchResponse> =
		ApiResponse.success(
			message = "방문지 검색 성공",
			data = tripPlaceService.searchPlaces(currentUser, tripId, keyword, TripPlaceCategory.from(category), page, size),
		)

	@Operation(
		summary = "방문지 상세 조회",
		description = "코스 편집 화면에서 TourAPI 방문지 상세와 무장애 주요 정보를 조회합니다.",
	)
	@ApiResponses(
		value = [
			SwaggerApiResponse(responseCode = "200", description = "처리 완료: 조회 성공(status=200) 또는 요청·인증·정책 오류(status=400/401/403/404)"),
			SwaggerApiResponse(responseCode = "500", description = "TourAPI 설정 또는 호출 실패"),
		],
	)
	@GetMapping("/{tripId}/places/{contentId}")
	fun getTripPlaceDetail(
		@CurrentUser currentUser: AuthenticatedUser,
		@Parameter(description = "여행 ID", example = "1")
		@PathVariable tripId: Long,
		@Parameter(description = "TourAPI contentId", example = "988449")
		@PathVariable contentId: String,
		@Parameter(
			description = "TourAPI 콘텐츠 타입 ID. 알고 있는 경우 전달합니다.",
			example = "12",
			schema = Schema(allowableValues = ["12", "14", "15", "28", "38", "39"]),
		)
		@RequestParam(required = false) contentTypeId: String?,
	): ApiResponse<TripPlaceDetailResponse> =
		ApiResponse.success(
			message = "방문지 상세 조회 성공",
			data = tripPlaceService.getPlaceDetail(currentUser, tripId, contentId, contentTypeId),
		)

	@Operation(
		summary = "여행 생성",
		description = "자녀 사용자가 여행 대상 부모, 도시, 날짜와 15자 이하의 제목을 입력해 여행 기본 정보를 생성합니다. 추천 코스는 별도 추천 생성 API에서 생성합니다.",
	)
	@ApiResponses(
		value = [
			SwaggerApiResponse(responseCode = "200", description = "처리 완료: 생성 성공(status=201) 또는 요청·인증·정책 오류(status=400/401)"),
		],
	)
	@PostMapping
	fun createTrip(
		@CurrentUser currentUser: AuthenticatedUser,
		@Valid @RequestBody request: CreateTripRequest,
	): ApiResponse<TripDetailResponse> =
		ApiResponse.success(
			status = HttpStatus.CREATED.value(),
			message = "여행 생성 성공",
			data = tripService.createTrip(currentUser, request),
		)

	@Operation(
		summary = "여행 기본정보 수정",
		description = "여행을 만든 자녀가 기간과 참여 부모를 수정합니다. 제목과 도시는 생성 후 변경할 수 없습니다. 날짜 또는 참여 부모가 바뀌면 기존 코스와 경로를 초기화하며, 참여 부모가 바뀌면 10계명과 모든 서명도 삭제합니다.",
	)
	@ApiResponses(
		value = [
			SwaggerApiResponse(responseCode = "200", description = "처리 완료: 수정 성공(status=200) 또는 요청·인증·정책 오류(status=400/401/403/404)"),
		],
	)
	@PutMapping("/{tripId}")
	fun updateTrip(
		@CurrentUser currentUser: AuthenticatedUser,
		@Parameter(description = "여행 ID", example = "1")
		@PathVariable tripId: Long,
		@Valid @RequestBody request: UpdateTripRequest,
	): ApiResponse<TripDetailResponse> =
		ApiResponse.success(
			message = "여행 기본정보 수정 성공",
			data = tripService.updateTrip(currentUser, tripId, request),
		)

	@Operation(
		summary = "여행 삭제",
		description = "여행을 만든 자녀가 planning, ready 또는 in_progress 여행을 soft delete합니다. 삭제된 여행은 목록과 상세 조회에서 제외되며 같은 날짜로 새 여행을 만들 수 있습니다.",
	)
	@ApiResponses(
		value = [
			SwaggerApiResponse(responseCode = "200", description = "처리 완료: 삭제 성공(status=200) 또는 인증·정책 오류(status=400/401/403/404)"),
		],
	)
	@DeleteMapping("/{tripId}")
	fun deleteTrip(
		@CurrentUser currentUser: AuthenticatedUser,
		@Parameter(description = "여행 ID", example = "1")
		@PathVariable tripId: Long,
	): ApiResponse<Unit> {
		tripService.deleteTrip(currentUser, tripId)
		return ApiResponse.success(message = "여행 삭제 성공")
	}

	@Operation(
		summary = "여행 수동 종료",
		description = "여행을 만든 자녀가 in_progress 여행을 수동 종료합니다. 정상 종료와 동일하게 completed 상태로 기록 탭에 남고, 바로 부모 평가를 진행할 수 있습니다.",
	)
	@ApiResponses(
		value = [
			SwaggerApiResponse(responseCode = "200", description = "처리 완료: 중단 성공(status=200) 또는 인증·정책 오류(status=400/401/403/404)"),
		],
	)
	@PostMapping("/{tripId}/stop")
	fun stopTrip(
		@CurrentUser currentUser: AuthenticatedUser,
		@Parameter(description = "여행 ID", example = "1")
		@PathVariable tripId: Long,
	): ApiResponse<TripDetailResponse> =
		ApiResponse.success(
			message = "여행 종료 성공",
			data = tripService.stopTrip(currentUser, tripId),
		)

	@Operation(
		summary = "여행 코스 저장",
		description = "여행을 만든 자녀가 준비 중 또는 여행 중인 방문지를 편집한 뒤 최종 일자별 코스를 전체 저장합니다. 방문지 단건 추가·수정·삭제 API가 아니므로 변경하지 않은 일자도 함께 보내야 하며, 포함하지 않은 일자는 빈 코스로 저장됩니다. 요청 배열 순서가 방문 순서가 됩니다. 방문지 구성이 바뀐 일자의 기존 경로만 초기화되므로 응답에서 route가 null인 변경 일자를 다시 최적화해야 합니다. 완료·중단·보관 여행은 수정할 수 없습니다.",
	)
	@ApiResponses(
		value = [
			SwaggerApiResponse(responseCode = "200", description = "처리 완료: 저장 성공(status=200) 또는 요청·인증·정책 오류(status=400/401/403/404)"),
		],
	)
	@PutMapping("/{tripId}/course")
	fun saveTripCourse(
		@CurrentUser currentUser: AuthenticatedUser,
		@Parameter(description = "여행 ID", example = "1")
		@PathVariable tripId: Long,
		@Valid @RequestBody request: SaveTripCourseRequest,
	): ApiResponse<TripCourseResponse> =
		ApiResponse.success(
			message = "여행 코스 저장 성공",
			data = tripService.saveTripCourse(currentUser, tripId, request),
		)

	@Operation(
		summary = "방문지 메모 수정",
		description = "여행을 만든 자녀가 코스 전체를 다시 저장하지 않고 방문지 메모를 저장하거나 삭제합니다. null 또는 공백 메모는 삭제로 처리합니다.",
	)
	@ApiResponses(
		value = [
			SwaggerApiResponse(responseCode = "200", description = "처리 완료: 수정 성공(status=200) 또는 요청·인증·정책 오류(status=400/401/403/404)"),
		],
	)
	@PutMapping("/{tripId}/stops/{stopId}/note")
	fun updateTripStopNote(
		@CurrentUser currentUser: AuthenticatedUser,
		@Parameter(description = "여행 ID", example = "1")
		@PathVariable tripId: Long,
		@Parameter(description = "방문지 ID", example = "1")
		@PathVariable stopId: Long,
		@Valid @RequestBody request: UpdateTripStopNoteRequest,
	): ApiResponse<TripStopResponse> =
		ApiResponse.success(
			message = "방문지 메모 수정 성공",
			data = tripService.updateTripStopNote(currentUser, tripId, stopId, request),
		)
}
