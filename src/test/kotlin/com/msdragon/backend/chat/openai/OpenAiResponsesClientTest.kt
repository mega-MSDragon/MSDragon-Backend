package com.msdragon.backend.chat.openai

import com.msdragon.backend.chat.config.OpenAiProperties
import com.msdragon.backend.chat.entity.ChatSender
import com.sun.net.httpserver.HttpServer
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import tools.jackson.databind.ObjectMapper
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@SpringBootTest
class OpenAiResponsesClientTest {
	@Autowired
	private lateinit var objectMapper: ObjectMapper

	@Test
	fun `Responses API 요청을 보내고 assistant output text를 추출한다`() {
		var authorization = ""
		var requestBody = ""
		val server = HttpServer.create(InetSocketAddress(0), 0)
		server.createContext("/v1/responses") { exchange ->
			authorization = exchange.requestHeaders.getFirst("Authorization")
			requestBody = exchange.requestBody.bufferedReader().use { it.readText() }
			val body =
				"""
				{
				  "id": "resp_123",
				  "output": [{
				    "type": "message",
				    "role": "assistant",
				    "content": [{"type": "output_text", "text": "오늘 첫 장소는 첨성대입니다."}]
				  }],
				  "usage": {"input_tokens": 20, "output_tokens": 8}
				}
				""".trimIndent().toByteArray()
			exchange.responseHeaders.add("Content-Type", "application/json")
			exchange.sendResponseHeaders(200, body.size.toLong())
			exchange.responseBody.use { it.write(body) }
		}
		server.start()

		try {
			val client = HttpOpenAiResponsesClient(
				properties = OpenAiProperties(
					baseUri = "http://localhost:${server.address.port}/v1",
					apiKey = "test-openai-key",
					model = "gpt-5.6-luna",
				),
				objectMapper = objectMapper,
			)
			val result = client.generate(
				OpenAiChatRequest(
					context = "{\"title\":\"경주 여행\"}",
					messages = listOf(OpenAiChatMessage(ChatSender.USER, "오늘 일정 알려줘")),
					safetyIdentifier = "anonymous-user-hash",
				),
			)

			assertEquals("resp_123", result.responseId)
			assertEquals("오늘 첫 장소는 첨성대입니다.", result.content)
			assertEquals(20, result.usage?.get("input_tokens"))
			assertEquals("Bearer test-openai-key", authorization)
			assertTrue(requestBody.contains("\"model\":\"gpt-5.6-luna\""))
			assertTrue(requestBody.contains("\"store\":false"))
			assertTrue(requestBody.contains("\"effort\":\"none\""))
			assertTrue(requestBody.contains("\"safety_identifier\":\"anonymous-user-hash\""))
			assertFalse(requestBody.contains("test-openai-key"))
		} finally {
			server.stop(0)
		}
	}

	@Test
	fun `function call을 실행하고 결과를 이어 보내 최종 답변을 추출한다`() {
		val requestCount = AtomicInteger()
		val requestBodies = mutableListOf<String>()
		val server = HttpServer.create(InetSocketAddress(0), 0)
		server.createContext("/v1/responses") { exchange ->
			requestBodies += exchange.requestBody.bufferedReader().use { it.readText() }
			val body = if (requestCount.getAndIncrement() == 0) {
				"""
				{
				  "id": "resp_tool",
				  "output": [{
				    "type": "function_call",
				    "call_id": "call_123",
				    "name": "get_trip_schedule",
				    "arguments": "{\"day_number\":1}"
				  }]
				}
				""".trimIndent()
			} else {
				"""
				{
				  "id": "resp_final",
				  "output": [{
				    "type": "message",
				    "role": "assistant",
				    "content": [{"type": "output_text", "text": "첫 장소는 첨성대예용."}]
				  }]
				}
				""".trimIndent()
			}.toByteArray()
			exchange.responseHeaders.add("Content-Type", "application/json")
			exchange.sendResponseHeaders(200, body.size.toLong())
			exchange.responseBody.use { it.write(body) }
		}
		server.start()

		try {
			val client = HttpOpenAiResponsesClient(
				properties = OpenAiProperties(
					baseUri = "http://localhost:${server.address.port}/v1",
					apiKey = "test-openai-key",
					model = "gpt-5.6-luna",
				),
				objectMapper = objectMapper,
			)
			val result = client.generate(
				OpenAiChatRequest(
					context = "{\"title\":\"경주 여행\"}",
					messages = listOf(OpenAiChatMessage(ChatSender.USER, "오늘 일정 알려줘")),
					safetyIdentifier = "anonymous-user-hash",
					tools = listOf(
						OpenAiFunctionTool(
							name = "get_trip_schedule",
							description = "일정 조회",
							parameters = mapOf("type" to "object", "properties" to emptyMap<String, Any>()),
						),
					),
					webSearchEnabled = true,
				),
			) { call ->
				assertEquals("get_trip_schedule", call.name)
				assertEquals(1, (call.arguments["day_number"] as Number).toInt())
				"{\"dayNumber\":1,\"stops\":[{\"name\":\"첨성대\"}]}"
			}

			assertEquals("resp_final", result.responseId)
			assertEquals("첫 장소는 첨성대예용.", result.content)
			assertEquals(2, requestBodies.size)
			assertTrue(requestBodies.first().contains("\"name\":\"get_trip_schedule\""))
			assertTrue(requestBodies.first().contains("\"type\":\"web_search\""))
			assertTrue(requestBodies.last().contains("\"type\":\"function_call_output\""))
			assertTrue(requestBodies.last().contains("\"type\":\"web_search\""))
			assertTrue(requestBodies.last().contains("call_123"))
			assertTrue(requestBodies.last().contains("첨성대"))
		} finally {
			server.stop(0)
		}
	}
}
