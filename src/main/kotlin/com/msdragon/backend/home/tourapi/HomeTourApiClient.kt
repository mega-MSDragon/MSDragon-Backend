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
}

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
