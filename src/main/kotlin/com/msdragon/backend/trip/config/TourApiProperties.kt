package com.msdragon.backend.trip.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "app.tour-api")
data class TourApiProperties(
	val baseUri: String = "https://apis.data.go.kr/B551011/KorWithService2",
	val serviceKey: String = "",
	val mobileOs: String = "ETC",
	val mobileApp: String = "MSDragon",
	val connectTimeout: Duration = Duration.ofSeconds(5),
	val requestTimeout: Duration = Duration.ofSeconds(10),
)
