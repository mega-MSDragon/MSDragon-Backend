package com.msdragon.backend.trip.support

import com.msdragon.backend.home.config.HomeProperties
import com.msdragon.backend.trip.entity.TripDestinationCode
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component

/**
 * 서버에 넣어둔 도시 대표 이미지 URL. 홈 추천 도시와 기록 썸네일이 같은 이미지를 쓴다.
 *
 * 파일이 없는 도시는 null을 반환하므로 호출부가 기존 대체 이미지로 넘어갈 수 있다.
 * 클래스패스 리소스는 실행 중 바뀌지 않으므로 첫 조회 결과를 재사용한다.
 */
@Component
class DestinationImageResolver(
	private val homeProperties: HomeProperties,
) {
	private val imageUrls: Map<TripDestinationCode, String> by lazy {
		TripDestinationCode.entries.mapNotNull { destination ->
			ClassPathResource("$IMAGE_CLASSPATH/${destination.value}.png")
				.takeIf(ClassPathResource::exists)
				?.let {
					destination to "${homeProperties.baseUrl.trimEnd('/')}/$IMAGE_URL_PATH/${destination.value}.png"
				}
		}.toMap()
	}

	fun imageUrlOf(destination: TripDestinationCode): String? = imageUrls[destination]

	companion object {
		private const val IMAGE_CLASSPATH = "static/images/destinations"
		private const val IMAGE_URL_PATH = "images/destinations"
	}
}
