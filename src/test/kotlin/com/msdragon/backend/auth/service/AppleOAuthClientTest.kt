package com.msdragon.backend.auth.service

import com.msdragon.backend.auth.config.AuthProperties
import com.msdragon.backend.auth.entity.OAuthProvider
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.crypto.ECDSAVerifier
import com.nimbusds.jwt.SignedJWT
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.security.KeyPairGenerator
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.util.Base64

class AppleOAuthClientTest {
	@Test
	fun createClientSecretSignsWithTeamKey() {
		val keyPair = generateKeyPair()
		val client = AppleOAuthClient(appleProperties(privateKey = toPem(keyPair.private.encoded)))

		val clientSecret = SignedJWT.parse(client.createClientSecret())

		assertThat(clientSecret.verify(ECDSAVerifier(keyPair.public as ECPublicKey))).isTrue()
		assertThat(clientSecret.header.algorithm).isEqualTo(JWSAlgorithm.ES256)
		assertThat(clientSecret.header.keyID).isEqualTo("test-key-id")
		assertThat(clientSecret.jwtClaimsSet.issuer).isEqualTo("test-team-id")
		assertThat(clientSecret.jwtClaimsSet.subject).isEqualTo("com.example.app")
		assertThat(clientSecret.jwtClaimsSet.audience).containsExactly("https://appleid.apple.com")
		assertThat(clientSecret.jwtClaimsSet.expirationTime).isAfter(clientSecret.jwtClaimsSet.issueTime)
	}

	@Test
	fun createClientSecretAcceptsSingleLinePrivateKey() {
		val keyPair = generateKeyPair()
		val singleLine = Base64.getEncoder().encodeToString(keyPair.private.encoded)
		val client = AppleOAuthClient(appleProperties(privateKey = singleLine))

		val clientSecret = SignedJWT.parse(client.createClientSecret())

		assertThat(clientSecret.verify(ECDSAVerifier(keyPair.public as ECPublicKey))).isTrue()
	}

	@Test
	fun skipsExchangeAndUnlinkWhenNotConfigured() {
		val client = AppleOAuthClient(AuthProperties())
		val target = OAuthUnlinkTarget(OAuthProvider.APPLE, "apple-subject", "apple-refresh-token")

		// 키 설정 전에도 배포할 수 있어야 한다. 외부 호출 없이 조용히 건너뛴다.
		assertThat(client.exchangeRefreshToken("authorization-code")).isNull()
		client.unlink(target)
	}

	@Test
	fun skipsUnlinkWhenRefreshTokenMissing() {
		val keyPair = generateKeyPair()
		val client = AppleOAuthClient(appleProperties(privateKey = toPem(keyPair.private.encoded)))

		// 이 기능 이전에 가입한 사용자는 저장된 refresh token이 없다. 탈퇴를 막지 않는다.
		client.unlink(OAuthUnlinkTarget(OAuthProvider.APPLE, "apple-subject", null))
	}

	private fun generateKeyPair() =
		KeyPairGenerator.getInstance("EC").apply { initialize(ECGenParameterSpec("secp256r1")) }.generateKeyPair()

	private fun toPem(encoded: ByteArray): String =
		"-----BEGIN PRIVATE KEY-----\n" +
			Base64.getMimeEncoder(64, "\n".toByteArray()).encodeToString(encoded) +
			"\n-----END PRIVATE KEY-----\n"

	private fun appleProperties(privateKey: String) =
		AuthProperties(
			oauth = AuthProperties.OAuth(
				apple = AuthProperties.Apple(
					clientId = "com.example.app",
					teamId = "test-team-id",
					keyId = "test-key-id",
					privateKey = privateKey,
				),
			),
		)
}
