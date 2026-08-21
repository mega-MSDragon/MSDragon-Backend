package com.msdragon.backend.trip.tourapi

interface TourApiClient {
	fun findPlaces(search: TourApiPlaceSearch): List<TourApiPlaceSummary>

	fun findNearbyPlaces(search: TourApiLocationSearch): List<TourApiPlaceSummary>

	fun searchPlaces(search: TourApiKeywordSearch): List<TourApiPlaceSummary>

	fun getPlaceDetail(contentId: String): TourApiPlaceDetail?

	fun getPlaceIntro(contentId: String, contentTypeId: String): TourApiPlaceIntro?

	fun getPlaceImages(contentId: String): List<TourApiPlaceImage>

	fun getAccessibility(contentId: String): TourApiAccessibility?
}
