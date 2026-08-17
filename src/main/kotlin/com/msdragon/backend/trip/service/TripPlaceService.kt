package com.msdragon.backend.trip.service

import com.msdragon.backend.auth.entity.User
import com.msdragon.backend.auth.repository.UserRepository
import com.msdragon.backend.auth.support.AuthenticatedUser
import com.msdragon.backend.common.exception.BadRequestException
import com.msdragon.backend.common.exception.ForbiddenException
import com.msdragon.backend.common.exception.NotFoundException
import com.msdragon.backend.common.exception.UnAuthorizedException
import com.msdragon.backend.family.repository.FamilyMemberRepository
import com.msdragon.backend.trip.dto.TripDestinationResponse
import com.msdragon.backend.trip.dto.TripPlaceDetailResponse
import com.msdragon.backend.trip.dto.TripPlaceSearchResponse
import com.msdragon.backend.trip.dto.TripPlaceSummaryResponse
import com.msdragon.backend.trip.entity.TripPlaceCategory
import com.msdragon.backend.trip.entity.Trip
import com.msdragon.backend.trip.repository.TripRepository
import com.msdragon.backend.trip.tourapi.DestinationTourApiPolicy
import com.msdragon.backend.trip.tourapi.TourApiClient
import com.msdragon.backend.trip.tourapi.TourApiContentType
import com.msdragon.backend.trip.tourapi.TourApiKeywordSearch
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TripPlaceService(
	private val userRepository: UserRepository,
	private val familyMemberRepository: FamilyMemberRepository,
	private val tripRepository: TripRepository,
	private val tourApiClient: TourApiClient,
) {
	@Transactional(readOnly = true)
	fun searchPlaces(
		currentUser: AuthenticatedUser,
		tripId: Long,
		keyword: String,
		category: TripPlaceCategory,
		page: Int,
		size: Int,
	): TripPlaceSearchResponse {
		getLoginUser(currentUser.id)
		val trip = getReadableTrip(currentUser.id, tripId)
		val normalizedKeyword = keyword.trim()
		if (normalizedKeyword.isBlank()) {
			throw BadRequestException("검색어를 입력해주세요.")
		}
		if (normalizedKeyword.length > MAX_KEYWORD_LENGTH) {
			throw BadRequestException("검색어는 ${MAX_KEYWORD_LENGTH}자 이하로 입력해주세요.")
		}
		val normalizedPage = validatePage(page)
		val normalizedSize = validateSize(size)
		val allowedContentTypeIds = category.contentTypes().map(TourApiContentType::id).toSet()

		val destinationPolicy = DestinationTourApiPolicy.of(trip.destinationCode)
		if (destinationPolicy.regions.isEmpty()) {
			throw BadRequestException("지원하지 않는 여행 도시입니다.")
		}

		val places = destinationPolicy.regions
			.flatMap { region ->
				tourApiClient.searchPlaces(
					TourApiKeywordSearch(
						region = region,
						keyword = normalizedKeyword,
						numOfRows = normalizedSize,
						pageNo = normalizedPage,
					),
				)
			}
			.filter { it.contentTypeId in allowedContentTypeIds }
			.distinctBy { it.contentId }
			.take(normalizedSize)
			.map(TripPlaceSummaryResponse::of)

		return TripPlaceSearchResponse(
			tripId = tripId,
			destination = TripDestinationResponse.from(trip.destinationCode),
			keyword = normalizedKeyword,
			category = category,
			page = normalizedPage,
			size = normalizedSize,
			places = places,
		)
	}

	@Transactional(readOnly = true)
	fun getPlaceDetail(
		currentUser: AuthenticatedUser,
		tripId: Long,
		contentId: String,
		contentTypeId: String?,
	): TripPlaceDetailResponse {
		getLoginUser(currentUser.id)
		getReadableTrip(currentUser.id, tripId)
		val normalizedContentId = contentId.trim()
		if (normalizedContentId.isBlank()) {
			throw BadRequestException("장소 ID를 입력해주세요.")
		}

		val requestedContentTypeId = normalizeContentTypeId(contentTypeId)
		val detail = tourApiClient.getPlaceDetail(normalizedContentId)
			?: throw NotFoundException("방문지를 찾을 수 없습니다.")
		val resolvedContentTypeId = detail.contentTypeId ?: requestedContentTypeId
		if (resolvedContentTypeId != null && TourApiContentType.fromId(resolvedContentTypeId) == null) {
			throw BadRequestException("지원하지 않는 콘텐츠 타입입니다.")
		}

		return TripPlaceDetailResponse.of(
			contentId = normalizedContentId,
			detail = detail,
			intro = resolvedContentTypeId?.let { tourApiClient.getPlaceIntro(normalizedContentId, it) },
			images = tourApiClient.getPlaceImages(normalizedContentId),
			accessibility = tourApiClient.getAccessibility(normalizedContentId),
			requestedContentTypeId = requestedContentTypeId,
		)
	}

	private fun normalizeContentTypeId(contentTypeId: String?): String? {
		val normalized = contentTypeId?.trim()?.takeIf { it.isNotEmpty() } ?: return null
		if (TourApiContentType.fromId(normalized) == null) {
			throw BadRequestException("지원하지 않는 콘텐츠 타입입니다.")
		}
		return normalized
	}

	private fun validatePage(page: Int): Int {
		if (page < 1) {
			throw BadRequestException("페이지 번호는 1 이상이어야 합니다.")
		}
		return page
	}

	private fun validateSize(size: Int): Int {
		if (size !in 1..MAX_PAGE_SIZE) {
			throw BadRequestException("페이지 크기는 1 이상 ${MAX_PAGE_SIZE} 이하이어야 합니다.")
		}
		return size
	}

	private fun getReadableTrip(userId: Long, tripId: Long): Trip {
		val trip = tripRepository.findByIdAndDeletedAtIsNull(tripId)
			?: throw NotFoundException("여행을 찾을 수 없습니다.")
		validateTripReadable(userId, trip)
		return trip
	}

	private fun validateTripReadable(userId: Long, trip: Trip) {
		val myMember = familyMemberRepository.findByUserId(userId)
			?: throw ForbiddenException("여행 조회 권한이 없습니다.")
		if (myMember.family.id != trip.family.id) {
			throw ForbiddenException("여행 조회 권한이 없습니다.")
		}
	}

	private fun getLoginUser(userId: Long): User =
		userRepository.findByIdAndDeletedAtIsNull(userId)
			?.takeIf { it.isSignupCompleted() }
			?: throw UnAuthorizedException("로그인할 수 없는 사용자입니다.")

	companion object {
		private const val MAX_KEYWORD_LENGTH = 50
		private const val MAX_PAGE_SIZE = 50
	}
}

internal fun TripPlaceCategory.contentTypes(): List<TourApiContentType> =
	when (this) {
		TripPlaceCategory.RESTAURANT -> listOf(TourApiContentType.FOOD)
		TripPlaceCategory.ATTRACTION -> TourApiContentType.recommendationTargets.filter { it != TourApiContentType.FOOD }
	}
