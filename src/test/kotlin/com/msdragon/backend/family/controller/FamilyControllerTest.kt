package com.msdragon.backend.family.controller

import com.jayway.jsonpath.JsonPath
import com.msdragon.backend.auth.entity.AgeBand
import com.msdragon.backend.auth.entity.GenderType
import com.msdragon.backend.auth.entity.OAuthProvider
import com.msdragon.backend.auth.entity.User
import com.msdragon.backend.auth.entity.UserRole
import com.msdragon.backend.auth.repository.UserRefreshTokenRepository
import com.msdragon.backend.auth.repository.UserRepository
import com.msdragon.backend.auth.service.TokenService
import com.msdragon.backend.family.repository.FamilyCodeRepository
import com.msdragon.backend.family.repository.FamilyCodeUsageRepository
import com.msdragon.backend.family.repository.FamilyMemberRepository
import com.msdragon.backend.family.repository.FamilyRepository
import com.msdragon.backend.parentprofile.repository.ParentProfileRepository
import org.hamcrest.Matchers.matchesPattern
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime

@SpringBootTest
@AutoConfigureMockMvc
class FamilyControllerTest {
	@Autowired
	private lateinit var mockMvc: MockMvc

	@Autowired
	private lateinit var tokenService: TokenService

	@Autowired
	private lateinit var userRepository: UserRepository

	@Autowired
	private lateinit var userRefreshTokenRepository: UserRefreshTokenRepository

	@Autowired
	private lateinit var familyRepository: FamilyRepository

	@Autowired
	private lateinit var familyMemberRepository: FamilyMemberRepository

	@Autowired
	private lateinit var familyCodeRepository: FamilyCodeRepository

	@Autowired
	private lateinit var familyCodeUsageRepository: FamilyCodeUsageRepository

	@Autowired
	private lateinit var parentProfileRepository: ParentProfileRepository

	@BeforeEach
	fun setUp() {
		parentProfileRepository.deleteAll()
		familyCodeUsageRepository.deleteAll()
		familyMemberRepository.deleteAll()
		familyCodeRepository.deleteAll()
		familyRepository.deleteAll()
		userRefreshTokenRepository.deleteAll()
		userRepository.deleteAll()
	}

	@Test
	fun `내 가족 코드를 발급한다`() {
		val child = saveUser(UserRole.CHILD, "child-1", "혜린")

		mockMvc.perform(
			post("/api/v1/family/code")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(child)}"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.data.code", matchesPattern("MSH-[0-9]{4}")))
	}

	@Test
	fun `매칭 전 내 가족을 조회하면 빈 구성원을 반환한다`() {
		val child = saveUser(UserRole.CHILD, "child-1", "혜린")

		mockMvc.perform(
			get("/api/v1/family")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(child)}"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.data.familyId").doesNotExist())
			.andExpect(jsonPath("$.data.members.length()").value(0))
	}

	@Test
	fun `부모가 자녀 코드로 가족을 매칭한다`() {
		val child = saveUser(UserRole.CHILD, "child-1", "혜린")
		val parent = saveUser(UserRole.PARENT, "parent-1", "아빠", GenderType.MALE)
		val childCode = issueCode(child)

		mockMvc.perform(
			post("/api/v1/family/matches")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(parent)}")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"code":"$childCode"}"""),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.data.familyId").isNumber)
			.andExpect(jsonPath("$.data.matchedUser.id").value(requireNotNull(child.id).toInt()))
			.andExpect(jsonPath("$.data.members.length()").value(2))
			.andExpect(jsonPath("$.data.members[1].relationLabel").value("아빠"))
	}

	@Test
	fun `매칭 후 내 가족 구성원을 조회한다`() {
		val child = saveUser(UserRole.CHILD, "child-1", "혜린")
		val parent = saveUser(UserRole.PARENT, "parent-1", "엄마", GenderType.FEMALE)
		val childCode = issueCode(child)
		match(parent, childCode)

		mockMvc.perform(
			get("/api/v1/family")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(child)}"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.data.familyId").isNumber)
			.andExpect(jsonPath("$.data.members.length()").value(2))
			.andExpect(jsonPath("$.data.members[0].role").value("child"))
			.andExpect(jsonPath("$.data.members[1].role").value("parent"))
			.andExpect(jsonPath("$.data.members[1].ageBand").value("60s"))
			.andExpect(jsonPath("$.data.members[1].gender").value("female"))
			.andExpect(jsonPath("$.data.members[1].relationLabel").value("엄마"))
	}

	@Test
	fun `같은 역할끼리는 가족 매칭을 거절한다`() {
		val parent1 = saveUser(UserRole.PARENT, "parent-1", "엄마")
		val parent2 = saveUser(UserRole.PARENT, "parent-2", "아빠")
		val parent1Code = issueCode(parent1)

		mockMvc.perform(
			post("/api/v1/family/matches")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(parent2)}")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"code":"$parent1Code"}"""),
		)
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.message").value("부모와 자녀만 가족으로 연결할 수 있습니다."))
	}

	@Test
	fun `부모가 두 명이면 추가 매칭을 거절한다`() {
		val child = saveUser(UserRole.CHILD, "child-1", "혜린")
		val parent1 = saveUser(UserRole.PARENT, "parent-1", "엄마")
		val parent2 = saveUser(UserRole.PARENT, "parent-2", "아빠")
		val parent3 = saveUser(UserRole.PARENT, "parent-3", "부모")
		val childCode = issueCode(child)

		match(parent1, childCode)
		match(parent2, childCode)

		mockMvc.perform(
			post("/api/v1/family/matches")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(parent3)}")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"code":"$childCode"}"""),
		)
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.message").value("가족에는 부모를 최대 2명까지만 연결할 수 있습니다."))
	}

	private fun issueCode(user: User): String {
		val response = mockMvc.perform(
			post("/api/v1/family/code")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(user)}"),
		)
			.andReturn()
			.response
			.contentAsString
		return JsonPath.read(response, "$.data.code")
	}

	private fun match(parent: User, childCode: String) {
		mockMvc.perform(
			post("/api/v1/family/matches")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(parent)}")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"code":"$childCode"}"""),
		)
			.andExpect(status().isOk)
	}

	private fun saveUser(
		role: UserRole,
		subject: String,
		displayName: String,
		gender: GenderType = GenderType.UNDISCLOSED,
	): User =
		userRepository.save(
			User(
				role = role,
				oauthProvider = OAuthProvider.KAKAO,
				oauthSubject = subject,
				displayName = displayName,
				ageBand = if (role == UserRole.CHILD) AgeBand.AGE_20S else AgeBand.AGE_60S,
				gender = gender,
				signupCompletedAt = LocalDateTime.now(),
			),
		)
}
