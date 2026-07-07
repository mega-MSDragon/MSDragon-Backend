package com.msdragon.backend.trip.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.tour-api")
data class TourApiProperties(
	val baseUri: String = "https://apis.data.go.kr/B551011/KorWithService2",
	val serviceKey: String = "",
	val mobileOs: String = "ETC",
	val mobileApp: String = "MSDragon",
)
