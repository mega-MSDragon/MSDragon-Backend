package com.msdragon.backend.home.service

import com.msdragon.backend.common.exception.InternalServerException
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
	fun `추천 여행과 축제는 서로의 TourAPI를 호출하지 않고 장애 시 축소 응답한다`() {
		var imageCalls = 0
		var festivalCalls = 0
		val service = HomeDiscoveryService(
			homeTourApiClient = object : HomeTourApiClient {
				override fun findDestinationImage(destination: TripDestinationCode): String? {
					imageCalls++
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
		)

		val recommendations = service.getMonthlyRecommendations(LocalDate.of(2026, 5, 1))

		assertEquals(5, recommendations.recommendationMonth)
		assertEquals(5, recommendations.recommendedCities.size)
		assertTrue(recommendations.recommendedCities.all { it.imageUrl == null })
		assertNull(recommendations.recommendedCities.first().imageUrl)
		assertEquals(5, imageCalls)
		assertEquals(0, festivalCalls)

		val festivals = service.getFestivals(LocalDate.of(2026, 5, 1))

		assertTrue(festivals.festivals.isEmpty())
		assertEquals(5, imageCalls)
		assertEquals(1, festivalCalls)
	}
}
