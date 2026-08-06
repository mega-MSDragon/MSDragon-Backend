package com.msdragon.backend.common.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.examples.Example
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springdoc.core.customizers.OpenApiCustomizer
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

	@Bean
	fun internalStatusExamples(): OpenApiCustomizer =
		OpenApiCustomizer { openApi ->
			openApi.paths.orEmpty().values
				.flatMap { it.readOperations() }
				.flatMap { it.responses.orEmpty().entries }
				.forEach { (httpStatus, response) ->
					val statuses = internalStatuses(httpStatus, response.description)
					if (statuses.isEmpty()) {
						return@forEach
					}
					val content = response.content ?: Content().also { response.content = it }
					val mediaType = content[APPLICATION_JSON] ?: MediaType().also {
						content.addMediaType(APPLICATION_JSON, it)
					}
					statuses.mapNotNull(STATUS_EXAMPLES::get).forEach { example ->
						mediaType.addExamples(
							example.name,
							Example()
								.summary("${example.summary} (status=${example.status})")
								.value(
									mapOf(
										"status" to example.status,
										"success" to example.success,
										"message" to example.message,
									),
								),
						)
					}
				}
		}

	private fun internalStatuses(httpStatus: String, description: String?): List<Int> {
		val documented = STATUS_PATTERN.findAll(description.orEmpty())
			.flatMap { it.groupValues[1].split('/').asSequence() }
			.mapNotNull(String::toIntOrNull)
			.toList()
		return (documented + listOfNotNull(httpStatus.toIntOrNull()?.takeIf { it != 200 }))
			.distinct()
	}

	companion object {
		private const val APPLICATION_JSON = "application/json"
		private val STATUS_PATTERN = Regex("status=([0-9/]+)")
		private val STATUS_EXAMPLES = listOf(
			StatusExample(200, "success", "성공", true, "요청을 성공적으로 처리했습니다."),
			StatusExample(201, "created", "생성 성공", true, "리소스를 생성했습니다."),
			StatusExample(400, "badRequest", "요청 오류", false, "요청 값이 올바르지 않습니다."),
			StatusExample(401, "unauthorized", "인증 오류", false, "로그인이 필요합니다."),
			StatusExample(403, "forbidden", "권한 오류", false, "접근 권한이 없습니다."),
			StatusExample(404, "notFound", "조회 오류", false, "요청한 리소스를 찾을 수 없습니다."),
			StatusExample(500, "internalServerError", "서버 오류", false, "서버 처리 중 오류가 발생했습니다."),
		).associateBy(StatusExample::status)
	}
}

private data class StatusExample(
	val status: Int,
	val name: String,
	val summary: String,
	val success: Boolean,
	val message: String,
)
