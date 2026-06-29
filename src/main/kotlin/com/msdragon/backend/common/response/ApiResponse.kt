package com.msdragon.backend.common.response

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ApiResponse<T>(
	val status: Int,
	val success: Boolean,
	val message: String,
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
