package com.msdragon.backend.supportfacility.service

import com.msdragon.backend.auth.support.AuthenticatedUser
import com.msdragon.backend.common.exception.BadRequestException
import com.msdragon.backend.supportfacility.dto.NearbyRestroomResponse
import com.msdragon.backend.supportfacility.entity.SupportFacility
import com.msdragon.backend.supportfacility.entity.SupportFacilityType
import com.msdragon.backend.supportfacility.repository.SupportFacilityRepository
import com.msdragon.backend.trip.service.TripService
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
			.map { facility -> FacilityDistance(facility, distanceMeters(latitude, longitude, facility)) }
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
		facility: SupportFacility,
	): Double {
		val facilityLatitude = facility.latitude.toDouble()
		val facilityLongitude = facility.longitude.toDouble()
		val latitudeDistance = Math.toRadians(facilityLatitude - latitude)
		val longitudeDistance = Math.toRadians(facilityLongitude - longitude)
		val startLatitude = Math.toRadians(latitude)
		val endLatitude = Math.toRadians(facilityLatitude)
		val haversine = sin(latitudeDistance / 2).let { it * it } +
			cos(startLatitude) * cos(endLatitude) * sin(longitudeDistance / 2).let { it * it }
		return 2 * EARTH_RADIUS_METERS * atan2(sqrt(haversine), sqrt(1 - haversine))
	}

	private data class FacilityDistance(
		val facility: SupportFacility,
		val distanceMeters: Double,
	)

	companion object {
		private const val RESULT_LIMIT = 10
		private const val SEARCH_RADIUS_METERS = 5_000.0
		private const val METERS_PER_LATITUDE_DEGREE = 111_320.0
		private const val EARTH_RADIUS_METERS = 6_371_000.0
	}
}
