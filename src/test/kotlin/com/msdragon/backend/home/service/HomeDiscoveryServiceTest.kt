package com.msdragon.backend.home.service

import com.msdragon.backend.home.config.HomeProperties
import com.msdragon.backend.common.exception.InternalServerException
import com.msdragon.backend.home.tourapi.HomeTourApiAttraction
import com.msdragon.backend.home.tourapi.HomeTourApiClient
import com.msdragon.backend.home.tourapi.HomeTourApiFestival
import com.msdragon.backend.trip.entity.TripDestinationCode
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Month
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HomeDiscoveryServiceTest {
	@Test
	fun `모든 월은 중복 없는 추천 도시 5개를 제공한다`() {
		Month.entries.forEach { month ->
			val destinations = HomeRecommendationPolicy.destinationsFor(month)
			assertEquals(5, destinations.size)
			assertEquals(5, destinations.distinct().size)
		}
	}

	@Test
	fun `5월 추천 도시는 디자인 확정 순서를 따른다`() {
		assertEquals(
			listOf(
				TripDestinationCode.GANGNEUNG_SOKCHO,
				TripDestinationCode.GYEONGJU,
				TripDestinationCode.BUSAN,
				TripDestinationCode.JEJU,
				TripDestinationCode.YEOSU,
			),
			HomeRecommendationPolicy.destinationsFor(Month.MAY),
		)
	}

	@Test
	fun `추천 도시 이미지는 서버 리소스를 쓰고 축제 장애 시 축소 응답한다`() {
		var imageCalls = 0
		var festivalCalls = 0
		var attractionCalls = 0
		val service = HomeDiscoveryService(
			homeTourApiClient = object : HomeTourApiClient {
				override fun findDestinationImage(destination: TripDestinationCode): String? {
					imageCalls++
					throw InternalServerException("호출 실패")
				}

				override fun findAttractions(
					destinations: List<TripDestinationCode>,
					limitPerDestination: Int,
				): List<HomeTourApiAttraction> {
					attractionCalls++
					throw InternalServerException("호출 실패")
				}
				override fun findFestivals(
					startDate: LocalDate,
					endDate: LocalDate,
					limit: Int,
				): List<HomeTourApiFestival> {
					festivalCalls++
					throw InternalServerException("호출 실패")
				}
			},
			homeProperties = HomeProperties(baseUrl = "https://api.example.com"),
		)

		val recommendations = service.getMonthlyRecommendations(LocalDate.of(2026, 5, 1))

		assertEquals(5, recommendations.recommendationMonth)
		assertEquals(5, recommendations.recommendedCities.size)
		// 12개 도시 이미지를 서버에 두었으므로 TourAPI 도시 이미지 조회가 사라진다.
		assertEquals(0, imageCalls)
		recommendations.recommendedCities.forEach { city ->
			assertEquals(
				"https://api.example.com/images/destinations/${city.code.value}.png",
				city.imageUrl,
			)
		}

		val festivals = service.getFestivals(LocalDate.of(2026, 5, 1))

		assertTrue(festivals.festivals.isEmpty())
		assertEquals(0, imageCalls)
		assertEquals(1, festivalCalls)
	}
	@Test
	fun `섹션은 정책 순서를 유지하고 외부 장애 시 빈 항목으로 축소한다`() {
		val today = LocalDate.of(2026, 5, 1)
		var attractionCalls = 0
		val service = HomeDiscoveryService(
			homeTourApiClient = object : HomeTourApiClient {
				override fun findDestinationImage(destination: TripDestinationCode): String? = null

				override fun findAttractions(
					destinations: List<TripDestinationCode>,
					limitPerDestination: Int,
				): List<HomeTourApiAttraction> {
					attractionCalls++
					return destinations.take(2).map { destination ->
						HomeTourApiAttraction(
							contentId = "c-${destination.value}",
							title = "${destination.displayName} 명소",
							imageUrl = "https://example.com/${destination.value}.jpg",
							address = "${destination.displayName}시 어딘가",
							regionName = destination.displayName,
							destination = destination,
						)
					}
				}

				override fun findFestivals(
					startDate: LocalDate,
					endDate: LocalDate,
					limit: Int,
				): List<HomeTourApiFestival> = throw InternalServerException("호출 실패")
			},
			homeProperties = HomeProperties(),
		)

		val sections = service.getSections(today).sections

		// 정책이 정한 순서를 그대로 유지한다.
		assertEquals(2, sections.size)
		assertEquals("festivals", sections[0].key)
		assertEquals("monthly_attractions", sections[1].key)

		// 축제 조회가 실패해도 섹션 자체는 남고 항목만 빈다.
		assertTrue(sections[0].items.isEmpty())

		// 모든 섹션이 같은 카드로 그려지므로 항목 형태도 하나다.
		assertEquals(2, sections[1].items.size)
		val attraction = sections[1].items.first()
		// 상세로 이동할 때 섹션 종류를 몰라도 되도록 항목이 contentTypeId를 들고 있다.
		assertEquals("12", attraction.contentTypeId)
		assertEquals(attraction.regionName, attraction.caption)
		assertTrue(attraction.tags.contains("관광지"))
		// 부제는 이번 달 추천 도시 이름을 이어 붙인다.
		assertTrue(sections[1].subtitle?.contains("·") == true)

		// 같은 날짜면 캐시를 재사용해 외부 호출을 반복하지 않는다.
		service.getSections(today)
		assertEquals(1, attractionCalls)
	}
	@Test
	fun `축제 기간은 카드에 넣을 문장으로 내려간다`() {
		val today = LocalDate.of(2026, 5, 1)
		val service = HomeDiscoveryService(
			homeTourApiClient = object : HomeTourApiClient {
				override fun findDestinationImage(destination: TripDestinationCode): String? = null

				override fun findAttractions(
					destinations: List<TripDestinationCode>,
					limitPerDestination: Int,
				): List<HomeTourApiAttraction> = emptyList()

				override fun findFestivals(
					startDate: LocalDate,
					endDate: LocalDate,
					limit: Int,
				): List<HomeTourApiFestival> = listOf(
					HomeTourApiFestival(
						contentId = "f-1",
						title = "여러 날 축제",
						summary = null,
						imageUrl = null,
						address = "경상북도 안동시",
						regionName = "안동",
						eventStartDate = LocalDate.of(2026, 5, 3),
						eventEndDate = LocalDate.of(2026, 5, 20),
					),
					HomeTourApiFestival(
						contentId = "f-2",
						title = "하루 축제",
						summary = null,
						imageUrl = null,
						address = "부산광역시",
						regionName = "부산",
						eventStartDate = LocalDate.of(2026, 5, 10),
						eventEndDate = LocalDate.of(2026, 5, 10),
					),
				)
			},
			homeProperties = HomeProperties(),
		)

		val items = service.getSections(today).sections.first { it.key == "festivals" }.items

		// 카드가 하나뿐이라 기간도 카드에 들어갈 문장으로 서버가 만든다.
		assertEquals("2026.05.03 - 05.20", items[0].caption)
		// 하루짜리는 기간을 반복하지 않는다.
		assertEquals("2026.05.10", items[1].caption)
		// 상세 이동에 쓸 콘텐츠 타입을 항목이 들고 있다.
		assertEquals("15", items[0].contentTypeId)
	}
}
