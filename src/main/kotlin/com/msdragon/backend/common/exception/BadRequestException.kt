package com.msdragon.backend.common.exception

import org.springframework.http.HttpStatus

class BadRequestException(
	message: String,
	status: Int = HttpStatus.BAD_REQUEST.value(),
) : BaseException(status, message)
