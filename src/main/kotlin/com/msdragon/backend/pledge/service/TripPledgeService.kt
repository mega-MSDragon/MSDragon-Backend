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
import com.msdragon.backend.pledge.dto.SavePledgeSignatureRequest
import com.msdragon.backend.pledge.dto.SaveTripPledgeRequest
import com.msdragon.backend.pledge.dto.TripPledgeCandidatesResponse
import com.msdragon.backend.pledge.dto.TripPledgeResponse
import com.msdragon.backend.pledge.entity.PledgeItem
import com.msdragon.backend.pledge.entity.PledgeSignature
import com.msdragon.backend.pledge.entity.PledgeTemplate
import com.msdragon.backend.pledge.entity.TripPledge
import com.msdragon.backend.pledge.entity.TripPledgeStatus
import com.msdragon.backend.pledge.repository.PledgeItemRepository
import com.msdragon.backend.pledge.repository.PledgeSignatureRepository
import com.msdragon.backend.pledge.repository.PledgeTemplateRepository
import com.msdragon.backend.pledge.repository.TripPledgeRepository
import com.msdragon.backend.trip.entity.Trip
import com.msdragon.backend.trip.entity.TripStatus
import com.msdragon.backend.trip.repository.TripRepository
import com.msdragon.backend.trip.repository.TripParticipantRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.Base64

