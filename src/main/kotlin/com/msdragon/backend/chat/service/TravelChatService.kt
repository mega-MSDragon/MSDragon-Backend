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
import com.msdragon.backend.chat.openai.OpenAiFunctionTool
import com.msdragon.backend.chat.openai.OpenAiResponsesClient
import com.msdragon.backend.chat.openai.OpenAiToolCall
import com.msdragon.backend.chat.repository.ChatMessageRepository
import com.msdragon.backend.chat.repository.ChatSessionRepository
import com.msdragon.backend.common.exception.BadRequestException
import com.msdragon.backend.common.exception.InternalServerException
import com.msdragon.backend.common.exception.NotFoundException
import com.msdragon.backend.common.exception.UnAuthorizedException
import com.msdragon.backend.supportfacility.entity.SupportFacilityType
import com.msdragon.backend.supportfacility.service.SupportFacilityService
import com.msdragon.backend.trip.dto.TripTravelModeResponse
import com.msdragon.backend.trip.entity.ExternalApiProvider
import com.msdragon.backend.trip.repository.TripRepository
import com.msdragon.backend.trip.service.TripPlaceService
import com.msdragon.backend.trip.service.TripService
import org.slf4j.LoggerFactory
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
	private val tripPlaceService: TripPlaceService,
	private val supportFacilityService: SupportFacilityService,
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
		validateCurrentLocation(request)
		val travelMode = tripService.getTravelMode(currentUser, tripId)
		val user = userRepository.findByIdAndDeletedAtIsNull(currentUser.id)
			?: throw UnAuthorizedException("로그인할 수 없는 사용자입니다.")
		val trip = tripRepository.findByIdAndDeletedAtIsNull(tripId)
			?: error("여행 모드 검증 후 여행을 찾을 수 없습니다.")
		val travelContext = ChatTravelContext.from(travelMode)
		val contextSnapshot = objectMapper.writeValueAsString(travelContext)
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
				tools = CHAT_TOOLS,
				webSearchEnabled = true,
			),
		) { call -> executeTool(call, currentUser, tripId, request, travelContext) }
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

	private fun validateCurrentLocation(request: SendChatMessageRequest) {
		if ((request.latitude == null) != (request.longitude == null)) {
			throw BadRequestException("현재 위치는 latitude와 longitude를 함께 입력해 주세요.")
		}
	}

	private fun executeTool(
		call: OpenAiToolCall,
		currentUser: AuthenticatedUser,
		tripId: Long,
		request: SendChatMessageRequest,
		context: ChatTravelContext,
	): String = when (call.name) {
		"get_trip_schedule" -> {
			val dayNumber = call.arguments.intValue("day_number")
			val result = if (dayNumber == null) context else context.days.firstOrNull { it.dayNumber == dayNumber }
			objectMapper.writeValueAsString(result ?: mapOf("error" to "TRIP_DAY_NOT_FOUND"))
		}
		"get_place_detail" -> {
			val stopId = call.arguments.longValue("stop_id")
			val stop = context.days.asSequence().flatMap { it.stops.asSequence() }.firstOrNull { it.id == stopId }
			val detail = stop?.let { getPlaceDetail(currentUser, tripId, it) }
			objectMapper.writeValueAsString(detail ?: mapOf("error" to "TRIP_STOP_NOT_FOUND"))
		}
		"find_nearby_facilities" -> nearbyFacilities(call, currentUser, tripId, request)
		else -> objectMapper.writeValueAsString(mapOf("error" to "UNSUPPORTED_TOOL"))
	}

	private fun getPlaceDetail(
		currentUser: AuthenticatedUser,
		tripId: Long,
		stop: ChatTravelStopContext,
	): Any {
		if (stop.sourceProvider != ExternalApiProvider.TOUR_API || stop.externalPlaceId == null) {
			return stop
		}
		return try {
			tripPlaceService.getPlaceDetail(currentUser, tripId, stop.externalPlaceId, stop.contentTypeId)
		} catch (exception: InternalServerException) {
			logger.warn("TourAPI 방문지 상세 보강 실패로 코스 스냅샷을 사용합니다. tripId={}, stopId={}, reason={}", tripId, stop.id, exception.message)
			stop
		} catch (exception: NotFoundException) {
			logger.warn("TourAPI 방문지 상세가 없어 코스 스냅샷을 사용합니다. tripId={}, stopId={}", tripId, stop.id)
			stop
		} catch (exception: BadRequestException) {
			logger.warn("TourAPI 방문지 상세 요청값이 유효하지 않아 코스 스냅샷을 사용합니다. tripId={}, stopId={}, reason={}", tripId, stop.id, exception.message)
			stop
		}
	}

	private fun nearbyFacilities(
		call: OpenAiToolCall,
		currentUser: AuthenticatedUser,
		tripId: Long,
		request: SendChatMessageRequest,
	): String {
		val latitude = request.latitude
		val longitude = request.longitude
		if (latitude == null || longitude == null) {
			return objectMapper.writeValueAsString(mapOf("error" to "CURRENT_LOCATION_REQUIRED"))
		}
		val limit = (call.arguments.intValue("limit") ?: 3).coerceIn(1, 10)
		val facilityType = call.arguments["facility_type"]?.toString()
		val facilities: List<Any> = when (facilityType) {
			"cafe" -> supportFacilityService.getNearbyCafes(currentUser, tripId, latitude, longitude)
			"restroom" -> supportFacilityService.getNearbyRestrooms(currentUser, tripId, latitude, longitude)
			"hospital" -> supportFacilityService.getNearbyMedicalFacilities(
				currentUser, tripId, latitude, longitude, SupportFacilityType.HOSPITAL,
			)
			"pharmacy" -> supportFacilityService.getNearbyMedicalFacilities(
				currentUser, tripId, latitude, longitude, SupportFacilityType.PHARMACY,
			)
			"medical" -> (
				supportFacilityService.getNearbyMedicalFacilities(
					currentUser, tripId, latitude, longitude, SupportFacilityType.HOSPITAL,
				) + supportFacilityService.getNearbyMedicalFacilities(
					currentUser, tripId, latitude, longitude, SupportFacilityType.PHARMACY,
				)
			).sortedBy { it.distanceMeters }
			else -> return objectMapper.writeValueAsString(mapOf("error" to "UNSUPPORTED_FACILITY_TYPE"))
		}
		return objectMapper.writeValueAsString(
			mapOf(
				"distanceBasis" to "straight_line_meters",
				"facilities" to facilities.take(limit),
			),
		)
	}

	private fun safetyIdentifier(userId: Long): String =
		MessageDigest.getInstance("SHA-256")
			.digest("msdragon:$userId".toByteArray(StandardCharsets.UTF_8))
			.joinToString("") { "%02x".format(it) }

	companion object {
		private val logger = LoggerFactory.getLogger(TravelChatService::class.java)
		private const val SYSTEM_PROMPT_VERSION = "travel-chat-v3"
		private val CHAT_TOOLS = listOf(
			OpenAiFunctionTool(
				name = "get_trip_schedule",
				description = "현재 여행의 최신 전체 일정 또는 특정 일차 일정과 경로를 조회합니다.",
				parameters = objectSchema(
					properties = mapOf(
						"day_number" to mapOf("type" to "integer", "description" to "조회할 여행 일차. 생략하면 전체 일정을 조회합니다."),
					),
				),
			),
			OpenAiFunctionTool(
				name = "get_place_detail",
				description = "현재 여행 코스에 포함된 방문지의 저장된 상세 정보와 메모를 조회합니다.",
				parameters = objectSchema(
					properties = mapOf("stop_id" to mapOf("type" to "integer", "description" to "travel_context에 포함된 방문지 ID")),
					required = listOf("stop_id"),
				),
			),
			OpenAiFunctionTool(
				name = "find_nearby_facilities",
				description = "사용자의 현재 위치 주변 카페, 화장실, 병원, 약국 또는 의료시설을 가까운 순으로 조회합니다.",
				parameters = objectSchema(
					properties = mapOf(
						"facility_type" to mapOf("type" to "string", "enum" to listOf("cafe", "restroom", "medical", "hospital", "pharmacy")),
						"limit" to mapOf("type" to "integer", "minimum" to 1, "maximum" to 10, "description" to "조회 개수"),
					),
					required = listOf("facility_type", "limit"),
				),
			),
		)

		private fun objectSchema(
			properties: Map<String, Any?>,
			required: List<String> = emptyList(),
		): Map<String, Any?> = mapOf(
			"type" to "object",
			"properties" to properties,
			"required" to required,
			"additionalProperties" to false,
		)
	}
}

