package com.msdragon.backend.supportfacility.service

import com.msdragon.backend.auth.entity.DevicePlatform
import com.msdragon.backend.auth.entity.UserRole
import com.msdragon.backend.auth.support.AuthenticatedUser
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import kotlin.test.assertEquals

@SpringBootTest
class LocationUsagePersistenceTest {
	@Autowired private lateinit var recorder: LocationUsageRecorder
	@Autowired private lateinit var jdbc: JdbcTemplate
	@Autowired private lateinit var transactionManager: PlatformTransactionManager

	@Test
	fun `호출자 롤백과 관계없이 이용 기록과 UTC 시각을 저장한다`() {
		val userId = -982731L
		try {
			TransactionTemplate(transactionManager).executeWithoutResult { status ->
				recorder.record(AuthenticatedUser(userId, UserRole.CHILD, DevicePlatform.IOS), 5, "nearby_restrooms")
				status.setRollbackOnly()
			}
			val row = jdbc.queryForMap("select * from location_usage_logs where user_id = ?", userId)
			assertEquals("USE", row["event_type"] ?: row["EVENT_TYPE"])
			assertEquals("APPLE", row["acquisition_source"] ?: row["ACQUISITION_SOURCE"])
			assertEquals(1, jdbc.queryForObject("select count(*) from location_usage_logs where user_id = ? and occurred_at is not null", Int::class.java, userId))
		} finally {
			jdbc.update("delete from location_usage_logs where user_id = ?", userId)
		}
	}
}
