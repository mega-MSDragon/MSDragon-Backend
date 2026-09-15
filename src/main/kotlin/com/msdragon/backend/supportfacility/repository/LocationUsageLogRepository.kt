package com.msdragon.backend.supportfacility.repository

import com.msdragon.backend.supportfacility.entity.LocationUsageLog
import org.springframework.data.repository.Repository

// 서비스에 수정·삭제·일반 사용자 조회 기능을 노출하지 않는다.
interface LocationUsageLogRepository : Repository<LocationUsageLog, Long> {
	fun save(log: LocationUsageLog): LocationUsageLog
}
