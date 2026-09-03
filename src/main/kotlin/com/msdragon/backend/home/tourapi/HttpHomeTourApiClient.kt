package com.msdragon.backend.home.tourapi

import com.msdragon.backend.common.exception.InternalServerException
import com.msdragon.backend.trip.config.TourApiProperties
import com.msdragon.backend.trip.entity.TripDestinationCode
import com.msdragon.backend.trip.tourapi.DestinationTourApiPolicy
import com.nimbusds.jose.util.JSONObjectUtils
import org.jsoup.Jsoup
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.CompletableFuture

@Component
class HttpHomeTourApiClient(
	private val tourApiProperties: TourApiProperties,
) : HomeTourApiClient {
	private val logger = LoggerFactory.getLogger(javaClass)
	private val httpClient: HttpClient = HttpClient.newBuilder()
		.connectTimeout(tourApiProperties.connectTimeout)
		.build()

	override fun findDestinationImage(destination: TripDestinationCode): String? {
		DestinationTourApiPolicy.of(destination).regions.forEach { region ->
			val items = requestItems(
				operation = "areaBasedList2",
				params = mapOf(
					"numOfRows" to "10",
					"pageNo" to "1",
					"arrange" to "Q",
					"lDongRegnCd" to region.lDongRegnCd,
				) + region.lDongSignguCd?.let { mapOf("lDongSignguCd" to it) }.orEmpty(),
			)
			val imageUrl = items.firstNotNullOfOrNull { item ->
				item.string("firstimage") ?: item.string("firstimage2")
			}
			if (imageUrl != null) {
				return imageUrl
			}
		}
		return null
	}

	override fun findFestivals(
		startDate: LocalDate,
		endDate: LocalDate,
		limit: Int,
	): List<HomeTourApiFestival> {
		val festivals = requestItems(
			operation = "searchFestival2",
			params = mapOf(
				"numOfRows" to limit.coerceAtLeast(1).toString(),
				"pageNo" to "1",
				"arrange" to "Q",
				"eventStartDate" to startDate.format(DateTimeFormatter.BASIC_ISO_DATE),
				"eventEndDate" to endDate.format(DateTimeFormatter.BASIC_ISO_DATE),
			),
		).mapNotNull(Map<String, Any?>::toFestivalSeed)
			.filter { festival ->
				!festival.eventEndDate.isBefore(startDate) && !festival.eventStartDate.isAfter(endDate)
			}
			.sortedWith(compareBy<HomeTourApiFestivalSeed> { it.eventStartDate }.thenBy { it.title })
			.take(limit)

		val summaryFutures = festivals.associate { festival ->
			festival.contentId to CompletableFuture.supplyAsync { findFestivalSummary(festival.contentId) }
		}
		return festivals.map { festival ->
			HomeTourApiFestival(
				contentId = festival.contentId,
				title = festival.title,
				summary = summaryFutures.getValue(festival.contentId).join(),
				imageUrl = festival.imageUrl,
				address = festival.address,
				regionName = regionNameOf(festival.address),
				eventStartDate = festival.eventStartDate,
				eventEndDate = festival.eventEndDate,
			)
		}
	}

	override fun findAttractions(
		destinations: List<TripDestinationCode>,
		limitPerDestination: Int,
	): List<HomeTourApiAttraction> {
		val futures = destinations.associateWith { destination ->
			CompletableFuture.supplyAsync { findAttractionsOf(destination, limitPerDestination) }
		}
		return destinations.flatMap { futures.getValue(it).join() }
	}

	/**
	 * 도시 하나의 관광지. 한 도시 조회가 실패해도 섹션 전체를 비우지 않도록 빈 목록을 반환한다.
	 */
	private fun findAttractionsOf(
		destination: TripDestinationCode,
		limit: Int,
	): List<HomeTourApiAttraction> =
		try {
			DestinationTourApiPolicy.of(destination).regions
				.asSequence()
				.flatMap { region ->
					requestItems(
						operation = "areaBasedList2",
						params = mapOf(
							"numOfRows" to (limit * 3).coerceAtLeast(1).toString(),
							"pageNo" to "1",
							"arrange" to "Q",
							"contentTypeId" to ATTRACTION_CONTENT_TYPE_ID,
							"lDongRegnCd" to region.lDongRegnCd,
						) + region.lDongSignguCd?.let { mapOf("lDongSignguCd" to it) }.orEmpty(),
					).asSequence()
				}
				.mapNotNull { item -> item.toAttraction(destination) }
				.distinctBy(HomeTourApiAttraction::contentId)
				.take(limit)
				.toList()
		} catch (exception: InternalServerException) {
			logger.warn(
				"홈 추천 관광지 조회 실패: destination={}, reason={}",
				destination.value,
				exception.message,
			)
			emptyList()
		}

	private fun Map<String, Any?>.toAttraction(destination: TripDestinationCode): HomeTourApiAttraction? {
		val contentId = string("contentid")?.trimToNull() ?: return null
		val title = string("title")?.trimToNull() ?: return null
		// 이미지가 없는 관광지는 카드가 비어 보이므로 제외한다.
		val imageUrl = (string("firstimage") ?: string("firstimage2"))?.trimToNull() ?: return null
		val address = (string("addr1") ?: string("addr2"))?.trimToNull()
		return HomeTourApiAttraction(
			contentId = contentId,
			title = title,
			imageUrl = imageUrl,
			address = address,
			regionName = regionNameOf(address),
			destination = destination,
		)
	}

	private fun findFestivalSummary(contentId: String): String? =
		try {
			requestItems(
				operation = "detailCommon2",
				params = mapOf(
					"numOfRows" to "1",
					"pageNo" to "1",
					"contentId" to contentId,
				),
			).firstOrNull()
				?.string("overview")
				?.let(::plainTextSummary)
		} catch (_: InternalServerException) {
			null
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
			throw InternalServerException("국문 TourAPI 호출이 중단되었습니다.")
		} catch (_: Exception) {
			throw InternalServerException("국문 TourAPI 호출 중 오류가 발생했습니다.")
		}
		if (response.statusCode() !in 200..299) {
			throw InternalServerException("국문 TourAPI 호출에 실패했습니다.")
		}

		val parsedBody = parseJson(response.body())
		val responseMap = parsedBody.map("response")
			?: throw InternalServerException("국문 TourAPI 응답 형식이 올바르지 않습니다.")
		val header = responseMap.map("header")
		val resultCode = header?.string("resultCode")
		if (resultCode != null && resultCode != "0000") {
			throw InternalServerException("국문 TourAPI 응답이 실패했습니다: ${header.string("resultMsg") ?: resultCode}")
		}

		val body = responseMap.map("body") ?: return emptyList()
		val item = body.map("items")?.get("item") ?: return emptyList()
		return when (item) {
			is List<*> -> item.mapNotNull { (it as? Map<*, *>)?.asStringKeyMap() }
			is Map<*, *> -> listOf(item.asStringKeyMap())
			else -> emptyList()
		}
	}

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
		return "${tourApiProperties.generalBaseUri.trimEnd('/')}/$operation?$query"
	}

	private fun parseJson(body: String): Map<String, Any?> =
		try {
			JSONObjectUtils.parse(body).asStringKeyMap()
		} catch (_: Exception) {
			throw InternalServerException("국문 TourAPI 응답을 해석할 수 없습니다.")
		}
}

