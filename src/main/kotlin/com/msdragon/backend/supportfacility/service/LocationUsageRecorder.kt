package com.msdragon.backend.supportfacility.service

import com.msdragon.backend.supportfacility.entity.LocationUsageLog
import com.msdragon.backend.supportfacility.repository.LocationUsageLogRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class LocationUsageRecorder(private val repository: LocationUsageLogRepository) {
	// AI 채팅 등 호출자의 트랜잭션이 롤백되어도 이미 시작한 이용 기록은 유지한다.
	// 저장 실패를 전파하여 기록 없이 위치정보를 사용하는 것을 막는다.
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	fun record(userId: Long, tripId: Long, serviceName: String, externalRecipient: String? = null) {
		repository.save(LocationUsageLog(
			userId = userId,
			tripId = tripId,
			serviceName = serviceName,
			eventType = if (externalRecipient == null) "USE" else "EXTERNAL_REQUEST_ATTEMPT",
			externalRecipient = externalRecipient,
		))
	}
}
