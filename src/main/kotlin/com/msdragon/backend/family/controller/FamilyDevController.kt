package com.msdragon.backend.family.controller

import com.msdragon.backend.auth.support.AuthenticatedUser
import com.msdragon.backend.auth.support.CurrentUser
import com.msdragon.backend.common.config.BEARER_AUTH_SCHEME
import com.msdragon.backend.common.response.ApiResponse
import com.msdragon.backend.family.dto.FamilyDisconnectResponse
import com.msdragon.backend.family.service.FamilyService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse

/**
 * 개발 편의용 API. 앱이 호출하지 않는다.
 * `APP_FAMILY_DEV_TOOLS_ENABLED=true`가 아니면 없는 API처럼 404를 돌려준다.
 */
@RestController
@RequestMapping("/api/v1/dev/family")
@Tag(name = "Dev", description = "개발 편의용 API입니다. 앱에서 호출하지 않으며 운영 환경에서는 꺼둡니다.")
@SecurityRequirement(name = BEARER_AUTH_SCHEME)
class FamilyDevController(
	private val familyService: FamilyService,
) {
	@Operation(
		summary = "[개발용] 내 가족 연결 해제",
		description = "가족 연결을 다시 테스트할 수 있도록 내 가족 연결을 끊습니다. " +
			"자녀가 호출하면 가족을 해체하고 그 가족의 여행을 모두 삭제합니다. 부모가 호출하면 본인 연결만 끊습니다. " +
			"되돌릴 수 없으며 `APP_FAMILY_DEV_TOOLS_ENABLED=true`일 때만 동작합니다.",
	)
	@ApiResponses(
		value = [
			SwaggerApiResponse(
				responseCode = "200",
				description = "처리 완료: 해제 성공(status=200), 인증 오류(status=401), 연결된 가족 없음(status=400), 기능 꺼짐(status=404)",
			),
		],
	)
	@DeleteMapping
	fun disconnectMyFamily(
		@CurrentUser currentUser: AuthenticatedUser,
	): ApiResponse<FamilyDisconnectResponse> =
		ApiResponse.success(
			message = "가족 연결 해제 성공",
			data = familyService.disconnectMyFamilyForDevelopment(currentUser.id),
		)
}
