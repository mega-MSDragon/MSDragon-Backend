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
