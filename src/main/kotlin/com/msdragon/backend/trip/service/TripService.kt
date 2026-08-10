package com.msdragon.backend.trip.service

import com.msdragon.backend.auth.entity.User
import com.msdragon.backend.auth.entity.UserRole
import com.msdragon.backend.auth.repository.UserRepository
import com.msdragon.backend.auth.support.AuthenticatedUser
import com.msdragon.backend.common.exception.BadRequestException
import com.msdragon.backend.common.exception.ForbiddenException
import com.msdragon.backend.common.exception.NotFoundException
import com.msdragon.backend.common.exception.UnAuthorizedException
import com.msdragon.backend.family.entity.FamilyMember
import com.msdragon.backend.family.repository.FamilyMemberRepository
import com.msdragon.backend.feedback.service.TripFeedbackService
import com.msdragon.backend.parentprofile.entity.ParentProfile
import com.msdragon.backend.parentprofile.entity.ParentProfileStatus
import com.msdragon.backend.parentprofile.entity.TravelThemeCode
import com.msdragon.backend.parentprofile.repository.ParentProfileRepository
import com.msdragon.backend.pledge.service.TripPledgeService
import com.msdragon.backend.trip.dto.CreateTripRequest
import com.msdragon.backend.trip.dto.MyTripsResponse
import com.msdragon.backend.trip.dto.SaveTripCourseRequest
import com.msdragon.backend.trip.dto.TripCourseDayResponse
import com.msdragon.backend.trip.dto.TripCourseResponse
import com.msdragon.backend.trip.dto.TripParentProfileSnapshotResponse
import com.msdragon.backend.trip.dto.TripDestinationResponse
import com.msdragon.backend.trip.dto.TripDetailResponse
import com.msdragon.backend.trip.dto.TripParentCandidateResponse
import com.msdragon.backend.trip.dto.TripParentCandidatesResponse
import com.msdragon.backend.trip.dto.TripRecommendationSnapshotResponse
import com.msdragon.backend.trip.dto.TripRouteSummaryResponse
import com.msdragon.backend.trip.dto.TripStopResponse
import com.msdragon.backend.trip.dto.TripSummaryResponse
import com.msdragon.backend.trip.dto.TripTravelModeResponse
import com.msdragon.backend.trip.dto.UpdateTripRequest
import com.msdragon.backend.trip.dto.relationLabelOf
import com.msdragon.backend.trip.entity.Trip
import com.msdragon.backend.trip.entity.TripDay
import com.msdragon.backend.trip.entity.TripDestinationCode
import com.msdragon.backend.trip.entity.TripParticipant
import com.msdragon.backend.trip.entity.TripStatus
import com.msdragon.backend.trip.entity.TripStop
import com.msdragon.backend.trip.repository.TripDayRepository
import com.msdragon.backend.trip.repository.TripParticipantRepository
import com.msdragon.backend.trip.repository.TripRepository
import com.msdragon.backend.trip.repository.TripStopRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

