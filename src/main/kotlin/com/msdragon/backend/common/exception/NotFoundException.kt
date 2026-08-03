package com.msdragon.backend.common.exception

import org.springframework.http.HttpStatus

class NotFoundException(
	message: String,
	status: Int = HttpStatus.NOT_FOUND.value(),
) : BaseException(status, message)
