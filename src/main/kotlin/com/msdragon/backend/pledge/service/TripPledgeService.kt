package com.msdragon.backend.pledge.service

import com.msdragon.backend.auth.entity.User
import com.msdragon.backend.auth.entity.UserRole
import com.msdragon.backend.auth.repository.UserRepository
import com.msdragon.backend.auth.support.AuthenticatedUser
import com.msdragon.backend.common.exception.BadRequestException
import com.msdragon.backend.common.exception.ForbiddenException
import com.msdragon.backend.common.exception.InternalServerException
import com.msdragon.backend.common.exception.NotFoundException
import com.msdragon.backend.common.exception.UnAuthorizedException
import com.msdragon.backend.pledge.dto.PledgeTemplateResponse
import com.msdragon.backend.pledge.dto.SaveTripPledgeRequest
import com.msdragon.backend.pledge.dto.TripPledgeCandidatesResponse
import com.msdragon.backend.pledge.dto.TripPledgeResponse
import com.msdragon.backend.pledge.entity.PledgeItem
import com.msdragon.backend.pledge.entity.PledgeTemplate
import com.msdragon.backend.pledge.entity.TripPledge
import com.msdragon.backend.pledge.entity.TripPledgeStatus
import com.msdragon.backend.pledge.repository.PledgeItemRepository
import com.msdragon.backend.pledge.repository.PledgeTemplateRepository
import com.msdragon.backend.pledge.repository.TripPledgeRepository
import com.msdragon.backend.trip.entity.Trip
import com.msdragon.backend.trip.entity.TripStatus
import com.msdragon.backend.trip.repository.TripRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class TripPledgeService(
	private val userRepository: UserRepository,
	private val tripRepository: TripRepository,
	private val pledgeTemplateRepository: PledgeTemplateRepository,
	private val tripPledgeRepository: TripPledgeRepository,
	private val pledgeItemRepository: PledgeItemRepository,
) {
	@Transactional(readOnly = true)
	fun getCandidates(currentUser: AuthenticatedUser, tripId: Long): TripPledgeCandidatesResponse {
		val user = getLoginUser(currentUser.id)
		val trip = getTrip(tripId)
		validatePledgeEditable(user, trip)
		if (tripPledgeRepository.findByTripId(tripId) != null) {
			throw BadRequestException("이미 저장된 여행 10계명이 있습니다.")
		}

		val activeTemplates = pledgeTemplateRepository.findAllByIsActiveTrueOrderByIdAsc()
		if (activeTemplates.size < PLEDGE_ITEM_COUNT) {
			throw InternalServerException("여행 10계명 후보가 부족합니다.")
		}

		return TripPledgeCandidatesResponse(
			tripId = tripId,
			candidates = activeTemplates.shuffled().take(PLEDGE_ITEM_COUNT).map(PledgeTemplateResponse::from),
		)
	}

	@Transactional(readOnly = true)
	fun getPledge(currentUser: AuthenticatedUser, tripId: Long): TripPledgeResponse {
		val user = getLoginUser(currentUser.id)
		val trip = getTrip(tripId)
		validatePledgeCreator(user, trip)
		val pledge = tripPledgeRepository.findByTripId(tripId)
			?: throw NotFoundException("저장된 여행 10계명이 없습니다.")

		return pledgeResponse(pledge)
	}

	@Transactional
	fun saveReviewedPledge(
		currentUser: AuthenticatedUser,
		tripId: Long,
		request: SaveTripPledgeRequest,
	): TripPledgeResponse {
		val user = getLoginUser(currentUser.id)
		val trip = getTrip(tripId)
		validatePledgeEditable(user, trip)
		if (request.items.size != PLEDGE_ITEM_COUNT) {
			throw BadRequestException("여행 10계명은 정확히 10개여야 합니다.")
		}
		validateTemplateIds(request)

		val templateIds = request.items.mapNotNull { it.templateId }.toSet()
		val templatesById = pledgeTemplateRepository.findAllById(templateIds)
			.filter(PledgeTemplate::isActive)
			.associateBy { requireNotNull(it.id) }
		if (templatesById.size != templateIds.size) {
			throw BadRequestException("사용할 수 없는 여행 10계명 템플릿이 포함되어 있습니다.")
		}

		val pledge = tripPledgeRepository.findByTripId(tripId)?.also { existing ->
			if (existing.status !in setOf(TripPledgeStatus.DRAFT, TripPledgeStatus.REVIEWED)) {
				throw BadRequestException("서명 요청 후에는 여행 10계명을 수정할 수 없습니다.")
			}
		} ?: tripPledgeRepository.save(
			TripPledge(
				trip = trip,
				createdByUser = user,
			),
		)
		pledge.review(DEFAULT_TITLE, LocalDateTime.now())

		val existingItems = pledgeItemRepository.findAllByTripPledgeIdOrderBySortOrderAsc(requireNotNull(pledge.id))
		if (existingItems.isNotEmpty()) {
			pledgeItemRepository.deleteAllInBatch(existingItems)
			pledgeItemRepository.flush()
		}
		val savedItems = pledgeItemRepository.saveAll(
			request.items.mapIndexed { index, item ->
				val content = item.content.trim()
				val template = item.templateId?.let(templatesById::get)
				PledgeItem(
					tripPledge = pledge,
					pledgeTemplate = template,
					sortOrder = index + 1,
					content = content,
					isFromTemplate = template?.content == content,
				)
			},
		)

		return TripPledgeResponse.of(pledge, savedItems)
	}

	private fun validateTemplateIds(request: SaveTripPledgeRequest) {
		val templateIds = request.items.mapNotNull { it.templateId }
		if (templateIds.distinct().size != templateIds.size) {
			throw BadRequestException("같은 여행 10계명 템플릿을 중복 사용할 수 없습니다.")
		}
	}

	private fun pledgeResponse(pledge: TripPledge): TripPledgeResponse =
		TripPledgeResponse.of(
			pledge = pledge,
			items = pledgeItemRepository.findAllByTripPledgeIdOrderBySortOrderAsc(requireNotNull(pledge.id)),
		)

	private fun getTrip(tripId: Long): Trip =
		tripRepository.findByIdAndDeletedAtIsNull(tripId)
			?: throw NotFoundException("여행을 찾을 수 없습니다.")

	private fun validatePledgeCreator(user: User, trip: Trip) {
		if (user.role != UserRole.CHILD || trip.createdByUser.id != user.id) {
			throw ForbiddenException("여행을 만든 자녀만 여행 10계명을 작성할 수 있습니다.")
		}
	}

	private fun validatePledgeEditable(user: User, trip: Trip) {
		validatePledgeCreator(user, trip)
		if (trip.status !in setOf(TripStatus.PLANNING, TripStatus.READY)) {
			throw BadRequestException("여행 준비 중에만 여행 10계명을 작성할 수 있습니다.")
		}
	}

	private fun getLoginUser(userId: Long): User =
		userRepository.findByIdAndDeletedAtIsNull(userId)
			?.takeIf { it.isSignupCompleted() }
			?: throw UnAuthorizedException("로그인할 수 없는 사용자입니다.")

	companion object {
		private const val PLEDGE_ITEM_COUNT = 10
		private const val DEFAULT_TITLE = "가족 여행 10계명"
	}
}
