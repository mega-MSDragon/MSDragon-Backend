package com.msdragon.backend.report.service

import com.msdragon.backend.auth.entity.User
import com.msdragon.backend.auth.entity.UserRole
import com.msdragon.backend.auth.repository.UserRepository
import com.msdragon.backend.auth.support.AuthenticatedUser
import com.msdragon.backend.common.exception.BadRequestException
import com.msdragon.backend.common.exception.ForbiddenException
import com.msdragon.backend.common.exception.NotFoundException
import com.msdragon.backend.common.exception.UnAuthorizedException
import com.msdragon.backend.family.repository.FamilyMemberRepository
import com.msdragon.backend.feedback.entity.FeedbackTag
import com.msdragon.backend.feedback.entity.FeedbackTagCategory
import com.msdragon.backend.feedback.entity.TripFeedback
import com.msdragon.backend.feedback.repository.TripFeedbackRepository
import com.msdragon.backend.report.dto.FilialReportBestPlaceResponse
import com.msdragon.backend.report.dto.FilialReportParentFeedbackResponse
import com.msdragon.backend.report.dto.FilialReportResponse
import com.msdragon.backend.report.dto.FilialReportStopResponse
import com.msdragon.backend.report.dto.TripRecordStatisticsResponse
import com.msdragon.backend.report.dto.TripRecordSummaryResponse
import com.msdragon.backend.report.dto.TripRecordsResponse
import com.msdragon.backend.report.entity.FilialReport
import com.msdragon.backend.pledge.repository.PledgeSignatureRepository
import com.msdragon.backend.pledge.repository.TripPledgeRepository
import com.msdragon.backend.report.dto.TripRecordDayResponse
import com.msdragon.backend.report.dto.TripRecordDetailResponse
import com.msdragon.backend.report.dto.TripRecordDetailSummaryResponse
import com.msdragon.backend.report.dto.TripRecordParentRatingResponse
import com.msdragon.backend.report.dto.TripRecordPlaceCountResponse
import com.msdragon.backend.report.dto.TripRecordPledgeResponse
import com.msdragon.backend.report.dto.TripRecordReportResponse
import com.msdragon.backend.report.dto.TripRecordStopResponse
import com.msdragon.backend.report.repository.FilialReportRepository
import com.msdragon.backend.trip.dto.TripDestinationResponse
import com.msdragon.backend.trip.dto.TripParticipantResponse
import com.msdragon.backend.trip.dto.relationLabelOf
import com.msdragon.backend.trip.entity.Trip
import com.msdragon.backend.trip.entity.TripDay
import com.msdragon.backend.trip.entity.TripParticipant
import com.msdragon.backend.trip.entity.TripStatus
import com.msdragon.backend.trip.entity.TripStop
import com.msdragon.backend.trip.repository.TripDayRepository
import com.msdragon.backend.trip.repository.TripParticipantRepository
import com.msdragon.backend.trip.repository.TripRepository
import com.msdragon.backend.trip.repository.TripStopRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

