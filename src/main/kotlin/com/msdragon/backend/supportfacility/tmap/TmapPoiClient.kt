package com.msdragon.backend.supportfacility.tmap

import com.msdragon.backend.common.exception.InternalServerException
import com.msdragon.backend.supportfacility.entity.SupportFacilityType
import com.msdragon.backend.trip.config.TmapProperties
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import java.math.BigDecimal
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets

interface TmapPoiClient {
	fun findNearby(
		facilityType: SupportFacilityType,
		latitude: BigDecimal,
		longitude: BigDecimal,
		radiusKilometers: Int,
		limit: Int,
	): List<TmapPoi>

	fun findNearbyCafes(
		latitude: BigDecimal,
		longitude: BigDecimal,
		radiusKilometers: Int,
		limit: Int,
	): List<TmapPoi>
}

data class TmapPoi(
	val id: String,
	val name: String,
	val address: String?,
	val latitude: BigDecimal,
	val longitude: BigDecimal,
	val phone: String?,
)

@Component
class HttpTmapPoiClient(
	private val tmapProperties: TmapProperties,
	private val objectMapper: ObjectMapper,
) : TmapPoiClient {
	private val httpClient: HttpClient = HttpClient.newBuilder()
		.connectTimeout(tmapProperties.connectTimeout)
		.build()

	override fun findNearby(
		facilityType: SupportFacilityType,
		latitude: BigDecimal,
		longitude: BigDecimal,
		radiusKilometers: Int,
		limit: Int,
	): List<TmapPoi> = findNearby(
		category = category(facilityType),
		latitude = latitude,
		longitude = longitude,
		radiusKilometers = radiusKilometers,
		limit = limit,
	)

	override fun findNearbyCafes(
		latitude: BigDecimal,
		longitude: BigDecimal,
		radiusKilometers: Int,
		limit: Int,
	): List<TmapPoi> = findNearby(
		category = "카페",
		latitude = latitude,
		longitude = longitude,
		radiusKilometers = radiusKilometers,
		limit = limit,
	)

	private fun findNearby(
		category: String,
		latitude: BigDecimal,
		longitude: BigDecimal,
		radiusKilometers: Int,
		limit: Int,
	): List<TmapPoi> {
		val appKey = tmapProperties.appKey.trimToNull()
			?: throw InternalServerException("Tmap 앱키 설정이 완료되지 않았습니다.")
		val request = HttpRequest.newBuilder(
			URI.create(url(category, latitude, longitude, radiusKilometers, limit, appKey)),
		)
			.timeout(tmapProperties.requestTimeout)
			.header("Accept", "application/json")
			.GET()
			.build()
		val response = try {
			httpClient.send(request, HttpResponse.BodyHandlers.ofString())
		} catch (exception: InterruptedException) {
			Thread.currentThread().interrupt()
			throw InternalServerException("Tmap 주변 장소 조회가 중단되었습니다.")
		} catch (_: Exception) {
			throw InternalServerException("Tmap 주변 장소 조회 중 오류가 발생했습니다.")
		}
		if (response.statusCode() !in 200..299) {
			throw InternalServerException("Tmap 주변 장소 조회에 실패했습니다. status=${response.statusCode()}")
		}

		val body = try {
			objectMapper.readValue(response.body(), TmapPoiAroundResponse::class.java)
		} catch (_: Exception) {
			throw InternalServerException("Tmap 주변 장소 응답을 해석할 수 없습니다.")
		}
		val searchPoiInfo = body.searchPoiInfo
			?: throw InternalServerException("Tmap 주변 장소 응답 형식이 올바르지 않습니다.")
		return searchPoiInfo.pois?.poi.orEmpty().mapNotNull(TmapPoiItem::toPoi)
	}

	private fun url(
		category: String,
		latitude: BigDecimal,
		longitude: BigDecimal,
		radiusKilometers: Int,
		limit: Int,
		appKey: String,
	): String =
		"${tmapProperties.baseUri.trimEnd('/')}/pois/search/around" +
			"?version=1&page=1&count=$limit" +
			"&categories=${encode(category)}" +
			"&centerLon=${longitude.toPlainString()}&centerLat=${latitude.toPlainString()}" +
			"&radius=$radiusKilometers&reqCoordType=WGS84GEO&resCoordType=WGS84GEO" +
			"&sort=distance&appKey=${encode(appKey)}"

	private fun category(facilityType: SupportFacilityType): String =
		when (facilityType) {
			SupportFacilityType.HOSPITAL -> "병원"
			SupportFacilityType.PHARMACY -> "약국"
			SupportFacilityType.RESTROOM -> error("공중화장실은 Tmap POI 조회 대상이 아닙니다.")
		}

	private fun encode(value: String): String =
		URLEncoder.encode(value, StandardCharsets.UTF_8)
}

