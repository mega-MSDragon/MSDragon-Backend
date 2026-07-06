package com.msdragon.backend.trip.controller

import com.jayway.jsonpath.JsonPath
import com.msdragon.backend.auth.entity.AgeBand
import com.msdragon.backend.auth.entity.GenderType
import com.msdragon.backend.auth.entity.OAuthProvider
import com.msdragon.backend.auth.entity.User
import com.msdragon.backend.auth.entity.UserRole
import com.msdragon.backend.auth.repository.UserRefreshTokenRepository
import com.msdragon.backend.auth.repository.UserRepository
import com.msdragon.backend.auth.service.TokenService
import com.msdragon.backend.family.entity.Family
import com.msdragon.backend.family.entity.FamilyMember
import com.msdragon.backend.family.repository.FamilyCodeRepository
import com.msdragon.backend.family.repository.FamilyCodeUsageRepository
import com.msdragon.backend.family.repository.FamilyMemberRepository
import com.msdragon.backend.family.repository.FamilyRepository
import com.msdragon.backend.parentprofile.entity.ActivityLevel
import com.msdragon.backend.parentprofile.entity.FoodPreference
import com.msdragon.backend.parentprofile.entity.ParentProfile
import com.msdragon.backend.parentprofile.entity.ParentProfileStatus
import com.msdragon.backend.parentprofile.entity.TravelPersonalityTypeCode
import com.msdragon.backend.parentprofile.repository.ParentProfileRepository
import com.msdragon.backend.trip.repository.TripDayRepository
import com.msdragon.backend.trip.repository.TripParticipantRepository
import com.msdragon.backend.trip.repository.TripRepository
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
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

@SpringBootTest
@AutoConfigureMockMvc
class TripControllerTest {
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

