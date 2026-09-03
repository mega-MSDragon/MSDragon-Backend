package com.msdragon.backend.home.service

import com.msdragon.backend.common.exception.InternalServerException
import com.msdragon.backend.home.dto.HomeFestivalResponse
import com.msdragon.backend.home.dto.HomeFestivalsResponse
import com.msdragon.backend.home.dto.HomeMonthlyRecommendationsResponse
import com.msdragon.backend.home.dto.HomeRecommendedCityResponse
import com.msdragon.backend.home.config.HomeProperties
import com.msdragon.backend.home.tourapi.HomeTourApiClient
import com.msdragon.backend.trip.entity.TripDestinationCode
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.concurrent.CompletableFuture

@Service
class HomeDiscoveryService(
	private val homeTourApiClient: HomeTourApiClient,
	private val homeProperties: HomeProperties,
) {
	private val logger = LoggerFactory.getLogger(javaClass)
	private val monthlyRecommendationsCacheLock = Any()
	private val festivalsCacheLock = Any()

	@Volatile
	private var monthlyRecommendationsCache: HomeMonthlyRecommendationsCache? = null

	@Volatile
	private var festivalsCache: HomeFestivalsCache? = null

	fun getMonthlyRecommendations(today: LocalDate): HomeMonthlyRecommendationsResponse {
		monthlyRecommendationsCache?.takeIf { it.cachedDate == today }?.let { return it.response }
		return synchronized(monthlyRecommendationsCacheLock) {
			monthlyRecommendationsCache?.takeIf { it.cachedDate == today }?.response
				?: refreshMonthlyRecommendations(today).also {
					monthlyRecommendationsCache = HomeMonthlyRecommendationsCache(today, it)
				}
		}
	}

	fun getFestivals(today: LocalDate): HomeFestivalsResponse {
		festivalsCache?.takeIf { it.cachedDate == today }?.let { return it.response }
		return synchronized(festivalsCacheLock) {
			festivalsCache?.takeIf { it.cachedDate == today }?.response
				?: refreshFestivals(today).also {
					festivalsCache = HomeFestivalsCache(today, it)
				}
		}
	}

	/**
	 * 서버에 넣어둔 도시 이미지 URL. 파일이 없으면 null을 반환해 TourAPI 조회로 넘긴다.
	 * 이미지를 아직 넣지 않은 도시가 있어도 기존 동작을 유지하므로 12장을 한 번에 채우지 않아도 된다.
	 *
	 * 클래스패스 리소스는 실행 중 바뀌지 않으므로 첫 조회 결과를 재사용한다.
	 */
	private val destinationImageUrls: Map<TripDestinationCode, String> by lazy {
		TripDestinationCode.entries.mapNotNull { destination ->
			val path = "$DESTINATION_IMAGE_CLASSPATH/${destination.value}.png"
			ClassPathResource(path)
				.takeIf(ClassPathResource::exists)
				?.let { destination to "${homeProperties.baseUrl.trimEnd('/')}/$DESTINATION_IMAGE_URL_PATH/${destination.value}.png" }
		}.toMap()
	}

	private fun refreshMonthlyRecommendations(today: LocalDate): HomeMonthlyRecommendationsResponse {
		val previousImages = monthlyRecommendationsCache?.response?.recommendedCities.orEmpty()
			.associate { it.code to it.imageUrl }
		val destinations = HomeRecommendationPolicy.destinationsFor(today.month)
		val cityFutures = destinations.associateWith { destination ->
			CompletableFuture.supplyAsync { loadCity(destination, previousImages[destination]) }
		}
		val recommendedCities = destinations.map { cityFutures.getValue(it).join() }

		return HomeMonthlyRecommendationsResponse(
			recommendationMonth = today.monthValue,
			recommendedCities = recommendedCities,
		)
	}

	private fun refreshFestivals(today: LocalDate): HomeFestivalsResponse =
		HomeFestivalsResponse(
			festivals = loadFestivals(today, festivalsCache?.response?.festivals.orEmpty()),
		)

	private fun loadCity(
		destination: TripDestinationCode,
		previousImageUrl: String?,
	): HomeRecommendedCityResponse {
		val imageUrl = destinationImageUrls[destination]
			?: try {
				homeTourApiClient.findDestinationImage(destination)
			} catch (exception: InternalServerException) {
				logger.warn("홈 추천 도시 이미지 조회 실패: destination={}, reason={}", destination.value, exception.message)
				previousImageUrl
			}
		return HomeRecommendedCityResponse(
			code = destination,
			displayName = destination.displayName,
			imageUrl = imageUrl,
		)
	}

	private fun loadFestivals(
		today: LocalDate,
		previousFestivals: List<HomeFestivalResponse>,
	): List<HomeFestivalResponse> =
		try {
			homeTourApiClient.findFestivals(
				startDate = today,
				endDate = today.plusDays(FESTIVAL_LOOKAHEAD_DAYS),
				limit = FESTIVAL_LIMIT,
			).map { festival ->
				HomeFestivalResponse(
					contentId = festival.contentId,
					title = festival.title,
					summary = festival.summary,
					imageUrl = festival.imageUrl,
					address = festival.address,
					regionName = festival.regionName,
					eventStartDate = festival.eventStartDate,
					eventEndDate = festival.eventEndDate,
					tags = listOfNotNull(festival.regionName, FESTIVAL_TAG).distinct(),
				)
			}
		} catch (exception: InternalServerException) {
			logger.warn("홈 추천 축제 조회 실패: reason={}", exception.message)
			previousFestivals
		}

	companion object {
		/** 도시 이미지 리소스 위치. `static` 하위라 인증 없이 URL로 바로 제공된다. */
		private const val DESTINATION_IMAGE_CLASSPATH = "static/images/destinations"
		private const val DESTINATION_IMAGE_URL_PATH = "images/destinations"
		private const val FESTIVAL_LOOKAHEAD_DAYS = 30L
		private const val FESTIVAL_LIMIT = 10
		private const val FESTIVAL_TAG = "축제"
	}
}

private data class HomeMonthlyRecommendationsCache(
	val cachedDate: LocalDate,
	val response: HomeMonthlyRecommendationsResponse,
)

private data class HomeFestivalsCache(
	val cachedDate: LocalDate,
	val response: HomeFestivalsResponse,
)
