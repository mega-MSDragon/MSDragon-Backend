package com.msdragon.backend.pledge.dto

import com.msdragon.backend.auth.entity.UserRole
import com.msdragon.backend.pledge.entity.PledgeItem
import com.msdragon.backend.pledge.entity.PledgeSignature
import com.msdragon.backend.pledge.entity.PledgeTemplate
import com.msdragon.backend.pledge.entity.TripPledge
import com.msdragon.backend.pledge.entity.TripPledgeStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime
import java.util.Base64

@Schema(description = "여행 10계명 후보 응답")
data class TripPledgeCandidatesResponse(
	@field:Schema(description = "여행 ID", example = "1")
	val tripId: Long,

	@field:Schema(description = "중복 없이 무작위 선택된 활성 후보 10개")
	val candidates: List<PledgeTemplateResponse>,
)

@Schema(description = "여행 10계명 템플릿 후보")
data class PledgeTemplateResponse(
	@field:Schema(description = "템플릿 ID", example = "1")
	val id: Long,

	@field:Schema(description = "후보 문구", example = "서로 재촉하지 않기")
	val content: String,
) {
	companion object {
		fun from(template: PledgeTemplate): PledgeTemplateResponse =
			PledgeTemplateResponse(
				id = requireNotNull(template.id),
				content = template.content,
			)
	}
}

@Schema(description = "여행별 10계명 확정본 응답")
data class TripPledgeResponse(
	@field:Schema(description = "여행 10계명 ID", example = "1")
	val id: Long,

	@field:Schema(description = "여행 ID", example = "1")
	val tripId: Long,

	@field:Schema(description = "문서 제목", example = "가족 여행 10계명", nullable = true)
	val title: String?,

	@field:Schema(
		description = "10계명 진행 상태",
		example = "reviewed",
		allowableValues = ["draft", "reviewed", "signature_requested", "completed"],
	)
	val status: TripPledgeStatus,

	@field:Schema(description = "확정된 10개 항목")
	val items: List<TripPledgeItemResponse>,

	@field:Schema(description = "내용 확인 시간", example = "2026-07-15T12:00:00", nullable = true)
	val reviewedAt: LocalDateTime?,

	@field:Schema(description = "서명 요청 시간", example = "2026-07-15T12:10:00", nullable = true)
	val requestedAt: LocalDateTime?,

	@field:Schema(description = "서명 완료 시간", example = "2026-07-15T12:20:00", nullable = true)
	val completedAt: LocalDateTime?,

	@field:Schema(description = "현재 로그인 사용자가 아직 본인 서명을 제출할 수 있는지 여부", example = "true")
	val canSign: Boolean,

	@field:Schema(description = "현재까지 제출된 전체 참여자 서명. 모든 여행 참여자에게 동일하게 노출됩니다.")
	val signatures: List<TripPledgeSignatureResponse>,

	@field:Schema(description = "자녀와 참여 부모 전체의 서명 상태. 서명 전 참여자도 포함됩니다.")
	val signers: List<TripPledgeSignerResponse>,
) {
	companion object {
		fun of(
			pledge: TripPledge,
			items: List<PledgeItem>,
			signatures: List<PledgeSignature>,
			signers: List<TripPledgeSignerResponse>,
			canSign: Boolean,
		): TripPledgeResponse =
			TripPledgeResponse(
				id = requireNotNull(pledge.id),
				tripId = requireNotNull(pledge.trip.id),
				title = pledge.title,
				status = pledge.status,
				items = items.map(TripPledgeItemResponse::from),
				reviewedAt = pledge.reviewedAt,
				requestedAt = pledge.requestedAt,
				completedAt = pledge.completedAt,
				canSign = canSign,
				signatures = signatures.map(TripPledgeSignatureResponse::from),
				signers = signers,
			)
	}
}

