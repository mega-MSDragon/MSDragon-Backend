package com.msdragon.backend.trip.tmap

import com.msdragon.backend.common.exception.InternalServerException
import com.msdragon.backend.trip.config.TmapProperties
import com.nimbusds.jose.util.JSONObjectUtils
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import java.math.BigDecimal
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Component
class HttpTmapRouteClient(
	private val tmapProperties: TmapProperties,
	private val objectMapper: ObjectMapper,
) : TmapRouteClient {
	private val httpClient: HttpClient = HttpClient.newBuilder()
		.connectTimeout(tmapProperties.connectTimeout)
		.build()

	override fun optimizeRoute(request: TmapRouteOptimizationRequest): TmapRouteOptimizationResult {
		val appKey = tmapProperties.appKey.trimToNull()
			?: throw InternalServerException("Tmap 앱키 설정이 완료되지 않았습니다.")
		val httpRequest = HttpRequest.newBuilder(URI.create(url()))
			.timeout(tmapProperties.requestTimeout)
			.header("Accept", "application/json")
			.header("Content-Type", "application/json")
			.header("appKey", appKey)
			.POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody(request))))
			.build()
		val response = try {
			httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString())
		} catch (exception: InterruptedException) {
			Thread.currentThread().interrupt()
			throw InternalServerException("Tmap 경로 최적화 호출이 중단되었습니다.")
		} catch (_: Exception) {
			throw InternalServerException("Tmap 경로 최적화 호출 중 오류가 발생했습니다.")
		}
		if (response.statusCode() !in 200..299) {
			throw InternalServerException("Tmap 경로 최적화 호출에 실패했습니다.")
		}

		val parsedBody = parseJson(response.body())
		val properties = parsedBody.map("properties")
			?: throw InternalServerException("Tmap 경로 최적화 응답 형식이 올바르지 않습니다.")
		val totalDistance = properties.int("totalDistance")
			?: throw InternalServerException("Tmap 경로 최적화 거리 응답이 없습니다.")
		val totalTime = properties.int("totalTime")
			?: throw InternalServerException("Tmap 경로 최적화 시간 응답이 없습니다.")
		val features = parsedBody.list("features")
			.mapNotNull { (it as? Map<*, *>)?.asStringKeyMap() }

		val pointFeatures = features.filter { it.map("geometry")?.string("type") == "Point" }
		val orderedStopIds = orderedStopIds(request, pointFeatures)
		val arrivalTimes = arrivalTimes(request, pointFeatures)
		val polyline = features
			.filter { it.map("geometry")?.string("type") == "LineString" }
			.flatMap { it.map("geometry")?.list("coordinates").orEmpty().mapNotNull(::coordinate) }

		return TmapRouteOptimizationResult(
			totalDistanceMeters = totalDistance,
			totalDurationSeconds = totalTime,
			totalFare = properties.int("totalFare"),
			orderedStopIds = orderedStopIds,
			arrivalTimes = arrivalTimes,
			polyline = polyline,
			rawProperties = properties,
		)
	}

	private fun url(): String =
		"${tmapProperties.baseUri.trimEnd('/')}/routes/routeOptimization10?version=1"

	private fun requestBody(request: TmapRouteOptimizationRequest): Map<String, Any?> =
		mapOf(
			"reqCoordType" to "WGS84GEO",
			"resCoordType" to "WGS84GEO",
			"startName" to request.start.name,
			"startX" to request.start.longitude.toPlainString(),
			"startY" to request.start.latitude.toPlainString(),
			"startTime" to request.startTime.format(TMAP_START_TIME_FORMATTER),
			"endName" to request.end.name,
			"endX" to request.end.longitude.toPlainString(),
			"endY" to request.end.latitude.toPlainString(),
			"endPoiId" to "",
			"searchOption" to tmapProperties.searchOption,
			"carType" to tmapProperties.carType,
			"coordinateFlag" to "0",
			"viaPoints" to request.viaPoints.map { stop ->
				mapOf(
					"viaPointId" to stop.stopId.toString(),
					"viaPointName" to stop.name,
					"viaDetailAddress" to "",
					"viaX" to stop.longitude.toPlainString(),
					"viaY" to stop.latitude.toPlainString(),
					"viaPoiId" to "",
					"viaTime" to stop.dwellSeconds,
					"wishStartTime" to "",
					"wishEndTime" to "",
				)
			},
		)

	private fun orderedStopIds(
		request: TmapRouteOptimizationRequest,
		pointFeatures: List<Map<String, Any?>>,
	): List<Long> {
		val requestedStopIds = request.requestedStopIds()
		val responseStopIds = pointFeatures
			.sortedBy { it.map("properties")?.int("index") ?: Int.MAX_VALUE }
			.mapNotNull { feature ->
				val properties = feature.map("properties") ?: return@mapNotNull null
				when (properties.string("pointType")) {
					"S" -> request.start.stopId
					"E" -> request.end.stopId
					else -> properties.string("viaPointId")?.toLongOrNull()
				}
			}

		return (responseStopIds + requestedStopIds)
			.distinct()
			.filter { it in requestedStopIds }
	}

	private fun arrivalTimes(
		request: TmapRouteOptimizationRequest,
		pointFeatures: List<Map<String, Any?>>,
	): Map<Long, LocalTime> =
		pointFeatures.mapNotNull { feature ->
			val properties = feature.map("properties") ?: return@mapNotNull null
			val stopId = when (properties.string("pointType")) {
				"S" -> request.start.stopId
				"E" -> request.end.stopId
				else -> properties.string("viaPointId")?.toLongOrNull()
			} ?: return@mapNotNull null
			val arriveTime = properties.string("arriveTime")?.let(::parseTmapDateTime)
				?: return@mapNotNull null
			stopId to arriveTime.toLocalTime()
		}.toMap()

	private fun parseJson(body: String): Map<String, Any?> =
		try {
			JSONObjectUtils.parse(body.unwrapJsonp()).asStringKeyMap()
		} catch (_: Exception) {
			throw InternalServerException("Tmap 경로 최적화 응답을 해석할 수 없습니다.")
		}

	private fun parseTmapDateTime(value: String): LocalDateTime? =
		runCatching { LocalDateTime.parse(value, TMAP_RESPONSE_TIME_FORMATTER) }.getOrNull()

	private fun TmapRouteOptimizationRequest.requestedStopIds(): List<Long> =
		listOf(start.stopId) + viaPoints.map { it.stopId } + end.stopId

	companion object {
		private val TMAP_START_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmm")
		private val TMAP_RESPONSE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
	}
}

