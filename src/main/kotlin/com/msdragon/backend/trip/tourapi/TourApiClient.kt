package com.msdragon.backend.trip.tourapi

interface TourApiClient {
	fun findPlaces(search: TourApiPlaceSearch): List<TourApiPlaceSummary>

	fun searchPlaces(search: TourApiKeywordSearch): List<TourApiPlaceSummary>

	fun getPlaceDetail(contentId: String): TourApiPlaceDetail?

	fun getAccessibility(contentId: String): TourApiAccessibility?
}
