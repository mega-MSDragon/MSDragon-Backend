package com.msdragon.backend.auth.controller

import com.jayway.jsonpath.JsonPath
import com.msdragon.backend.auth.entity.OAuthProvider
import com.msdragon.backend.auth.entity.UserConsentType
import com.msdragon.backend.auth.repository.UserConsentRepository
import com.msdragon.backend.auth.repository.UserRefreshTokenRepository
import com.msdragon.backend.auth.repository.UserRepository
import com.msdragon.backend.auth.service.KakaoOAuthClient
import com.msdragon.backend.auth.service.OAuthUserInfo
import com.msdragon.backend.common.exception.InternalServerException
import com.msdragon.backend.common.exception.UnAuthorizedException
import com.msdragon.backend.family.repository.FamilyCodeRepository
import com.msdragon.backend.family.repository.FamilyCodeUsageRepository
import com.msdragon.backend.family.repository.FamilyMemberRepository
import com.msdragon.backend.family.repository.FamilyRepository
import com.msdragon.backend.parentprofile.repository.ParentProfileRepository
import com.msdragon.backend.trip.repository.TripDayRepository
import com.msdragon.backend.trip.repository.TripParticipantRepository
import com.msdragon.backend.trip.repository.TripRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
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
	private lateinit var userConsentRepository: UserConsentRepository

	@Autowired
	private lateinit var familyCodeUsageRepository: FamilyCodeUsageRepository

	@Autowired
	private lateinit var familyMemberRepository: FamilyMemberRepository

	@Autowired
	private lateinit var familyCodeRepository: FamilyCodeRepository

	@Autowired
	private lateinit var familyRepository: FamilyRepository

	@Autowired
	private lateinit var parentProfileRepository: ParentProfileRepository

	@Autowired
	private lateinit var tripDayRepository: TripDayRepository

	@Autowired
	private lateinit var tripParticipantRepository: TripParticipantRepository

	@Autowired
	private lateinit var tripRepository: TripRepository

	@MockitoBean
	private lateinit var kakaoOAuthClient: KakaoOAuthClient

	@BeforeEach
	fun setUp() {
		tripDayRepository.deleteAll()
		tripParticipantRepository.deleteAll()
		tripRepository.deleteAll()
		parentProfileRepository.deleteAll()
		familyCodeUsageRepository.deleteAll()
		familyMemberRepository.deleteAll()
		familyCodeRepository.deleteAll()
		familyRepository.deleteAll()
		userRefreshTokenRepository.deleteAll()
		userConsentRepository.deleteAll()
		userRepository.deleteAll()
	}

	@AfterEach
	fun tearDown() {
		userConsentRepository.deleteAll()
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
					  "privacyConsentAgreed": true,
					  "platform": "android"
					}
					""".trimIndent(),
				),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.status").value(201))
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
					  "gender": "undisclosed",
					  "privacyConsentAgreed": true
					}
					""".trimIndent(),
				),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.success").value(false))
	}

	@Test
	fun `지원하지 않는 enum 값이면 공통 실패 응답을 반환한다`() {
		mockMvc.perform(
			post("/api/v1/auth/social-login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"provider":"google","token":"google-token"}"""),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.message").value("지원하지 않는 값입니다: google"))
	}

	@Test
	fun `유효하지 않은 소셜 토큰은 HTTP 200과 본문 status 401을 반환한다`() {
		given(kakaoOAuthClient.verify("invalid-token"))
			.willThrow(UnAuthorizedException("카카오 로그인 토큰이 유효하지 않습니다."))

		mockMvc.perform(
			post("/api/v1/auth/social-login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"provider":"kakao","token":"invalid-token"}"""),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.status").value(401))
			.andExpect(jsonPath("$.success").value(false))
	}

	@Test
	fun `소셜 인증 연동 실패는 HTTP 500을 반환한다`() {
		given(kakaoOAuthClient.verify("provider-error"))
			.willThrow(InternalServerException("카카오 인증 서버 호출에 실패했습니다."))

		mockMvc.perform(
			post("/api/v1/auth/social-login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"provider":"kakao","token":"provider-error"}"""),
		)
			.andExpect(status().isInternalServerError)
			.andExpect(jsonPath("$.status").value(500))
			.andExpect(jsonPath("$.success").value(false))
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
					  "gender": "female",
					  "privacyConsentAgreed": true
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

	@Test
	fun `로그아웃하면 refresh token을 폐기하고 반복 요청도 성공한다`() {
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
					  "gender": "female",
					  "privacyConsentAgreed": true
					}
					""".trimIndent(),
				),
		).andReturn().response.contentAsString
		val refreshToken = JsonPath.read<String>(signupResponse, "$.data.refreshToken")
		val logoutBody = """{"refreshToken":"$refreshToken"}"""

		repeat(2) {
			mockMvc.perform(
				post("/api/v1/auth/logout")
					.contentType(MediaType.APPLICATION_JSON)
					.content(logoutBody),
			)
				.andExpect(status().isOk)
				.andExpect(jsonPath("$.status").value(200))
				.andExpect(jsonPath("$.success").value(true))
		}

		mockMvc.perform(
			post("/api/v1/auth/refresh")
				.contentType(MediaType.APPLICATION_JSON)
				.content(logoutBody),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.status").value(401))
			.andExpect(jsonPath("$.success").value(false))
	}

	@Test
	fun `부모는 30대를 선택하고 성별을 생략할 수 있으며 약관 결정을 저장한다`() {
		given(kakaoOAuthClient.verify("parent-token"))
			.willReturn(OAuthUserInfo(OAuthProvider.KAKAO, "parent-30s", "카카오이름"))

		val loginResponse = mockMvc.perform(
			post("/api/v1/auth/social-login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"provider":"kakao","token":"parent-token"}"""),
		).andReturn().response.contentAsString
		val signupToken = JsonPath.read<String>(loginResponse, "$.data.signupToken")

		val signupResponse = mockMvc.perform(
			post("/api/v1/auth/signup/complete")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "signupToken": "$signupToken",
					  "role": "parent",
					  "displayName": "엄마",
					  "ageBand": "30s",
					  "privacyConsentAgreed": true,
					  "locationBasedFacilityConsentAgreed": true
					}
					""".trimIndent(),
				),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.status").value(201))
			.andExpect(jsonPath("$.data.user.ageBand").value("30s"))
			.andExpect(jsonPath("$.data.user.gender").value("undisclosed"))
			.andReturn()
			.response
			.contentAsString

		val userId = JsonPath.read<Int>(signupResponse, "$.data.user.id").toLong()
		val consents = userConsentRepository.findAllByUserIdOrderByIdAsc(userId)
		assertEquals(
			listOf(UserConsentType.PRIVACY_COLLECTION, UserConsentType.LOCATION_BASED_FACILITY),
			consents.map { it.consentType },
		)
		assertEquals(listOf(true, true), consents.map { it.agreed })
		assertEquals(listOf("v1", "v1"), consents.map { it.termsVersion })
	}

	@Test
	fun `개인정보 수집 약관에 동의하지 않으면 회원가입을 거절한다`() {
		given(kakaoOAuthClient.verify("no-consent-token"))
			.willReturn(OAuthUserInfo(OAuthProvider.KAKAO, "no-consent", "카카오이름"))

		val loginResponse = mockMvc.perform(
			post("/api/v1/auth/social-login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"provider":"kakao","token":"no-consent-token"}"""),
		).andReturn().response.contentAsString
		val signupToken = JsonPath.read<String>(loginResponse, "$.data.signupToken")

		mockMvc.perform(
			post("/api/v1/auth/signup/complete")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "signupToken": "$signupToken",
					  "role": "child",
					  "displayName": "혜린",
					  "ageBand": "20s",
					  "privacyConsentAgreed": false
					}
					""".trimIndent(),
				),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.message").value("개인정보 수집 및 이용에 동의해주세요."))
	}
}
