package com.msdragon.backend.trip.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration
import java.time.LocalTime

@ConfigurationProperties(prefix = "app.tmap")
data class TmapProperties(
	val baseUri: String = "https://apis.openapi.sk.com/tmap",
	val appKey: String = "",
	val searchOption: String = "0",
	val carType: String = "1",
	val defaultStartTime: LocalTime = LocalTime.of(10, 0),
	val connectTimeout: Duration = Duration.ofSeconds(5),
	val requestTimeout: Duration = Duration.ofSeconds(15),
)
