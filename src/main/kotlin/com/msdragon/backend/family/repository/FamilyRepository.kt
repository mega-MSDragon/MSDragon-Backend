package com.msdragon.backend.family.repository

import com.msdragon.backend.family.entity.Family
import org.springframework.data.jpa.repository.JpaRepository

interface FamilyRepository : JpaRepository<Family, Long>
