package com.msdragon.backend.home.service

import com.msdragon.backend.common.exception.InternalServerException
import com.msdragon.backend.home.dto.HomeFestivalResponse
import com.msdragon.backend.home.dto.HomeFestivalsResponse
import com.msdragon.backend.home.dto.HomeMonthlyRecommendationsResponse
import com.msdragon.backend.home.dto.HomeRecommendedCityResponse
import com.msdragon.backend.home.config.HomeProperties
import com.msdragon.backend.home.dto.HomeSectionItemResponse
import com.msdragon.backend.home.dto.HomeSectionResponse
import com.msdragon.backend.home.dto.HomeSectionsResponse
import com.msdragon.backend.home.tourapi.HomeTourApiAttraction
import com.msdragon.backend.home.tourapi.HomeTourApiClient
import com.msdragon.backend.trip.entity.TripDestinationCode
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.CompletableFuture

@Service
class HomeDiscoveryService(
	private val homeTourApiClient: HomeTourApiClient,
	private val homeProperties: HomeProperties,
) {
	private val logger = LoggerFactory.getLogger(javaClass)
	private val monthlyRecommendationsCacheLock = Any()
	private val festivalsCacheLock = Any()
	private val sectionsCacheLock = Any()

	@Volatile
	private var monthlyRecommendationsCache: HomeMonthlyRecommendationsCache? = null

	@Volatile
	private var festivalsCache: HomeFestivalsCache? = null

	@Volatile
	private var sectionsCache: HomeSectionsCache? = null

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

	/**
	 * 홈 축제 영역부터 아래까지의 동적 섹션. 구성과 순서는 [HomeSectionPolicy]가 정한다.
	 * 축제는 기존 축제 캐시를 그대로 재사용해 TourAPI를 두 번 호출하지 않는다.
	 */
	fun getSections(today: LocalDate): HomeSectionsResponse {
		sectionsCache?.takeIf { it.cachedDate == today }?.let { return it.response }
		return synchronized(sectionsCacheLock) {
			sectionsCache?.takeIf { it.cachedDate == today }?.response
				?: refreshSections(today).also {
					sectionsCache = HomeSectionsCache(today, it)
				}
		}
	}

	private fun refreshSections(today: LocalDate): HomeSectionsResponse {
		val attractions = loadAttractions(today)
		return HomeSectionsResponse(
			sections = HomeSectionPolicy.sections.map { definition ->
				when (definition.source) {
					HomeSectionSource.FESTIVAL -> section(definition, festivalItems(today), null)
					HomeSectionSource.MONTHLY_ATTRACTION -> section(
						definition,
						attractions.map(HomeTourApiAttraction::toSectionItem),
						attractionSubtitle(today),
					)
				}
			},
		)
	}

	private fun section(
		definition: HomeSectionDefinition,
		items: List<HomeSectionItemResponse>,
		subtitleOverride: String?,
	): HomeSectionResponse =
		HomeSectionResponse(
			key = definition.key,
			title = definition.title,
			subtitle = subtitleOverride ?: definition.subtitle,
			items = items,
		)

	/** 축제 섹션은 `/festivals`와 같은 데이터를 쓴다. 같은 날짜면 캐시를 공유한다. */
	private fun festivalItems(today: LocalDate): List<HomeSectionItemResponse> =
		getFestivals(today).festivals.map { festival ->
			HomeSectionItemResponse(
				contentId = festival.contentId,
				contentTypeId = FESTIVAL_CONTENT_TYPE_ID,
				title = festival.title,
				caption = festivalPeriodCaption(festival.eventStartDate, festival.eventEndDate),
				summary = festival.summary,
				imageUrl = festival.imageUrl,
				address = festival.address,
				regionName = festival.regionName,
				tags = festival.tags,
			)
		}

	private fun loadAttractions(today: LocalDate): List<HomeTourApiAttraction> =
		try {
			homeTourApiClient.findAttractions(
				destinations = HomeRecommendationPolicy.destinationsFor(today.month),
				limitPerDestination = HomeSectionPolicy.ATTRACTIONS_PER_DESTINATION,
			)
		} catch (exception: InternalServerException) {
			logger.warn("홈 추천 관광지 조회 실패: reason={}", exception.message)
			// 직전 캐시의 관광지 섹션을 유지한다. 없으면 빈 섹션으로 축소한다.
			emptyList()
		}

	/** 카드가 하나뿐이라 축제 기간도 카드에 들어갈 문장으로 서버가 만든다. */
	private fun festivalPeriodCaption(startDate: LocalDate, endDate: LocalDate): String =
		if (startDate == endDate) {
			startDate.format(FESTIVAL_CAPTION_FULL)
		} else {
			"${startDate.format(FESTIVAL_CAPTION_FULL)} - ${endDate.format(FESTIVAL_CAPTION_SHORT)}"
		}

	/** 관광지 섹션 부제는 이번 달 추천 도시 이름을 이어 붙인다. */
	private fun attractionSubtitle(today: LocalDate): String =
		HomeRecommendationPolicy.destinationsFor(today.month)
			.joinToString(" · ") { it.displayName }

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
		private const val FESTIVAL_CONTENT_TYPE_ID = "15"
		private val FESTIVAL_CAPTION_FULL: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
		private val FESTIVAL_CAPTION_SHORT: DateTimeFormatter = DateTimeFormatter.ofPattern("MM.dd")
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

/** 관광지는 행사 날짜가 없으므로 날짜 필드를 null로 둔다. */
private fun HomeTourApiAttraction.toSectionItem(): HomeSectionItemResponse =
	HomeSectionItemResponse(
		contentId = contentId,
		contentTypeId = ATTRACTION_CONTENT_TYPE_ID,
		title = title,
		// 축제는 기간이 들어가는 자리에 관광지는 지역을 넣는다. 카드 형태가 같아야 한다.
		caption = regionName,
		summary = null,
		imageUrl = imageUrl,
		address = address,
		regionName = regionName,
		tags = listOfNotNull(regionName, HomeSectionPolicy.ATTRACTION_TAG),
	)

private const val ATTRACTION_CONTENT_TYPE_ID = "12"

private data class HomeSectionsCache(
	val cachedDate: LocalDate,
	val response: HomeSectionsResponse,
)
