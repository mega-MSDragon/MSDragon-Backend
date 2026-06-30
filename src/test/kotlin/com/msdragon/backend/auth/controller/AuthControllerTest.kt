package com.msdragon.backend.auth.controller

import com.jayway.jsonpath.JsonPath
import com.msdragon.backend.auth.entity.OAuthProvider
import com.msdragon.backend.auth.repository.UserRefreshTokenRepository
import com.msdragon.backend.auth.repository.UserRepository
import com.msdragon.backend.auth.service.KakaoOAuthClient
import com.msdragon.backend.auth.service.OAuthUserInfo
import com.msdragon.backend.family.repository.FamilyCodeRepository
import com.msdragon.backend.family.repository.FamilyCodeUsageRepository
import com.msdragon.backend.family.repository.FamilyMemberRepository
import com.msdragon.backend.family.repository.FamilyRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {
	@Autowired
	private lateinit var mockMvc: MockMvc

	@Autowired
	private lateinit var userRepository: UserRepository

	@Autowired
	private lateinit var userRefreshTokenRepository: UserRefreshTokenRepository

	@Autowired
	private lateinit var familyCodeUsageRepository: FamilyCodeUsageRepository

	@Autowired
	private lateinit var familyMemberRepository: FamilyMemberRepository

	@Autowired
	private lateinit var familyCodeRepository: FamilyCodeRepository

	@Autowired
	private lateinit var familyRepository: FamilyRepository

	@MockitoBean
	private lateinit var kakaoOAuthClient: KakaoOAuthClient

	@BeforeEach
	fun setUp() {
		familyCodeUsageRepository.deleteAll()
		familyMemberRepository.deleteAll()
		familyCodeRepository.deleteAll()
		familyRepository.deleteAll()
		userRefreshTokenRepository.deleteAll()
		userRepository.deleteAll()
	}

	@Test
	fun `미가입 소셜 로그인 후 회원가입을 완료한다`() {
		given(kakaoOAuthClient.verify("kakao-token"))
			.willReturn(OAuthUserInfo(OAuthProvider.KAKAO, "12345", "카카오이름"))

		val loginResponse = mockMvc.perform(
			post("/api/v1/auth/social-login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "provider": "kakao",
					  "token": "kakao-token",
					  "platform": "android"
					}
					""".trimIndent(),
				),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.signupRequired").value(true))
			.andExpect(jsonPath("$.data.signupToken").isString)
			.andReturn()
			.response
			.contentAsString

		val signupToken = JsonPath.read<String>(loginResponse, "$.data.signupToken")

		mockMvc.perform(
			post("/api/v1/auth/signup/complete")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "signupToken": "$signupToken",
					  "role": "child",
					  "displayName": "최혜린",
					  "ageBand": "20s",
					  "gender": "female",
					  "platform": "android"
					}
					""".trimIndent(),
				),
		)
			.andExpect(status().isCreated)
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.signupRequired").value(false))
			.andExpect(jsonPath("$.data.accessToken").isString)
			.andExpect(jsonPath("$.data.refreshToken").isString)
			.andExpect(jsonPath("$.data.user.role").value("child"))
			.andExpect(jsonPath("$.data.user.displayName").value("최혜린"))
			.andExpect(jsonPath("$.data.user.signupCompleted").value(true))
	}

	@Test
	fun `역할에서 허용하지 않는 연령대면 회원가입을 거절한다`() {
		given(kakaoOAuthClient.verify("kakao-token"))
			.willReturn(OAuthUserInfo(OAuthProvider.KAKAO, "12345", "카카오이름"))

		val loginResponse = mockMvc.perform(
			post("/api/v1/auth/social-login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"provider":"kakao","token":"kakao-token"}"""),
		).andReturn().response.contentAsString
		val signupToken = JsonPath.read<String>(loginResponse, "$.data.signupToken")

		mockMvc.perform(
			post("/api/v1/auth/signup/complete")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "signupToken": "$signupToken",
					  "role": "parent",
					  "displayName": "부모",
					  "ageBand": "20s",
					  "gender": "undisclosed"
					}
					""".trimIndent(),
				),
		)
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.success").value(false))
	}

	@Test
	fun `지원하지 않는 enum 값이면 공통 실패 응답을 반환한다`() {
		mockMvc.perform(
			post("/api/v1/auth/social-login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"provider":"google","token":"google-token"}"""),
		)
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.message").value("지원하지 않는 값입니다: google"))
	}

	@Test
	fun `refresh token으로 토큰을 재발급한다`() {
		given(kakaoOAuthClient.verify("kakao-token"))
			.willReturn(OAuthUserInfo(OAuthProvider.KAKAO, "12345", "카카오이름"))

		val loginResponse = mockMvc.perform(
			post("/api/v1/auth/social-login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"provider":"kakao","token":"kakao-token"}"""),
		).andReturn().response.contentAsString
		val signupToken = JsonPath.read<String>(loginResponse, "$.data.signupToken")

		val signupResponse = mockMvc.perform(
			post("/api/v1/auth/signup/complete")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "signupToken": "$signupToken",
					  "role": "child",
					  "displayName": "최혜린",
					  "ageBand": "20s",
					  "gender": "female"
					}
					""".trimIndent(),
				),
		).andReturn().response.contentAsString
		val refreshToken = JsonPath.read<String>(signupResponse, "$.data.refreshToken")

		mockMvc.perform(
			post("/api/v1/auth/refresh")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"refreshToken":"$refreshToken"}"""),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.data.signupRequired").value(false))
			.andExpect(jsonPath("$.data.accessToken").isString)
			.andExpect(jsonPath("$.data.refreshToken").isString)
	}
}
