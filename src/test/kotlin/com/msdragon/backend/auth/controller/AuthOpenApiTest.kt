package com.msdragon.backend.auth.controller

import org.hamcrest.Matchers.containsInAnyOrder
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
class AuthOpenApiTest {
	@Autowired
	private lateinit var mockMvc: MockMvc

	@Test
	fun `인증 요청 enum 값이 OpenAPI schema에 노출된다`() {
		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk)
			.andExpect(
				jsonPath(
					"$.components.schemas.SocialLoginRequest.properties.provider.enum",
					containsInAnyOrder("kakao", "apple"),
				),
			)
			.andExpect(
				jsonPath(
					"$.components.schemas.SocialLoginRequest.properties.platform.enum",
					containsInAnyOrder("ios", "android", "web"),
				),
			)
			.andExpect(
				jsonPath(
					"$.components.schemas.CompleteSignupRequest.properties.role.enum",
					containsInAnyOrder("child", "parent"),
				),
			)
			.andExpect(
				jsonPath(
					"$.components.schemas.CompleteSignupRequest.properties.ageBand.enum",
					containsInAnyOrder(
						"10s",
						"20s",
						"30s",
						"40s",
						"50s",
						"60s",
						"60s_plus",
						"70s",
						"80s",
						"90s_plus",
						"undisclosed",
					),
				),
			)
			.andExpect(
				jsonPath(
					"$.components.schemas.CompleteSignupRequest.properties.gender.enum",
					containsInAnyOrder("female", "male", "undisclosed"),
				),
			)
	}
}
