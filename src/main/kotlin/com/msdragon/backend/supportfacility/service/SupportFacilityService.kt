package com.msdragon.backend.supportfacility.service

import com.msdragon.backend.auth.support.AuthenticatedUser
import com.msdragon.backend.common.exception.BadRequestException
import com.msdragon.backend.common.exception.InternalServerException
import com.msdragon.backend.supportfacility.dto.NearbyCafeResponse
import com.msdragon.backend.supportfacility.dto.NearbyMedicalFacilityResponse
import com.msdragon.backend.supportfacility.dto.NearbyRestroomResponse
import com.msdragon.backend.supportfacility.entity.SupportFacility
import com.msdragon.backend.supportfacility.entity.SupportFacilityType
import com.msdragon.backend.supportfacility.repository.SupportFacilityRepository
import com.msdragon.backend.supportfacility.tmap.TmapPoi
import com.msdragon.backend.supportfacility.tmap.TmapPoiClient
import com.msdragon.backend.trip.service.TripService
import com.msdragon.backend.trip.tourapi.TourApiClient
import com.msdragon.backend.trip.tourapi.TourApiLocationSearch
import com.msdragon.backend.trip.tourapi.TourApiPlaceSummary
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigDecimal
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

@Service
class SupportFacilityService(
	private val supportFacilityRepository: SupportFacilityRepository,
	private val tmapPoiClient: TmapPoiClient,
	private val tourApiClient: TourApiClient,
	private val tripService: TripService,
) {
	fun getNearbyRestrooms(
		currentUser: AuthenticatedUser,
		tripId: Long,
		latitude: Double,
		longitude: Double,
	): List<NearbyRestroomResponse> {
		validateCoordinate(latitude, longitude)
		tripService.validateTravelModeAccess(currentUser, tripId)

		val latitudeDelta = SEARCH_RADIUS_METERS / METERS_PER_LATITUDE_DEGREE
		val longitudeDelta = latitudeDelta / abs(cos(Math.toRadians(latitude))).coerceAtLeast(0.01)
		return supportFacilityRepository.findWithinBoundingBox(
			facilityType = SupportFacilityType.RESTROOM,
			minLatitude = BigDecimal.valueOf(latitude - latitudeDelta),
			maxLatitude = BigDecimal.valueOf(latitude + latitudeDelta),
			minLongitude = BigDecimal.valueOf(longitude - longitudeDelta),
			maxLongitude = BigDecimal.valueOf(longitude + longitudeDelta),
		)
			.map { facility ->
				FacilityDistance(
					facility,
					distanceMeters(latitude, longitude, facility.latitude, facility.longitude),
				)
			}
			.filter { it.distanceMeters <= SEARCH_RADIUS_METERS }
			.sortedWith(compareBy<FacilityDistance> { it.distanceMeters }.thenBy { it.facility.id })
			.take(RESULT_LIMIT)
			.map { result ->
				NearbyRestroomResponse(
					id = requireNotNull(result.facility.id),
					name = result.facility.name,
					address = result.facility.address,
					latitude = result.facility.latitude,
					longitude = result.facility.longitude,
					distanceMeters = result.distanceMeters.roundToInt(),
					phone = result.facility.phone,
					operatingHours = result.facility.operatingHours,
				)
			}
	}

	fun getNearbyMedicalFacilities(
		currentUser: AuthenticatedUser,
		tripId: Long,
		latitude: Double,
		longitude: Double,
		facilityType: SupportFacilityType,
	): List<NearbyMedicalFacilityResponse> {
		check(facilityType == SupportFacilityType.HOSPITAL || facilityType == SupportFacilityType.PHARMACY)
		validateCoordinate(latitude, longitude)
		tripService.validateTravelModeAccess(currentUser, tripId)

		return tmapPoiClient.findNearby(
			facilityType = facilityType,
			latitude = BigDecimal.valueOf(latitude),
			longitude = BigDecimal.valueOf(longitude),
			radiusKilometers = SEARCH_RADIUS_KILOMETERS,
			limit = RESULT_LIMIT,
		)
			.map { poi ->
				MedicalFacilityDistance(
					poi,
					distanceMeters(latitude, longitude, poi.latitude, poi.longitude),
				)
			}
			.filter { it.distanceMeters <= SEARCH_RADIUS_METERS }
			.sortedWith(compareBy<MedicalFacilityDistance> { it.distanceMeters }.thenBy { it.poi.id })
			.take(RESULT_LIMIT)
			.map { result ->
				NearbyMedicalFacilityResponse(
					id = result.poi.id,
					type = facilityType,
					name = result.poi.name,
					address = result.poi.address,
					latitude = result.poi.latitude,
					longitude = result.poi.longitude,
					distanceMeters = result.distanceMeters.roundToInt(),
					phone = result.poi.phone,
				)
			}
	}

	fun getNearbyCafes(
		currentUser: AuthenticatedUser,
		tripId: Long,
		latitude: Double,
		longitude: Double,
	): List<NearbyCafeResponse> {
		validateCoordinate(latitude, longitude)
		tripService.validateTravelModeAccess(currentUser, tripId)

		val cafes = tmapPoiClient.findNearbyCafes(
			latitude = BigDecimal.valueOf(latitude),
			longitude = BigDecimal.valueOf(longitude),
			radiusKilometers = SEARCH_RADIUS_KILOMETERS,
			limit = RESULT_LIMIT,
		)
			.map { poi -> MedicalFacilityDistance(poi, distanceMeters(latitude, longitude, poi.latitude, poi.longitude)) }
			.filter { it.distanceMeters <= SEARCH_RADIUS_METERS }
			.sortedWith(compareBy<MedicalFacilityDistance> { it.distanceMeters }.thenBy { it.poi.id })
			.take(RESULT_LIMIT)
		val imageCandidates = findNearbyCafeImages(latitude, longitude)

		return cafes
			.map { result ->
				NearbyCafeResponse(
					id = result.poi.id,
					name = result.poi.name,
					address = result.poi.address,
					latitude = result.poi.latitude,
					longitude = result.poi.longitude,
					distanceMeters = result.distanceMeters.roundToInt(),
					phone = result.poi.phone,
					thumbnailImageUrl = matchingThumbnail(result.poi, imageCandidates),
				)
			}
	}

	private fun findNearbyCafeImages(latitude: Double, longitude: Double): List<TourApiPlaceSummary> =
		try {
			tourApiClient.findNearbyPlaces(
				TourApiLocationSearch(
					latitude = BigDecimal.valueOf(latitude),
					longitude = BigDecimal.valueOf(longitude),
					radiusMeters = SEARCH_RADIUS_METERS.toInt(),
					contentTypeId = TOUR_API_FOOD_CONTENT_TYPE_ID,
				),
			).filter { !it.firstImageThumbnail.isNullOrBlank() || !it.firstImage.isNullOrBlank() }
		} catch (exception: InternalServerException) {
			logger.warn("TourAPI 카페 썸네일 보강에 실패했습니다. reason={}", exception.message)
			emptyList()
		}

	private fun matchingThumbnail(poi: TmapPoi, candidates: List<TourApiPlaceSummary>): String? {
		val normalizedPoiName = normalizeName(poi.name)
		return candidates.asSequence()
			.filter { candidate ->
				val candidateName = normalizeName(candidate.title)
				candidateName.contains(normalizedPoiName) || normalizedPoiName.contains(candidateName)
			}
			.mapNotNull { candidate ->
				val candidateLatitude = candidate.latitude ?: return@mapNotNull null
				val candidateLongitude = candidate.longitude ?: return@mapNotNull null
				val distance = distanceMeters(
					poi.latitude.toDouble(),
					poi.longitude.toDouble(),
					candidateLatitude,
					candidateLongitude,
				)
				candidate to distance
			}
			.filter { (_, distance) -> distance <= CAFE_IMAGE_MATCH_RADIUS_METERS }
			.minByOrNull { (_, distance) -> distance }
			?.first
			?.let { it.firstImageThumbnail ?: it.firstImage }
	}

	private fun normalizeName(value: String): String =
		value.lowercase().filter(Char::isLetterOrDigit)

	private fun validateCoordinate(latitude: Double, longitude: Double) {
		if (!latitude.isFinite() || latitude !in -90.0..90.0) {
			throw BadRequestException("latitude는 -90 이상 90 이하여야 합니다.")
		}
		if (!longitude.isFinite() || longitude !in -180.0..180.0) {
			throw BadRequestException("longitude는 -180 이상 180 이하여야 합니다.")
		}
	}

	private fun distanceMeters(
		latitude: Double,
		longitude: Double,
		facilityLatitude: BigDecimal,
		facilityLongitude: BigDecimal,
	): Double {
		val endLatitudeValue = facilityLatitude.toDouble()
		val endLongitudeValue = facilityLongitude.toDouble()
		val latitudeDistance = Math.toRadians(endLatitudeValue - latitude)
		val longitudeDistance = Math.toRadians(endLongitudeValue - longitude)
		val startLatitude = Math.toRadians(latitude)
		val endLatitude = Math.toRadians(endLatitudeValue)
		val haversine = sin(latitudeDistance / 2).let { it * it } +
			cos(startLatitude) * cos(endLatitude) * sin(longitudeDistance / 2).let { it * it }
		return 2 * EARTH_RADIUS_METERS * atan2(sqrt(haversine), sqrt(1 - haversine))
	}

	private data class FacilityDistance(
		val facility: SupportFacility,
		val distanceMeters: Double,
	)

	private data class MedicalFacilityDistance(
		val poi: TmapPoi,
		val distanceMeters: Double,
	)

	companion object {
		private val logger = LoggerFactory.getLogger(SupportFacilityService::class.java)
		private const val RESULT_LIMIT = 10
		private const val SEARCH_RADIUS_KILOMETERS = 5
		private const val SEARCH_RADIUS_METERS = 5_000.0
		private const val METERS_PER_LATITUDE_DEGREE = 111_320.0
		private const val EARTH_RADIUS_METERS = 6_371_000.0
		private const val CAFE_IMAGE_MATCH_RADIUS_METERS = 200.0
		private const val TOUR_API_FOOD_CONTENT_TYPE_ID = "39"
	}
}