@Service
class TripPledgeService(
	private val userRepository: UserRepository,
	private val tripRepository: TripRepository,
	private val pledgeTemplateRepository: PledgeTemplateRepository,
	private val tripPledgeRepository: TripPledgeRepository,
	private val pledgeItemRepository: PledgeItemRepository,
	private val pledgeSignatureRepository: PledgeSignatureRepository,
	private val tripParticipantRepository: TripParticipantRepository,
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
		val pledge = tripPledgeRepository.findByTripId(tripId)
			?: throw NotFoundException("저장된 여행 10계명이 없습니다.")
		validatePledgeViewer(user, trip, pledge)

		return pledgeResponse(pledge, user)
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

		return TripPledgeResponse.of(
			pledge = pledge,
			items = savedItems,
			signatures = emptyList(),
			canSign = true,
		)
	}

	@Transactional
	fun saveSignature(
		currentUser: AuthenticatedUser,
		tripId: Long,
		request: SavePledgeSignatureRequest,
	): TripPledgeResponse {
		val user = getLoginUser(currentUser.id)
		val trip = getTrip(tripId)
		val pledge = tripPledgeRepository.findByTripId(tripId)
			?: throw NotFoundException("저장된 여행 10계명이 없습니다.")
		val pledgeId = requireNotNull(pledge.id)

		if (pledgeSignatureRepository.findByTripPledgeIdAndUserId(pledgeId, requireNotNull(user.id)) != null) {
			throw BadRequestException("이미 여행 10계명에 서명했습니다.")
		}
		validateSigner(user, trip, pledge)

		val signedAt = LocalDateTime.now()
		pledgeSignatureRepository.save(
			PledgeSignature(
				tripPledge = pledge,
				user = user,
				signatureImageData = decodePng(request.signatureImageBase64),
				signatureMimeType = PNG_MIME_TYPE,
				signedAt = signedAt,
			),
		)

		when (user.role) {
			UserRole.CHILD -> pledge.requestSignatures(signedAt)
			UserRole.PARENT -> pledge.complete(signedAt)
		}

		return pledgeResponse(pledge, user)
	}

	@Transactional
	fun resetForParticipantChange(tripId: Long) {
		val pledge = tripPledgeRepository.findByTripId(tripId) ?: return
		val pledgeId = requireNotNull(pledge.id)
		val signatures = pledgeSignatureRepository.findAllByTripPledgeIdOrderBySignedAtAsc(pledgeId)
		if (signatures.isNotEmpty()) {
			pledgeSignatureRepository.deleteAll(signatures)
			pledgeSignatureRepository.flush()
		}
		val items = pledgeItemRepository.findAllByTripPledgeIdOrderBySortOrderAsc(pledgeId)
		if (items.isNotEmpty()) {
			pledgeItemRepository.deleteAll(items)
			pledgeItemRepository.flush()
		}
		tripPledgeRepository.delete(pledge)
		tripPledgeRepository.flush()
	}

	private fun validateTemplateIds(request: SaveTripPledgeRequest) {
		val templateIds = request.items.mapNotNull { it.templateId }
		if (templateIds.distinct().size != templateIds.size) {
			throw BadRequestException("같은 여행 10계명 템플릿을 중복 사용할 수 없습니다.")
		}
	}

	private fun pledgeResponse(pledge: TripPledge, currentUser: User): TripPledgeResponse {
		val pledgeId = requireNotNull(pledge.id)
		val signatures = pledgeSignatureRepository.findAllByTripPledgeIdOrderBySignedAtAsc(pledgeId)
			.sortedWith(
				compareBy<PledgeSignature> { if (it.user.role == UserRole.CHILD) 0 else 1 }
					.thenBy(PledgeSignature::signedAt),
			)
		return TripPledgeResponse.of(
			pledge = pledge,
			items = pledgeItemRepository.findAllByTripPledgeIdOrderBySortOrderAsc(pledgeId),
			signatures = signatures,
			canSign = canSign(currentUser, pledge, signatures),
		)
	}

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

	private fun validatePledgeViewer(user: User, trip: Trip, pledge: TripPledge) {
		if (user.role == UserRole.CHILD && trip.createdByUser.id == user.id) {
			return
		}
		if (user.role != UserRole.PARENT || !isTripParticipant(trip, user)) {
			throw ForbiddenException("여행 참여자만 여행 10계명을 조회할 수 있습니다.")
		}
		if (pledge.status !in setOf(TripPledgeStatus.SIGNATURE_REQUESTED, TripPledgeStatus.COMPLETED)) {
			throw ForbiddenException("자녀가 서명을 요청한 후 여행 10계명을 조회할 수 있습니다.")
		}
	}

	private fun validateSigner(user: User, trip: Trip, pledge: TripPledge) {
		when (user.role) {
			UserRole.CHILD -> {
				validatePledgeEditable(user, trip)
				if (pledge.status != TripPledgeStatus.REVIEWED) {
					throw BadRequestException("내용 확인이 완료된 여행 10계명에만 서명할 수 있습니다.")
				}
			}

			UserRole.PARENT -> {
				if (!isTripParticipant(trip, user)) {
					throw ForbiddenException("여행 참여자만 여행 10계명에 서명할 수 있습니다.")
				}
				if (pledge.status !in setOf(TripPledgeStatus.SIGNATURE_REQUESTED, TripPledgeStatus.COMPLETED)) {
					throw BadRequestException("자녀가 먼저 여행 10계명에 서명해야 합니다.")
				}
			}
		}
	}

	private fun canSign(user: User, pledge: TripPledge, signatures: List<PledgeSignature>): Boolean {
		if (signatures.any { it.user.id == user.id }) {
			return false
		}
		return when (user.role) {
			UserRole.CHILD ->
				pledge.trip.createdByUser.id == user.id &&
					pledge.trip.status in setOf(TripStatus.PLANNING, TripStatus.READY) &&
					pledge.status == TripPledgeStatus.REVIEWED

			UserRole.PARENT ->
				isTripParticipant(pledge.trip, user) &&
					pledge.status in setOf(TripPledgeStatus.SIGNATURE_REQUESTED, TripPledgeStatus.COMPLETED)
		}
	}

	private fun isTripParticipant(trip: Trip, user: User): Boolean =
		tripParticipantRepository.existsByTripIdAndUserId(requireNotNull(trip.id), requireNotNull(user.id))

	private fun decodePng(value: String): ByteArray {
		val decoded = try {
			Base64.getDecoder().decode(value.trim())
		} catch (_: IllegalArgumentException) {
			throw BadRequestException("올바른 Base64 서명 이미지가 아닙니다.")
		}
		if (decoded.size > MAX_SIGNATURE_IMAGE_BYTES) {
			throw BadRequestException("서명 이미지가 허용 크기를 초과했습니다.")
		}
		if (decoded.size < PNG_HEADER.size || PNG_HEADER.indices.any { decoded[it] != PNG_HEADER[it] }) {
			throw BadRequestException("PNG 형식의 서명 이미지만 사용할 수 있습니다.")
		}
		return decoded
	}

	private fun getLoginUser(userId: Long): User =
		userRepository.findByIdAndDeletedAtIsNull(userId)
			?.takeIf { it.isSignupCompleted() }
			?: throw UnAuthorizedException("로그인할 수 없는 사용자입니다.")

	companion object {
		private const val PLEDGE_ITEM_COUNT = 10
		private const val DEFAULT_TITLE = "가족 여행 10계명"
		private const val PNG_MIME_TYPE = "image/png"
		private const val MAX_SIGNATURE_IMAGE_BYTES = 512 * 1024
		private val PNG_HEADER = byteArrayOf(
			0x89.toByte(),
			0x50,
			0x4E,
			0x47,
			0x0D,
			0x0A,
			0x1A,
			0x0A,
		)
	}
}
