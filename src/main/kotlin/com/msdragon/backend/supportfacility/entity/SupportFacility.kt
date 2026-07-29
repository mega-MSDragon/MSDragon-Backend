package com.msdragon.backend.supportfacility.entity

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import com.msdragon.backend.auth.entity.DbEnum
import com.msdragon.backend.common.entity.BaseTimeEntity
import com.msdragon.backend.common.exception.BadRequestException
import com.msdragon.backend.trip.entity.ExternalApiProvider
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Column
import jakarta.persistence.Converter
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(
	name = "support_facilities",
	uniqueConstraints = [
		UniqueConstraint(
			name = "uk_support_facilities_type_provider_source",
			columnNames = ["facility_type", "provider", "source_id"],
		),
	],
	indexes = [
		Index(
			name = "ix_support_facilities_type_lat_lng",
			columnList = "facility_type, latitude, longitude",
		),
	],
)
class SupportFacility(
	@Column(name = "facility_type", nullable = false, length = 30)
	val facilityType: SupportFacilityType,

	@Column(name = "provider", nullable = false, length = 30)
	val provider: ExternalApiProvider,

	@Column(name = "source_id", nullable = false, length = 120)
	val sourceId: String,

	@Column(name = "name", nullable = false, length = 120)
	val name: String,

	@Column(name = "address", length = 255)
	val address: String?,

	@Column(name = "latitude", nullable = false, precision = 10, scale = 7)
	val latitude: BigDecimal,

	@Column(name = "longitude", nullable = false, precision = 10, scale = 7)
	val longitude: BigDecimal,

	@Column(name = "phone", length = 30)
	val phone: String? = null,

	@Column(name = "operating_hours", length = 255)
	val operatingHours: String? = null,

	@Column(name = "raw_data", columnDefinition = "text")
	val rawData: String? = null,

	@Column(name = "last_synced_at")
	val lastSyncedAt: LocalDateTime? = null,
) : BaseTimeEntity() {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	var id: Long? = null
		protected set
}

enum class SupportFacilityType(
	@get:JsonValue
	override val value: String,
) : DbEnum {
	RESTROOM("restroom"),
	HOSPITAL("hospital"),
	PHARMACY("pharmacy"),
	;

	companion object {
		@JvmStatic
		@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
		fun from(value: String): SupportFacilityType =
			entries.firstOrNull { it.value == value.lowercase() || it.name.equals(value, ignoreCase = true) }
				?: throw BadRequestException("지원하지 않는 시설 유형입니다: $value")
	}
}

@Converter(autoApply = true)
class SupportFacilityTypeConverter : AttributeConverter<SupportFacilityType, String> {
	override fun convertToDatabaseColumn(attribute: SupportFacilityType?): String? = attribute?.value

	override fun convertToEntityAttribute(dbData: String?): SupportFacilityType? =
		dbData?.let(SupportFacilityType::from)
}
