package com.msdragon.backend.feedback.service

import com.msdragon.backend.auth.entity.User
import com.msdragon.backend.auth.entity.UserRole
import com.msdragon.backend.auth.repository.UserRepository
import com.msdragon.backend.auth.support.AuthenticatedUser
import com.msdragon.backend.common.exception.BadRequestException
import com.msdragon.backend.common.exception.ForbiddenException
import com.msdragon.backend.common.exception.NotFoundException
import com.msdragon.backend.common.exception.UnAuthorizedException
import com.msdragon.backend.feedback.dto.SubmitTripFeedbackRequest
import com.msdragon.backend.feedback.dto.TripFeedbackBestPlaceResponse
import com.msdragon.backend.feedback.dto.TripFeedbackResponse
import com.msdragon.backend.feedback.dto.TripFeedbackStatusResponse
import com.msdragon.backend.feedback.dto.TripParentFeedbackStatusResponse
import com.msdragon.backend.feedback.entity.FeedbackTag
import com.msdragon.backend.feedback.entity.FeedbackTagCategory
import com.msdragon.backend.feedback.entity.TripFeedback
import com.msdragon.backend.feedback.entity.TripFeedbackRequest
import com.msdragon.backend.feedback.repository.TripFeedbackRepository
import com.msdragon.backend.feedback.repository.TripFeedbackRequestRepository
import com.msdragon.backend.report.service.FilialReportService
import com.msdragon.backend.trip.dto.relationLabelOf
import com.msdragon.backend.trip.entity.Trip
import com.msdragon.backend.trip.repository.TripParticipantRepository
import com.msdragon.backend.trip.repository.TripRepository
import com.msdragon.backend.trip.repository.TripStopRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

