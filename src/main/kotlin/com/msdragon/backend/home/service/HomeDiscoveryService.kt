package com.msdragon.backend.home.service

import com.msdragon.backend.common.exception.InternalServerException
import com.msdragon.backend.home.dto.HomeFestivalResponse
import com.msdragon.backend.home.dto.HomeFestivalsResponse
import com.msdragon.backend.home.dto.HomeMonthlyRecommendationsResponse
import com.msdragon.backend.home.dto.HomeRecommendedCityResponse
import com.msdragon.backend.home.tourapi.HomeTourApiClient
import com.msdragon.backend.trip.entity.TripDestinationCode
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.concurrent.CompletableFuture

@Service
class HomeDiscoveryService(
	private val homeTourApiClient: HomeTourApiClient,
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
		val imageUrl = try {
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
