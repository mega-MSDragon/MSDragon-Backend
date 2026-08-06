package com.msdragon.backend.chat.service

import com.msdragon.backend.auth.repository.UserRepository
import com.msdragon.backend.auth.support.AuthenticatedUser
import com.msdragon.backend.chat.config.OpenAiProperties
import com.msdragon.backend.chat.dto.ChatConversationResponse
import com.msdragon.backend.chat.dto.ChatMessageExchangeResponse
import com.msdragon.backend.chat.dto.ChatMessageResponse
import com.msdragon.backend.chat.dto.SendChatMessageRequest
import com.msdragon.backend.chat.entity.ChatMessage
import com.msdragon.backend.chat.entity.ChatSender
import com.msdragon.backend.chat.entity.ChatSession
import com.msdragon.backend.chat.entity.ChatSessionScope
import com.msdragon.backend.chat.openai.OpenAiChatMessage
import com.msdragon.backend.chat.openai.OpenAiChatRequest
import com.msdragon.backend.chat.openai.OpenAiResponsesClient
import com.msdragon.backend.chat.repository.ChatMessageRepository
import com.msdragon.backend.chat.repository.ChatSessionRepository
import com.msdragon.backend.common.exception.UnAuthorizedException
import com.msdragon.backend.trip.dto.TripTravelModeResponse
import com.msdragon.backend.trip.repository.TripRepository
import com.msdragon.backend.trip.service.TripService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

@Service
class TravelChatService(
	private val userRepository: UserRepository,
	private val tripRepository: TripRepository,
	private val chatSessionRepository: ChatSessionRepository,
	private val chatMessageRepository: ChatMessageRepository,
	private val tripService: TripService,
	private val openAiResponsesClient: OpenAiResponsesClient,
	private val openAiProperties: OpenAiProperties,
	private val objectMapper: ObjectMapper,
) {
	@Transactional
	fun getMessages(currentUser: AuthenticatedUser, tripId: Long): ChatConversationResponse {
		tripService.validateTravelModeAccess(currentUser, tripId)
		val session = findSession(currentUser.id, tripId) ?: return ChatConversationResponse.empty()
		return ChatConversationResponse(
			sessionId = requireNotNull(session.id),
			messages = chatMessageRepository.findAllByChatSessionIdOrderByIdAsc(requireNotNull(session.id))
				.map(ChatMessageResponse::from),
		)
	}

	@Transactional
	fun sendMessage(
		currentUser: AuthenticatedUser,
		tripId: Long,
		request: SendChatMessageRequest,
	): ChatMessageExchangeResponse {
		val travelMode = tripService.getTravelMode(currentUser, tripId)
		val user = userRepository.findByIdAndDeletedAtIsNull(currentUser.id)
			?: throw UnAuthorizedException("로그인할 수 없는 사용자입니다.")
		val trip = tripRepository.findByIdAndDeletedAtIsNull(tripId)
			?: error("여행 모드 검증 후 여행을 찾을 수 없습니다.")
		val contextSnapshot = objectMapper.writeValueAsString(ChatTravelContext.from(travelMode))
		val session = findSession(currentUser.id, tripId)?.also {
			it.refreshContext(SYSTEM_PROMPT_VERSION, openAiProperties.model, contextSnapshot)
		} ?: chatSessionRepository.save(
			ChatSession(
				user = user,
				trip = trip,
				systemPromptVersion = SYSTEM_PROMPT_VERSION,
				modelName = openAiProperties.model,
				contextSnapshot = contextSnapshot,
			),
		)
		val userMessage = chatMessageRepository.save(
			ChatMessage(
				chatSession = session,
				sender = ChatSender.USER,
				content = request.message.trim(),
			),
		)
		val recentMessages = chatMessageRepository
			.findTop20ByChatSessionIdOrderByIdDesc(requireNotNull(session.id))
			.asReversed()
			.map { OpenAiChatMessage(role = it.sender, content = it.content) }
		val aiResult = openAiResponsesClient.generate(
			OpenAiChatRequest(
				context = contextSnapshot,
				messages = recentMessages,
				safetyIdentifier = safetyIdentifier(currentUser.id),
			),
		)
		val assistantMessage = chatMessageRepository.save(
			ChatMessage(
				chatSession = session,
				sender = ChatSender.ASSISTANT,
				content = aiResult.content,
				metadata = objectMapper.writeValueAsString(
					mapOf(
						"responseId" to aiResult.responseId,
						"model" to openAiProperties.model,
						"usage" to aiResult.usage,
					),
				),
			),
		)

		return ChatMessageExchangeResponse(
			sessionId = requireNotNull(session.id),
			userMessage = ChatMessageResponse.from(userMessage),
			assistantMessage = ChatMessageResponse.from(assistantMessage),
		)
	}

	private fun findSession(userId: Long, tripId: Long): ChatSession? =
		chatSessionRepository.findFirstByUserIdAndTripIdAndScopeAndClosedAtIsNullOrderByIdDesc(
			userId = userId,
			tripId = tripId,
			scope = ChatSessionScope.TRAVEL_MODE,
		)

	private fun safetyIdentifier(userId: Long): String =
		MessageDigest.getInstance("SHA-256")
			.digest("msdragon:$userId".toByteArray(StandardCharsets.UTF_8))
			.joinToString("") { "%02x".format(it) }

	companion object {
		private const val SYSTEM_PROMPT_VERSION = "travel-chat-v1"
	}
}

private data class ChatTravelContext(
	val tripId: Long,
	val title: String,
	val destination: String,
	val startDate: String,
	val endDate: String,
	val currentDayNumber: Int,
	val days: List<ChatTravelDayContext>,
) {
	companion object {
		fun from(travelMode: TripTravelModeResponse): ChatTravelContext =
			ChatTravelContext(
				tripId = travelMode.tripId,
				title = travelMode.title,
				destination = travelMode.destination.displayName,
				startDate = travelMode.startDate.toString(),
				endDate = travelMode.endDate.toString(),
				currentDayNumber = travelMode.currentDayNumber,
				days = travelMode.days.map { day ->
					ChatTravelDayContext(
						dayNumber = day.dayNumber,
						travelDate = day.travelDate.toString(),
						routeDistanceMeters = day.route?.totalDistanceMeters,
						routeDurationSeconds = day.route?.totalDurationSeconds,
						stops = day.stops.map { stop ->
							ChatTravelStopContext(
								sortOrder = stop.sortOrder,
								name = stop.name,
								category = stop.category,
								address = stop.address,
								phone = stop.phone,
								overview = stop.overview?.take(500),
								arrivalTime = stop.arrivalTime?.toString(),
								dwellMinutes = stop.dwellMinutes,
								note = stop.note,
							)
						},
					)
				},
			)
	}
}

private data class ChatTravelDayContext(
	val dayNumber: Int,
	val travelDate: String,
	val routeDistanceMeters: Int?,
	val routeDurationSeconds: Int?,
	val stops: List<ChatTravelStopContext>,
)

private data class ChatTravelStopContext(
	val sortOrder: Int,
	val name: String,
	val category: String?,
	val address: String?,
	val phone: String?,
	val overview: String?,
	val arrivalTime: String?,
	val dwellMinutes: Int?,
	val note: String?,
)
