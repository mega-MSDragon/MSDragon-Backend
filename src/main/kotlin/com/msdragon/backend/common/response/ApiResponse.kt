package com.msdragon.backend.common.response

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "공통 API 응답. 서버가 처리한 요청·인증·정책 오류도 HTTP 200으로 반환하며 status와 success로 구분합니다.")
data class ApiResponse<T>(
	@field:Schema(description = "내부 처리 상태 코드. 실제 HTTP 상태 코드와 다를 수 있습니다.", example = "200")
	val status: Int,

	@field:Schema(description = "요청 처리 성공 여부", example = "true")
	val success: Boolean,

	@field:Schema(description = "처리 결과 메시지", example = "요청 성공")
	val message: String,

	@field:Schema(description = "성공 응답 데이터", nullable = true)
	val data: T? = null,
) {
	companion object {
		fun <T> success(status: Int = 200, message: String = "요청 성공", data: T? = null): ApiResponse<T> =
			ApiResponse(
				status = status,
				success = true,
				message = message,
				data = data,
			)

		fun failure(status: Int, message: String): ApiResponse<Unit> =
			ApiResponse(
				status = status,
				success = false,
				message = message,
			)
	}
}
