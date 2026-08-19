package com.msdragon.backend.common.exception

import com.msdragon.backend.common.response.ApiResponse
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
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
	fun handleBaseException(
		exception: BaseException,
		request: HttpServletRequest,
	): ResponseEntity<ApiResponse<Unit>> {
		logHandledException(request, exception.status, exception.message, exception)
		return ResponseEntity
			.status(exception.httpStatus)
			.contentType(MediaType.APPLICATION_JSON)
			.body(ApiResponse.failure(exception.status, exception.message))
	}

	@ExceptionHandler(MethodArgumentNotValidException::class)
	fun handleValidationException(
		exception: MethodArgumentNotValidException,
		request: HttpServletRequest,
	): ResponseEntity<ApiResponse<Unit>> {
		val message = exception.bindingResult.fieldErrors.firstOrNull()?.defaultMessage ?: "요청 값이 올바르지 않습니다."
		logHandledException(request, HttpStatus.BAD_REQUEST.value(), message, exception)
		return ResponseEntity
			.status(HttpStatus.OK)
			.contentType(MediaType.APPLICATION_JSON)
			.body(ApiResponse.failure(HttpStatus.BAD_REQUEST.value(), message))
	}

	@ExceptionHandler(MissingServletRequestParameterException::class)
	fun handleMissingParameter(
		exception: MissingServletRequestParameterException,
		request: HttpServletRequest,
	): ResponseEntity<ApiResponse<Unit>> {
		val message = "${exception.parameterName} 값이 입력되지 않았습니다."
		logHandledException(request, HttpStatus.BAD_REQUEST.value(), message, exception)
		return ResponseEntity
			.status(HttpStatus.OK)
			.contentType(MediaType.APPLICATION_JSON)
			.body(ApiResponse.failure(HttpStatus.BAD_REQUEST.value(), message))
	}

	@ExceptionHandler(HttpMessageNotReadableException::class)
	fun handleHttpMessageNotReadable(
		exception: HttpMessageNotReadableException,
		request: HttpServletRequest,
	): ResponseEntity<ApiResponse<Unit>> {
		val baseException = exception.findCause<BaseException>()
		val message = baseException?.message ?: "요청 본문이 올바르지 않습니다."
		logHandledException(request, HttpStatus.BAD_REQUEST.value(), message, exception)
		return ResponseEntity
			.status(HttpStatus.OK)
			.contentType(MediaType.APPLICATION_JSON)
			.body(
				ApiResponse.failure(
					status = HttpStatus.BAD_REQUEST.value(),
					message = message,
				),
			)
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException::class)
	fun handleMethodArgumentTypeMismatch(
		exception: MethodArgumentTypeMismatchException,
		request: HttpServletRequest,
	): ResponseEntity<ApiResponse<Unit>> {
		val message = "${exception.name} 값이 올바르지 않습니다."
		logHandledException(request, HttpStatus.BAD_REQUEST.value(), message, exception)
		return ResponseEntity
			.status(HttpStatus.OK)
			.contentType(MediaType.APPLICATION_JSON)
			.body(ApiResponse.failure(HttpStatus.BAD_REQUEST.value(), message))
	}

	private fun logHandledException(
		request: HttpServletRequest,
		status: Int,
		message: String,
		exception: Exception,
	) {
		logger.warn(
			"Handled API error: method={} uri={} status={} message=\"{}\" clientIp={} userAgent=\"{}\" exception={}",
			request.method,
			request.requestURI,
			status,
			message,
			request.clientIp(),
			request.getHeader(USER_AGENT_HEADER).orEmpty(),
			exception.javaClass.simpleName,
		)
	}

	private fun HttpServletRequest.clientIp(): String =
		getHeader(X_FORWARDED_FOR_HEADER)
			?.split(",")
			?.firstOrNull()
			?.trim()
			?.takeIf { it.isNotBlank() }
			?: remoteAddr

	companion object {
		private val logger = LoggerFactory.getLogger(ControllerExceptionAdvice::class.java)
		private const val X_FORWARDED_FOR_HEADER = "X-Forwarded-For"
		private const val USER_AGENT_HEADER = "User-Agent"
	}
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
