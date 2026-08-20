package com.msdragon.backend.supportfacility.tmap

import com.msdragon.backend.supportfacility.entity.SupportFacilityType
import com.msdragon.backend.trip.config.TmapProperties
import com.sun.net.httpserver.HttpServer
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import tools.jackson.databind.ObjectMapper
import java.net.InetSocketAddress
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest
class TmapPoiClientTest {
	@Autowired
	private lateinit var objectMapper: ObjectMapper

	@Test
	fun `Tmap 주변 카테고리 검색 응답을 의료시설로 변환한다`() {
		var rawQuery = ""
		val server = HttpServer.create(InetSocketAddress(0), 0)
		server.createContext("/tmap/pois/search/around") { exchange ->
			rawQuery = exchange.requestURI.rawQuery
			val body =
				"""
				{
				  "searchPoiInfo": {
				    "totalCount": "1",
				    "pois": {
				      "poi": [{
				        "id": "12345678",
				        "name": "서울대학교병원",
				        "telNo": "02-2072-2114",
				        "frontLat": "37.579617",
				        "frontLon": "126.998998",
				        "noorLat": "37.580000",
				        "noorLon": "127.000000",
				        "upperAddrName": "서울",
				        "middleAddrName": "종로구",
				        "roadName": "대학로",
				        "buildingNo1": "101",
				        "buildingNo2": "0"
				      }]
				    }
				  }
				}
				""".trimIndent().toByteArray()
			exchange.responseHeaders.add("Content-Type", "application/json")
			exchange.sendResponseHeaders(200, body.size.toLong())
			exchange.responseBody.use { it.write(body) }
		}
		server.start()

		try {
			val client = HttpTmapPoiClient(
				TmapProperties(
					baseUri = "http://localhost:${server.address.port}/tmap",
					appKey = "test-key",
				),
				objectMapper,
			)

			val result = client.findNearby(
				facilityType = SupportFacilityType.HOSPITAL,
				latitude = "37.5758692".toBigDecimal(),
				longitude = "126.9684817".toBigDecimal(),
				radiusKilometers = 5,
				limit = 10,
			)

			assertEquals(1, result.size)
			assertEquals("12345678", result[0].id)
			assertEquals("서울 종로구 대학로 101", result[0].address)
			assertEquals("37.579617".toBigDecimal(), result[0].latitude)
			assertTrue(rawQuery.contains("categories=%EB%B3%91%EC%9B%90"))
			assertTrue(rawQuery.contains("radius=5"))
			assertTrue(rawQuery.contains("count=10"))
			assertTrue(rawQuery.contains("sort=distance"))
			assertTrue(rawQuery.contains("appKey=test-key"))

			client.findNearbyCafes(
				latitude = "37.5758692".toBigDecimal(),
				longitude = "126.9684817".toBigDecimal(),
				radiusKilometers = 5,
				limit = 10,
			)
			assertTrue(rawQuery.contains("categories=%EC%B9%B4%ED%8E%98"))
		} finally {
			server.stop(0)
		}
	}
}
