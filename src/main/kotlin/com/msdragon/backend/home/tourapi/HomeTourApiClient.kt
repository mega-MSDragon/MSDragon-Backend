package com.msdragon.backend.home.tourapi

import com.msdragon.backend.trip.entity.TripDestinationCode
import java.time.LocalDate

interface HomeTourApiClient {
	fun findDestinationImage(destination: TripDestinationCode): String?

	fun findFestivals(
		startDate: LocalDate,
		endDate: LocalDate,
		limit: Int,
	): List<HomeTourApiFestival>

	/**
	 * 도시별 추천 관광지. 이미지가 있는 결과를 우선하며 도시 순서를 유지한다.
	 * 특정 도시 조회가 실패해도 나머지 도시 결과를 반환한다.
	 */
	fun findAttractions(
		destinations: List<TripDestinationCode>,
		limitPerDestination: Int,
	): List<HomeTourApiAttraction>
}

data class HomeTourApiAttraction(
	val contentId: String,
	val title: String,
	val imageUrl: String?,
	val address: String?,
	val regionName: String?,
	val destination: TripDestinationCode,
)

data class HomeTourApiFestival(
	val contentId: String,
	val title: String,
	val summary: String?,
	val imageUrl: String?,
	val address: String?,
	val regionName: String?,
	val eventStartDate: LocalDate,
	val eventEndDate: LocalDate,
)
