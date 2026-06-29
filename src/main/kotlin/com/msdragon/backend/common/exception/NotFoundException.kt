package com.msdragon.backend.common.exception

import org.springframework.http.HttpStatus

class NotFoundException(
	message: String,
) : BaseException(HttpStatus.NOT_FOUND, message)
