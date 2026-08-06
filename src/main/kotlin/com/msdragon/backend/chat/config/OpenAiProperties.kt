package com.msdragon.backend.chat.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "app.openai")
data class OpenAiProperties(
	val baseUri: String = "https://api.openai.com/v1",
	val apiKey: String = "",
	val model: String = "gpt-5.6-luna",
	val connectTimeout: Duration = Duration.ofSeconds(5),
	val requestTimeout: Duration = Duration.ofSeconds(30),
	val maxOutputTokens: Int = 800,
)
