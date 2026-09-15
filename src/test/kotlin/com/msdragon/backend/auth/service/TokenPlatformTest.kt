package com.msdragon.backend.auth.service

import com.msdragon.backend.auth.config.AuthProperties
import com.msdragon.backend.auth.entity.DevicePlatform
import com.msdragon.backend.auth.entity.User
import com.msdragon.backend.auth.entity.UserRole
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TokenPlatformTest {
	private val tokens = TokenService(AuthProperties())
	private val user = mock(User::class.java).also {
		`when`(it.id).thenReturn(7L)
		`when`(it.role).thenReturn(UserRole.CHILD)
	}

	@Test
	fun `같은 사용자도 플랫폼이 다른 토큰을 독립적으로 유지한다`() {
		val ios = tokens.createAccessToken(user, DevicePlatform.IOS)
		val android = tokens.createAccessToken(user, DevicePlatform.ANDROID)
		assertEquals(DevicePlatform.IOS, tokens.parseAccessToken(ios).platform)
		assertEquals(DevicePlatform.ANDROID, tokens.parseAccessToken(android).platform)
		assertEquals(7L, tokens.parseAccessToken(ios).userId)
	}

	@Test
	fun `플랫폼 없는 기존 토큰과 웹은 모바일 플랫폼으로 추정하지 않는다`() {
		assertNull(tokens.parseAccessToken(tokens.createAccessToken(user)).platform)
		assertEquals(DevicePlatform.WEB, tokens.parseAccessToken(tokens.createAccessToken(user, DevicePlatform.WEB)).platform)
	}
}
