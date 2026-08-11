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
	fun `5월 추천 도시는 디자인 확정 순서를 따른다`() {
		assertEquals(
			listOf(
				TripDestinationCode.GANGNEUNG_SOKCHO,
				TripDestinationCode.GYEONGJU,
				TripDestinationCode.BUSAN,
			),
			HomeRecommendationPolicy.destinationsFor(Month.MAY),
		)
	}

	@Test
	fun `TourAPI 호출에 실패해도 도시와 빈 축제 목록을 반환한다`() {
		val service = HomeDiscoveryService(
			homeTourApiClient = object : HomeTourApiClient {
				override fun findDestinationImage(destination: TripDestinationCode): String? =
					throw InternalServerException("호출 실패")

				override fun findFestivals(
					startDate: LocalDate,
					endDate: LocalDate,
					limit: Int,
				): List<HomeTourApiFestival> = throw InternalServerException("호출 실패")
			},
		)

		val discovery = service.getDiscovery(LocalDate.of(2026, 5, 1))

		assertEquals(5, discovery.recommendationMonth)
		assertEquals(3, discovery.recommendedCities.size)
		assertTrue(discovery.recommendedCities.all { it.imageUrl == null })
		assertTrue(discovery.festivals.isEmpty())
		assertNull(discovery.recommendedCities.first().imageUrl)
	}
}
