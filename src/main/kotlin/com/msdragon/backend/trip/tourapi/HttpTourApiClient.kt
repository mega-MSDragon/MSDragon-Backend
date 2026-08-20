package com.msdragon.backend.trip.tourapi

import com.msdragon.backend.common.exception.InternalServerException
import com.msdragon.backend.trip.config.TourApiProperties
import com.nimbusds.jose.util.JSONObjectUtils
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets

@Component
class HttpTourApiClient(
	private val tourApiProperties: TourApiProperties,
) : TourApiClient {
	private val httpClient: HttpClient = HttpClient.newBuilder()
		.connectTimeout(tourApiProperties.connectTimeout)
		.build()

	override fun findPlaces(search: TourApiPlaceSearch): List<TourApiPlaceSummary> {
		val items = requestItems(
			operation = "areaBasedList2",
			params = mapOf(
				"numOfRows" to search.numOfRows.toString(),
				"pageNo" to search.pageNo.toString(),
				"arrange" to "Q",
				"contentTypeId" to search.contentTypeId,
				"lDongRegnCd" to search.region.lDongRegnCd,
			) + search.region.lDongSignguCd?.let { mapOf("lDongSignguCd" to it) }.orEmpty(),
		)

		return items.mapNotNull { item -> item.toPlaceSummary(defaultContentTypeId = search.contentTypeId) }
	}

	override fun searchPlaces(search: TourApiKeywordSearch): List<TourApiPlaceSummary> {
		val items = requestItems(
			operation = "searchKeyword2",
			params = mapOf(
				"numOfRows" to search.numOfRows.toString(),
				"pageNo" to search.pageNo.toString(),
				"arrange" to "Q",
				"keyword" to search.keyword,
				"lDongRegnCd" to search.region.lDongRegnCd,
			) + search.region.lDongSignguCd?.let { mapOf("lDongSignguCd" to it) }.orEmpty(),
		)

		return items.mapNotNull { item -> item.toPlaceSummary() }
	}

	override fun getPlaceDetail(contentId: String): TourApiPlaceDetail? {
		val item = requestItems(
			operation = "detailCommon2",
			params = mapOf(
				"numOfRows" to "10",
				"pageNo" to "1",
				"contentId" to contentId,
			),
		).firstOrNull() ?: return null

		return TourApiPlaceDetail(
			homepage = item.string("homepage"),
			overview = item.string("overview"),
			raw = item,
			contentId = item.string("contentid") ?: contentId,
			contentTypeId = item.string("contenttypeid"),
			title = item.string("title"),
			address = listOfNotNull(item.string("addr1"), item.string("addr2"))
				.joinToString(" ")
				.trimToNull(),
			latitude = item.decimal("mapy"),
			longitude = item.decimal("mapx"),
			tel = item.string("tel"),
			firstImage = item.string("firstimage"),
			firstImageThumbnail = item.string("firstimage2"),
			lclsSystm1 = item.string("lclsSystm1"),
			lclsSystm2 = item.string("lclsSystm2"),
			lclsSystm3 = item.string("lclsSystm3"),
		)
	}

	override fun getPlaceIntro(contentId: String, contentTypeId: String): TourApiPlaceIntro? {
		val item = requestItems(
			operation = "detailIntro2",
			params = mapOf(
				"numOfRows" to "10",
				"pageNo" to "1",
				"contentId" to contentId,
				"contentTypeId" to contentTypeId,
			),
		).firstOrNull() ?: return null

		return TourApiPlaceIntro(
			operatingHours = item.firstString("usetime", "usetimeculture", "usetimeleports", "opentime", "opentimefood"),
			closedDays = item.firstString("restdate", "restdateculture", "restdateleports", "restdateshopping", "restdatefood"),
			admissionFee = item.firstString("usefee", "usefeeleports", "saleitemcost"),
			raw = item,
		)
	}

	override fun getPlaceImages(contentId: String): List<TourApiPlaceImage> =
		requestItems(
			operation = "detailImage2",
			params = mapOf(
				"numOfRows" to "50",
				"pageNo" to "1",
				"contentId" to contentId,
				"imageYN" to "Y",
				"subImageYN" to "Y",
			),
		).map { item ->
			TourApiPlaceImage(
				imageUrl = item.string("originimgurl"),
				thumbnailUrl = item.string("smallimageurl"),
				raw = item,
			)
		}

	override fun getAccessibility(contentId: String): TourApiAccessibility? {
		val item = requestItems(
			operation = "detailWithTour2",
			params = mapOf(
				"numOfRows" to "10",
				"pageNo" to "1",
				"contentId" to contentId,
			),
		).firstOrNull() ?: return null

		return TourApiAccessibility(
			parking = item.string("parking"),
			publicTransport = item.string("publictransport"),
			route = item.string("route"),
			wheelchair = item.string("wheelchair"),
			exit = item.string("exit"),
			elevator = item.string("elevator"),
			restroom = item.string("restroom"),
			raw = item,
		)
	}

	private fun requestItems(operation: String, params: Map<String, String>): List<Map<String, Any?>> {
		val request = HttpRequest.newBuilder(URI.create(url(operation, params)))
			.timeout(tourApiProperties.requestTimeout)
			.GET()
			.build()
		val response = try {
			httpClient.send(request, HttpResponse.BodyHandlers.ofString())
		} catch (exception: InterruptedException) {
			Thread.currentThread().interrupt()
			throw InternalServerException("TourAPI 호출이 중단되었습니다.")
		} catch (_: Exception) {
			throw InternalServerException("TourAPI 호출 중 오류가 발생했습니다.")
		}
		if (response.statusCode() !in 200..299) {
			throw InternalServerException("TourAPI 호출에 실패했습니다: operation=$operation, httpStatus=${response.statusCode()}")
		}

		val parsedBody = parseJson(response.body())
		val responseMap = parsedBody.map("response") ?: throw directApiError(parsedBody, operation)
		val header = responseMap.map("header")
		val resultCode = header?.string("resultCode")
		if (resultCode != null && resultCode != "0000") {
			throw tourApiError(operation, resultCode, header.string("resultMsg"))
		}

		val body = responseMap.map("body") ?: return emptyList()
		val item = body.map("items")?.get("item") ?: return emptyList()
		return when (item) {
			is List<*> -> item.mapNotNull { (it as? Map<*, *>)?.asStringKeyMap() }
			is Map<*, *> -> listOf(item.asStringKeyMap())
			else -> emptyList()
		}
	}

	private fun directApiError(parsedBody: Map<String, Any?>, operation: String): InternalServerException {
		val resultCode = parsedBody.string("resultCode")
		if (resultCode != null && resultCode != "0000") {
			return tourApiError(operation, resultCode, parsedBody.string("resultMsg"))
		}
		return InternalServerException("TourAPI 응답 형식이 올바르지 않습니다: operation=$operation")
	}

	private fun tourApiError(operation: String, resultCode: String, resultMessage: String?): InternalServerException =
		InternalServerException(
			"TourAPI 응답이 실패했습니다: operation=$operation, resultCode=$resultCode, resultMsg=${resultMessage ?: "unknown"}",
		)

	private fun url(operation: String, params: Map<String, String>): String {
		val serviceKey = tourApiProperties.serviceKey.trimToNull()
			?: throw InternalServerException("TourAPI 서비스키 설정이 완료되지 않았습니다.")
		val baseParams = mapOf(
			"serviceKey" to serviceKey,
			"MobileOS" to tourApiProperties.mobileOs,
			"MobileApp" to tourApiProperties.mobileApp,
			"_type" to "json",
		)
		val query = (baseParams + params)
			.entries
			.joinToString("&") { (key, value) -> "${key.encode()}=${value.encode()}" }
		return "${tourApiProperties.baseUri.trimEnd('/')}/$operation?$query"
	}

	private fun parseJson(body: String): Map<String, Any?> =
		try {
			JSONObjectUtils.parse(body).asStringKeyMap()
		} catch (_: Exception) {
			throw InternalServerException("TourAPI 응답을 해석할 수 없습니다.")
		}
}

