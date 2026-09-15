package com.msdragon.backend.supportfacility.service

import com.msdragon.backend.auth.entity.UserRole
import com.msdragon.backend.auth.entity.DevicePlatform
import com.msdragon.backend.auth.support.AuthenticatedUser
import com.msdragon.backend.common.exception.BadRequestException
import com.msdragon.backend.supportfacility.entity.LocationUsageLog
import com.msdragon.backend.supportfacility.entity.SupportFacilityType
import com.msdragon.backend.supportfacility.repository.LocationUsageLogRepository
import com.msdragon.backend.supportfacility.repository.SupportFacilityRepository
import com.msdragon.backend.supportfacility.tmap.TmapPoiClient
import com.msdragon.backend.trip.service.TripService
import com.msdragon.backend.trip.tourapi.TourApiClient
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class LocationUsageTest {
	private val logs = mutableListOf<LocationUsageLog>()
	private val repository = object : LocationUsageLogRepository {
		override fun save(log: LocationUsageLog): LocationUsageLog = log.also { logs.add(it) }
	}
	private val tripService = mock(TripService::class.java)
	private val tmap = mock(TmapPoiClient::class.java)
	private val tourApi = mock(TourApiClient::class.java)
	private val facilities = mock(SupportFacilityRepository::class.java)
	private val recorder = LocationUsageRecorder(repository)
	private val service = SupportFacilityService(facilities, tmap, tourApi, tripService, recorder)
	private val user = AuthenticatedUser(7, UserRole.CHILD, DevicePlatform.ANDROID)

	@Test
	fun `화장실은 이용만 기록하고 카페는 두 외부 요청 시도를 구분한다`() {
		service.getNearbyRestrooms(user, 9, 37.0, 127.0)
		assertEquals(listOf("USE"), logs.map { it.eventType })
		logs.clear()
		service.getNearbyCafes(user, 9, 37.0, 127.0)
		assertEquals(listOf("USE", "EXTERNAL_REQUEST_ATTEMPT", "EXTERNAL_REQUEST_ATTEMPT"), logs.map { it.eventType })
		assertEquals(listOf(null, "TMAP", "TOUR_API"), logs.map { it.externalRecipient })
		assertTrue(logs.all { it.userId == 7L && it.tripId == 9L && it.acquisitionSource == "GOOGLE" })
		assertTrue(LocationUsageLog::class.java.declaredFields.none { it.name in listOf("latitude", "longitude", "token", "request") })
	}

	@Test
	fun `같은 이용자도 요청 토큰의 플랫폼별로 취득 경로를 구분한다`() {
		for (platform in listOf(DevicePlatform.IOS, DevicePlatform.ANDROID, DevicePlatform.WEB, null)) {
			service.getNearbyCafes(user.copy(platform = platform), 9, 37.0, 127.0)
		}
		assertEquals(listOf("APPLE", "GOOGLE", "UNKNOWN", "UNKNOWN"), logs.filter { it.eventType == "USE" }.map { it.acquisitionSource })
		assertEquals(listOf("APPLE", "GOOGLE", "UNKNOWN", "UNKNOWN"), logs.filter { it.externalRecipient == "TOUR_API" }.map { it.acquisitionSource })
	}

	@Test
	fun `병원과 약국을 각각 기록한다`() {
		for (type in listOf(SupportFacilityType.HOSPITAL, SupportFacilityType.PHARMACY)) {
			service.getNearbyMedicalFacilities(user, 9, 37.0, 127.0, type)
		}
		assertEquals(listOf("nearby_hospital", "nearby_hospital", "nearby_pharmacy", "nearby_pharmacy"), logs.map { it.serviceName })
	}

	@Test
	fun `좌표나 접근권한 검증 실패는 이용으로 기록하지 않는다`() {
		assertFailsWith<BadRequestException> { service.getNearbyCafes(user, 9, Double.NaN, 127.0) }
		doThrow(BadRequestException("denied")).`when`(tripService).validateTravelModeAccess(user, 9)
		assertFailsWith<BadRequestException> { service.getNearbyCafes(user, 9, 37.0, 127.0) }
		assertTrue(logs.isEmpty())
		verifyNoInteractions(tmap, tourApi)
	}

	@Test
	fun `Tmap 실패 시 TourAPI 요청 기록은 남기지 않는다`() {
		`when`(tmap.findNearbyCafes(BigDecimal.valueOf(37.0), BigDecimal.valueOf(127.0), 5, 10))
			.thenThrow(IllegalStateException("upstream failed"))
		assertFailsWith<IllegalStateException> { service.getNearbyCafes(user, 9, 37.0, 127.0) }
		assertEquals(listOf(null, "TMAP"), logs.map { it.externalRecipient })
		verifyNoInteractions(tourApi)
	}

	@Test
	fun `기록 저장 실패 시 외부 API를 호출하지 않는다`() {
		val broken = LocationUsageRecorder(object : LocationUsageLogRepository {
			override fun save(log: LocationUsageLog): LocationUsageLog = error("DB unavailable")
		})
		val target = SupportFacilityService(facilities, tmap, tourApi, tripService, broken)
		assertFailsWith<IllegalStateException> { target.getNearbyCafes(user, 9, 37.0, 127.0) }
		verifyNoInteractions(tmap, tourApi)
	}
}