private fun Map<String, Any?>.intValue(name: String): Int? = (this[name] as? Number)?.toInt()
	?: this[name]?.toString()?.toIntOrNull()

private fun Map<String, Any?>.longValue(name: String): Long? = (this[name] as? Number)?.toLong()
	?: this[name]?.toString()?.toLongOrNull()

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
								id = stop.id,
								sortOrder = stop.sortOrder,
								sourceProvider = stop.sourceProvider,
								externalPlaceId = stop.externalPlaceId,
								contentTypeId = stop.contentTypeId,
								name = stop.name,
								category = stop.category,
								address = stop.address,
								latitude = stop.latitude?.toString(),
								longitude = stop.longitude?.toString(),
								phone = stop.phone,
								homepageUrl = stop.homepageUrl,
								overview = stop.overview?.take(500),
								arrivalTime = stop.arrivalTime?.toString(),
								dwellMinutes = stop.dwellMinutes,
								note = stop.note,
								recommendationReason = stop.recommendationReason,
								recommendationTags = stop.recommendationTags,
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
	val id: Long,
	val sortOrder: Int,
	val sourceProvider: ExternalApiProvider,
	val externalPlaceId: String?,
	val contentTypeId: String?,
	val name: String,
	val category: String?,
	val address: String?,
	val latitude: String?,
	val longitude: String?,
	val phone: String?,
	val homepageUrl: String?,
	val overview: String?,
	val arrivalTime: String?,
	val dwellMinutes: Int?,
	val note: String?,
	val recommendationReason: String?,
	val recommendationTags: List<String>,
)