private data class TmapPoiAroundResponse(
	val searchPoiInfo: TmapSearchPoiInfo? = null,
)

private data class TmapSearchPoiInfo(
	val pois: TmapPois? = null,
)

private data class TmapPois(
	val poi: List<TmapPoiItem> = emptyList(),
)

private data class TmapPoiItem(
	val id: String? = null,
	val name: String? = null,
	val telNo: String? = null,
	val frontLat: String? = null,
	val frontLon: String? = null,
	val noorLat: String? = null,
	val noorLon: String? = null,
	val upperAddrName: String? = null,
	val middleAddrName: String? = null,
	val lowerAddrName: String? = null,
	val firstNo: String? = null,
	val secondNo: String? = null,
	val roadName: String? = null,
	val buildingNo1: String? = null,
	val buildingNo2: String? = null,
) {
	fun toPoi(): TmapPoi? {
		val poiId = id.trimToNull() ?: return null
		val poiName = name.trimToNull() ?: return null
		val coordinate = coordinate(frontLat, frontLon) ?: coordinate(noorLat, noorLon) ?: return null
		return TmapPoi(
			id = poiId,
			name = poiName,
			address = address(),
			latitude = coordinate.first,
			longitude = coordinate.second,
			phone = telNo.trimToNull(),
		)
	}

	private fun coordinate(latitude: String?, longitude: String?): Pair<BigDecimal, BigDecimal>? {
		val parsedLatitude = latitude?.toBigDecimalOrNull() ?: return null
		val parsedLongitude = longitude?.toBigDecimalOrNull() ?: return null
		if (parsedLatitude !in MIN_LATITUDE..MAX_LATITUDE || parsedLongitude !in MIN_LONGITUDE..MAX_LONGITUDE) {
			return null
		}
		return parsedLatitude to parsedLongitude
	}

	private fun address(): String? {
		val road = roadName.trimToNull()
		if (road != null) {
			return listOfNotNull(
				upperAddrName.trimToNull(),
				middleAddrName.trimToNull(),
				road,
				buildingNumber(),
			).joinToString(" ").trimToNull()
		}
		return listOfNotNull(
			upperAddrName.trimToNull(),
			middleAddrName.trimToNull(),
			lowerAddrName.trimToNull(),
			lotNumber(),
		).joinToString(" ").trimToNull()
	}

	private fun buildingNumber(): String? =
		number(buildingNo1, buildingNo2)

	private fun lotNumber(): String? =
		number(firstNo, secondNo)

	private fun number(primary: String?, secondary: String?): String? {
		val first = primary.trimToNull() ?: return null
		val second = secondary.trimToNull()?.takeUnless { it == "0" }
		return if (second == null) first else "$first-$second"
	}

	companion object {
		private val MIN_LATITUDE = BigDecimal("32")
		private val MAX_LATITUDE = BigDecimal("39.5")
		private val MIN_LONGITUDE = BigDecimal("123")
		private val MAX_LONGITUDE = BigDecimal("132")
	}
}

private fun String?.trimToNull(): String? =
	this?.trim()?.takeIf(String::isNotEmpty)
