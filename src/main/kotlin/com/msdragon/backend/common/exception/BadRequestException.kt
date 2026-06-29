package com.msdragon.backend.common.exception

import org.springframework.http.HttpStatus

class BadRequestException(
	message: String,
) : BaseException(HttpStatus.BAD_REQUEST, message)
