package com.msdragon.backend.auth.service

import com.msdragon.backend.auth.entity.AgeBand
import com.msdragon.backend.auth.entity.UserRole
import com.msdragon.backend.common.exception.BadRequestException
import org.springframework.stereotype.Component

@Component
class UserProfilePolicy {
	fun validateAgeBand(role: UserRole, ageBand: AgeBand) {
		val allowed = when (role) {
			UserRole.CHILD -> setOf(
				AgeBand.AGE_10S,
				AgeBand.AGE_20S,
				AgeBand.AGE_30S,
				AgeBand.AGE_40S,
				AgeBand.AGE_50S,
				AgeBand.AGE_60S_PLUS,
				AgeBand.UNDISCLOSED,
			)

			UserRole.PARENT -> setOf(
				AgeBand.AGE_50S,
				AgeBand.AGE_60S,
				AgeBand.AGE_70S,
				AgeBand.AGE_80S,
				AgeBand.AGE_90S_PLUS,
				AgeBand.UNDISCLOSED,
			)
		}

		if (ageBand !in allowed) {
			throw BadRequestException("선택한 역할에서 사용할 수 없는 연령대입니다.")
		}
	}
}
