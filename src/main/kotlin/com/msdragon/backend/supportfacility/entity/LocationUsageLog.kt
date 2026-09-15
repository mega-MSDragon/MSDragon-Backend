package com.msdragon.backend.supportfacility.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant

/** 좌표·토큰·요청 본문 없이 위치정보 이용 사실만 보관한다. */
@Entity
@Table(name = "location_usage_logs", indexes = [
	Index(name = "ix_location_usage_logs_user_time", columnList = "user_id, occurred_at"),
	Index(name = "ix_location_usage_logs_time", columnList = "occurred_at"),
])
class LocationUsageLog(
	@Column(name = "user_id", nullable = false, updatable = false)
	val userId: Long,
	@Column(name = "trip_id", nullable = false, updatable = false)
	val tripId: Long,
	@Column(name = "service_name", nullable = false, updatable = false, length = 40)
	val serviceName: String,
	@Column(name = "event_type", nullable = false, updatable = false, length = 40)
	val eventType: String,
	@Column(name = "external_recipient", updatable = false, length = 40)
	val externalRecipient: String? = null,
	@Column(name = "acquisition_source", nullable = false, updatable = false, length = 40)
	val acquisitionSource: String = "APP_DEVICE_LOCATION",
	@Column(name = "occurred_at", nullable = false, updatable = false)
	val occurredAt: Instant = Instant.now(),
) {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	var id: Long? = null
		protected set
}
