package com.msdragon.backend.common.exception

import org.springframework.http.HttpStatus

abstract class BaseException(
	val status: HttpStatus,
	override val message: String,
) : RuntimeException(message)
