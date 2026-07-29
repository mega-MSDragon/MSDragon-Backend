package com.msdragon.backend.supportfacility.repository

import com.msdragon.backend.supportfacility.entity.SupportFacility
import com.msdragon.backend.supportfacility.entity.SupportFacilityType
import com.msdragon.backend.trip.entity.ExternalApiProvider
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.math.BigDecimal

interface SupportFacilityRepository : JpaRepository<SupportFacility, Long> {
	@Query(
		"""
		select f
		from SupportFacility f
		where f.facilityType = :facilityType
		  and f.latitude between :minLatitude and :maxLatitude
		  and f.longitude between :minLongitude and :maxLongitude
		""",
	)
	fun findWithinBoundingBox(
		@Param("facilityType") facilityType: SupportFacilityType,
		@Param("minLatitude") minLatitude: BigDecimal,
		@Param("maxLatitude") maxLatitude: BigDecimal,
		@Param("minLongitude") minLongitude: BigDecimal,
		@Param("maxLongitude") maxLongitude: BigDecimal,
	): List<SupportFacility>

	@Query(
		"""
		select f.sourceId
		from SupportFacility f
		where f.facilityType = :facilityType
		  and f.provider = :provider
		""",
	)
	fun findSourceIds(
		@Param("facilityType") facilityType: SupportFacilityType,
		@Param("provider") provider: ExternalApiProvider,
	): List<String>
}
