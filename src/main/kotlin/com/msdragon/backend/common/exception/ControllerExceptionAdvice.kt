package com.msdragon.backend.common.exception

import com.msdragon.backend.common.response.ApiResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

@RestControllerAdvice
class ControllerExceptionAdvice {
	@ExceptionHandler(BaseException::class)
	fun handleBaseException(exception: BaseException): ResponseEntity<ApiResponse<Unit>> =
		ResponseEntity
			.status(exception.httpStatus)
			.contentType(MediaType.APPLICATION_JSON)
			.body(ApiResponse.failure(exception.status, exception.message))

	@ExceptionHandler(MethodArgumentNotValidException::class)
	fun handleValidationException(exception: MethodArgumentNotValidException): ResponseEntity<ApiResponse<Unit>> {
		val message = exception.bindingResult.fieldErrors.firstOrNull()?.defaultMessage ?: "요청 값이 올바르지 않습니다."
		return ResponseEntity
			.status(HttpStatus.OK)
			.contentType(MediaType.APPLICATION_JSON)
			.body(ApiResponse.failure(HttpStatus.BAD_REQUEST.value(), message))
	}

	@ExceptionHandler(MissingServletRequestParameterException::class)
	fun handleMissingParameter(exception: MissingServletRequestParameterException): ResponseEntity<ApiResponse<Unit>> =
		ResponseEntity
			.status(HttpStatus.OK)
			.contentType(MediaType.APPLICATION_JSON)
			.body(ApiResponse.failure(HttpStatus.BAD_REQUEST.value(), "${exception.parameterName} 값이 입력되지 않았습니다."))

	@ExceptionHandler(HttpMessageNotReadableException::class)
	fun handleHttpMessageNotReadable(exception: HttpMessageNotReadableException): ResponseEntity<ApiResponse<Unit>> {
		val baseException = exception.findCause<BaseException>()
		return ResponseEntity
			.status(HttpStatus.OK)
			.contentType(MediaType.APPLICATION_JSON)
			.body(
				ApiResponse.failure(
					status = HttpStatus.BAD_REQUEST.value(),
					message = baseException?.message ?: "요청 본문이 올바르지 않습니다.",
				),
			)
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException::class)
	fun handleMethodArgumentTypeMismatch(exception: MethodArgumentTypeMismatchException): ResponseEntity<ApiResponse<Unit>> =
		ResponseEntity
			.status(HttpStatus.OK)
			.contentType(MediaType.APPLICATION_JSON)
			.body(ApiResponse.failure(HttpStatus.BAD_REQUEST.value(), "${exception.name} 값이 올바르지 않습니다."))
}

private inline fun <reified T : Throwable> Throwable.findCause(): T? {
	var current: Throwable? = this
	while (current != null) {
		if (current is T) {
			return current
		}
		current = current.cause
	}
	return null
}
