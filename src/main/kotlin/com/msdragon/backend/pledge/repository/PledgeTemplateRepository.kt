package com.msdragon.backend.pledge.repository

import com.msdragon.backend.pledge.entity.PledgeTemplate
import org.springframework.data.jpa.repository.JpaRepository

interface PledgeTemplateRepository : JpaRepository<PledgeTemplate, Long> {
	fun findAllByIsActiveTrueOrderByIdAsc(): List<PledgeTemplate>
}