private data class HomeTourApiFestivalSeed(
	val contentId: String,
	val title: String,
	val imageUrl: String?,
	val address: String?,
	val eventStartDate: LocalDate,
	val eventEndDate: LocalDate,
)

private fun Map<String, Any?>.toFestivalSeed(): HomeTourApiFestivalSeed? {
	val contentId = string("contentid") ?: return null
	val title = string("title") ?: return null
	val eventStartDate = date("eventstartdate") ?: return null
	val eventEndDate = date("eventenddate") ?: eventStartDate
	return HomeTourApiFestivalSeed(
		contentId = contentId,
		title = title,
		imageUrl = string("firstimage") ?: string("firstimage2"),
		address = listOfNotNull(string("addr1"), string("addr2"))
			.joinToString(" ")
			.trimToNull(),
		eventStartDate = eventStartDate,
		eventEndDate = eventEndDate,
	)
}

private fun plainTextSummary(value: String): String =
	Jsoup.parse(value).text().trim().take(MAX_SUMMARY_LENGTH)

private fun regionNameOf(address: String?): String? {
	val parts = address?.trim()?.split(Regex("\\s+"))?.filter(String::isNotBlank).orEmpty()
	if (parts.isEmpty()) {
		return null
	}
	val region = if (parts.first().endsWith("도") && parts.size > 1) parts[1] else parts.first()
	return region.removeSuffix("특별자치시")
		.removeSuffix("광역시")
		.removeSuffix("특별시")
		.removeSuffix("시")
		.removeSuffix("군")
		.removeSuffix("구")
}

private fun String.encode(): String = URLEncoder.encode(this, StandardCharsets.UTF_8)

private fun Map<*, *>.asStringKeyMap(): Map<String, Any?> =
	entries.associate { (key, value) -> key.toString() to value }

private fun Map<String, Any?>.map(key: String): Map<String, Any?>? =
	(this[key] as? Map<*, *>)?.asStringKeyMap()

private fun Map<String, Any?>.string(key: String): String? =
	this[key]?.toString()?.trimToNull()

private fun Map<String, Any?>.date(key: String): LocalDate? =
	string(key)?.let { runCatching { LocalDate.parse(it, DateTimeFormatter.BASIC_ISO_DATE) }.getOrNull() }

private fun String?.trimToNull(): String? = this?.trim()?.takeIf(String::isNotEmpty)

private const val MAX_SUMMARY_LENGTH = 180

/** TourAPI 관광지 콘텐츠 타입. 숙박·행사 등을 섞지 않기 위해 고정한다. */
private const val ATTRACTION_CONTENT_TYPE_ID = "12"
