package com.msdragon.backend.common.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

const val BEARER_AUTH_SCHEME = "bearerAuth"

@Configuration
class OpenApiConfig {
	@Bean
	fun openApi(): OpenAPI =
		OpenAPI()
			.info(
				Info()
					.title("MSDragon API")
					.description("서버가 처리한 성공·요청·인증·정책 결과는 HTTP 200으로 반환하며, 본문의 status와 success로 구분합니다."),
			)
			.components(
				Components()
					.addSecuritySchemes(
						BEARER_AUTH_SCHEME,
						SecurityScheme()
							.type(SecurityScheme.Type.HTTP)
							.scheme("bearer")
							.bearerFormat("JWT"),
					),
			)
}
