package com.msdragon.backend.common.exception

import org.springframework.http.HttpStatus

class ForbiddenException(
	message: String,
	status: Int = HttpStatus.FORBIDDEN.value(),
) : BaseException(status, message)
