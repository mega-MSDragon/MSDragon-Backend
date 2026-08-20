package com.msdragon.backend.pledge.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "여행 10계명 저장 요청")
data class SaveTripPledgeRequest(
	@field:Schema(description = "저장할 10계명 항목. 배열 순서가 화면 표시 순서가 됩니다.")
	@field:Valid
	@field:Size(min = 10, max = 10, message = "여행 10계명은 정확히 10개여야 합니다.")
	val items: List<SaveTripPledgeItemRequest>,
)

@Schema(description = "여행 10계명 항목 저장 요청")
data class SaveTripPledgeItemRequest(
	@field:Schema(description = "후보 템플릿 ID. 사용자가 직접 작성한 항목이면 생략할 수 있습니다.", example = "1", nullable = true)
	val templateId: Long? = null,

	@field:Schema(description = "저장할 문구", example = "서로 재촉하지 않기")
	@field:NotBlank(message = "10계명 문구를 입력해주세요.")
	@field:Size(max = 255, message = "10계명 문구는 255자 이하로 입력해주세요.")
	val content: String,
)

@Schema(description = "여행 10계명 본인 서명 저장 요청")
data class SavePledgeSignatureRequest(
	@field:Schema(
		description = "PNG 서명 이미지의 data URI prefix 없는 Base64 문자열. 디코딩 결과는 최대 512KB까지 허용합니다.",
		example = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAAB...",
	)
	@field:NotBlank(message = "서명 이미지를 입력해주세요.")
	@field:Size(max = 700_000, message = "서명 이미지가 허용 크기를 초과했습니다.")
	val signatureImageBase64: String,
)
