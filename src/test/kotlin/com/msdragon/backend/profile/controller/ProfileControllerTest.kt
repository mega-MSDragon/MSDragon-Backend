package com.msdragon.backend.profile.controller

import com.msdragon.backend.auth.entity.AgeBand
import com.msdragon.backend.auth.entity.DevicePlatform
import com.msdragon.backend.auth.entity.GenderType
import com.msdragon.backend.auth.entity.OAuthProvider
import com.msdragon.backend.auth.entity.User
import com.msdragon.backend.auth.entity.UserRefreshToken
import com.msdragon.backend.auth.entity.UserRole
import com.msdragon.backend.auth.repository.UserRefreshTokenRepository
import com.msdragon.backend.auth.repository.UserRepository
import com.msdragon.backend.auth.service.TokenService
import com.msdragon.backend.family.entity.Family
import com.msdragon.backend.family.entity.FamilyCode
import com.msdragon.backend.family.entity.FamilyMember
import com.msdragon.backend.family.repository.FamilyCodeRepository
import com.msdragon.backend.family.repository.FamilyCodeUsageRepository
import com.msdragon.backend.family.repository.FamilyMemberRepository
import com.msdragon.backend.family.repository.FamilyRepository
import com.msdragon.backend.parentprofile.repository.ParentProfileRepository
import com.msdragon.backend.trip.entity.Trip
import com.msdragon.backend.trip.entity.TripDestinationCode
import com.msdragon.backend.trip.entity.TripParticipant
import com.msdragon.backend.trip.entity.TripStatus
import com.msdragon.backend.trip.repository.TripDayRepository
import com.msdragon.backend.trip.repository.TripParticipantRepository
import com.msdragon.backend.trip.repository.TripRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate
import java.time.LocalDateTime

@SpringBootTest
@AutoConfigureMockMvc
class ProfileControllerTest {
	@Autowired
	private lateinit var mockMvc: MockMvc

	@Autowired
	private lateinit var tokenService: TokenService

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

	@Autowired
	private lateinit var parentProfileRepository: ParentProfileRepository

	@Autowired
	private lateinit var tripDayRepository: TripDayRepository

	@Autowired
	private lateinit var tripParticipantRepository: TripParticipantRepository

