package com.msdragon.backend.home.service

import com.msdragon.backend.trip.entity.TripDestinationCode
import java.time.Month

object HomeRecommendationPolicy {
	fun destinationsFor(month: Month): List<TripDestinationCode> = monthlyDestinations.getValue(month)

	private val monthlyDestinations: Map<Month, List<TripDestinationCode>> = mapOf(
		Month.JANUARY to listOf(
			TripDestinationCode.GANGNEUNG_SOKCHO,
			TripDestinationCode.INCHEON,
			TripDestinationCode.SEOUL,
		),
		Month.FEBRUARY to listOf(
			TripDestinationCode.BUSAN,
			TripDestinationCode.JEJU,
			TripDestinationCode.YEOSU,
		),
		Month.MARCH to listOf(
			TripDestinationCode.GYEONGJU,
			TripDestinationCode.JEONJU,
			TripDestinationCode.DAEGU,
		),
		Month.APRIL to listOf(
			TripDestinationCode.JEJU,
			TripDestinationCode.SUWON_YONGIN,
			TripDestinationCode.GYEONGJU,
		),
		Month.MAY to listOf(
			TripDestinationCode.GANGNEUNG_SOKCHO,
			TripDestinationCode.GYEONGJU,
			TripDestinationCode.BUSAN,
		),
		Month.JUNE to listOf(
			TripDestinationCode.BUSAN,
			TripDestinationCode.YEOSU,
			TripDestinationCode.JEJU,
		),
		Month.JULY to listOf(
			TripDestinationCode.GANGNEUNG_SOKCHO,
			TripDestinationCode.TONGYEONG_GEOJE_NAMHAE,
			TripDestinationCode.BUSAN,
		),
		Month.AUGUST to listOf(
			TripDestinationCode.GANGNEUNG_SOKCHO,
			TripDestinationCode.JEJU,
			TripDestinationCode.INCHEON,
		),
		Month.SEPTEMBER to listOf(
			TripDestinationCode.GYEONGJU,
			TripDestinationCode.JEONJU,
			TripDestinationCode.POHANG_ANDONG,
		),
		Month.OCTOBER to listOf(
			TripDestinationCode.GYEONGJU,
			TripDestinationCode.SUWON_YONGIN,
			TripDestinationCode.DAEGU,
		),
		Month.NOVEMBER to listOf(
			TripDestinationCode.JEJU,
			TripDestinationCode.YEOSU,
			TripDestinationCode.TONGYEONG_GEOJE_NAMHAE,
		),
		Month.DECEMBER to listOf(
			TripDestinationCode.SEOUL,
			TripDestinationCode.GANGNEUNG_SOKCHO,
			TripDestinationCode.BUSAN,
		),
	)
}