@Service
class TripFeedbackService(
	private val userRepository: UserRepository,
	private val tripRepository: TripRepository,
	private val tripParticipantRepository: TripParticipantRepository,
	private val tripStopRepository: TripStopRepository,
	private val tripFeedbackRequestRepository: TripFeedbackRequestRepository,
	private val tripFeedbackRepository: TripFeedbackRepository,
	private val filialReportService: FilialReportService,
) {
	@Transactional
	fun requestFeedback(currentUser: AuthenticatedUser, tripId: Long): TripFeedbackStatusResponse {
		val child = getLoginUser(currentUser.id)
		val trip = getTrip(tripId)
		validateTripCreator(child, trip)
		val today = currentDate()
		trip.synchronizeStatus(today)
		validateFeedbackAvailable(trip, today)

		val parents = participatingParents(tripId)
		val submittedParentIds = tripFeedbackRepository.findAllByTripIdOrderByParentUserIdAsc(tripId)
			.mapTo(mutableSetOf()) { requireNotNull(it.parentUser.id) }
		val requestedParentIds = tripFeedbackRequestRepository.findAllByTripIdOrderByParentUserIdAsc(tripId)
			.mapTo(mutableSetOf()) { requireNotNull(it.parentUser.id) }
		val requestedAt = currentDateTime()
		val newRequests = parents
			.filter { parent ->
				val parentId = requireNotNull(parent.id)
				parentId !in submittedParentIds && parentId !in requestedParentIds
			}
			.map { parent ->
				TripFeedbackRequest(
					trip = trip,
					requestedByUser = child,
					parentUser = parent,
					requestedAt = requestedAt,
				)
			}
		if (newRequests.isNotEmpty()) {
			tripFeedbackRequestRepository.saveAll(newRequests)
		}

		return feedbackStatus(child, trip, today)
	}

	@Transactional
	fun getStatus(currentUser: AuthenticatedUser, tripId: Long): TripFeedbackStatusResponse {
		val user = getLoginUser(currentUser.id)
		val trip = getTrip(tripId)
		validateStatusViewer(user, trip)
		val today = currentDate()
		trip.synchronizeStatus(today)

		return feedbackStatus(user, trip, today)
	}

	@Transactional
	fun submitFeedback(
		currentUser: AuthenticatedUser,
		tripId: Long,
		request: SubmitTripFeedbackRequest,
	): TripFeedbackResponse {
		val parent = getLoginUser(currentUser.id)
		val trip = getTrip(tripId)
		validateParticipatingParent(parent, trip)
		val today = currentDate()
		trip.synchronizeStatus(today)
		validateFeedbackAvailable(trip, today)

		val parentId = requireNotNull(parent.id)
		if (tripFeedbackRepository.findByTripIdAndParentUserId(tripId, parentId) != null) {
			throw BadRequestException("이미 여행 피드백을 제출했습니다.")
		}
		validateRating(request.overallRating)
		validateTags(request.goodTags, request.improvementTags)

		val bestStop = tripStopRepository.findById(request.bestTripStopId).orElse(null)
			?.takeIf { it.tripDay.trip.id == tripId }
			?: throw BadRequestException("해당 여행에 포함된 방문지만 베스트 장소로 선택할 수 있습니다.")
		val tags = (request.goodTags + request.improvementTags)
			.mapTo(mutableSetOf(), FeedbackTag::value)
		val feedback = tripFeedbackRepository.saveAndFlush(
			TripFeedback(
				trip = trip,
				parentUser = parent,
				overallRating = request.overallRating.setScale(1),
				bodyCondition = request.bodyCondition,
				bestTripStopId = requireNotNull(bestStop.id),
				bestPlaceNameSnapshot = bestStop.name,
				freeComment = request.freeComment?.trim()?.takeIf(String::isNotEmpty),
				tags = tags,
				submittedAt = currentDateTime(),
			),
		)
		val reportReady = reportReady(tripId)
		if (reportReady) {
			filialReportService.generateIfReady(trip)
		}

		return feedbackResponse(feedback, reportReady)
	}

	@Transactional(readOnly = true)
	fun getMyFeedback(currentUser: AuthenticatedUser, tripId: Long): TripFeedbackResponse {
		val parent = getLoginUser(currentUser.id)
		val trip = getTrip(tripId)
		validateParticipatingParent(parent, trip)
		val feedback = tripFeedbackRepository.findByTripIdAndParentUserId(tripId, requireNotNull(parent.id))
			?: throw NotFoundException("제출한 여행 피드백이 없습니다.")

		return feedbackResponse(feedback, reportReady(tripId))
	}

	@Transactional
	fun resetForTripChange(tripId: Long) {
		filialReportService.deleteForTripChange(tripId)
		val feedbacks = tripFeedbackRepository.findAllByTripIdOrderByParentUserIdAsc(tripId)
		if (feedbacks.isNotEmpty()) {
			tripFeedbackRepository.deleteAll(feedbacks)
			tripFeedbackRepository.flush()
		}
		val requests = tripFeedbackRequestRepository.findAllByTripIdOrderByParentUserIdAsc(tripId)
		if (requests.isNotEmpty()) {
			tripFeedbackRequestRepository.deleteAll(requests)
			tripFeedbackRequestRepository.flush()
		}
	}

	private fun feedbackStatus(user: User, trip: Trip, today: LocalDate): TripFeedbackStatusResponse {
		val tripId = requireNotNull(trip.id)
		val parents = participatingParents(tripId)
		val requestsByParentId = tripFeedbackRequestRepository.findAllByTripIdOrderByParentUserIdAsc(tripId)
			.associateBy { requireNotNull(it.parentUser.id) }
		val feedbacksByParentId = tripFeedbackRepository.findAllByTripIdOrderByParentUserIdAsc(tripId)
			.associateBy { requireNotNull(it.parentUser.id) }
		val feedbackAvailable = !today.isBefore(trip.endDate)
		val currentUserId = requireNotNull(user.id)
		val isCreator = user.role == UserRole.CHILD && trip.createdByUser.id == currentUserId
		val isParticipatingParent = user.role == UserRole.PARENT && parents.any { it.id == currentUserId }
		val submittedParentCount = feedbacksByParentId.size

		return TripFeedbackStatusResponse(
			tripId = tripId,
			feedbackAvailable = feedbackAvailable,
			totalParentCount = parents.size,
			requestedParentCount = requestsByParentId.size,
			submittedParentCount = submittedParentCount,
			canRequest = isCreator && feedbackAvailable && submittedParentCount < parents.size,
			canSubmit = isParticipatingParent && feedbackAvailable && currentUserId !in feedbacksByParentId,
			reportReady = parents.isNotEmpty() && submittedParentCount == parents.size,
			parents = parents.map { parent ->
				val parentId = requireNotNull(parent.id)
				TripParentFeedbackStatusResponse(
					parentUserId = parentId,
					displayName = parent.displayName,
					relationLabel = relationLabelOf(parent),
					requestedAt = requestsByParentId[parentId]?.requestedAt,
					submittedAt = feedbacksByParentId[parentId]?.submittedAt,
				)
			},
		)
	}

	private fun feedbackResponse(feedback: TripFeedback, reportReady: Boolean): TripFeedbackResponse {
		val tags = feedback.tags.map(FeedbackTag::from)
			.sortedBy(FeedbackTag.entries::indexOf)
		return TripFeedbackResponse(
			id = requireNotNull(feedback.id),
			tripId = requireNotNull(feedback.trip.id),
			parentUserId = requireNotNull(feedback.parentUser.id),
			overallRating = feedback.overallRating,
			bodyCondition = feedback.bodyCondition,
			goodTags = tags.filter { it.category == FeedbackTagCategory.GOOD },
			improvementTags = tags.filter { it.category == FeedbackTagCategory.IMPROVEMENT },
			bestPlace = TripFeedbackBestPlaceResponse(
				tripStopId = feedback.bestTripStopId,
				name = feedback.bestPlaceNameSnapshot,
			),
			freeComment = feedback.freeComment,
			submittedAt = feedback.submittedAt,
			reportReady = reportReady,
		)
	}

	private fun reportReady(tripId: Long): Boolean {
		val parentCount = participatingParents(tripId).size
		return parentCount > 0 && tripFeedbackRepository.countByTripId(tripId) == parentCount.toLong()
	}

	private fun participatingParents(tripId: Long): List<User> =
		tripParticipantRepository.findAllByTripIdOrderByIdAsc(tripId)
			.map { it.user }
			.filter { it.role == UserRole.PARENT }

	private fun validateRating(rating: BigDecimal) {
		if (rating.remainder(RATING_STEP).compareTo(BigDecimal.ZERO) != 0) {
			throw BadRequestException("전체 만족도는 0.5 단위로 입력해주세요.")
		}
	}

	private fun validateTags(goodTags: List<FeedbackTag>, improvementTags: List<FeedbackTag>) {
		val allTags = goodTags + improvementTags
		if (allTags.distinct().size != allTags.size) {
			throw BadRequestException("피드백 태그는 중복 없이 선택해주세요.")
		}
		if (goodTags.any { it.category != FeedbackTagCategory.GOOD }) {
			throw BadRequestException("좋았던 점에 사용할 수 없는 태그가 포함되어 있습니다.")
		}
		if (improvementTags.any { it.category != FeedbackTagCategory.IMPROVEMENT }) {
			throw BadRequestException("개선할 점에 사용할 수 없는 태그가 포함되어 있습니다.")
		}
	}

	private fun validateTripCreator(user: User, trip: Trip) {
		if (user.role != UserRole.CHILD || trip.createdByUser.id != user.id) {
			throw ForbiddenException("여행을 만든 자녀만 부모 평가를 요청할 수 있습니다.")
		}
	}

	private fun validateStatusViewer(user: User, trip: Trip) {
		if (user.role == UserRole.CHILD && trip.createdByUser.id == user.id) {
			return
		}
		if (user.role == UserRole.PARENT && isTripParticipant(trip, user)) {
			return
		}
		throw ForbiddenException("여행 참여자만 피드백 현황을 조회할 수 있습니다.")
	}

	private fun validateParticipatingParent(user: User, trip: Trip) {
		if (user.role != UserRole.PARENT || !isTripParticipant(trip, user)) {
			throw ForbiddenException("여행에 참여한 부모만 피드백을 작성할 수 있습니다.")
		}
	}

	private fun validateFeedbackAvailable(trip: Trip, today: LocalDate) {
		if (today.isBefore(trip.endDate)) {
			throw BadRequestException("여행 마지막 날부터 피드백을 작성할 수 있습니다.")
		}
	}

	private fun isTripParticipant(trip: Trip, user: User): Boolean =
		tripParticipantRepository.existsByTripIdAndUserId(requireNotNull(trip.id), requireNotNull(user.id))

	private fun getTrip(tripId: Long): Trip =
		tripRepository.findByIdAndDeletedAtIsNull(tripId)
			?: throw NotFoundException("여행을 찾을 수 없습니다.")

	private fun getLoginUser(userId: Long): User =
		userRepository.findByIdAndDeletedAtIsNull(userId)
			?.takeIf { it.isSignupCompleted() }
			?: throw UnAuthorizedException("로그인할 수 없는 사용자입니다.")

	private fun currentDate(): LocalDate = LocalDate.now(SERVICE_ZONE_ID)

	private fun currentDateTime(): LocalDateTime =
		LocalDateTime.now(SERVICE_ZONE_ID).truncatedTo(ChronoUnit.MICROS)

	companion object {
		private val SERVICE_ZONE_ID: ZoneId = ZoneId.of("Asia/Seoul")
		private val RATING_STEP = BigDecimal("0.5")
	}
}
