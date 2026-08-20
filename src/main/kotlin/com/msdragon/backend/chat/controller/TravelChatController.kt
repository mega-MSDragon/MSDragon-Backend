package com.msdragon.backend.chat.controller

import com.msdragon.backend.auth.support.AuthenticatedUser
import com.msdragon.backend.auth.support.CurrentUser
import com.msdragon.backend.chat.dto.ChatConversationResponse
import com.msdragon.backend.chat.dto.ChatMessageExchangeResponse
import com.msdragon.backend.chat.dto.SendChatMessageRequest
import com.msdragon.backend.chat.service.TravelChatService
import com.msdragon.backend.common.config.BEARER_AUTH_SCHEME
import com.msdragon.backend.common.response.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/trips/{tripId}/chat/messages")
@Tag(name = "Travel Chat", description = "여행 모드 AI 챗봇 API 입니다.")
@SecurityRequirement(name = BEARER_AUTH_SCHEME)
class TravelChatController(
	private val travelChatService: TravelChatService,
) {
	@Operation(
		summary = "여행 AI 대화 이력 조회",
		description = "여행 기간 중 같은 가족 구성원이 본인의 여행별 AI 대화 이력과 현재 표시할 추천 질문을 조회합니다. 아직 질문하지 않았으면 빈 대화와 최초 추천 질문을 반환합니다.",
	)
	@ApiResponses(
		value = [
			SwaggerApiResponse(responseCode = "200", description = "처리 완료: 조회 성공(status=200) 또는 인증·정책 오류(status=400/401/403/404)"),
		],
	)
	@GetMapping
	fun getMessages(
		@CurrentUser currentUser: AuthenticatedUser,
		@Parameter(description = "여행 ID", example = "1")
		@PathVariable tripId: Long,
	): ApiResponse<ChatConversationResponse> =
		ApiResponse.success(
			message = "여행 AI 대화 이력 조회 성공",
			data = travelChatService.getMessages(currentUser, tripId),
		)

	@Operation(
		summary = "여행 AI 질문 전송",
		description = "여행 기간 중 같은 가족 구성원이 여행 일정이나 여행지에 관해 질문하고, AI 답변과 다음 추천 질문 2~3개를 받습니다. 주변 카페·화장실·의료시설 질문에는 latitude와 longitude를 함께 전달합니다. 첫 질문이면 사용자별 여행 채팅 세션을 자동으로 생성합니다.",
	)
	@ApiResponses(
		value = [
			SwaggerApiResponse(responseCode = "200", description = "처리 완료: 답변 생성 성공(status=200) 또는 요청·인증·정책 오류(status=400/401/403/404)"),
			SwaggerApiResponse(responseCode = "500", description = "OpenAI 설정 또는 호출 실패"),
		],
	)
	@PostMapping
	fun sendMessage(
		@CurrentUser currentUser: AuthenticatedUser,
		@Parameter(description = "여행 ID", example = "1")
		@PathVariable tripId: Long,
		@Valid @RequestBody request: SendChatMessageRequest,
	): ApiResponse<ChatMessageExchangeResponse> =
		ApiResponse.success(
			message = "여행 AI 답변 생성 성공",
			data = travelChatService.sendMessage(currentUser, tripId, request),
		)
}
