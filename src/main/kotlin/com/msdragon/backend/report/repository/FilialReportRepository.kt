package com.msdragon.backend.report.repository

import com.msdragon.backend.report.entity.FilialReport
import org.springframework.data.jpa.repository.JpaRepository

interface FilialReportRepository : JpaRepository<FilialReport, Long> {
	fun findByTripId(tripId: Long): FilialReport?

	fun deleteByTripId(tripId: Long)
}
