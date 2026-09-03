package com.msdragon.backend.home.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.home")
data class HomeProperties(
	/**
	 * 추천 도시 이미지 URL을 만들 때 붙이는 서버 주소. 응답이 하루 단위로 캐시되므로
	 * 요청에서 호스트를 추론하지 않고 설정값을 사용한다.
	 */
	val baseUrl: String = "http://localhost:8080",
)
