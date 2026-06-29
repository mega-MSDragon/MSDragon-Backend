package com.msdragon.backend.common.exception

import org.springframework.http.HttpStatus

class UnAuthorizedException(
	message: String,
) : BaseException(HttpStatus.UNAUTHORIZED, message)
