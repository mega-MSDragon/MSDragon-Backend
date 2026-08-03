package com.msdragon.backend.common.exception

import org.springframework.http.HttpStatus

class InternalServerException(
	message: String,
	status: Int = HttpStatus.INTERNAL_SERVER_ERROR.value(),
) : BaseException(status, message, HttpStatus.INTERNAL_SERVER_ERROR)
