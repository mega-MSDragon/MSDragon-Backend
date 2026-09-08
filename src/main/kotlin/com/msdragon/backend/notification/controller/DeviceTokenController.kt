package com.msdragon.backend.notification.controller

import com.msdragon.backend.auth.support.AuthenticatedUser
import com.msdragon.backend.auth.support.CurrentUser
import com.msdragon.backend.common.config.BEARER_AUTH_SCHEME
import com.msdragon.backend.common.response.ApiResponse
import com.msdragon.backend.notification.dto.RegisterDeviceTokenRequest
import com.msdragon.backend.notification.dto.SendTestNotificationRequest
import com.msdragon.backend.notification.dto.TestNotificationResponse
import com.msdragon.backend.notification.dto.UnregisterDeviceTokenRequest
import com.msdragon.backend.notification.service.NotificationService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/users/me/device-tokens")
@Tag(name = "Notification", description = "푸시 알림 기기 토큰 API 입니다.")
@SecurityRequirement(name = BEARER_AUTH_SCHEME)
class DeviceTokenController(
	private val notificationService: NotificationService,
) {
	@Operation(
		summary = "기기 토큰 등록",
		description = "로그인 후 발급받은 FCM 기기 토큰을 등록합니다. 같은 토큰을 다시 보내면 갱신만 하므로 여러 번 호출해도 안전합니다. " +
			"같은 기기에 다른 사용자가 로그인하면 소유자를 옮겨 이전 사용자에게 알림이 가지 않게 합니다.",
	)
	@ApiResponses(
		value = [
			SwaggerApiResponse(
				responseCode = "200",
				description = "처리 완료: 등록 성공(status=200), 잘못된 요청(status=400), 인증 오류(status=401)",
			),
		],
	)
	@PostMapping
	fun registerDeviceToken(
		@CurrentUser currentUser: AuthenticatedUser,
		@Valid @RequestBody request: RegisterDeviceTokenRequest,
	): ApiResponse<Unit> {
		notificationService.registerDeviceToken(currentUser.id, request.token, request.platform)
		return ApiResponse.success(message = "기기 토큰 등록 성공", data = Unit)
	}

	@Operation(
		summary = "테스트 알림 발송",
		description = "**내 기기로만** 테스트 알림을 보냅니다. 다른 사용자를 대상으로 지정할 수 없습니다. " +
			"실제 알림과 같은 형태의 페이로드를 보내므로 클라이언트가 수신과 화면 이동을 확인할 수 있습니다. " +
			"`type`을 생략하면 `test`를 보내 앱의 '모르는 type은 홈으로' 처리를 확인할 수 있습니다. " +
			"알림이 오지 않을 때 응답의 `deviceCount`, `pushConfigured`, `notificationEnabled`로 원인을 좁힙니다.",
	)
	@ApiResponses(
		value = [
			SwaggerApiResponse(
				responseCode = "200",
				description = "처리 완료: 발송 시도 결과 반환(status=200), 인증 오류(status=401)",
			),
		],
	)
	@PostMapping("/test")
	fun sendTestNotification(
		@CurrentUser currentUser: AuthenticatedUser,
		@RequestBody(required = false) request: SendTestNotificationRequest?,
	): ApiResponse<TestNotificationResponse> =
		ApiResponse.success(
			message = "테스트 알림 발송 처리 완료",
			data = notificationService.sendTestNotification(
				userId = currentUser.id,
				type = request?.type,
				tripId = request?.tripId,
			),
		)

	@Operation(
		summary = "기기 토큰 해제",
		description = "로그아웃할 때 호출합니다. 해제하지 않으면 같은 기기를 쓰는 다음 사용자에게 이전 사용자의 알림이 갈 수 있습니다. " +
			"이미 없는 토큰도 성공으로 처리합니다.",
	)
	@ApiResponses(
		value = [
			SwaggerApiResponse(
				responseCode = "200",
				description = "처리 완료: 해제 성공(status=200), 잘못된 요청(status=400), 인증 오류(status=401)",
			),
		],
	)
	@DeleteMapping
	fun unregisterDeviceToken(
		@CurrentUser currentUser: AuthenticatedUser,
		@Valid @RequestBody request: UnregisterDeviceTokenRequest,
	): ApiResponse<Unit> {
		notificationService.unregisterDeviceToken(currentUser.id, request.token)
		return ApiResponse.success(message = "기기 토큰 해제 성공", data = Unit)
	}
}
