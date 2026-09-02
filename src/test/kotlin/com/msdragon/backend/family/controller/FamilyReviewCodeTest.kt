package com.msdragon.backend.family.controller

import com.msdragon.backend.auth.entity.AgeBand
import com.msdragon.backend.auth.entity.GenderType
import com.msdragon.backend.auth.entity.OAuthProvider
import com.msdragon.backend.auth.entity.User
import com.msdragon.backend.auth.entity.UserRole
import com.msdragon.backend.auth.repository.UserRepository
import com.msdragon.backend.auth.service.TokenService
import com.msdragon.backend.parentprofile.entity.ParentProfileStatus
import com.msdragon.backend.parentprofile.repository.ParentProfileRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import com.jayway.jsonpath.JsonPath
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime

/**
 * 앱 스토어 심사용 가족 코드. 일반 코드는 부모 슬롯이 소진되어 재심사가 막히지만
 * 심사용 코드는 매번 새 데모 가족을 만들어야 한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = ["app.family.review-code=MSH-0901"])
class FamilyReviewCodeTest {
	@Autowired
	private lateinit var mockMvc: MockMvc

	@Autowired
	private lateinit var tokenService: TokenService

	@Autowired
	private lateinit var userRepository: UserRepository

	@Autowired
	private lateinit var parentProfileRepository: ParentProfileRepository

	@Test
	fun `심사용 코드는 여러 심사자가 반복해서 사용할 수 있다`() {
		// 일반 코드라면 세 번째 자녀는 부모 슬롯 소진으로 실패한다.
		repeat(3) { index ->
			val reviewer = saveUser(UserRole.CHILD, "apple-reviewer-$index", "리뷰어$index")

			mockMvc.perform(matchRequest(reviewer, "MSH-0901"))
				.andExpect(status().isOk)
				.andExpect(jsonPath("$.status").value(200))
				.andExpect(jsonPath("$.data.familyId").isNumber)
				.andExpect(jsonPath("$.data.members.length()").value(2))
				.andExpect(jsonPath("$.data.matchedUser.role").value("parent"))
		}
	}

	@Test
	fun `심사용 데모 부모는 프로필이 완료되어 바로 여행을 만들 수 있다`() {
		val reviewer = saveUser(UserRole.CHILD, "apple-reviewer-profile", "리뷰어")

		mockMvc.perform(matchRequest(reviewer, "MSH-0901"))
			.andExpect(status().isOk)
			// 마이페이지·여행 생성이 완료 프로필과 여행 MBTI를 요구한다.
			.andExpect(jsonPath("$.data.members[1].profileCompleted").value(true))
			.andExpect(jsonPath("$.data.members[1].personalityResult.code").value("healing_traveler"))
			.andExpect(jsonPath("$.data.members[1].relationLabel").value("엄마"))

		val demo = userRepository.findAll().first { it.oauthSubject.startsWith("review-demo:") }
		assertThat(parentProfileRepository.findByUserId(requireNotNull(demo.id))?.status)
			.isEqualTo(ParentProfileStatus.COMPLETED)
	}

	@Test
	fun `부모가 심사용 코드를 입력하면 데모 자녀와 연결한다`() {
		val reviewer = saveUser(UserRole.PARENT, "apple-reviewer-parent", "리뷰어", GenderType.MALE)

		mockMvc.perform(matchRequest(reviewer, "MSH-0901"))
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.data.members.length()").value(2))
			.andExpect(jsonPath("$.data.matchedUser.role").value("child"))
	}

	@Test
	fun `심사용 코드를 두 번 입력해도 오류 없이 같은 가족을 반환한다`() {
		val reviewer = saveUser(UserRole.CHILD, "apple-reviewer-twice", "리뷰어")

		val first = mockMvc.perform(matchRequest(reviewer, "MSH-0901"))
			.andExpect(status().isOk)
			.andReturn().response.contentAsString

		mockMvc.perform(matchRequest(reviewer, "MSH-0901"))
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.status").value(200))
			.andExpect(jsonPath("$.data.members.length()").value(2))

		assertThat(first).contains("familyId")
	}

	@Test
	fun `하이픈 없이 입력해도 심사용 코드로 인식한다`() {
		val reviewer = saveUser(UserRole.CHILD, "apple-reviewer-nohyphen", "리뷰어")

		mockMvc.perform(matchRequest(reviewer, "MSH0901"))
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.data.members.length()").value(2))
	}

	@Test
	fun `부모로 가입한 심사자도 데모 여행으로 앱을 확인할 수 있다`() {
		val reviewer = saveUser(UserRole.PARENT, "apple-reviewer-trip", "리뷰어", GenderType.MALE)
		val authorization = "Bearer ${tokenService.createAccessToken(reviewer)}"

		mockMvc.perform(matchRequest(reviewer, "MSH-0901")).andExpect(status().isOk)

		// 여행 생성은 대표 자녀 권한이라 부모 심사자는 볼 여행이 없으면 앱을 확인할 수 없다.
		val tripId = JsonPath.read<Int>(
			mockMvc.perform(get("/api/v1/trips").header("Authorization", authorization))
				.andExpect(status().isOk)
				.andExpect(jsonPath("$.data.trips.length()").value(1))
				.andReturn().response.contentAsString,
			"$.data.trips[0].id",
		)

		// 오늘이 기간에 포함되어 여행 모드가 열린다.
		mockMvc.perform(get("/api/v1/trips/$tripId/travel-mode").header("Authorization", authorization))
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.status").value(200))
			.andExpect(jsonPath("$.data.days.length()").value(3))

		mockMvc.perform(get("/api/v1/trips/$tripId/course").header("Authorization", authorization))
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.data.days[0].stops.length()").value(3))
			.andExpect(jsonPath("$.data.days[0].stops[0].name").value("대릉원"))
			.andExpect(jsonPath("$.data.days[0].route.totalDistanceMeters").value(12400))

		// 종료일이 오늘이라 피드백을 바로 제출할 수 있다.
		mockMvc.perform(get("/api/v1/trips/$tripId/feedback/status").header("Authorization", authorization))
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.data.feedbackAvailable").value(true))
			.andExpect(jsonPath("$.data.canSubmit").value(true))
	}

	private fun matchRequest(user: User, code: String) =
		post("/api/v1/family/matches")
			.header("Authorization", "Bearer ${tokenService.createAccessToken(user)}")
			.contentType(MediaType.APPLICATION_JSON)
			.content("""{"code":"$code"}""")

	private fun saveUser(
		role: UserRole,
		subject: String,
		displayName: String,
		gender: GenderType = GenderType.FEMALE,
	): User =
		userRepository.save(
			User(
				role = role,
				oauthProvider = OAuthProvider.APPLE,
				oauthSubject = subject,
				displayName = displayName,
				ageBand = if (role == UserRole.CHILD) AgeBand.AGE_20S else AgeBand.AGE_60S,
				gender = gender,
				signupCompletedAt = LocalDateTime.now(),
			),
		)
}
