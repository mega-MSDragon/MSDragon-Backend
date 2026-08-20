package com.msdragon.backend.home.controller

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
class HomeOpenApiTest {
	@Autowired
	private lateinit var mockMvc: MockMvc

	@Test
	fun `홈 섹션 API 보안과 실제 응답 예시가 OpenAPI에 노출된다`() {
		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.paths['/api/v1/home']").doesNotExist())
			.andExpect(jsonPath("$.paths['/api/v1/home/my-trips'].get.security[0].bearerAuth").isArray)
			.andExpect(jsonPath("$.paths['/api/v1/home/monthly-recommendations'].get.security[0].bearerAuth").isArray)
			.andExpect(jsonPath("$.paths['/api/v1/home/festivals'].get.security[0].bearerAuth").isArray)
			.andExpect(
				jsonPath("$.paths['/api/v1/home/my-trips'].get.responses['200'].content['application/json'].schema")
					.exists(),
			)
			.andExpect(
				jsonPath("$.paths['/api/v1/home/my-trips'].get.responses['200'].content['*/*']")
					.doesNotExist(),
			)
			.andExpect(
				jsonPath("$.paths['/api/v1/home/my-trips'].get.responses['200'].content['application/json'].examples.success.value.data.userRole")
					.value("child"),
			)
			.andExpect(
				jsonPath("$.paths['/api/v1/home/my-trips'].get.responses['200'].content['application/json'].examples.success.value.data.parentProfiles[0].profileCompleted")
					.value(true),
			)
			.andExpect(
				jsonPath("$.paths['/api/v1/home/my-trips'].get.responses['200'].content['application/json'].examples.success.value.data.trips[0].primaryTheme")
					.value("history_culture"),
			)
			.andExpect(
				jsonPath("$.paths['/api/v1/home/my-trips'].get.responses['200'].content['application/json'].examples.success.value.data.trips[0].intensity")
					.value("low"),
			)
			.andExpect(
				jsonPath("$.paths['/api/v1/home/monthly-recommendations'].get.responses['200'].content['application/json'].examples.success.value.data.recommendedCities[0].imageUrl")
					.value("https://example.com/gyeongju.jpg"),
			)
			.andExpect(
				jsonPath("$.paths['/api/v1/home/festivals'].get.responses['200'].content['application/json'].examples.success.value.data.festivals[0].title")
					.value("안동 선유줄불놀이"),
			)
			.andExpect(
				jsonPath("$.paths['/api/v1/home/my-trips'].get.responses['200'].content['application/json'].examples.unauthorized.value.status")
					.value(401),
			)
	}
}
