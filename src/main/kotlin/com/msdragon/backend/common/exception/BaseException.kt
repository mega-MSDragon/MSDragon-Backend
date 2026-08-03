package com.msdragon.backend.common.exception

import org.springframework.http.HttpStatus

abstract class BaseException(
	val status: Int,
	override val message: String,
	val httpStatus: HttpStatus = HttpStatus.OK,
) : RuntimeException(message)
