package com.msdragon.backend.auth.controller

import org.hamcrest.Matchers.containsInAnyOrder
import org.hamcrest.Matchers.hasItem
import org.hamcrest.Matchers.not
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
	fun `Bearer 인증 스키마와 보호 API 보안 요구사항이 OpenAPI에 노출된다`() {
		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type").value("http"))
			.andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
			.andExpect(jsonPath("$.components.securitySchemes.bearerAuth.bearerFormat").value("JWT"))
			.andExpect(jsonPath("$.paths['/api/v1/family'].get.security[0].bearerAuth").isArray)
			.andExpect(jsonPath("$.paths['/api/v1/users/me'].get.security[0].bearerAuth").isArray)
			.andExpect(jsonPath("$.paths['/api/v1/users/me'].delete.security[0].bearerAuth").isArray)
			.andExpect(jsonPath("$.paths['/api/v1/auth/social-login'].post.security").doesNotExist())
			.andExpect(jsonPath("$.paths['/api/v1/auth/logout'].post.security").doesNotExist())
			.andExpect(jsonPath("$.paths['/api/v1/family'].get.parameters").doesNotExist())
			.andExpect(jsonPath("$.paths['/api/v1/family/code'].post.parameters").doesNotExist())
			.andExpect(jsonPath("$.paths['/api/v1/family/matches'].post.parameters").doesNotExist())
			.andExpect(jsonPath("$.paths['/api/v1/users/me'].get.parameters").doesNotExist())
			.andExpect(jsonPath("$.paths['/api/v1/users/me'].patch.parameters").doesNotExist())
			.andExpect(jsonPath("$.paths['/api/v1/users/me'].delete.parameters").doesNotExist())
	}

	@Test
	fun `인증 요청 enum 값이 OpenAPI schema에 노출된다`() {
		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk)
			.andExpect(
				jsonPath("$.paths['/api/v1/auth/social-login'].post.responses['200'].content['application/json'].schema")
					.exists(),
			)
			.andExpect(
				jsonPath("$.paths['/api/v1/auth/social-login'].post.responses['200'].content['*/*']")
					.doesNotExist(),
			)
			.andExpect(
				jsonPath(
					"$.components.schemas.SocialLoginRequest.properties.provider.enum",
					containsInAnyOrder("kakao", "apple"),
				),
			)
			.andExpect(
				jsonPath(
					"$.components.schemas.CompleteSignupRequest.required",
					hasItem("privacyConsentAgreed"),
				),
			)
			.andExpect(
				jsonPath(
					"$.components.schemas.CompleteSignupRequest.required",
					not(hasItem("gender")),
				),
			)
			.andExpect(
				jsonPath("$.components.schemas.CompleteSignupRequest.properties.locationBasedFacilityConsentAgreed.default")
					.value(false),
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

	@Test
	fun `HTTP 200 응답에서 내부 status별 예시를 선택할 수 있다`() {
		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk)
			.andExpect(
				jsonPath("$.paths['/api/v1/auth/social-login'].post.responses['200'].content['application/json'].examples.success.value.status")
					.value(200),
			)
			.andExpect(
				jsonPath("$.paths['/api/v1/auth/social-login'].post.responses['200'].content['application/json'].examples.success.value.data.signupRequired")
					.value(false),
			)
			.andExpect(
				jsonPath("$.paths['/api/v1/auth/social-login'].post.responses['200'].content['application/json'].examples.success.value.data.user.id")
					.value(1),
			)
			.andExpect(
				jsonPath("$.paths['/api/v1/auth/signup/complete'].post.responses['200'].content['application/json'].examples.created.value.status")
					.value(201),
			)
			.andExpect(
				jsonPath("$.paths['/api/v1/trips/{tripId}'].get.responses['200'].content['application/json'].examples.success.value.data.id")
					.value(1),
			)
			.andExpect(
				jsonPath("$.paths['/api/v1/auth/social-login'].post.responses['200'].content['application/json'].examples.badRequest.value.status")
					.value(400),
			)
			.andExpect(
				jsonPath("$.paths['/api/v1/auth/social-login'].post.responses['200'].content['application/json'].examples.unauthorized.value.status")
					.value(401),
			)
			.andExpect(
				jsonPath("$.paths['/api/v1/users/me'].delete.responses['200'].content['application/json'].examples.success.value.status")
					.value(200),
			)
			.andExpect(
				jsonPath("$.paths['/api/v1/users/me'].delete.responses['200'].content['application/json'].examples.unauthorized.value.status")
					.value(401),
			)
			.andExpect(
				jsonPath("$.paths['/api/v1/auth/logout'].post.responses['200'].content['application/json'].examples.success.value.status")
					.value(200),
			)
			.andExpect(
				jsonPath("$.paths['/api/v1/auth/logout'].post.responses['200'].content['application/json'].examples.badRequest.value.status")
					.value(400),
			)
			.andExpect(
				jsonPath("$.paths['/api/v1/auth/social-login'].post.responses['200'].content['application/json'].examples.forbidden")
					.doesNotExist(),
			)
			.andExpect(
				jsonPath("$.paths['/api/v1/auth/social-login'].post.responses['500'].content['application/json'].examples.internalServerError.value.status")
					.value(500),
			)
			.andExpect(
				jsonPath("$.paths['/api/v1/trips/{tripId}/pledge/pdf'].get.responses['200'].content['application/json'].examples.badRequest")
					.doesNotExist(),
			)
			.andExpect(
				jsonPath("$.paths['/api/v1/trips/{tripId}/pledge/pdf'].get.responses['200'].content['application/pdf'].examples")
					.doesNotExist(),
			)
	}

	@Test
	fun `홈 여행의 디데이 필드명은 실제 응답과 같은 dDay로 노출된다`() {
		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.components.schemas.HomeTripSummaryResponse.properties.dDay").exists())
			.andExpect(jsonPath("$.components.schemas.HomeTripSummaryResponse.properties.dday").doesNotExist())
			.andExpect(
				jsonPath("$.paths['/api/v1/home/my-trips'].get.responses['200'].content['application/json'].examples.success.value.data.trips[0].dDay")
					.value(48),
			)
			.andExpect(
				jsonPath("$.paths['/api/v1/home/my-trips'].get.responses['200'].content['application/json'].examples.success.value.data.trips[0].dday")
					.doesNotExist(),
			)
	}
}
