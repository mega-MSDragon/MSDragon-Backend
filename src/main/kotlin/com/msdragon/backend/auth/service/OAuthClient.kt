package com.msdragon.backend.auth.service

import com.msdragon.backend.auth.entity.OAuthProvider

interface OAuthClient {
	fun verify(token: String): OAuthUserInfo

	/**
	 * 소셜 인증 코드를 provider refresh token으로 교환한다.
	 * 탈퇴 시 연결 해제에 필요한 provider 자격증명을 미리 확보하는 용도이며,
	 * 교환이 필요 없는 provider는 null을 반환한다.
	 */
	fun exchangeRefreshToken(authorizationCode: String): String? = null

	/**
	 * provider 쪽 앱 연결을 해제한다. 탈퇴 처리에서 best-effort로 호출하므로
	 * 설정이 없거나 대상 정보가 부족하면 조용히 건너뛰고, 실패는 예외로 알린다.
	 */
	fun unlink(target: OAuthUnlinkTarget)
}

/**
 * 탈퇴 시 provider 연결 해제에 필요한 정보.
 * 익명화가 [com.msdragon.backend.auth.entity.User.oauthSubject]를 덮어쓰므로 그 전에 만들어 둔다.
 */
data class OAuthUnlinkTarget(
	val provider: OAuthProvider,
	val subject: String,
	val oauthRefreshToken: String?,
)
