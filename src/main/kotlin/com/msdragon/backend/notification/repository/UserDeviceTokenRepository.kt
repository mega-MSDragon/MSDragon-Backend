package com.msdragon.backend.notification.repository

import com.msdragon.backend.notification.entity.UserDeviceToken
import org.springframework.data.jpa.repository.JpaRepository

interface UserDeviceTokenRepository : JpaRepository<UserDeviceToken, Long> {
	fun findByToken(token: String): UserDeviceToken?

	fun findAllByUserId(userId: Long): List<UserDeviceToken>

	fun findAllByUserIdIn(userIds: Collection<Long>): List<UserDeviceToken>

	fun deleteByToken(token: String)

	fun deleteAllByUserId(userId: Long)
}