private fun String.encode(): String =
	URLEncoder.encode(this, StandardCharsets.UTF_8)

private fun Map<*, *>.asStringKeyMap(): Map<String, Any?> =
	entries.associate { (key, value) -> key.toString() to value }

private fun Map<String, Any?>.map(key: String): Map<String, Any?>? =
	(this[key] as? Map<*, *>)?.asStringKeyMap()

private fun Map<String, Any?>.string(key: String): String? =
	this[key]?.toString()?.trimToNull()

private fun Map<String, Any?>.decimal(key: String): BigDecimal? =
	string(key)?.let { value ->
		runCatching { value.toBigDecimal() }.getOrNull()
	}

private fun Map<String, Any?>.firstString(vararg keys: String): String? =
	keys.firstNotNullOfOrNull(::string)

private fun Map<String, Any?>.toPlaceSummary(defaultContentTypeId: String? = null): TourApiPlaceSummary? {
	val contentId = string("contentid") ?: return null
	val contentTypeId = string("contenttypeid") ?: defaultContentTypeId ?: return null
	val title = string("title") ?: return null
	return TourApiPlaceSummary(
		contentId = contentId,
		contentTypeId = contentTypeId,
		title = title,
		address = listOfNotNull(string("addr1"), string("addr2"))
			.joinToString(" ")
			.trimToNull(),
		latitude = decimal("mapy"),
		longitude = decimal("mapx"),
		tel = string("tel"),
		firstImage = string("firstimage"),
		firstImageThumbnail = string("firstimage2"),
		lclsSystm1 = string("lclsSystm1"),
		lclsSystm2 = string("lclsSystm2"),
		lclsSystm3 = string("lclsSystm3"),
		raw = this,
	)
}

private fun String?.trimToNull(): String? =
	this?.trim()?.takeIf { it.isNotEmpty() }
