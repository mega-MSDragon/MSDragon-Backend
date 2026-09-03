package com.msdragon.backend.common

import com.msdragon.backend.trip.entity.TripDestinationCode
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * 개인정보처리방침과 이용약관은 클라이언트가 웹뷰로 띄우는 정적 페이지이므로
 * 로그인 없이 접근할 수 있어야 한다. 인증 인터셉터 경로가 넓어지면 이 테스트가 실패한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PolicyPageTest {
	@Autowired
	private lateinit var mockMvc: MockMvc

	@Test
	fun getPrivacyPolicyWithoutAuthorization() {
		mockMvc.perform(get("/policies/privacy.html"))
			.andExpect(status().isOk)
			.andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
	}

	@Test
	fun getTermsOfServiceWithoutAuthorization() {
		mockMvc.perform(get("/policies/terms.html"))
			.andExpect(status().isOk)
			.andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
	}

	@Test
	fun getDestinationImagesWithoutAuthorization() {
		// 홈 추천 도시 이미지는 로그인 없이 URL로 바로 노출된다.
		TripDestinationCode.entries.forEach { destination ->
			mockMvc.perform(get("/images/destinations/${destination.value}.png"))
				.andExpect(status().isOk)
				.andExpect(content().contentTypeCompatibleWith(MediaType.IMAGE_PNG))
		}
	}
}
