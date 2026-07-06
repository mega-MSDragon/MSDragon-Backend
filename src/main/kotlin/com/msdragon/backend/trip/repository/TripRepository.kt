package com.msdragon.backend.trip.repository

import com.msdragon.backend.trip.entity.Trip
import com.msdragon.backend.trip.entity.TripStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate

interface TripRepository : JpaRepository<Trip, Long> {
	fun findByIdAndDeletedAtIsNull(id: Long): Trip?

	fun findAllByFamilyIdAndDeletedAtIsNullOrderByStartDateAscIdAsc(familyId: Long): List<Trip>

	@Query(
		"""
		select count(t) > 0
		from Trip t
		where t.family.id = :familyId
		  and t.deletedAt is null
		  and t.status <> :excludedStatus
		  and t.startDate <= :endDate
		  and t.endDate >= :startDate
		""",
	)
	fun existsOverlappingTrip(
		@Param("familyId") familyId: Long,
		@Param("startDate") startDate: LocalDate,
		@Param("endDate") endDate: LocalDate,
		@Param("excludedStatus") excludedStatus: TripStatus,
	): Boolean
}
