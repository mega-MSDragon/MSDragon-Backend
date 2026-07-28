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
) {
	@Transactional
	fun createReport(currentUser: AuthenticatedUser, tripId: Long): FilialReportResponse {
		val user = getLoginUser(currentUser.id)
		val trip = getTrip(tripId)
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
			?: return TripRecordsResponse.empty()
		val familyId = requireNotNull(member.family.id)
		val today = currentDate()
		val records = tripRepository.findAllByFamilyIdAndDeletedAtIsNullOrderByStartDateAscIdAsc(familyId)
			.onEach { it.synchronizeStatus(today) }
			.filter { it.status == TripStatus.COMPLETED }
			.sortedWith(compareByDescending<Trip> { it.endDate }.thenByDescending { requireNotNull(it.id) })
			.map(::recordAggregate)

		if (records.isEmpty()) {
			return TripRecordsResponse.empty(familyId)
		}

		val tripAverages = records.mapNotNull(RecordAggregate::averageRating)
		val averageRating = tripAverages
			.takeIf(List<BigDecimal>::isNotEmpty)
			?.fold(BigDecimal.ZERO, BigDecimal::add)
			?.divide(BigDecimal(tripAverages.size), 1, RoundingMode.HALF_UP)
		val routeDistances = records.mapNotNull(RecordAggregate::totalDistanceMeters)
		val totalDistanceKm = routeDistances
			.takeIf(List<Long>::isNotEmpty)
			?.sum()
			?.let { BigDecimal.valueOf(it).divide(METERS_PER_KILOMETER, 2, RoundingMode.HALF_UP) }

		return TripRecordsResponse(
			familyId = familyId,
			statistics = TripRecordStatisticsResponse(
				completedTripCount = records.size,
				averageRating = averageRating,
				totalPlaceCount = records.sumOf { it.response.totalPlaceCount },
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
			totalScore = report.totalScore,
			satisfactionScore = report.satisfactionScore,
			legComfortScore = report.legComfortScore,
			naggingPreventionScore = report.naggingPreventionScore,
			mealSatisfactionScore = report.mealSatisfactionScore,
			restroomSafetyScore = report.restroomSafetyScore,
			awardTitle = report.awardTitle,
			summary = report.summary,
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
			totalStepCount = report.totalStepCount,
			shareImageUrl = report.shareImageUrl,
			generatedAt = report.generatedAt,
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
		val member = familyMemberRepository.findByUserId(requireNotNull(user.id))
			?: throw ForbiddenException("효도 리포트 조회 권한이 없습니다.")
		if (member.family.id != trip.family.id) {
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
	}
}
