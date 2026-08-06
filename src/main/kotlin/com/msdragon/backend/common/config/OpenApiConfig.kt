package com.msdragon.backend.common.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.examples.Example
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.Schema
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
				.forEach { operation ->
					operation.responses.orEmpty().forEach { (httpStatus, response) ->
						val statuses = internalStatuses(httpStatus, response.description)
						if (statuses.isEmpty()) {
							return@forEach
						}
						val content = response.content ?: Content().also { response.content = it }
						val mediaType = content[APPLICATION_JSON]
							?: content.remove(ANY_MEDIA_TYPE)
							?: MediaType()
						content.addMediaType(APPLICATION_JSON, mediaType)
						statuses.mapNotNull(STATUS_EXAMPLES::get).forEach { example ->
							mediaType.addExamples(
								example.name,
								Example()
									.summary("${example.summary} (status=${example.status})")
									.value(
										if (example.success) {
											successExample(
												schema = mediaType.schema,
												schemas = openApi.components?.schemas.orEmpty(),
												status = example.status,
												message = "${operation.summary ?: example.summary} 성공",
											)
										} else {
											mapOf(
												"status" to example.status,
												"success" to false,
												"message" to example.message,
											)
										},
									),
							)
						}
					}
				}
		}

	private fun successExample(
		schema: Schema<*>?,
		schemas: Map<String, Schema<*>>,
		status: Int,
		message: String,
	): Map<String, Any?> {
		val generated = schemaExample(schema, schemas) as? Map<*, *>
		return linkedMapOf<String, Any?>(
			"status" to status,
			"success" to true,
			"message" to message,
		).apply {
			generated.orEmpty().forEach { (key, value) ->
				if (key is String && key !in COMMON_RESPONSE_FIELDS) {
					put(key, value)
				}
			}
		}
	}

	private fun schemaExample(
		schema: Schema<*>?,
		schemas: Map<String, Schema<*>>,
		visitedRefs: Set<String> = emptySet(),
		depth: Int = 0,
	): Any? {
		if (schema == null || depth > MAX_EXAMPLE_DEPTH) {
			return null
		}
		schema.example?.let { return it }
		schema.default?.let { return it }
		schema.enum?.firstOrNull()?.let { return it }

		val refName = schema.`$ref`?.substringAfterLast('/')
		if (refName != null) {
			if (refName in visitedRefs) {
				return emptyMap<String, Any?>()
			}
			return schemaExample(schemas[refName], schemas, visitedRefs + refName, depth + 1)
		}

		schema.oneOf.orEmpty()
			.firstOrNull { it.type != "null" }
			?.let { return schemaExample(it, schemas, visitedRefs, depth + 1) }

		if (!schema.allOf.isNullOrEmpty()) {
			return linkedMapOf<String, Any?>().apply {
				schema.allOf.orEmpty().forEach { part ->
					val value = schemaExample(part, schemas, visitedRefs, depth + 1) as? Map<*, *>
					value.orEmpty().forEach { (key, item) ->
						if (key is String) {
							put(key, item)
						}
					}
				}
			}
		}

		if (!schema.properties.isNullOrEmpty()) {
			return linkedMapOf<String, Any?>().apply {
				schema.properties.orEmpty().forEach { (name, property) ->
					put(name, schemaExample(property, schemas, visitedRefs, depth + 1))
				}
			}
		}

		val type = schema.type ?: schema.types?.firstOrNull { it != "null" }
		return when (type) {
			"array" -> listOfNotNull(schemaExample(schema.items, schemas, visitedRefs, depth + 1))
			"object" -> emptyMap<String, Any?>()
			"integer" -> 1
			"number" -> 1.0
			"boolean" -> true
			"string" -> when (schema.format) {
				"date" -> "2026-08-06"
				"date-time" -> "2026-08-06T12:00:00"
				"uri", "url" -> "https://example.com"
				else -> "string"
			}
			else -> null
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
		private const val MAX_EXAMPLE_DEPTH = 12
		private const val APPLICATION_JSON = "application/json"
		private const val ANY_MEDIA_TYPE = "*/*"
		private val COMMON_RESPONSE_FIELDS = setOf("status", "success", "message")
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