@Service
class FilialReportService(
	private val userRepository: UserRepository,
	private val familyMemberRepository: FamilyMemberRepository,
	private val tripRepository: TripRepository,
	private val tripParticipantRepository: TripParticipantRepository,
	private val tripDayRepository: TripDayRepository,
	private val tripStopRepository: TripStopRepository,
	private val tripFeedbackRepository: TripFeedbackRepository,
	private val filialReportRepository: FilialReportRepository,
	private val tripPledgeRepository: TripPledgeRepository,
	private val pledgeSignatureRepository: PledgeSignatureRepository,
) {
	@Transactional
	fun createReport(currentUser: AuthenticatedUser, tripId: Long): FilialReportResponse {
		val user = getLoginUser(currentUser.id)
		val trip = getTrip(tripId)
		trip.synchronizeStatus(currentDate())
		validateFamilyAccess(user, trip)
		val source = loadSource(tripId)
		validateReportReady(source)
		val report = createOrRefresh(trip, source)
		return reportResponse(report, source)
	}

	@Transactional
	fun getReport(currentUser: AuthenticatedUser, tripId: Long): FilialReportResponse {
		val user = getLoginUser(currentUser.id)
		val trip = getTrip(tripId)
		trip.synchronizeStatus(currentDate())
		validateFamilyAccess(user, trip)
		val report = filialReportRepository.findByTripId(tripId)
			?: throw NotFoundException("생성된 효도 리포트가 없습니다.")
		val source = loadSource(tripId)
		validateReportReady(source)
		refreshReport(report, source)
		return reportResponse(report, source)
	}

	@Transactional
	fun getRecords(currentUser: AuthenticatedUser): TripRecordsResponse {
		val user = getLoginUser(currentUser.id)
		val member = familyMemberRepository.findByUserId(requireNotNull(user.id))
		val familyId = member?.family?.takeIf { it.isActive }?.id
		val today = currentDate()
		// 가족 여행 전체가 아니라 내가 참여한 여행만 집계한다. 합집합으로 두면 나중에 참여자가 된
		// 부모의 기록과 통계에 본인이 빠진 여행이 섞인다. 작성 자녀도 참여자로 저장되고 가족당
		// 자녀는 1명이므로 자녀 쪽 결과는 달라지지 않는다.
		val records = tripParticipantRepository.findAllByUserId(requireNotNull(user.id))
			.map(TripParticipant::trip)
			.filter { it.deletedAt == null }
			.distinctBy { requireNotNull(it.id) }
			.onEach { it.synchronizeStatus(today) }
			.filter { it.status in RECORD_TRIP_STATUSES }
			.sortedWith(compareByDescending<Trip> { it.endDate }.thenByDescending { requireNotNull(it.id) })
			.map(::recordAggregate)

		if (records.isEmpty()) {
			return TripRecordsResponse.empty(familyId)
		}

		val completedRecords = records.filter { it.response.status == TripStatus.COMPLETED }
		val tripAverages = completedRecords.mapNotNull(RecordAggregate::averageRating)
		val averageRating = tripAverages
			.takeIf(List<BigDecimal>::isNotEmpty)
			?.fold(BigDecimal.ZERO, BigDecimal::add)
			?.divide(BigDecimal(tripAverages.size), 1, RoundingMode.HALF_UP)
		val routeDistances = completedRecords.mapNotNull(RecordAggregate::totalDistanceMeters)
		val totalDistanceKm = routeDistances
			.takeIf(List<Long>::isNotEmpty)
			?.sum()
			?.let { BigDecimal.valueOf(it).divide(METERS_PER_KILOMETER, 2, RoundingMode.HALF_UP) }

		return TripRecordsResponse(
			familyId = familyId,
			statistics = TripRecordStatisticsResponse(
				completedTripCount = completedRecords.size,
				averageRating = averageRating,
				totalPlaceCount = completedRecords.sumOf { it.response.totalPlaceCount },
				totalDistanceKm = totalDistanceKm,
			),
			records = records.map(RecordAggregate::response),
		)
	}

	@Transactional
	fun generateIfReady(trip: Trip) {
		val source = loadSource(requireNotNull(trip.id))
		if (isReportReady(source)) {
			createOrRefresh(trip, source)
		}
	}

	@Transactional
	fun deleteForTripChange(tripId: Long) {
		filialReportRepository.deleteByTripId(tripId)
	}

	private fun createOrRefresh(trip: Trip, source: ReportSource): FilialReport {
		val summary = summarize(source)
		val existing = filialReportRepository.findByTripId(requireNotNull(trip.id))
		if (existing != null) {
			existing.refreshCourseSummary(
				coverImageUrl = summary.coverImageUrl,
				totalPlaceCount = summary.totalPlaceCount,
				averageRating = summary.averageRating,
				totalDistanceKm = summary.totalDistanceKm,
			)
			return existing
		}

		return filialReportRepository.save(
			FilialReport(
				trip = trip,
				coverImageUrl = summary.coverImageUrl,
				totalPlaceCount = summary.totalPlaceCount,
				averageRating = summary.averageRating,
				totalDistanceKm = summary.totalDistanceKm,
				generatedAt = currentDateTime(),
			),
		)
	}

	private fun refreshReport(report: FilialReport, source: ReportSource) {
		val summary = summarize(source)
		report.refreshCourseSummary(
			coverImageUrl = summary.coverImageUrl,
			totalPlaceCount = summary.totalPlaceCount,
			averageRating = summary.averageRating,
			totalDistanceKm = summary.totalDistanceKm,
		)
	}

	private fun summarize(source: ReportSource): ReportSummary {
		val stopsById = source.stops.associateBy { requireNotNull(it.id) }
		val coverImageUrl = source.feedbacks.firstNotNullOfOrNull { feedback ->
			stopsById[feedback.bestTripStopId]?.imageUrl?.takeIf(String::isNotBlank)
		} ?: source.stops.firstNotNullOfOrNull { it.imageUrl?.takeIf(String::isNotBlank) }
		val averageRating = source.feedbacks
			.fold(BigDecimal.ZERO) { sum, feedback -> sum + feedback.overallRating }
			.divide(BigDecimal(source.feedbacks.size), 1, RoundingMode.HALF_UP)
		val routeDistances = source.days.mapNotNull(TripDay::routeTotalDistanceMeters)
		val totalDistanceKm = routeDistances
			.takeIf(List<Int>::isNotEmpty)
			?.sumOf(Int::toLong)
			?.let { BigDecimal.valueOf(it).divide(METERS_PER_KILOMETER, 2, RoundingMode.HALF_UP) }

		return ReportSummary(
			coverImageUrl = coverImageUrl,
			totalPlaceCount = source.stops.size,
			averageRating = averageRating,
			totalDistanceKm = totalDistanceKm,
		)
	}

	private fun reportResponse(report: FilialReport, source: ReportSource): FilialReportResponse {
		val tags = source.feedbacks
			.flatMap(TripFeedback::tags)
			.map(FeedbackTag::from)
			.distinct()
			.sortedBy { FeedbackTag.entries.indexOf(it) }
		val stopsById = source.stops.associateBy { requireNotNull(it.id) }

		return FilialReportResponse(
			id = requireNotNull(report.id),
			tripId = requireNotNull(report.trip.id),
			title = report.trip.title,
			destination = TripDestinationResponse.from(report.trip.destinationCode),
			startDate = report.trip.startDate,
			endDate = report.trip.endDate,
			participants = source.participants.map(TripParticipantResponse::from),
			coverImageUrl = report.coverImageUrl,
			totalPlaceCount = report.totalPlaceCount,
			averageRating = report.averageRating,
			totalDistanceKm = report.totalDistanceKm,
			goodTags = tags.filter { it.category == FeedbackTagCategory.GOOD },
			improvementTags = tags.filter { it.category == FeedbackTagCategory.IMPROVEMENT },
			parentFeedbacks = source.feedbacks.map { feedback ->
				FilialReportParentFeedbackResponse(
					parentUserId = requireNotNull(feedback.parentUser.id),
					displayName = feedback.parentUser.displayName,
					relationLabel = relationLabelOf(feedback.parentUser),
					overallRating = feedback.overallRating,
					bodyCondition = feedback.bodyCondition,
					bestPlace = FilialReportBestPlaceResponse(
						tripStopId = feedback.bestTripStopId,
						name = feedback.bestPlaceNameSnapshot,
						imageUrl = stopsById[feedback.bestTripStopId]?.imageUrl,
					),
					freeComment = feedback.freeComment,
					submittedAt = feedback.submittedAt,
				)
			},
			stops = source.stops.map { stop ->
				FilialReportStopResponse(
					tripStopId = requireNotNull(stop.id),
					dayNumber = stop.tripDay.dayNumber,
					sortOrder = stop.sortOrder,
					name = stop.name,
					category = stop.category,
					imageUrl = stop.imageUrl,
				)
			},
			generatedAt = report.generatedAt,
		)
	}

	/**
	 * 기록 상세 화면 한 벌. 여행 상세·피드백·효도 리포트를 각각 부르지 않도록 한 번에 내려준다.
	 * 효도 리포트 본문은 여기 넣지 않는다. `보러가기`를 눌렀을 때만 필요하고 응답이 커진다.
	 */
	@Transactional
	fun getRecordDetail(currentUser: AuthenticatedUser, tripId: Long): TripRecordDetailResponse {
		val user = getLoginUser(currentUser.id)
		val userId = requireNotNull(user.id)
		val trip = getTrip(tripId)
		if (!tripParticipantRepository.existsByTripIdAndUserId(tripId, userId)) {
			throw ForbiddenException("참여한 여행만 기록을 조회할 수 있습니다.")
		}
		trip.synchronizeStatus(currentDate())

		val source = loadSource(tripId)
		val report = filialReportRepository.findByTripId(tripId)
		val stopsByDayId = source.stops.groupBy { requireNotNull(it.tripDay.id) }
		val feedbackByParentId = source.feedbacks.associateBy { requireNotNull(it.parentUser.id) }
		val parents = source.participants.filter { it.user.role == UserRole.PARENT }
		val signedUserIds = tripPledgeRepository.findByTripId(tripId)
			?.let { pledge ->
				pledgeSignatureRepository.findAllByTripPledgeIdOrderBySignedAtAsc(requireNotNull(pledge.id))
					.mapTo(mutableSetOf()) { requireNotNull(it.user.id) }
			}

		return TripRecordDetailResponse(
			tripId = tripId,
			title = trip.title,
			coverImageUrl = report?.coverImageUrl?.takeIf(String::isNotBlank)
				?: source.stops.firstNotNullOfOrNull { it.imageUrl?.takeIf(String::isNotBlank) },
			status = trip.status,
			destination = TripDestinationResponse.from(trip.destinationCode),
			startDate = trip.startDate,
			endDate = trip.endDate,
			participants = source.participants.map(TripParticipantResponse::from),
			summary = recordDetailSummary(source, parents, feedbackByParentId),
			days = source.days.map { day ->
				TripRecordDayResponse(
					dayNumber = day.dayNumber,
					travelDate = day.travelDate,
					stops = stopsByDayId[requireNotNull(day.id)].orEmpty().map { stop ->
						TripRecordStopResponse(
							tripStopId = requireNotNull(stop.id),
							sortOrder = stop.sortOrder,
							name = stop.name,
							category = stop.category,
							note = stop.note?.takeIf(String::isNotBlank),
							latitude = stop.latitude,
							longitude = stop.longitude,
						)
					},
				)
			},
			pledge = TripRecordPledgeResponse(
				exists = signedUserIds != null,
				allSigned = signedUserIds != null &&
					source.participants.all { requireNotNull(it.user.id) in signedUserIds },
				signedParticipants = source.participants
					.filter { requireNotNull(it.user.id) in signedUserIds.orEmpty() }
					.map(TripParticipantResponse::from),
				pendingParticipants = source.participants
					.filterNot { requireNotNull(it.user.id) in signedUserIds.orEmpty() }
					.map(TripParticipantResponse::from)
					.takeIf { signedUserIds != null }
					.orEmpty(),
			),
			report = TripRecordReportResponse(
				ready = report != null,
				submittedParentCount = parents.count { requireNotNull(it.user.id) in feedbackByParentId },
				totalParentCount = parents.size,
				submittedParents = parents
					.filter { requireNotNull(it.user.id) in feedbackByParentId }
					.map(TripParticipantResponse::from),
				pendingParents = parents
					.filterNot { requireNotNull(it.user.id) in feedbackByParentId }
					.map(TripParticipantResponse::from),
			),
			// 삭제 권한은 여행 삭제 API와 같다. 권한이 없으면 클라이언트가 휴지통을 숨긴다.
			canDelete = user.role == UserRole.CHILD && trip.createdByUser.id == userId,
		)
	}

	private fun recordDetailSummary(
		source: ReportSource,
		parents: List<TripParticipant>,
		feedbackByParentId: Map<Long, TripFeedback>,
	): TripRecordDetailSummaryResponse {
		val submitted = parents.mapNotNull { feedbackByParentId[requireNotNull(it.user.id)] }
		val averageRating = submitted
			.takeIf(List<TripFeedback>::isNotEmpty)
			?.fold(BigDecimal.ZERO) { sum, feedback -> sum + feedback.overallRating }
			?.divide(BigDecimal(submitted.size), 4, RoundingMode.HALF_UP)
			?.setScale(1, RoundingMode.HALF_UP)
		val routeDistances = source.days.mapNotNull(TripDay::routeTotalDistanceMeters)

		return TripRecordDetailSummaryResponse(
			totalDistanceKm = routeDistances
				.takeIf(List<Int>::isNotEmpty)
				?.sumOf(Int::toLong)
				?.toBigDecimal()
				?.divide(BigDecimal(1_000), 1, RoundingMode.HALF_UP),
			totalPlaceCount = source.stops.size,
			// 방문 순서대로 처음 나온 카테고리부터 센다. 카테고리가 없는 방문지는 제외한다.
			placeCounts = source.stops
				.mapNotNull { it.category?.takeIf(String::isNotBlank) }
				.groupingBy { it }
				.eachCount()
				.map { (category, count) -> TripRecordPlaceCountResponse(category, count) },
			averageRating = averageRating,
			parentRatings = parents.mapNotNull { participant ->
				val feedback = feedbackByParentId[requireNotNull(participant.user.id)] ?: return@mapNotNull null
				TripRecordParentRatingResponse(
					parentUserId = requireNotNull(participant.user.id),
					displayName = participant.user.displayName,
					relationLabel = TripParticipantResponse.from(participant).relationLabel,
					overallRating = feedback.overallRating,
				)
			},
		)
	}

	private fun recordAggregate(trip: Trip): RecordAggregate {
		val tripId = requireNotNull(trip.id)
		val source = loadSource(tripId)
		val report = filialReportRepository.findByTripId(tripId)
		val averageRating = source.feedbacks
			.takeIf(List<TripFeedback>::isNotEmpty)
			?.fold(BigDecimal.ZERO) { sum, feedback -> sum + feedback.overallRating }
			?.divide(BigDecimal(source.feedbacks.size), 4, RoundingMode.HALF_UP)
		val routeDistances = source.days.mapNotNull(TripDay::routeTotalDistanceMeters)
		val totalDistanceMeters = routeDistances
			.takeIf(List<Int>::isNotEmpty)
			?.sumOf(Int::toLong)
		val coverImageUrl = report?.coverImageUrl?.takeIf(String::isNotBlank)
			?: source.stops.firstNotNullOfOrNull { it.imageUrl?.takeIf(String::isNotBlank) }

		return RecordAggregate(
			response = TripRecordSummaryResponse(
				tripId = tripId,
				title = trip.title,
				destination = TripDestinationResponse.from(trip.destinationCode),
				startDate = trip.startDate,
				endDate = trip.endDate,
				status = trip.status,
				participants = source.participants.map(TripParticipantResponse::from),
				coverImageUrl = coverImageUrl,
				totalPlaceCount = source.stops.size,
				averageRating = averageRating?.setScale(1, RoundingMode.HALF_UP),
				reportReady = report != null,
			),
			averageRating = averageRating,
			totalDistanceMeters = totalDistanceMeters,
		)
	}

	private fun loadSource(tripId: Long): ReportSource =
		ReportSource(
			participants = tripParticipantRepository.findAllByTripIdOrderByIdAsc(tripId),
			days = tripDayRepository.findAllByTripIdOrderByDayNumberAsc(tripId),
			stops = tripStopRepository.findAllByTripDayTripIdOrderByTripDayDayNumberAscSortOrderAsc(tripId),
			feedbacks = tripFeedbackRepository.findAllByTripIdOrderByParentUserIdAsc(tripId),
		)

	private fun validateReportReady(source: ReportSource) {
		if (!isReportReady(source)) {
			throw BadRequestException("모든 참여 부모가 피드백을 제출한 후 효도 리포트를 생성할 수 있습니다.")
		}
	}

	private fun isReportReady(source: ReportSource): Boolean {
		val parentIds = source.participants
			.filter { it.user.role == UserRole.PARENT }
			.mapTo(mutableSetOf()) { requireNotNull(it.user.id) }
		val feedbackParentIds = source.feedbacks
			.mapTo(mutableSetOf()) { requireNotNull(it.parentUser.id) }
		return parentIds.isNotEmpty() && parentIds == feedbackParentIds
	}

	private fun validateFamilyAccess(user: User, trip: Trip) {
		val userId = requireNotNull(user.id)
		val isCurrentFamilyMember = familyMemberRepository.findByUserId(userId)
			?.let { it.family.isActive && it.family.id == trip.family.id }
			?: false
		val isCompletedTripParticipant = trip.status == TripStatus.COMPLETED &&
			tripParticipantRepository.existsByTripIdAndUserId(requireNotNull(trip.id), userId)
		if (!isCurrentFamilyMember && !isCompletedTripParticipant) {
			throw ForbiddenException("효도 리포트 조회 권한이 없습니다.")
		}
	}

	private fun getTrip(tripId: Long): Trip =
		tripRepository.findByIdAndDeletedAtIsNull(tripId)
			?: throw NotFoundException("여행을 찾을 수 없습니다.")

	private fun getLoginUser(userId: Long): User =
		userRepository.findByIdAndDeletedAtIsNull(userId)
			?.takeIf { it.isSignupCompleted() }
			?: throw UnAuthorizedException("로그인할 수 없는 사용자입니다.")

	private fun currentDateTime(): LocalDateTime =
		LocalDateTime.now(SERVICE_ZONE_ID).truncatedTo(ChronoUnit.MICROS)

	private fun currentDate(): LocalDate = LocalDate.now(SERVICE_ZONE_ID)

	private data class ReportSource(
		val participants: List<TripParticipant>,
		val days: List<TripDay>,
		val stops: List<TripStop>,
		val feedbacks: List<TripFeedback>,
	)

	private data class ReportSummary(
		val coverImageUrl: String?,
		val totalPlaceCount: Int,
		val averageRating: BigDecimal,
		val totalDistanceKm: BigDecimal?,
	)

	private data class RecordAggregate(
		val response: TripRecordSummaryResponse,
		val averageRating: BigDecimal?,
		val totalDistanceMeters: Long?,
	)

	companion object {
		private val SERVICE_ZONE_ID: ZoneId = ZoneId.of("Asia/Seoul")
		private val METERS_PER_KILOMETER = BigDecimal("1000")
		private val RECORD_TRIP_STATUSES = setOf(TripStatus.COMPLETED, TripStatus.STOPPED)
	}
}
