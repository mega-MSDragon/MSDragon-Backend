package com.msdragon.backend.auth.repository

import com.msdragon.backend.auth.entity.OAuthProvider
import com.msdragon.backend.auth.entity.User
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<User, Long> {
	fun findByOauthProviderAndOauthSubjectAndDeletedAtIsNull(
		oauthProvider: OAuthProvider,
		oauthSubject: String,
	): User?

	fun findByIdAndDeletedAtIsNull(id: Long): User?
}
