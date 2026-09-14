package com.msdragon.backend.trip.tourapi

import com.msdragon.backend.common.exception.InternalServerException
import com.msdragon.backend.trip.config.TourApiProperties
import com.sun.net.httpserver.HttpServer
import org.junit.jupiter.api.Test
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class HttpTourApiClientTest {
	@Test
	fun `위치 기반 음식점 조회는 일반 관광 API에서 썸네일을 읽는다`() {
		val query = AtomicReference<String>()
		val server = HttpServer.create(InetSocketAddress(0), 0)
		server.createContext("/KorService2/locationBasedList2") { exchange ->
			query.set(exchange.requestURI.rawQuery)
			val body =
				"""
				{"response":{"header":{"resultCode":"0000","resultMsg":"OK"},"body":{"items":{"item":[{
				  "contentid":"cafe-1","contenttypeid":"39","title":"해운대 바다 카페",
				  "mapy":"35.159132","mapx":"129.161181",
				  "firstimage":"https://example.com/cafe.jpg",
				  "firstimage2":"https://example.com/cafe-thumb.jpg"
				}]}}}}
				""".trimIndent().toByteArray()
			exchange.responseHeaders.add("Content-Type", "application/json")
			exchange.sendResponseHeaders(200, body.size.toLong())
			exchange.responseBody.use { it.write(body) }
		}
		server.start()

		try {
			val client = HttpTourApiClient(
				TourApiProperties(
					baseUri = "http://localhost:${server.address.port}/WithTourService2",
					generalBaseUri = "http://localhost:${server.address.port}/KorService2",
					serviceKey = "test-key",
				),
			)

			val result = client.findNearbyPlaces(
				TourApiLocationSearch(
					latitude = "35.1587".toBigDecimal(),
					longitude = "129.1604".toBigDecimal(),
					radiusMeters = 5000,
					contentTypeId = "39",
				),
			)

			assertEquals("https://example.com/cafe-thumb.jpg", result.single().firstImageThumbnail)
			assertTrue(query.get().contains("contentTypeId=39"))
			assertTrue(query.get().contains("mapX=129.1604"))
			assertTrue(query.get().contains("mapY=35.1587"))
			assertTrue(query.get().contains("radius=5000"))
		} finally {
			server.stop(0)
		}
	}

	@Test
	fun `상세 조회는 일반 관광 API를 쓰고 무장애 정보만 무장애 API에서 읽는다`() {
		// 무장애 관광정보는 일반 관광정보의 부분집합이다. 홈 섹션에서 고른 콘텐츠를 무장애 쪽에
		// 물으면 빈 결과라 상세가 통째로 404가 됐다.
		val hit = mutableSetOf<String>()
		val server = HttpServer.create(InetSocketAddress(0), 0)
		fun respond(path: String, itemJson: String) {
			server.createContext(path) { exchange ->
				hit.add(path)
				val body =
					"""{"response":{"header":{"resultCode":"0000","resultMsg":"OK"},"body":{"items":{"item":[$itemJson]}}}}"""
						.toByteArray()
				exchange.responseHeaders.add("Content-Type", "application/json")
				exchange.sendResponseHeaders(200, body.size.toLong())
				exchange.responseBody.use { it.write(body) }
			}
		}
		respond("/KorService2/detailCommon2", """{"contentid":"2699287","contenttypeid":"15","overview":"축제 소개"}""")
		respond("/KorService2/detailIntro2", """{"contentid":"2699287","eventstartdate":"20260912"}""")
		respond("/KorService2/detailImage2", """{"originimgurl":"https://example.com/a.jpg"}""")
		respond("/WithTourService2/detailWithTour2", """{"contentid":"2699287","elevator":"있음"}""")
		// 무장애 서비스에 없는 콘텐츠를 흉내 낸다. 여기로 요청이 가면 빈 결과가 돼야 한다.
		respond("/WithTourService2/detailCommon2", "")
		server.start()

		try {
			val client = HttpTourApiClient(
				TourApiProperties(
					baseUri = "http://localhost:${server.address.port}/WithTourService2",
					generalBaseUri = "http://localhost:${server.address.port}/KorService2",
					serviceKey = "test-key",
				),
			)

			assertEquals("축제 소개", client.getPlaceDetail("2699287")?.overview)
			assertEquals(1, client.getPlaceImages("2699287").size)
			assertTrue(client.getPlaceIntro("2699287", "15") != null)
			assertTrue(client.getAccessibility("2699287") != null)

			assertTrue(hit.contains("/KorService2/detailCommon2"))
			assertTrue(hit.contains("/KorService2/detailIntro2"))
			assertTrue(hit.contains("/KorService2/detailImage2"))
			assertTrue(hit.contains("/WithTourService2/detailWithTour2"))
			assertFalse(hit.contains("/WithTourService2/detailCommon2"))
		} finally {
			server.stop(0)
		}
	}

	@Test
	fun `최상위 TourAPI 오류 응답의 작업명과 오류 코드를 전달한다`() {
		val server = HttpServer.create(InetSocketAddress(0), 0)
		server.createContext("/detailCommon2") { exchange ->
			val body =
				"""{"responseTime":"2026-08-20T16:31:05.967","resultCode":"22","resultMsg":"LIMITED_NUMBER_OF_SERVICE_REQUESTS_EXCEEDS_ERROR"}"""
					.toByteArray()
			exchange.responseHeaders.add("Content-Type", "application/json")
			exchange.sendResponseHeaders(200, body.size.toLong())
			exchange.responseBody.use { it.write(body) }
		}
		server.start()

		try {
			val client = HttpTourApiClient(
				TourApiProperties(
					baseUri = "http://localhost:${server.address.port}",
					generalBaseUri = "http://localhost:${server.address.port}",
					serviceKey = "test-key",
				),
			)
			val exception = assertFailsWith<InternalServerException> {
				client.getPlaceDetail("988449")
			}

			assertEquals(
				"TourAPI 응답이 실패했습니다: operation=detailCommon2, resultCode=22, resultMsg=LIMITED_NUMBER_OF_SERVICE_REQUESTS_EXCEEDS_ERROR",
				exception.message,
			)
		} finally {
			server.stop(0)
		}
	}

	@Test
	fun `이미지 상세 조회에 지원하지 않는 subImageYN을 보내지 않는다`() {
		val query = AtomicReference<String>()
		val server = HttpServer.create(InetSocketAddress(0), 0)
		server.createContext("/detailImage2") { exchange ->
			query.set(exchange.requestURI.rawQuery)
			val body =
				"""{"response":{"header":{"resultCode":"0000","resultMsg":"OK"},"body":{"items":"","numOfRows":0,"pageNo":1,"totalCount":0}}}"""
					.toByteArray()
			exchange.responseHeaders.add("Content-Type", "application/json")
			exchange.sendResponseHeaders(200, body.size.toLong())
			exchange.responseBody.use { it.write(body) }
		}
		server.start()

		try {
			val client = HttpTourApiClient(
				TourApiProperties(
					baseUri = "http://localhost:${server.address.port}",
					generalBaseUri = "http://localhost:${server.address.port}",
					serviceKey = "test-key",
				),
			)

			assertTrue(client.getPlaceImages("988449").isEmpty())
			assertTrue(query.get().contains("imageYN=Y"))
			assertFalse(query.get().contains("subImageYN"))
		} finally {
			server.stop(0)
		}
	}
}