@Schema(description = "여행 10계명 서명 대상자와 제출 상태")
data class TripPledgeSignerResponse(
	@field:Schema(description = "서명 대상 사용자 ID", example = "2")
	val userId: Long,

	@field:Schema(description = "서명 대상 역할", example = "parent", allowableValues = ["child", "parent"])
	val role: UserRole,

	@field:Schema(description = "화면 표시 관계", example = "엄마")
	val relationLabel: String,

	@field:Schema(description = "표시 이름", example = "김지영")
	val displayName: String,

	@field:Schema(description = "서명 제출 여부", example = "false")
	val signed: Boolean,

	@field:Schema(description = "서명 이미지 MIME 타입. 서명 전이면 null입니다.", example = "image/png", nullable = true)
	val signatureImageMimeType: String?,

	@field:Schema(description = "PNG Base64 문자열. 서명 전이면 null입니다.", nullable = true)
	val signatureImageBase64: String?,

	@field:Schema(description = "서명 시간. 서명 전이면 null입니다.", example = "2026-07-15T12:10:00", nullable = true)
	val signedAt: LocalDateTime?,
) {
	companion object {
		fun of(
			userId: Long,
			role: UserRole,
			relationLabel: String,
			displayName: String,
			signature: PledgeSignature?,
		): TripPledgeSignerResponse =
			TripPledgeSignerResponse(
				userId = userId,
				role = role,
				relationLabel = relationLabel,
				displayName = displayName,
				signed = signature != null,
				signatureImageMimeType = signature?.signatureMimeType,
				signatureImageBase64 = signature?.signatureImageData?.let(Base64.getEncoder()::encodeToString),
				signedAt = signature?.signedAt,
			)
	}
}

@Schema(description = "여행 10계명 참여자 서명")
data class TripPledgeSignatureResponse(
	@field:Schema(description = "서명 ID", example = "1")
	val id: Long,

	@field:Schema(description = "서명한 사용자 ID", example = "2")
	val userId: Long,

	@field:Schema(
		description = "서명한 사용자의 역할",
		example = "parent",
		allowableValues = ["child", "parent"],
	)
	val role: UserRole,

	@field:Schema(description = "서명한 사용자 표시 이름", example = "엄마")
	val displayName: String,

	@field:Schema(description = "서명 이미지 MIME 타입", example = "image/png")
	val signatureImageMimeType: String,

	@field:Schema(description = "data URI prefix 없는 PNG Base64 문자열", example = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAAB...")
	val signatureImageBase64: String,

	@field:Schema(description = "서명 시간", example = "2026-07-15T12:10:00")
	val signedAt: LocalDateTime,
) {
	companion object {
		fun from(signature: PledgeSignature): TripPledgeSignatureResponse =
			TripPledgeSignatureResponse(
				id = requireNotNull(signature.id),
				userId = requireNotNull(signature.user.id),
				role = signature.user.role,
				displayName = signature.user.displayName,
				signatureImageMimeType = signature.signatureMimeType,
				signatureImageBase64 = Base64.getEncoder().encodeToString(signature.signatureImageData),
				signedAt = signature.signedAt,
			)
	}
}

@Schema(description = "여행 10계명 확정 항목")
data class TripPledgeItemResponse(
	@field:Schema(description = "항목 ID", example = "1")
	val id: Long,

	@field:Schema(description = "표시 순서", example = "1")
	val sortOrder: Int,

	@field:Schema(description = "원본 템플릿 ID", example = "1", nullable = true)
	val templateId: Long?,

	@field:Schema(description = "확정 문구", example = "서로 재촉하지 않기")
	val content: String,

	@field:Schema(description = "원본 템플릿을 수정 없이 사용했는지 여부", example = "true")
	val isFromTemplate: Boolean,
) {
	companion object {
		fun from(item: PledgeItem): TripPledgeItemResponse =
			TripPledgeItemResponse(
				id = requireNotNull(item.id),
				sortOrder = item.sortOrder,
				templateId = item.pledgeTemplate?.id,
				content = item.content,
				isFromTemplate = item.isFromTemplate,
			)
	}
}
