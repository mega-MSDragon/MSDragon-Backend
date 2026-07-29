package com.msdragon.backend.supportfacility.importer

import com.msdragon.backend.supportfacility.entity.SupportFacility
import com.msdragon.backend.supportfacility.entity.SupportFacilityType
import com.msdragon.backend.supportfacility.repository.SupportFacilityRepository
import com.msdragon.backend.supportfacility.tmap.TmapGeocodingClient
import com.msdragon.backend.trip.entity.ExternalApiProvider
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import org.apache.commons.csv.CSVRecord
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDateTime

@Component
class RestroomCsvImportService(
	private val supportFacilityRepository: SupportFacilityRepository,
	private val tmapGeocodingClient: TmapGeocodingClient,
	private val objectMapper: ObjectMapper,
) {
	fun importFrom(file: Path): RestroomImportResult {
		require(Files.isRegularFile(file)) { "화장실 CSV 파일을 찾을 수 없습니다: $file" }

		val existingSourceIds = supportFacilityRepository.findSourceIds(
			SupportFacilityType.RESTROOM,
			ExternalApiProvider.LOCAL_EXCEL,
		).toMutableSet()
		val pending = mutableListOf<SupportFacility>()
		val failures = mutableListOf<RestroomImportFailure>()
		var totalCount = 0
		var importedCount = 0
		var existingCount = 0
		var invalidCount = 0
		var notFoundCount = 0

		try {
			Files.newBufferedReader(file, CSV_CHARSET).use { reader ->
				CSV_FORMAT.parse(reader).use { parser ->
					for (record in parser) {
						totalCount++
						val row = record.toRestroomRow()
						if (row == null) {
							invalidCount++
							failures += RestroomImportFailure.invalid(record)
							continue
						}
						if (row.sourceId in existingSourceIds) {
							existingCount++
							continue
						}

						val coordinate = try {
							tmapGeocodingClient.geocode(row.address)
						} catch (exception: Exception) {
							failures += RestroomImportFailure.from(row, record.recordNumber, "geocoding_api_error")
							throw exception
						}
						if (coordinate == null) {
							notFoundCount++
							failures += RestroomImportFailure.from(row, record.recordNumber, "geocoding_not_found")
							continue
						}
						pending += SupportFacility(
							facilityType = SupportFacilityType.RESTROOM,
							provider = ExternalApiProvider.LOCAL_EXCEL,
							sourceId = row.sourceId,
							name = row.name,
							address = row.address,
							latitude = coordinate.latitude,
							longitude = coordinate.longitude,
							phone = row.phone,
							operatingHours = row.operatingHours,
							rawData = objectMapper.writeValueAsString(
								record.toMap() + ("geocodingProvider" to ExternalApiProvider.TMAP.value),
							),
							lastSyncedAt = LocalDateTime.now(),
						)
						existingSourceIds += row.sourceId
						importedCount++
						if (pending.size == BATCH_SIZE) {
							save(pending)
						}
					}
				}
			}
		} finally {
			try {
				save(pending)
			} finally {
				writeFailures(file, failures)
			}
		}

		return RestroomImportResult(
			totalCount = totalCount,
			importedCount = importedCount,
			existingCount = existingCount,
			invalidCount = invalidCount,
			notFoundCount = notFoundCount,
			failureFile = failureFile(file).toString(),
		)
	}

	private fun save(pending: MutableList<SupportFacility>) {
		if (pending.isEmpty()) {
			return
		}
		supportFacilityRepository.saveAll(pending)
		pending.clear()
	}

	private fun CSVRecord.toRestroomRow(): RestroomCsvRow? {
		val localGovernmentCode = value("개방자치단체코드") ?: return null
		val managementNumber = value("관리번호") ?: return null
		val name = value("화장실명") ?: return null
		val address = value("소재지도로명주소") ?: value("소재지지번주소") ?: return null
		val openingType = value("개방시간")
		val openingDetail = value("개방시간상세")
		return RestroomCsvRow(
			sourceId = "$localGovernmentCode:$managementNumber",
			name = name,
			address = address,
			phone = value("전화번호"),
			operatingHours = listOfNotNull(openingType, openingDetail)
				.distinct()
				.joinToString(" · ")
				.takeIf(String::isNotBlank),
		)
	}

	private fun CSVRecord.value(header: String): String? =
		takeIf { isMapped(header) && isSet(header) }
			?.get(header)
			?.trim()
			?.takeIf(String::isNotEmpty)

	private fun writeFailures(sourceFile: Path, failures: List<RestroomImportFailure>) {
		Files.newBufferedWriter(failureFile(sourceFile), StandardCharsets.UTF_8).use { writer ->
			CSVPrinter(writer, FAILURE_CSV_FORMAT).use { printer ->
				failures.forEach { failure ->
					printer.printRecord(
						failure.recordNumber,
						failure.sourceId,
						failure.name,
						failure.address,
						failure.reason,
					)
				}
			}
		}
	}

	private fun failureFile(sourceFile: Path): Path {
		val fileName = sourceFile.fileName.toString()
		val baseName = fileName.substringBeforeLast('.', fileName)
		return sourceFile.resolveSibling("$baseName-failures.csv")
	}

	companion object {
		private val CSV_CHARSET: Charset = Charset.forName("MS949")
		private val CSV_FORMAT: CSVFormat = CSVFormat.DEFAULT.builder()
			.setHeader()
			.setSkipHeaderRecord(true)
			.setIgnoreEmptyLines(true)
			.get()
		private val FAILURE_CSV_FORMAT: CSVFormat = CSVFormat.DEFAULT.builder()
			.setHeader("recordNumber", "sourceId", "name", "address", "reason")
			.get()
		private const val BATCH_SIZE = 100
	}
}

