package com.msdragon.backend.trip.tmap

import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.LocalTime

data class TmapRouteStop(
	val stopId: Long,
	val name: String,
	val longitude: BigDecimal,
	val latitude: BigDecimal,
	val dwellSeconds: Int,
)

data class TmapRouteOptimizationRequest(
	val start: TmapRouteStop,
	val end: TmapRouteStop,
	val viaPoints: List<TmapRouteStop>,
	val startTime: LocalDateTime,
)

data class TmapRouteCoordinate(
	val longitude: BigDecimal,
	val latitude: BigDecimal,
)

data class TmapRouteOptimizationResult(
	val totalDistanceMeters: Int,
	val totalDurationSeconds: Int,
	val totalFare: Int?,
	val orderedStopIds: List<Long>,
	val arrivalTimes: Map<Long, LocalTime>,
	val polyline: List<TmapRouteCoordinate>,
	val rawProperties: Map<String, Any?>,
)
