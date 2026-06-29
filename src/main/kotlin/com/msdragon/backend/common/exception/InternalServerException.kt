package com.msdragon.backend.common.exception

import org.springframework.http.HttpStatus

class InternalServerException(
	message: String,
) : BaseException(HttpStatus.INTERNAL_SERVER_ERROR, message)
