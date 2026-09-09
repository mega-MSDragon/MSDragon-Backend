package com.msdragon.backend.family.controller

import com.msdragon.backend.auth.entity.AgeBand
import com.msdragon.backend.auth.entity.GenderType
import com.msdragon.backend.auth.entity.OAuthProvider
import com.msdragon.backend.auth.entity.User
import com.msdragon.backend.auth.entity.UserRole
import com.msdragon.backend.auth.repository.UserRepository
import com.msdragon.backend.auth.service.TokenService
import com.msdragon.backend.family.entity.Family
import com.msdragon.backend.family.entity.FamilyMember
import com.msdragon.backend.family.repository.FamilyMemberRepository
import com.msdragon.backend.family.repository.FamilyRepository
import com.msdragon.backend.trip.entity.Trip
import com.msdragon.backend.trip.entity.TripDestinationCode
import com.msdragon.backend.trip.repository.TripRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate
import java.time.LocalDateTime

/** 개발용 가족 연결 해제. 운영에서 열리면 가족과 여행이 사라지므로 기본값이 꺼짐인지도 함께 확인한다. */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = ["app.family.dev-tools-enabled=true"])
class FamilyDevControllerTest {
	@Autowired
	private lateinit var mockMvc: MockMvc

	@Autowired
	private lateinit var tokenService: TokenService

	@Autowired
	private lateinit var userRepository: UserRepository

	@Autowired
	private lateinit var familyRepository: FamilyRepository

	@Autowired
	private lateinit var familyMemberRepository: FamilyMemberRepository

	@Autowired
	private lateinit var tripRepository: TripRepository

	@Test
	fun `자녀가 호출하면 가족을 해체하고 여행까지 지운다`() {
		val child = saveUser(UserRole.CHILD, "dev-disconnect-child", "혜린")
		val mother = saveUser(UserRole.PARENT, "dev-disconnect-mother", "길순")
		val family = connectFamily(child, mother)
		val trip = tripRepository.save(
			Trip(
				family = family,
				createdByUser = child,
				destinationCode = TripDestinationCode.BUSAN,
				title = "부산 여행",
				startDate = LocalDate.now().plusDays(7),
				endDate = LocalDate.now().plusDays(8),
			),
		)

		mockMvc.perform(
			delete("/api/v1/dev/family")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(child)}"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.status").value(200))
			.andExpect(jsonPath("$.data.dissolved").value(true))
			.andExpect(jsonPath("$.data.removedUserIds.length()").value(2))
			.andExpect(jsonPath("$.data.deletedTripIds.length()").value(1))

		// 자녀와 부모 모두 다시 연결할 수 있는 상태가 된다.
		assertThat(familyMemberRepository.findByUserId(requireNotNull(child.id))).isNull()
		assertThat(familyMemberRepository.findByUserId(requireNotNull(mother.id))).isNull()
		assertThat(tripRepository.findById(requireNotNull(trip.id)).orElseThrow().deletedAt).isNotNull()
	}

	@Test
	fun `부모가 호출하면 본인 연결만 끊고 가족은 남는다`() {
		val child = saveUser(UserRole.CHILD, "dev-disconnect-child2", "혜린")
		val mother = saveUser(UserRole.PARENT, "dev-disconnect-mother2", "길순")
		connectFamily(child, mother)

		mockMvc.perform(
			delete("/api/v1/dev/family")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(mother)}"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.data.dissolved").value(false))
			.andExpect(jsonPath("$.data.removedUserIds.length()").value(1))
			.andExpect(jsonPath("$.data.deletedTripIds").isEmpty)

		assertThat(familyMemberRepository.findByUserId(requireNotNull(mother.id))).isNull()
		assertThat(familyMemberRepository.findByUserId(requireNotNull(child.id))).isNotNull()
	}

	@Test
	fun `연결된 가족이 없으면 해제할 수 없다`() {
		val child = saveUser(UserRole.CHILD, "dev-disconnect-alone", "혜린")

		mockMvc.perform(
			delete("/api/v1/dev/family")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(child)}"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.message").value("연결된 가족이 없습니다."))
	}

	private fun connectFamily(child: User, mother: User): Family {
		val family = familyRepository.save(Family(ownerUser = child))
		familyMemberRepository.saveAll(
			listOf(
				FamilyMember(family = family, user = child, memberRole = UserRole.CHILD),
				FamilyMember(family = family, user = mother, memberRole = UserRole.PARENT),
			),
		)
		return family
	}

	private fun saveUser(role: UserRole, subject: String, displayName: String): User =
		userRepository.save(
			User(
				role = role,
				oauthProvider = OAuthProvider.KAKAO,
				oauthSubject = subject,
				displayName = displayName,
				ageBand = if (role == UserRole.CHILD) AgeBand.AGE_20S else AgeBand.AGE_60S,
				gender = GenderType.FEMALE,
				signupCompletedAt = LocalDateTime.now(),
			),
		)
}
