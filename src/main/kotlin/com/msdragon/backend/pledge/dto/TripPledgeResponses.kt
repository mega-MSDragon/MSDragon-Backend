package com.msdragon.backend.pledge.dto

import com.msdragon.backend.pledge.entity.PledgeItem
import com.msdragon.backend.pledge.entity.PledgeTemplate
import com.msdragon.backend.pledge.entity.TripPledge
import com.msdragon.backend.pledge.entity.TripPledgeStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

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

	@field:Schema(description = "현재 서명 상태를 반영한 비트맵 URL", nullable = true)
	val renderedImageUrl: String?,

	@field:Schema(description = "공유용 PDF URL", nullable = true)
	val pdfUrl: String?,
) {
	companion object {
		fun of(pledge: TripPledge, items: List<PledgeItem>): TripPledgeResponse =
			TripPledgeResponse(
				id = requireNotNull(pledge.id),
				tripId = requireNotNull(pledge.trip.id),
				title = pledge.title,
				status = pledge.status,
				items = items.map(TripPledgeItemResponse::from),
				reviewedAt = pledge.reviewedAt,
				requestedAt = pledge.requestedAt,
				completedAt = pledge.completedAt,
				renderedImageUrl = pledge.renderedImageUrl,
				pdfUrl = pledge.pdfUrl,
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
