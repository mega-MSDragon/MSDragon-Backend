package com.msdragon.backend.family.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.family")
data class FamilyProperties(
	/**
	 * 앱 스토어 심사용 가족 코드. 심사자가 이 코드를 입력하면 매번 새 데모 가족을 만들어 연결한다.
	 * 비어 있으면 기능이 꺼지므로 심사가 끝나면 환경변수를 제거한다.
	 */
	val reviewCode: String = "",

	/**
	 * 개발용 가족 연결 해제 API를 열지 여부. **기본값은 꺼짐이다.**
	 * 가족과 여행을 되돌릴 수 없이 지우므로 로컬과 개발 서버에서만 켠다.
	 */
	val devToolsEnabled: Boolean = false,
) {
	fun isReviewCodeEnabled(): Boolean = reviewCode.isNotBlank()
}