data class RestroomImportResult(
	val totalCount: Int,
	val importedCount: Int,
	val existingCount: Int,
	val invalidCount: Int,
	val notFoundCount: Int,
	val failureFile: String,
)

private data class RestroomCsvRow(
	val sourceId: String,
	val name: String,
	val address: String,
	val phone: String?,
	val operatingHours: String?,
)

private data class RestroomImportFailure(
	val recordNumber: Long,
	val sourceId: String?,
	val name: String?,
	val address: String?,
	val reason: String,
) {
	companion object {
		fun invalid(record: CSVRecord): RestroomImportFailure =
			RestroomImportFailure(
				recordNumber = record.recordNumber,
				sourceId = listOfNotNull(record.optionalValue("개방자치단체코드"), record.optionalValue("관리번호"))
					.joinToString(":")
					.takeIf(String::isNotBlank),
				name = record.optionalValue("화장실명"),
				address = record.optionalValue("소재지도로명주소") ?: record.optionalValue("소재지지번주소"),
				reason = "invalid_row",
			)

		fun from(row: RestroomCsvRow, recordNumber: Long, reason: String): RestroomImportFailure =
			RestroomImportFailure(
				recordNumber = recordNumber,
				sourceId = row.sourceId,
				name = row.name,
				address = row.address,
				reason = reason,
			)
	}
}

private fun CSVRecord.optionalValue(header: String): String? =
	takeIf { isMapped(header) && isSet(header) }
		?.get(header)
		?.trim()
		?.takeIf(String::isNotEmpty)

@ConfigurationProperties(prefix = "app.restroom-import")
data class RestroomImportProperties(
	val enabled: Boolean = false,
	val file: String = "",
)

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
@ConditionalOnProperty(prefix = "app.restroom-import", name = ["enabled"], havingValue = "true")
class RestroomCsvImportRunner(
	private val properties: RestroomImportProperties,
	private val importService: RestroomCsvImportService,
) : ApplicationRunner {
	override fun run(args: ApplicationArguments) {
		require(properties.file.isNotBlank()) { "app.restroom-import.file 설정이 필요합니다." }
		val result = importService.importFrom(Path.of(properties.file))
		LOGGER.info(
			"공중화장실 적재 완료: total={}, imported={}, existing={}, invalid={}, notFound={}, failureFile={}",
			result.totalCount,
			result.importedCount,
			result.existingCount,
			result.invalidCount,
			result.notFoundCount,
			result.failureFile,
		)
	}

	companion object {
		private val LOGGER = LoggerFactory.getLogger(RestroomCsvImportRunner::class.java)
	}
}