private fun coordinate(value: Any?): TmapRouteCoordinate? {
	val coordinates = value as? List<*> ?: return null
	val longitude = coordinates.getOrNull(0)?.toString()?.toBigDecimalOrNull() ?: return null
	val latitude = coordinates.getOrNull(1)?.toString()?.toBigDecimalOrNull() ?: return null
	return TmapRouteCoordinate(
		longitude = longitude,
		latitude = latitude,
	)
}

private fun String.unwrapJsonp(): String {
	val trimmed = trim()
	val openIndex = trimmed.indexOf('(')
	val closeIndex = trimmed.lastIndexOf(')')
	return if (openIndex > 0 && closeIndex > openIndex) {
		trimmed.substring(openIndex + 1, closeIndex)
	} else {
		trimmed
	}
}

private fun Map<*, *>.asStringKeyMap(): Map<String, Any?> =
	entries.associate { (key, value) -> key.toString() to value }

private fun Map<String, Any?>.map(key: String): Map<String, Any?>? =
	(this[key] as? Map<*, *>)?.asStringKeyMap()

private fun Map<String, Any?>.list(key: String): List<*> =
	this[key] as? List<*> ?: emptyList<Any?>()

private fun Map<String, Any?>.string(key: String): String? =
	this[key]?.toString()?.trimToNull()

private fun Map<String, Any?>.int(key: String): Int? =
	string(key)?.toIntOrNull()

private fun String?.trimToNull(): String? =
	this?.trim()?.takeIf { it.isNotEmpty() }