@Service
class TripService(
	private val userRepository: UserRepository,
	private val familyMemberRepository: FamilyMemberRepository,
	private val parentProfileRepository: ParentProfileRepository,
	private val tripRepository: TripRepository,
	private val tripParticipantRepository: TripParticipantRepository,
	private val tripDayRepository: TripDayRepository,
	private val tripStopRepository: TripStopRepository,
	private val tripPledgeService: TripPledgeService,
	private val tripFeedbackService: TripFeedbackService,
	private val objectMapper: ObjectMapper,
) {
	@Transactional(readOnly = true)
	fun getParentCandidates(currentUser: AuthenticatedUser): TripParentCandidatesResponse {
		val user = getLoginUser(currentUser.id)
		validateChild(user)

		val myMember = familyMemberRepository.findByUserId(currentUser.id)
			?: return TripParentCandidatesResponse.empty()
		val familyId = requireNotNull(myMember.family.id)
		val parents = familyMemberRepository.findAllByFamilyIdOrderByJoinedAtAsc(familyId)
			.filter { it.memberRole == UserRole.PARENT }
			.map { TripParentCandidateResponse.of(it, parentProfileRepository.findByUserId(requireNotNull(it.user.id))) }

		return TripParentCandidatesResponse(familyId = familyId, parents = parents)
	}

	@Transactional(readOnly = true)
	fun getDestinations(): List<TripDestinationResponse> =
		TripDestinationCode.entries
			.sortedBy { it.displayOrder }
			.map(TripDestinationResponse::from)

	@Transactional
	fun getMyTrips(currentUser: AuthenticatedUser): MyTripsResponse {
		getLoginUser(currentUser.id)
		val myMember = familyMemberRepository.findByUserId(currentUser.id)
			?: return MyTripsResponse.empty()
		val familyId = requireNotNull(myMember.family.id)
		val today = currentDate()
		val trips = tripRepository.findAllByFamilyIdAndDeletedAtIsNullOrderByStartDateAscIdAsc(familyId)
			.map { trip ->
				trip.synchronizeStatus(today)
				TripSummaryResponse.of(
					trip = trip,
					participantCount = tripParticipantRepository.findAllByTripIdOrderByIdAsc(requireNotNull(trip.id)).size,
				)
			}

		return MyTripsResponse(familyId = familyId, trips = trips)
	}

	@Transactional
	fun getTrip(currentUser: AuthenticatedUser, tripId: Long): TripDetailResponse {
		getLoginUser(currentUser.id)
		val trip = tripRepository.findByIdAndDeletedAtIsNull(tripId)
			?: throw NotFoundException("여행을 찾을 수 없습니다.")
		trip.synchronizeStatus(currentDate())
		validateTripReadable(currentUser.id, trip)

		return tripDetail(trip)
	}

	@Transactional
	fun getTripCourse(currentUser: AuthenticatedUser, tripId: Long): TripCourseResponse {
		getLoginUser(currentUser.id)
		val trip = tripRepository.findByIdAndDeletedAtIsNull(tripId)
			?: throw NotFoundException("여행을 찾을 수 없습니다.")
		trip.synchronizeStatus(currentDate())
		validateTripReadable(currentUser.id, trip)

		return tripCourse(trip)
	}

	@Transactional(noRollbackFor = [BadRequestException::class])
	fun getTravelMode(currentUser: AuthenticatedUser, tripId: Long): TripTravelModeResponse {
		val trip = requireTravelModeTrip(currentUser, tripId)
		val today = currentDate()
		val course = tripCourse(trip)
		val currentDay = course.days.firstOrNull { it.travelDate == today }
			?: throw NotFoundException("현재 일자의 여행 코스를 찾을 수 없습니다.")
		return TripTravelModeResponse(
			tripId = tripId,
			title = trip.title,
			destination = TripDestinationResponse.from(trip.destinationCode),
			startDate = trip.startDate,
			endDate = trip.endDate,
			status = trip.status,
			currentDayNumber = currentDay.dayNumber,
			currentTripDayId = currentDay.tripDayId,
			isLastDay = today == trip.endDate,
			pledgeCompleted = tripPledgeService.isCompleted(tripId),
			days = course.days,
		)
	}

	@Transactional(noRollbackFor = [BadRequestException::class])
	fun validateTravelModeAccess(currentUser: AuthenticatedUser, tripId: Long) {
		requireTravelModeTrip(currentUser, tripId)
	}

	private fun requireTravelModeTrip(currentUser: AuthenticatedUser, tripId: Long): Trip {
		getLoginUser(currentUser.id)
		val trip = tripRepository.findByIdAndDeletedAtIsNull(tripId)
			?: throw NotFoundException("여행을 찾을 수 없습니다.")
		validateTripReadable(currentUser.id, trip)

		val today = currentDate()
		trip.synchronizeStatus(today)
		when {
			trip.status == TripStatus.ARCHIVED -> throw BadRequestException("보관된 여행은 여행 모드를 이용할 수 없습니다.")
			today.isBefore(trip.startDate) -> throw BadRequestException("여행 시작일부터 여행 모드를 이용할 수 있습니다.")
			today.isAfter(trip.endDate) -> throw BadRequestException("종료된 여행은 여행 모드를 이용할 수 없습니다.")
		}
		return trip
	}

	@Transactional
	fun createTrip(currentUser: AuthenticatedUser, request: CreateTripRequest): TripDetailResponse {
		val child = getLoginUser(currentUser.id)
		validateChild(child)

		val myMember = familyMemberRepository.findByUserId(currentUser.id)
			?: throw BadRequestException("가족 매칭 후 여행을 만들 수 있습니다.")
		val family = myMember.family
		val familyId = requireNotNull(family.id)

		val dayCount = validateDateRange(request.startDate, request.endDate)
		if (tripRepository.existsOverlappingTrip(familyId, request.startDate, request.endDate, TripStatus.ARCHIVED)) {
			throw BadRequestException("선택한 날짜에 이미 등록된 여행이 있습니다.")
		}

		val selectedParents = resolveSelectedParents(familyId, request.parentUserIds)
		val selectedParentProfiles = resolveSelectedParentProfiles(selectedParents)

		val destination = request.destinationCode
		val recommendationSnapshot = buildRecommendationSnapshot(
			destination = destination,
			startDate = request.startDate,
			endDate = request.endDate,
			selectedParents = selectedParents,
			selectedParentProfiles = selectedParentProfiles,
		)
		val trip = tripRepository.save(
			Trip(
				family = family,
				createdByUser = child,
				destinationCode = destination,
				title = request.title?.trim()?.takeIf { it.isNotBlank() } ?: "${destination.displayName} 여행",
				startDate = request.startDate,
				endDate = request.endDate,
				recommendationSnapshot = objectMapper.writeValueAsString(recommendationSnapshot),
			),
		)

		tripParticipantRepository.save(TripParticipant(trip = trip, user = child))
		selectedParents.forEach { parent ->
			tripParticipantRepository.save(TripParticipant(trip = trip, user = parent.user))
		}
		repeat(dayCount) { index ->
			tripDayRepository.save(
				TripDay(
					trip = trip,
					dayNumber = index + 1,
					travelDate = request.startDate.plusDays(index.toLong()),
				),
			)
		}
		trip.synchronizeStatus(currentDate())

		return tripDetail(trip)
	}

	@Transactional
	fun updateTrip(
		currentUser: AuthenticatedUser,
		tripId: Long,
		request: UpdateTripRequest,
	): TripDetailResponse {
		val child = getLoginUser(currentUser.id)
		val trip = tripRepository.findByIdAndDeletedAtIsNull(tripId)
			?: throw NotFoundException("여행을 찾을 수 없습니다.")
		val today = currentDate()
		trip.synchronizeStatus(today)
		validateTripEditable(child, trip)
		val editingInProgress = trip.status == TripStatus.IN_PROGRESS
		val normalizedTitle = request.title.trim()
		if (editingInProgress && normalizedTitle != trip.title) {
			throw BadRequestException("여행 중에는 여행 제목을 수정할 수 없습니다.")
		}
		if (editingInProgress && request.destinationCode != trip.destinationCode) {
			throw BadRequestException("여행 중에는 여행 도시를 수정할 수 없습니다.")
		}

		val familyId = requireNotNull(trip.family.id)
		val selectedParents = resolveSelectedParents(familyId, request.parentUserIds)
		val existingParticipants = tripParticipantRepository.findAllByTripIdOrderByIdAsc(tripId)
		val existingParentIds = existingParticipants
			.filter { it.user.role == UserRole.PARENT }
			.map { requireNotNull(it.user.id) }
			.toSet()
		val requestedParentIds = request.parentUserIds.toSet()
		val destinationChanged = trip.destinationCode != request.destinationCode
		val datesChanged = trip.startDate != request.startDate || trip.endDate != request.endDate
		val participantsChanged = existingParentIds != requestedParentIds
		val recommendationInputsChanged = destinationChanged || datesChanged || participantsChanged

		val dayCount = if (datesChanged) {
			validateUpdateDateRange(trip.status, request.startDate, request.endDate, today).also {
				if (
					tripRepository.existsOverlappingTripExcludingId(
						familyId = familyId,
						tripId = tripId,
						startDate = request.startDate,
						endDate = request.endDate,
						excludedStatus = TripStatus.ARCHIVED,
					)
				) {
					throw BadRequestException("선택한 날짜에 이미 등록된 여행이 있습니다.")
				}
			}
		} else {
			0
		}

		val tripDays = tripDayRepository.findAllByTripIdOrderByDayNumberAsc(tripId)
		val existingStops = tripStopRepository.findAllByTripDayTripIdOrderByTripDayDayNumberAscSortOrderAsc(tripId)
		val hasSavedCourse = existingStops.isNotEmpty() || tripDays.any { it.routeOptimizedAt != null }
		if (recommendationInputsChanged && hasSavedCourse && !request.courseResetConfirmed) {
			throw BadRequestException("도시, 날짜 또는 참여 부모를 변경하면 기존 코스가 삭제됩니다. 코스 초기화에 동의해주세요.")
		}

		if (recommendationInputsChanged) {
			if (datesChanged || participantsChanged) {
				tripFeedbackService.resetForTripChange(tripId)
			}
			resetCourse(existingStops, tripDays)
			if (datesChanged) {
				replaceTripDays(trip, tripDays, request.startDate, dayCount)
			}
			if (participantsChanged) {
				tripPledgeService.resetForParticipantChange(tripId)
				replaceTripParticipants(trip, existingParticipants, selectedParents)
			}
		}

		val recommendationSnapshot = if (recommendationInputsChanged) {
			val selectedParentProfiles = resolveSelectedParentProfiles(selectedParents)
			objectMapper.writeValueAsString(
				buildRecommendationSnapshot(
					destination = request.destinationCode,
					startDate = request.startDate,
					endDate = request.endDate,
					selectedParents = selectedParents,
					selectedParentProfiles = selectedParentProfiles,
				),
			)
		} else {
			trip.recommendationSnapshot
		}
		trip.updateInfo(
			title = normalizedTitle,
			destinationCode = request.destinationCode,
			startDate = request.startDate,
			endDate = request.endDate,
			recommendationSnapshot = recommendationSnapshot,
			resetToPlanning = recommendationInputsChanged,
		)
		trip.synchronizeStatus(today)

		return tripDetail(trip)
	}

	@Transactional
	fun saveTripCourse(
		currentUser: AuthenticatedUser,
		tripId: Long,
		request: SaveTripCourseRequest,
	): TripCourseResponse {
		val child = getLoginUser(currentUser.id)
		val trip = tripRepository.findByIdAndDeletedAtIsNull(tripId)
			?: throw NotFoundException("여행을 찾을 수 없습니다.")
		validateTripReadable(currentUser.id, trip)
		validateCourseEditable(child, trip)

		val tripDays = tripDayRepository.findAllByTripIdOrderByDayNumberAsc(tripId)
		val tripDaysByNumber = tripDays.associateBy { it.dayNumber }
		val requestedDayNumbers = request.days.map { it.dayNumber }
		if (requestedDayNumbers.distinct().size != requestedDayNumbers.size) {
			throw BadRequestException("같은 여행 일자를 중복 저장할 수 없습니다.")
		}
		val invalidDayNumber = requestedDayNumbers.firstOrNull { it !in tripDaysByNumber.keys }
		if (invalidDayNumber != null) {
			throw BadRequestException("여행에 존재하지 않는 일자입니다: ${invalidDayNumber}일차")
		}
		tripDays.forEach { it.clearRouteOptimization() }

		val existingStops = tripStopRepository.findAllByTripDayTripIdOrderByTripDayDayNumberAscSortOrderAsc(tripId)
		if (existingStops.isNotEmpty()) {
			tripStopRepository.deleteAllInBatch(existingStops)
			tripStopRepository.flush()
		}

		request.days.forEach { dayRequest ->
			val tripDay = requireNotNull(tripDaysByNumber[dayRequest.dayNumber])
			val stops = dayRequest.stops.mapIndexed { index, stopRequest ->
				TripStop(
					tripDay = tripDay,
					sortOrder = index + 1,
					stopType = stopRequest.stopType,
					sourceProvider = stopRequest.sourceProvider,
					externalPlaceId = stopRequest.externalPlaceId.trimToNull(),
					contentTypeId = stopRequest.contentTypeId.trimToNull(),
					name = stopRequest.name.trim(),
					category = stopRequest.category.trimToNull(),
					address = stopRequest.address.trimToNull(),
					latitude = stopRequest.latitude,
					longitude = stopRequest.longitude,
					phone = stopRequest.phone.trimToNull(),
					homepageUrl = stopRequest.homepageUrl.trimToNull(),
					imageUrl = stopRequest.imageUrl.trimToNull(),
					overview = stopRequest.overview.trimToNull(),
					arrivalTime = stopRequest.arrivalTime,
					dwellMinutes = stopRequest.dwellMinutes,
					note = stopRequest.note.trimToNull(),
					recommendationReason = stopRequest.recommendationReason.trimToNull(),
					recommendationTags = writeRecommendationTags(stopRequest.recommendationTags),
					sourcePayload = stopRequest.sourcePayload?.let { objectMapper.writeValueAsString(it) },
					isManualAdded = stopRequest.isManualAdded,
				)
			}
			tripStopRepository.saveAll(stops)
		}

		return tripCourse(trip, tripDays)
	}

	private fun resolveSelectedParents(familyId: Long, parentUserIds: List<Long>): List<FamilyMember> {
		val distinctParentUserIds = parentUserIds.distinct()
		if (distinctParentUserIds.size != parentUserIds.size) {
			throw BadRequestException("여행 대상 부모는 중복 없이 선택해주세요.")
		}
		if (distinctParentUserIds.isEmpty()) {
			throw BadRequestException("여행 대상 부모를 선택해주세요.")
		}
		if (distinctParentUserIds.size > MAX_PARENT_COUNT) {
			throw BadRequestException("부모는 최대 2명까지 선택할 수 있습니다.")
		}

		val familyParents = familyMemberRepository.findAllByFamilyIdOrderByJoinedAtAsc(familyId)
			.filter { it.memberRole == UserRole.PARENT }
			.associateBy { requireNotNull(it.user.id) }

		return distinctParentUserIds.map { parentUserId ->
			familyParents[parentUserId]
				?: throw BadRequestException("같은 가족에 연결된 부모만 선택할 수 있습니다.")
		}
	}

	private fun resetCourse(existingStops: List<TripStop>, tripDays: List<TripDay>) {
		if (existingStops.isNotEmpty()) {
			tripStopRepository.deleteAllInBatch(existingStops)
			tripStopRepository.flush()
		}
		tripDays.forEach { it.clearRouteOptimization() }
	}

	private fun replaceTripDays(trip: Trip, existingDays: List<TripDay>, startDate: LocalDate, dayCount: Int) {
		if (existingDays.isNotEmpty()) {
			tripDayRepository.deleteAllInBatch(existingDays)
			tripDayRepository.flush()
		}
		tripDayRepository.saveAll(
			(0 until dayCount).map { index ->
				TripDay(
					trip = trip,
					dayNumber = index + 1,
					travelDate = startDate.plusDays(index.toLong()),
				)
			},
		)
	}

	private fun replaceTripParticipants(
		trip: Trip,
		existingParticipants: List<TripParticipant>,
		selectedParents: List<FamilyMember>,
	) {
		if (existingParticipants.isNotEmpty()) {
			tripParticipantRepository.deleteAllInBatch(existingParticipants)
			tripParticipantRepository.flush()
		}
		tripParticipantRepository.save(TripParticipant(trip = trip, user = trip.createdByUser))
		tripParticipantRepository.saveAll(selectedParents.map { TripParticipant(trip = trip, user = it.user) })
	}

	private fun resolveSelectedParentProfiles(selectedParents: List<FamilyMember>): List<ParentProfile> =
		selectedParents.map { parent ->
			val profile = parentProfileRepository.findByUserId(requireNotNull(parent.user.id))
			if (
				profile?.status != ParentProfileStatus.COMPLETED ||
				profile.walkingPace == null ||
				profile.needsMobilityAssistance == null ||
				profile.travelThemes.isEmpty() ||
				profile.foodPreference == null ||
				profile.personalityType == null
			) {
				throw BadRequestException("부모님 상세 프로필 작성이 필요합니다.")
			}
			profile
		}

	private fun buildRecommendationSnapshot(
		destination: TripDestinationCode,
		startDate: LocalDate,
		endDate: LocalDate,
		selectedParents: List<FamilyMember>,
		selectedParentProfiles: List<ParentProfile>,
	): TripRecommendationSnapshotResponse =
		TripRecommendationSnapshotResponse(
			policyVersion = PARENT_TRAVEL_MBTI_POLICY_VERSION,
			capturedAt = LocalDateTime.now(),
			destinationCode = destination,
			startDate = startDate,
			endDate = endDate,
			parents = selectedParents.zip(selectedParentProfiles).map { (parent, profile) ->
				TripParentProfileSnapshotResponse(
					parentUserId = requireNotNull(parent.user.id),
					parentProfileId = requireNotNull(profile.id),
					displayName = parent.user.displayName,
					relationLabel = relationLabelOf(parent.user),
					walkingPace = requireNotNull(profile.walkingPace),
					needsMobilityAssistance = requireNotNull(profile.needsMobilityAssistance),
					travelThemes = profile.travelThemes.map(TravelThemeCode::from)
						.sortedBy { TravelThemeCode.entries.indexOf(it) },
					foodPreference = requireNotNull(profile.foodPreference),
					personalityType = requireNotNull(profile.personalityType),
					profileCompletedAt = profile.completedAt,
				)
			},
		)

	private fun validateDateRange(startDate: LocalDate, endDate: LocalDate): Int {
		if (endDate.isBefore(startDate)) {
			throw BadRequestException("여행 종료일은 시작일 이후여야 합니다.")
		}
		if (startDate.isBefore(LocalDate.now(SERVICE_ZONE_ID))) {
			throw BadRequestException("오늘 또는 이후 날짜를 선택해주세요.")
		}

		return ChronoUnit.DAYS.between(startDate, endDate).toInt() + 1
	}

	private fun validateUpdateDateRange(
		status: TripStatus,
		startDate: LocalDate,
		endDate: LocalDate,
		today: LocalDate,
	): Int {
		if (status != TripStatus.IN_PROGRESS) {
			return validateDateRange(startDate, endDate)
		}
		if (endDate.isBefore(startDate)) {
			throw BadRequestException("여행 종료일은 시작일 이후여야 합니다.")
		}
		if (today.isBefore(startDate) || today.isAfter(endDate)) {
			throw BadRequestException("여행 중 변경한 기간에는 오늘이 포함되어야 합니다.")
		}

		return ChronoUnit.DAYS.between(startDate, endDate).toInt() + 1
	}

	private fun validateTripReadable(userId: Long, trip: Trip) {
		val isCurrentFamilyMember = familyMemberRepository.findByUserId(userId)
			?.let { it.family.isActive && it.family.id == trip.family.id }
			?: false
		val isCompletedTripParticipant = trip.status == TripStatus.COMPLETED &&
			tripParticipantRepository.existsByTripIdAndUserId(requireNotNull(trip.id), userId)
		if (!isCurrentFamilyMember && !isCompletedTripParticipant) {
			throw ForbiddenException("여행 조회 권한이 없습니다.")
		}
	}

	private fun validateTripEditable(user: User, trip: Trip) {
		if (user.role != UserRole.CHILD || trip.createdByUser.id != user.id) {
			throw ForbiddenException("여행을 만든 자녀만 여행 정보를 수정할 수 있습니다.")
		}
		if (trip.status !in EDITABLE_TRIP_STATUSES) {
			throw BadRequestException("완료되거나 보관된 여행은 여행 정보를 수정할 수 없습니다.")
		}
	}

	internal fun validateCourseEditable(user: User, trip: Trip) {
		trip.synchronizeStatus(currentDate())
		if (user.role != UserRole.CHILD || trip.createdByUser.id != user.id) {
			throw ForbiddenException("여행을 만든 자녀만 여행 코스를 수정할 수 있습니다.")
		}
		if (trip.status !in EDITABLE_TRIP_STATUSES) {
			throw BadRequestException("완료되거나 보관된 여행은 여행 코스를 수정할 수 없습니다.")
		}
	}

	private fun tripDetail(trip: Trip): TripDetailResponse {
		val tripId = requireNotNull(trip.id)
		return TripDetailResponse.of(
			trip = trip,
			participants = tripParticipantRepository.findAllByTripIdOrderByIdAsc(tripId),
			days = tripDayRepository.findAllByTripIdOrderByDayNumberAsc(tripId),
			recommendationSnapshot = trip.recommendationSnapshot
				?.let { objectMapper.readValue(it, TripRecommendationSnapshotResponse::class.java) },
		)
	}

	private fun tripCourse(
		trip: Trip,
		tripDays: List<TripDay> = tripDayRepository.findAllByTripIdOrderByDayNumberAsc(requireNotNull(trip.id)),
	): TripCourseResponse {
		val tripId = requireNotNull(trip.id)
		val stopsByDayId = tripStopRepository.findAllByTripDayTripIdOrderByTripDayDayNumberAscSortOrderAsc(tripId)
			.groupBy { requireNotNull(it.tripDay.id) }
		return TripCourseResponse(
			tripId = tripId,
			title = trip.title,
			destination = TripDestinationResponse.from(trip.destinationCode),
			status = trip.status,
			days = tripDays.map { day ->
				TripCourseDayResponse(
					tripDayId = requireNotNull(day.id),
					dayNumber = day.dayNumber,
					travelDate = day.travelDate,
					route = TripRouteSummaryResponse.of(
						day = day,
						polyline = readSourcePayload(day.routePolyline),
						sourcePayload = readSourcePayload(day.routeSourcePayload),
					),
					stops = stopsByDayId[requireNotNull(day.id)]
						.orEmpty()
						.map { stop ->
							TripStopResponse.of(
								stop = stop,
								recommendationTags = readRecommendationTags(stop.recommendationTags),
								sourcePayload = readSourcePayload(stop.sourcePayload),
							)
						},
				)
			},
		)
	}

	private fun writeRecommendationTags(tags: List<String>): String? =
		tags.mapNotNull { it.trimToNull() }
			.takeIf { it.isNotEmpty() }
			?.let { objectMapper.writeValueAsString(it) }

	private fun readRecommendationTags(value: String?): List<String> =
		value?.let { objectMapper.readValue(it, Array<String>::class.java).toList() }
			.orEmpty()

	private fun readSourcePayload(value: String?): JsonNode? =
		value?.let { objectMapper.readValue(it, JsonNode::class.java) }

	private fun validateChild(user: User) {
		if (user.role != UserRole.CHILD) {
			throw BadRequestException("자녀 사용자만 여행을 만들 수 있습니다.")
		}
	}

	private fun getLoginUser(userId: Long): User =
		userRepository.findByIdAndDeletedAtIsNull(userId)
			?.takeIf { it.isSignupCompleted() }
			?: throw UnAuthorizedException("로그인할 수 없는 사용자입니다.")

	private fun currentDate(): LocalDate = LocalDate.now(SERVICE_ZONE_ID)

	companion object {
		private val SERVICE_ZONE_ID: ZoneId = ZoneId.of("Asia/Seoul")
		private val EDITABLE_TRIP_STATUSES = setOf(TripStatus.PLANNING, TripStatus.READY, TripStatus.IN_PROGRESS)
		private const val MAX_PARENT_COUNT = 2
		private const val PARENT_TRAVEL_MBTI_POLICY_VERSION = "parent-travel-mbti-v1"
	}
}

private fun String?.trimToNull(): String? =
	this?.trim()?.takeIf { it.isNotEmpty() }