	@Autowired
	private lateinit var tripRepository: TripRepository

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
		userRepository.deleteAll()
	}

	@AfterEach
	fun tearDown() {
		setUp()
	}

	@Test
	fun `내 프로필을 조회한다`() {
		val child = saveUser(UserRole.CHILD, "child-1", "혜린")

		mockMvc.perform(
			get("/api/v1/users/me")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(child)}"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.data.id").value(requireNotNull(child.id).toInt()))
			.andExpect(jsonPath("$.data.role").value("child"))
			.andExpect(jsonPath("$.data.displayName").value("혜린"))
			.andExpect(jsonPath("$.data.ageBand").value("20s"))
	}

	@Test
	fun `내 프로필을 수정한다`() {
		val child = saveUser(UserRole.CHILD, "child-1", "혜린")

		mockMvc.perform(
			patch("/api/v1/users/me")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(child)}")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "displayName": "최혜린",
					  "ageBand": "30s",
					  "gender": "female"
					}
					""".trimIndent(),
				),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.data.displayName").value("최혜린"))
			.andExpect(jsonPath("$.data.ageBand").value("30s"))
			.andExpect(jsonPath("$.data.gender").value("female"))
	}
	@Test
	fun `프리셋 프로필 이미지를 선택하고 지운다`() {
		val child = saveUser(UserRole.CHILD, "child-1", "혜린")
		val authorization = "Bearer ${tokenService.createAccessToken(child)}"

		mockMvc.perform(
			get("/api/v1/users/me").header("Authorization", authorization),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.data.profileImage").doesNotExist())

		mockMvc.perform(
			patch("/api/v1/users/me")
				.header("Authorization", authorization)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"profileImage":"flower"}"""),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.data.profileImage").value("flower"))

		// 다른 필드만 보내면 아바타를 유지한다.
		mockMvc.perform(
			patch("/api/v1/users/me")
				.header("Authorization", authorization)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"displayName":"최혜린"}"""),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.data.profileImage").value("flower"))

		// none은 지우기 요청이다.
		mockMvc.perform(
			patch("/api/v1/users/me")
				.header("Authorization", authorization)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"profileImage":"none"}"""),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.data.profileImage").doesNotExist())
	}


	@Test
	fun `역할에 맞지 않는 연령대로 프로필 수정을 거절한다`() {
		val parent = saveUser(UserRole.PARENT, "parent-1", "엄마")

		mockMvc.perform(
			patch("/api/v1/users/me")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(parent)}")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"ageBand":"20s"}"""),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.message").value("선택한 역할에서 사용할 수 없는 연령대입니다."))
	}

	@Test
	fun `부모 회원 탈퇴 시 개인정보와 토큰을 폐기하고 가족 연결을 해제한다`() {
		val child = saveUser(UserRole.CHILD, "child-1", "혜린")
		val parent = saveUser(UserRole.PARENT, "parent-1", "엄마")
		val family = familyRepository.save(Family(ownerUser = child))
		familyMemberRepository.saveAll(
			listOf(
				FamilyMember(family = family, user = child, memberRole = UserRole.CHILD),
				FamilyMember(family = family, user = parent, memberRole = UserRole.PARENT),
			),
		)
		val familyCode = familyCodeRepository.save(FamilyCode(user = parent, code = "MSH-1001"))
		val now = LocalDateTime.now()
		val refreshToken = userRefreshTokenRepository.save(
			UserRefreshToken(
				user = parent,
				refreshTokenHash = "parent-refresh-token-hash",
				platform = DevicePlatform.ANDROID,
				issuedAt = now,
				expiresAt = now.plusDays(30),
			),
		)
		val accessToken = tokenService.createAccessToken(parent)

		mockMvc.perform(
			delete("/api/v1/users/me")
				.header("Authorization", "Bearer $accessToken"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.status").value(200))
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.message").value("회원 탈퇴 성공"))
			.andExpect(jsonPath("$.data").doesNotExist())

		val withdrawnParent = userRepository.findById(requireNotNull(parent.id)).orElseThrow()
		assertThat(withdrawnParent.deletedAt).isNotNull()
		assertThat(withdrawnParent.oauthSubject).startsWith("withdrawn:${parent.id}:")
		assertThat(withdrawnParent.displayName).isEqualTo("탈퇴한 사용자")
		assertThat(withdrawnParent.ageBand).isEqualTo(AgeBand.UNDISCLOSED)
		assertThat(withdrawnParent.gender).isEqualTo(GenderType.UNDISCLOSED)
		assertThat(withdrawnParent.signupCompletedAt).isNull()
		assertThat(familyMemberRepository.findByUserId(requireNotNull(parent.id))).isNull()
		assertThat(familyMemberRepository.findByUserId(requireNotNull(child.id))).isNotNull()
		assertThat(familyCodeRepository.findById(requireNotNull(familyCode.id)).orElseThrow().isActive).isFalse()
		assertThat(userRefreshTokenRepository.findById(requireNotNull(refreshToken.id)).orElseThrow().revokedAt).isNotNull()

		userRepository.saveAndFlush(
			User(
				role = UserRole.PARENT,
				oauthProvider = OAuthProvider.KAKAO,
				oauthSubject = "parent-1",
				displayName = "재가입 부모",
			),
		)

		mockMvc.perform(
			get("/api/v1/users/me")
				.header("Authorization", "Bearer $accessToken"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.status").value(401))
			.andExpect(jsonPath("$.success").value(false))
	}

	@Test
	fun `대표 자녀 탈퇴 시 가족을 해제하고 미완료 여행만 보관한다`() {
		val child = saveUser(UserRole.CHILD, "child-1", "혜린")
		val parent = saveUser(UserRole.PARENT, "parent-1", "엄마")
		val family = familyRepository.save(Family(ownerUser = child))
		familyMemberRepository.saveAll(
			listOf(
				FamilyMember(family = family, user = child, memberRole = UserRole.CHILD),
				FamilyMember(family = family, user = parent, memberRole = UserRole.PARENT),
			),
		)
		val completedTrip = tripRepository.save(
			Trip(
				family = family,
				createdByUser = child,
				destinationCode = TripDestinationCode.BUSAN,
				title = "완료 여행",
				startDate = LocalDate.now().minusDays(2),
				endDate = LocalDate.now().minusDays(1),
				status = TripStatus.COMPLETED,
			),
		)
		val planningTrip = tripRepository.save(
			Trip(
				family = family,
				createdByUser = child,
				destinationCode = TripDestinationCode.GYEONGJU,
				title = "준비 여행",
				startDate = LocalDate.now().plusDays(1),
				endDate = LocalDate.now().plusDays(2),
			),
		)
		tripParticipantRepository.saveAll(
			listOf(
				TripParticipant(trip = completedTrip, user = child),
				TripParticipant(trip = completedTrip, user = parent),
				TripParticipant(trip = planningTrip, user = child),
				TripParticipant(trip = planningTrip, user = parent),
			),
		)
		val parentAccessToken = tokenService.createAccessToken(parent)

		mockMvc.perform(
			delete("/api/v1/users/me")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(child)}"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.status").value(200))

		assertThat(familyRepository.findById(requireNotNull(family.id)).orElseThrow().isActive).isFalse()
		assertThat(familyMemberRepository.findAllByFamilyIdOrderByJoinedAtAsc(requireNotNull(family.id))).isEmpty()
		assertThat(tripRepository.findById(requireNotNull(completedTrip.id)).orElseThrow().status)
			.isEqualTo(TripStatus.COMPLETED)
		assertThat(tripRepository.findById(requireNotNull(planningTrip.id)).orElseThrow().status)
			.isEqualTo(TripStatus.ARCHIVED)

		mockMvc.perform(
			get("/api/v1/trips/${completedTrip.id}")
				.header("Authorization", "Bearer $parentAccessToken"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.status").value(200))
			.andExpect(jsonPath("$.data.id").value(requireNotNull(completedTrip.id).toInt()))

		mockMvc.perform(
			get("/api/v1/trips/${planningTrip.id}")
				.header("Authorization", "Bearer $parentAccessToken"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.status").value(403))

		mockMvc.perform(
			get("/api/v1/records")
				.header("Authorization", "Bearer $parentAccessToken"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.status").value(200))
			.andExpect(jsonPath("$.data.familyId").doesNotExist())
			.andExpect(jsonPath("$.data.statistics.completedTripCount").value(1))
			.andExpect(jsonPath("$.data.records[0].tripId").value(requireNotNull(completedTrip.id).toInt()))
	}

	private fun saveUser(role: UserRole, subject: String, displayName: String): User =
		userRepository.save(
			User(
				role = role,
				oauthProvider = OAuthProvider.KAKAO,
				oauthSubject = subject,
				displayName = displayName,
				ageBand = if (role == UserRole.CHILD) AgeBand.AGE_20S else AgeBand.AGE_60S,
				gender = GenderType.UNDISCLOSED,
				signupCompletedAt = LocalDateTime.now(),
			),
		)
}
