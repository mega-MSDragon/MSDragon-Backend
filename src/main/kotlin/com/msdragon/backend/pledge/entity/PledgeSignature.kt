package com.msdragon.backend.pledge.entity

import com.msdragon.backend.auth.entity.User
import com.msdragon.backend.common.entity.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime

@Entity
@Table(
	name = "pledge_signatures",
	uniqueConstraints = [
		UniqueConstraint(name = "uk_pledge_signatures_pledge_user", columnNames = ["trip_pledge_id", "user_id"]),
	],
)
class PledgeSignature(
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "trip_pledge_id", nullable = false)
	val tripPledge: TripPledge,

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	val user: User,

	@Column(name = "signature_image_data", nullable = false, columnDefinition = "bytea")
	val signatureImageData: ByteArray,

	@Column(name = "signature_mime_type", nullable = false, length = 30)
	val signatureMimeType: String,

	@Column(name = "signed_at", nullable = false)
	val signedAt: LocalDateTime,
) : BaseTimeEntity() {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	var id: Long? = null
		protected set
}
