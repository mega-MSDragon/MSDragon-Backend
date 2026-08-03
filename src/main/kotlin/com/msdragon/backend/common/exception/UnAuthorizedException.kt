package com.msdragon.backend.common.exception

import org.springframework.http.HttpStatus

class UnAuthorizedException(
	message: String,
	status: Int = HttpStatus.UNAUTHORIZED.value(),
) : BaseException(status, message)
