package com.msdragon.backend.home.tourapi

import com.msdragon.backend.trip.config.TourApiProperties
import com.msdragon.backend.trip.entity.TripDestinationCode
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import org.junit.jupiter.api.Test
import java.net.InetSocketAddress
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HttpHomeTourApiClientTest {
	@Test
	fun `국문 TourAPI에서 도시 이미지와 축제를 조회한다`() {
		val requestedUris = mutableListOf<String>()
		val server = HttpServer.create(InetSocketAddress(0), 0)
		server.createContext("/KorService2") { exchange ->
			requestedUris += exchange.requestURI.toString()
			val response = when (exchange.requestURI.path.substringAfterLast('/')) {
				"areaBasedList2" -> itemsResponse(
					"""{"contentid":"1","firstimage":"https://example.com/gyeongju.jpg"}""",
				)
				"searchFestival2" -> itemsResponse(
					"""{"contentid":"250119","title":"안동 선유줄불놀이","addr1":"경상북도 안동시 풍천면","firstimage":"https://example.com/festival.jpg","eventstartdate":"20260801","eventenddate":"20260831"}""",
				)
				"detailCommon2" -> itemsResponse(
					"""{"contentid":"250119","overview":"<p>낙동강 위로 <b>불꽃이 이어지는</b> 축제입니다.</p>"}""",
				)
				else -> error("지원하지 않는 테스트 경로입니다: ${exchange.requestURI.path}")
			}
			exchange.respond(response)
		}
		server.start()

		try {
			val client = HttpHomeTourApiClient(
				TourApiProperties(
					generalBaseUri = "http://localhost:${server.address.port}/KorService2",
					serviceKey = "test-service-key",
				),
			)

			val imageUrl = client.findDestinationImage(TripDestinationCode.GYEONGJU)
			val festivals = client.findFestivals(
				startDate = LocalDate.of(2026, 8, 1),
				endDate = LocalDate.of(2026, 8, 31),
				limit = 10,
			)

			assertEquals("https://example.com/gyeongju.jpg", imageUrl)
			assertEquals(1, festivals.size)
			assertEquals("안동 선유줄불놀이", festivals.single().title)
			assertEquals("낙동강 위로 불꽃이 이어지는 축제입니다.", festivals.single().summary)
			assertEquals("안동", festivals.single().regionName)
			assertTrue(requestedUris.any { it.contains("/areaBasedList2?") && it.contains("lDongRegnCd=47") })
			assertTrue(requestedUris.any { it.contains("/searchFestival2?") && it.contains("eventStartDate=20260801") })
			assertTrue(requestedUris.any { it.contains("/detailCommon2?") && it.contains("contentId=250119") })
		} finally {
			server.stop(0)
		}
	}

	private fun itemsResponse(item: String): String =
		"""
		{
		  "response": {
		    "header": {"resultCode":"0000","resultMsg":"OK"},
		    "body": {"items":{"item":[$item]},"numOfRows":10,"pageNo":1,"totalCount":1}
		  }
		}
		""".trimIndent()

	private fun HttpExchange.respond(response: String) {
		val bytes = response.toByteArray()
		responseHeaders.add("Content-Type", "application/json")
		sendResponseHeaders(200, bytes.size.toLong())
		responseBody.use { it.write(bytes) }
	}
}
