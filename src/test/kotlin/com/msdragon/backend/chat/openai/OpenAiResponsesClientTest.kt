package com.msdragon.backend.chat.openai

import com.msdragon.backend.chat.config.OpenAiProperties
import com.msdragon.backend.chat.entity.ChatSender
import com.sun.net.httpserver.HttpServer
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import tools.jackson.databind.ObjectMapper
import java.net.InetSocketAddress
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
}