	@Test
	fun `여행 대상 부모 후보를 조회한다`() {
		val child = saveUser(UserRole.CHILD, "child-1", "혜린")
		val mother = saveUser(UserRole.PARENT, "parent-1", "엄마", GenderType.FEMALE)
		val father = saveUser(UserRole.PARENT, "parent-2", "아빠", GenderType.MALE)
		connectFamily(child, mother, father)
		saveCompletedParentProfile(mother)

		mockMvc.perform(
			get("/api/v1/trips/parent-candidates")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(child)}"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.data.familyId").isNumber)
			.andExpect(jsonPath("$.data.parents.length()").value(2))
			.andExpect(jsonPath("$.data.parents[0].relationLabel").value("엄마"))
			.andExpect(jsonPath("$.data.parents[0].profileCompleted").value(true))
			.andExpect(jsonPath("$.data.parents[1].relationLabel").value("아빠"))
			.andExpect(jsonPath("$.data.parents[1].profileCompleted").value(false))
	}

	@Test
	fun `여행 도시 목록을 조회한다`() {
		val child = saveUser(UserRole.CHILD, "child-1", "혜린")

		mockMvc.perform(
			get("/api/v1/trips/destinations")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(child)}"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.data.length()").value(12))
			.andExpect(jsonPath("$.data[2].code").value("gyeongju"))
			.andExpect(jsonPath("$.data[2].displayName").value("경주"))
	}

	@Test
	fun `자녀가 여행을 생성한다`() {
		val child = saveUser(UserRole.CHILD, "child-1", "혜린")
		val mother = saveUser(UserRole.PARENT, "parent-1", "엄마", GenderType.FEMALE)
		connectFamily(child, mother)
		saveCompletedParentProfile(mother)
		val startDate = futureDate(10)
		val endDate = startDate.plusDays(1)

		mockMvc.perform(
			post("/api/v1/trips")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(child)}")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "parentUserIds": [${requireNotNull(mother.id)}],
					  "destinationCode": "gyeongju",
					  "startDate": "$startDate",
					  "endDate": "$endDate"
					}
					""".trimIndent(),
				),
		)
			.andExpect(status().isCreated)
			.andExpect(jsonPath("$.data.title").value("경주 여행"))
			.andExpect(jsonPath("$.data.destination.code").value("gyeongju"))
			.andExpect(jsonPath("$.data.status").value("planning"))
			.andExpect(jsonPath("$.data.participants.length()").value(2))
			.andExpect(jsonPath("$.data.participants[1].relationLabel").value("엄마"))
			.andExpect(jsonPath("$.data.days.length()").value(2))
				.andExpect(jsonPath("$.data.days[0].dayNumber").value(1))
				.andExpect(jsonPath("$.data.days[1].dayNumber").value(2))
	}

	@Test
	fun `여행 기간은 3일 이상도 생성한다`() {
		val child = saveUser(UserRole.CHILD, "child-1", "혜린")
		val mother = saveUser(UserRole.PARENT, "parent-1", "엄마", GenderType.FEMALE)
		connectFamily(child, mother)
		saveCompletedParentProfile(mother)
		val startDate = futureDate(20)
		val endDate = startDate.plusDays(3)

		mockMvc.perform(
			post("/api/v1/trips")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(child)}")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "parentUserIds": [${requireNotNull(mother.id)}],
					  "destinationCode": "busan",
					  "startDate": "$startDate",
					  "endDate": "$endDate"
					}
					""".trimIndent(),
				),
		)
			.andExpect(status().isCreated)
			.andExpect(jsonPath("$.data.days.length()").value(4))
			.andExpect(jsonPath("$.data.days[3].dayNumber").value(4))
			.andExpect(jsonPath("$.data.days[3].travelDate").value(endDate.toString()))
	}

	@Test
	fun `부모 프로필이 완료되지 않으면 여행 생성을 거절한다`() {
		val child = saveUser(UserRole.CHILD, "child-1", "혜린")
		val mother = saveUser(UserRole.PARENT, "parent-1", "엄마", GenderType.FEMALE)
		connectFamily(child, mother)
		val startDate = futureDate(10)

		mockMvc.perform(
			post("/api/v1/trips")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(child)}")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "parentUserIds": [${requireNotNull(mother.id)}],
					  "destinationCode": "gyeongju",
					  "startDate": "$startDate",
					  "endDate": "$startDate"
					}
					""".trimIndent(),
				),
		)
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.message").value("부모님 상세 프로필 작성이 필요합니다."))
	}

	@Test
	fun `같은 가족의 겹치는 날짜 여행 생성을 거절한다`() {
		val child = saveUser(UserRole.CHILD, "child-1", "혜린")
		val mother = saveUser(UserRole.PARENT, "parent-1", "엄마", GenderType.FEMALE)
		connectFamily(child, mother)
		saveCompletedParentProfile(mother)
		val startDate = futureDate(10)
		createTrip(child, mother, startDate, startDate.plusDays(1))

		mockMvc.perform(
			post("/api/v1/trips")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(child)}")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "parentUserIds": [${requireNotNull(mother.id)}],
					  "destinationCode": "busan",
					  "startDate": "${startDate.plusDays(1)}",
					  "endDate": "${startDate.plusDays(1)}"
					}
					""".trimIndent(),
				),
		)
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.message").value("선택한 날짜에 이미 등록된 여행이 있습니다."))
	}

	@Test
	fun `가족 구성원은 여행 상세와 목록을 조회한다`() {
		val child = saveUser(UserRole.CHILD, "child-1", "혜린")
		val mother = saveUser(UserRole.PARENT, "parent-1", "엄마", GenderType.FEMALE)
		connectFamily(child, mother)
		saveCompletedParentProfile(mother)
		val tripId = createTrip(child, mother, futureDate(10), futureDate(10))

		mockMvc.perform(
			get("/api/v1/trips/$tripId")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(mother)}"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.data.id").value(tripId))
			.andExpect(jsonPath("$.data.participants.length()").value(2))

		mockMvc.perform(
			get("/api/v1/trips")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(child)}"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.data.trips.length()").value(1))
			.andExpect(jsonPath("$.data.trips[0].id").value(tripId))
	}

	@Test
	fun `부모는 여행을 생성할 수 없다`() {
		val parent = saveUser(UserRole.PARENT, "parent-1", "엄마", GenderType.FEMALE)

		mockMvc.perform(
			post("/api/v1/trips")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(parent)}")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "parentUserIds": [${requireNotNull(parent.id)}],
					  "destinationCode": "gyeongju",
					  "startDate": "${futureDate(10)}",
					  "endDate": "${futureDate(10)}"
					}
					""".trimIndent(),
				),
		)
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.message").value("자녀 사용자만 여행을 만들 수 있습니다."))
	}

	private fun createTrip(child: User, parent: User, startDate: LocalDate, endDate: LocalDate): Int {
		val response = mockMvc.perform(
			post("/api/v1/trips")
				.header("Authorization", "Bearer ${tokenService.createAccessToken(child)}")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "parentUserIds": [${requireNotNull(parent.id)}],
					  "destinationCode": "gyeongju",
					  "startDate": "$startDate",
					  "endDate": "$endDate"
					}
					""".trimIndent(),
				),
		)
			.andExpect(status().isCreated)
			.andReturn()
			.response
			.contentAsString

		return JsonPath.read(response, "$.data.id")
	}

	private fun saveCompletedParentProfile(parent: User) {
		parentProfileRepository.save(
			ParentProfile(
				user = parent,
				status = ParentProfileStatus.COMPLETED,
				currentStep = 3,
				activityLevel = ActivityLevel.SLOW,
				foodPreference = FoodPreference.FAMILIAR_FOOD,
				needsMobilityAssistance = false,
				personalityType = TravelPersonalityTypeCode.RELAXED_EXPLORER,
				completionPercent = 100,
				completedAt = LocalDateTime.now(),
			),
		)
	}

	private fun connectFamily(child: User, vararg parents: User) {
		val family = familyRepository.save(Family(ownerUser = child))
		familyMemberRepository.save(
			FamilyMember(
				family = family,
				user = child,
				memberRole = UserRole.CHILD,
			),
		)
		parents.forEach { parent ->
			familyMemberRepository.save(
				FamilyMember(
					family = family,
					user = parent,
					memberRole = UserRole.PARENT,
				),
			)
		}
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

	private fun futureDate(daysFromToday: Long): LocalDate =
		LocalDate.now(ZoneId.of("Asia/Seoul")).plusDays(daysFromToday)
}
