package com.msdragon.backend.trip.tmap

interface TmapRouteClient {
	fun optimizeRoute(request: TmapRouteOptimizationRequest): TmapRouteOptimizationResult
}
