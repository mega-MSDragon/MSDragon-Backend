package com.msdragon.backend.supportfacility.tmap

import com.msdragon.backend.common.exception.InternalServerException
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

interface TmapGeocodingClient {
	fun geocode(address: String): TmapGeocodingCoordinate?
}

data class TmapGeocodingCoordinate(
	val latitude: BigDecimal,
	val longitude: BigDecimal,
)

@Component
class HttpTmapGeocodingClient(
	private val tmapProperties: TmapProperties,
	private val objectMapper: ObjectMapper,
) : TmapGeocodingClient {
	private val httpClient: HttpClient = HttpClient.newBuilder()
		.connectTimeout(tmapProperties.connectTimeout)
		.build()

	override fun geocode(address: String): TmapGeocodingCoordinate? {
		val appKey = tmapProperties.appKey.trimToNull()
			?: throw InternalServerException("Tmap 앱키 설정이 완료되지 않았습니다.")
		val request = HttpRequest.newBuilder(URI.create(url(address)))
			.timeout(tmapProperties.requestTimeout)
			.header("Accept", "application/json")
			.header("appKey", appKey)
			.GET()
			.build()
		val response = try {
			httpClient.send(request, HttpResponse.BodyHandlers.ofString())
		} catch (exception: InterruptedException) {
			Thread.currentThread().interrupt()
			throw InternalServerException("Tmap 주소 좌표 변환 호출이 중단되었습니다.")
		} catch (_: Exception) {
			throw InternalServerException("Tmap 주소 좌표 변환 호출 중 오류가 발생했습니다.")
		}
		if (response.statusCode() !in 200..299) {
			throw InternalServerException("Tmap 주소 좌표 변환 호출에 실패했습니다. status=${response.statusCode()}")
		}

		val body = try {
			objectMapper.readValue(response.body(), TmapFullAddressResponse::class.java)
		} catch (_: Exception) {
			throw InternalServerException("Tmap 주소 좌표 변환 응답을 해석할 수 없습니다.")
		}
		return body.coordinateInfo?.coordinate
			.orEmpty()
			.firstNotNullOfOrNull(TmapAddressCoordinate::preferredCoordinate)
	}

	private fun url(address: String): String =
		"${tmapProperties.baseUri.trimEnd('/')}/geo/fullAddrGeo" +
			"?version=1&format=json&addressFlag=F00&coordType=WGS84GEO&count=1" +
			"&fullAddr=${URLEncoder.encode(address, StandardCharsets.UTF_8)}"
}

private data class TmapFullAddressResponse(
	val coordinateInfo: TmapCoordinateInfo? = null,
)

private data class TmapCoordinateInfo(
	val coordinate: List<TmapAddressCoordinate> = emptyList(),
)

private data class TmapAddressCoordinate(
	val lat: String? = null,
	val lon: String? = null,
	val latEntr: String? = null,
	val lonEntr: String? = null,
	val newLat: String? = null,
	val newLon: String? = null,
	val newLatEntr: String? = null,
	val newLonEntr: String? = null,
) {
	fun preferredCoordinate(): TmapGeocodingCoordinate? =
		coordinate(newLatEntr, newLonEntr)
			?: coordinate(newLat, newLon)
			?: coordinate(latEntr, lonEntr)
			?: coordinate(lat, lon)

	private fun coordinate(latitude: String?, longitude: String?): TmapGeocodingCoordinate? {
		val parsedLatitude = latitude?.toBigDecimalOrNull() ?: return null
		val parsedLongitude = longitude?.toBigDecimalOrNull() ?: return null
		if (parsedLatitude !in MIN_KOREA_LATITUDE..MAX_KOREA_LATITUDE ||
			parsedLongitude !in MIN_KOREA_LONGITUDE..MAX_KOREA_LONGITUDE
		) {
			return null
		}
		return TmapGeocodingCoordinate(parsedLatitude, parsedLongitude)
	}

	companion object {
		private val MIN_KOREA_LATITUDE = BigDecimal("32")
		private val MAX_KOREA_LATITUDE = BigDecimal("39.5")
		private val MIN_KOREA_LONGITUDE = BigDecimal("123")
		private val MAX_KOREA_LONGITUDE = BigDecimal("132")
	}
}

private fun String?.trimToNull(): String? =
	this?.trim()?.takeIf { it.isNotEmpty() }
