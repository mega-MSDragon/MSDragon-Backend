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
import com.msdragon.backend.trip.service.TripCourseRecommendationService
import com.msdragon.backend.trip.service.TripService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/trips")
@Tag(name = "Trip", description = "여행 생성 기본 API 입니다.")
@SecurityRequirement(name = BEARER_AUTH_SCHEME)
class TripController(
	private val tripService: TripService,
	private val tripCourseRecommendationService: TripCourseRecommendationService,
) {
	@Operation(
		summary = "여행 대상 부모 후보 조회",
		description = "자녀 사용자가 같은 가족에 연결된 부모 목록과 부모 상세 프로필 완료 여부를 조회합니다.",
	)
	@ApiResponses(
		value = [
			SwaggerApiResponse(responseCode = "200", description = "여행 대상 부모 후보 조회 성공"),
			SwaggerApiResponse(responseCode = "400", description = "자녀 사용자가 아님"),
			SwaggerApiResponse(responseCode = "401", description = "인증 실패"),
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
			SwaggerApiResponse(responseCode = "200", description = "여행 도시 목록 조회 성공"),
			SwaggerApiResponse(responseCode = "401", description = "인증 실패"),
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
			SwaggerApiResponse(responseCode = "200", description = "내 가족 여행 목록 조회 성공"),
			SwaggerApiResponse(responseCode = "401", description = "인증 실패"),
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
			SwaggerApiResponse(responseCode = "200", description = "여행 상세 조회 성공"),
			SwaggerApiResponse(responseCode = "401", description = "인증 실패"),
			SwaggerApiResponse(responseCode = "403", description = "조회 권한 없음"),
			SwaggerApiResponse(responseCode = "404", description = "여행을 찾을 수 없음"),
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
			SwaggerApiResponse(responseCode = "200", description = "여행 코스 조회 성공"),
			SwaggerApiResponse(responseCode = "401", description = "인증 실패"),
			SwaggerApiResponse(responseCode = "403", description = "조회 권한 없음"),
			SwaggerApiResponse(responseCode = "404", description = "여행을 찾을 수 없음"),
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
		summary = "여행 추천 코스 생성",
		description = "여행 생성 시 저장한 부모님 프로필 스냅샷과 TourAPI 장소/무장애 정보를 기반으로 일자별 추천 코스를 생성해 저장합니다. 기존 코스가 있으면 추천 결과로 덮어씁니다.",
	)
	@ApiResponses(
		value = [
			SwaggerApiResponse(responseCode = "200", description = "여행 추천 코스 생성 성공"),
			SwaggerApiResponse(responseCode = "400", description = "추천 코스 생성 요청 상태가 올바르지 않음"),
			SwaggerApiResponse(responseCode = "401", description = "인증 실패"),
			SwaggerApiResponse(responseCode = "403", description = "생성 권한 없음"),
			SwaggerApiResponse(responseCode = "404", description = "여행을 찾을 수 없음"),
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
		summary = "여행 생성",
		description = "자녀 사용자가 여행 대상 부모, 도시, 날짜를 선택해 여행 기본 정보를 생성합니다. 추천 코스는 별도 추천 생성 API에서 생성합니다.",
	)
	@ApiResponses(
		value = [
			SwaggerApiResponse(responseCode = "201", description = "여행 생성 성공"),
			SwaggerApiResponse(responseCode = "400", description = "여행 생성 요청 값이 올바르지 않음"),
			SwaggerApiResponse(responseCode = "401", description = "인증 실패"),
		],
	)
	@ResponseStatus(HttpStatus.CREATED)
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
		summary = "여행 코스 저장",
		description = "같은 가족 구성원이 일자별 방문지 코스를 전체 저장합니다. 요청 배열 순서가 해당 일자의 방문 순서가 되며, 포함하지 않은 일자는 빈 코스로 저장됩니다.",
	)
	@ApiResponses(
		value = [
			SwaggerApiResponse(responseCode = "200", description = "여행 코스 저장 성공"),
			SwaggerApiResponse(responseCode = "400", description = "코스 저장 요청 값이 올바르지 않음"),
			SwaggerApiResponse(responseCode = "401", description = "인증 실패"),
			SwaggerApiResponse(responseCode = "403", description = "저장 권한 없음"),
			SwaggerApiResponse(responseCode = "404", description = "여행을 찾을 수 없음"),
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
}
