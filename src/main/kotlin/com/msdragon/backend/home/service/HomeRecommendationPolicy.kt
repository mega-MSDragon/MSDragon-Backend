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
			TripDestinationCode.BUSAN,
			TripDestinationCode.JEJU,
		),
		Month.FEBRUARY to listOf(
			TripDestinationCode.BUSAN,
			TripDestinationCode.JEJU,
			TripDestinationCode.YEOSU,
			TripDestinationCode.GYEONGJU,
			TripDestinationCode.SEOUL,
		),
		Month.MARCH to listOf(
			TripDestinationCode.GYEONGJU,
			TripDestinationCode.JEONJU,
			TripDestinationCode.DAEGU,
			TripDestinationCode.SEOUL,
			TripDestinationCode.JEJU,
		),
		Month.APRIL to listOf(
			TripDestinationCode.JEJU,
			TripDestinationCode.SUWON_YONGIN,
			TripDestinationCode.GYEONGJU,
			TripDestinationCode.JEONJU,
			TripDestinationCode.YEOSU,
		),
		Month.MAY to listOf(
			TripDestinationCode.GANGNEUNG_SOKCHO,
			TripDestinationCode.GYEONGJU,
			TripDestinationCode.BUSAN,
			TripDestinationCode.JEJU,
			TripDestinationCode.YEOSU,
		),
		Month.JUNE to listOf(
			TripDestinationCode.BUSAN,
			TripDestinationCode.YEOSU,
			TripDestinationCode.JEJU,
			TripDestinationCode.GANGNEUNG_SOKCHO,
			TripDestinationCode.TONGYEONG_GEOJE_NAMHAE,
		),
		Month.JULY to listOf(
			TripDestinationCode.GANGNEUNG_SOKCHO,
			TripDestinationCode.TONGYEONG_GEOJE_NAMHAE,
			TripDestinationCode.BUSAN,
			TripDestinationCode.YEOSU,
			TripDestinationCode.JEJU,
		),
		Month.AUGUST to listOf(
			TripDestinationCode.GANGNEUNG_SOKCHO,
			TripDestinationCode.JEJU,
			TripDestinationCode.INCHEON,
			TripDestinationCode.BUSAN,
			TripDestinationCode.TONGYEONG_GEOJE_NAMHAE,
		),
		Month.SEPTEMBER to listOf(
			TripDestinationCode.GYEONGJU,
			TripDestinationCode.JEONJU,
			TripDestinationCode.POHANG_ANDONG,
			TripDestinationCode.SEOUL,
			TripDestinationCode.SUWON_YONGIN,
		),
		Month.OCTOBER to listOf(
			TripDestinationCode.GYEONGJU,
			TripDestinationCode.SUWON_YONGIN,
			TripDestinationCode.DAEGU,
			TripDestinationCode.JEONJU,
			TripDestinationCode.POHANG_ANDONG,
		),
		Month.NOVEMBER to listOf(
			TripDestinationCode.JEJU,
			TripDestinationCode.YEOSU,
			TripDestinationCode.TONGYEONG_GEOJE_NAMHAE,
			TripDestinationCode.BUSAN,
			TripDestinationCode.GYEONGJU,
		),
		Month.DECEMBER to listOf(
			TripDestinationCode.SEOUL,
			TripDestinationCode.GANGNEUNG_SOKCHO,
			TripDestinationCode.BUSAN,
			TripDestinationCode.JEJU,
			TripDestinationCode.INCHEON,
		),
	)
}
