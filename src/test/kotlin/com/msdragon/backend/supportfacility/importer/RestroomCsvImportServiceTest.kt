package com.msdragon.backend.supportfacility.importer

import com.msdragon.backend.supportfacility.tmap.HttpTmapGeocodingClient
import com.msdragon.backend.supportfacility.repository.SupportFacilityRepository
import com.msdragon.backend.supportfacility.tmap.TmapGeocodingClient
import com.msdragon.backend.supportfacility.tmap.TmapGeocodingCoordinate
import com.msdragon.backend.trip.config.TmapProperties
import com.sun.net.httpserver.HttpServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import tools.jackson.databind.ObjectMapper
import java.net.InetSocketAddress
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@SpringBootTest
@Import(RestroomCsvImportServiceTest.Config::class)
class RestroomCsvImportServiceTest {
	@Autowired
	private lateinit var importService: RestroomCsvImportService

	@Autowired
	private lateinit var supportFacilityRepository: SupportFacilityRepository

	@Autowired
	private lateinit var fakeTmapGeocodingClient: FakeTmapGeocodingClient

	@Autowired
	private lateinit var objectMapper: ObjectMapper

	@TempDir
	lateinit var tempDirectory: Path

	@BeforeEach
	fun setUp() {
		supportFacilityRepository.deleteAll()
		fakeTmapGeocodingClient.addresses.clear()
	}

	@AfterEach
	fun tearDown() {
		supportFacilityRepository.deleteAll()
	}

	@Test
	fun `CP949 CSV를 적재하고 재실행 시 기존 시설을 건너뛴다`() {
		val csv = """
			개방자치단체코드,관리번호,화장실명,소재지도로명주소,소재지지번주소,전화번호,개방시간,개방시간상세
			3000000,restroom-1,사직단 공중화장실,서울특별시 종로구 사직동 1-28,,02-1234-5678,상시,
			3000000,restroom-2,창덕공원 화장실,,서울특별시 종로구 권농동 31,,정시,09:00-18:00
		""".trimIndent()
		val file = tempDirectory.resolve("공중화장실정보.csv")
		Files.writeString(file, csv, Charset.forName("MS949"))

		val firstResult = importService.importFrom(file)
		val secondResult = importService.importFrom(file)

		assertEquals(2, firstResult.importedCount)
		assertEquals(0, firstResult.existingCount)
		assertEquals(0, secondResult.importedCount)
		assertEquals(2, secondResult.existingCount)
		assertEquals(2, supportFacilityRepository.count())
		assertEquals(2, fakeTmapGeocodingClient.addresses.size)
		val facilities = supportFacilityRepository.findAll().sortedBy { it.sourceId }
		assertEquals("상시", facilities[0].operatingHours)
		assertEquals("정시 · 09:00-18:00", facilities[1].operatingHours)
		assertEquals("서울특별시 종로구 권농동 31", facilities[1].address)
		assertEquals(true, Files.exists(Path.of(firstResult.failureFile)))
	}

	@Test
	fun `Tmap 주소 좌표 응답에서 입구점 좌표를 우선 사용한다`() {
		val server = HttpServer.create(InetSocketAddress(0), 0)
		var requestedAppKey: String? = null
		var requestedQuery: String? = null
		server.createContext("/geo/fullAddrGeo") { exchange ->
			requestedAppKey = exchange.requestHeaders.getFirst("appKey")
			requestedQuery = exchange.requestURI.rawQuery
			val body = """
				{
				  "coordinateInfo": {
				    "coordinate": [
				      {
				        "newLat": "37.5758000",
				        "newLon": "126.9684000",
				        "newLatEntr": "37.5758692",
				        "newLonEntr": "126.9684817",
				        "unknownField": "ignored"
				      }
				    ]
				  }
				}
			""".trimIndent().toByteArray()
			exchange.sendResponseHeaders(200, body.size.toLong())
			exchange.responseBody.use { it.write(body) }
		}
		server.start()

		try {
			val client = HttpTmapGeocodingClient(
				tmapProperties = TmapProperties(
					baseUri = "http://localhost:${server.address.port}",
					appKey = "test-app-key",
					requestTimeout = Duration.ofSeconds(2),
				),
				objectMapper = objectMapper,
			)

			val coordinate = assertNotNull(client.geocode("서울특별시 종로구 사직동 1-28"))

			assertEquals("37.5758692".toBigDecimal(), coordinate.latitude)
			assertEquals("126.9684817".toBigDecimal(), coordinate.longitude)
			assertEquals("test-app-key", requestedAppKey)
			assertTrue(requireNotNull(requestedQuery).contains("addressFlag=F00"))
			assertTrue(requireNotNull(requestedQuery).contains("fullAddr="))
		} finally {
			server.stop(0)
		}
	}

	@TestConfiguration
	class Config {
		@Bean
		@Primary
		fun fakeTmapGeocodingClient(): FakeTmapGeocodingClient = FakeTmapGeocodingClient()
	}

	class FakeTmapGeocodingClient : TmapGeocodingClient {
		val addresses = mutableListOf<String>()

		override fun geocode(address: String): TmapGeocodingCoordinate {
			addresses += address
			return TmapGeocodingCoordinate(
				latitude = "37.5758692".toBigDecimal(),
				longitude = "126.9684817".toBigDecimal(),
			)
		}
	}
}
