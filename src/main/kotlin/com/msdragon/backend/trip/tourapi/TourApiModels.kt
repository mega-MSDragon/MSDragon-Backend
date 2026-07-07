package com.msdragon.backend.trip.tourapi

import com.msdragon.backend.parentprofile.entity.FoodPreference
import com.msdragon.backend.parentprofile.entity.TravelPersonalityTypeCode
import com.msdragon.backend.parentprofile.entity.TravelThemeCode
import com.msdragon.backend.trip.entity.StopType
import com.msdragon.backend.trip.entity.TripDestinationCode
import java.math.BigDecimal

data class TourApiRegion(
	val lDongRegnCd: String,
	val lDongSignguCd: String? = null,
	val displayName: String,
)

data class TourApiPlaceSearch(
	val region: TourApiRegion,
	val contentTypeId: String,
	val numOfRows: Int = 20,
	val pageNo: Int = 1,
)

data class TourApiPlaceSummary(
	val contentId: String,
	val contentTypeId: String,
	val title: String,
	val address: String?,
	val latitude: BigDecimal?,
	val longitude: BigDecimal?,
	val tel: String?,
	val firstImage: String?,
	val firstImageThumbnail: String?,
	val lclsSystm1: String?,
	val lclsSystm2: String?,
	val lclsSystm3: String?,
	val raw: Map<String, Any?>,
)

data class TourApiPlaceDetail(
	val homepage: String?,
	val overview: String?,
	val raw: Map<String, Any?>,
)

data class TourApiAccessibility(
	val parking: String?,
	val publicTransport: String?,
	val route: String?,
	val wheelchair: String?,
	val exit: String?,
	val elevator: String?,
	val restroom: String?,
	val raw: Map<String, Any?>,
) {
	fun hasAnyPriorityInfo(): Boolean =
		listOf(parking, publicTransport, route, wheelchair, exit, elevator, restroom)
			.any { !it.isNullOrBlank() }
}

enum class TourApiContentType(
	val id: String,
	val displayName: String,
	val stopType: StopType,
) {
	TOURIST_ATTRACTION("12", "관광지", StopType.SIGHTSEEING),
	CULTURE_FACILITY("14", "문화시설", StopType.SIGHTSEEING),
	FESTIVAL_EVENT("15", "행사/공연/축제", StopType.SIGHTSEEING),
	LEPORTS("28", "레포츠", StopType.SIGHTSEEING),
	SHOPPING("38", "쇼핑", StopType.SIGHTSEEING),
	FOOD("39", "음식점", StopType.MEAL),
	;

	companion object {
		val recommendationTargets: List<TourApiContentType> = entries

		fun fromId(id: String): TourApiContentType? =
			entries.firstOrNull { it.id == id }
	}
}

data class DestinationTourApiPolicy(
	val destination: TripDestinationCode,
	val regions: List<TourApiRegion>,
) {
	companion object {
		fun of(destination: TripDestinationCode): DestinationTourApiPolicy =
			DestinationTourApiPolicy(
				destination = destination,
				regions = destinationRegions[destination].orEmpty(),
			)

		private val destinationRegions: Map<TripDestinationCode, List<TourApiRegion>> = mapOf(
			TripDestinationCode.DAEGU to listOf(TourApiRegion("27", displayName = "대구광역시")),
			TripDestinationCode.GANGNEUNG_SOKCHO to listOf(
				TourApiRegion("51", "150", "강릉시"),
				TourApiRegion("51", "210", "속초시"),
			),
			TripDestinationCode.GYEONGJU to listOf(TourApiRegion("47", "130", "경주시")),
			TripDestinationCode.BUSAN to listOf(TourApiRegion("26", displayName = "부산광역시")),
			TripDestinationCode.YEOSU to listOf(TourApiRegion("46", "130", "여수시")),
			TripDestinationCode.INCHEON to listOf(TourApiRegion("28", displayName = "인천광역시")),
			TripDestinationCode.JEONJU to listOf(
				TourApiRegion("52", "111", "전주시 완산구"),
				TourApiRegion("52", "113", "전주시 덕진구"),
			),
			TripDestinationCode.JEJU to listOf(
				TourApiRegion("50", "110", "제주시"),
				TourApiRegion("50", "130", "서귀포시"),
			),
			TripDestinationCode.SEOUL to listOf(TourApiRegion("11", displayName = "서울특별시")),
			TripDestinationCode.SUWON_YONGIN to listOf(
				TourApiRegion("41", "111", "수원시 장안구"),
				TourApiRegion("41", "113", "수원시 권선구"),
				TourApiRegion("41", "115", "수원시 팔달구"),
				TourApiRegion("41", "117", "수원시 영통구"),
				TourApiRegion("41", "461", "용인시 처인구"),
				TourApiRegion("41", "463", "용인시 기흥구"),
				TourApiRegion("41", "465", "용인시 수지구"),
			),
			TripDestinationCode.TONGYEONG_GEOJE_NAMHAE to listOf(
				TourApiRegion("48", "220", "통영시"),
				TourApiRegion("48", "310", "거제시"),
				TourApiRegion("48", "840", "남해군"),
			),
			TripDestinationCode.POHANG_ANDONG to listOf(
				TourApiRegion("47", "111", "포항시 남구"),
				TourApiRegion("47", "113", "포항시 북구"),
				TourApiRegion("47", "170", "안동시"),
			),
		)
	}
}

data class TourApiProfileSignals(
	val themeSystems: Set<String>,
	val personalitySystems: Set<String>,
	val foodPreference: FoodPreference,
) {
	companion object {
		fun of(
			travelThemes: Collection<TravelThemeCode>,
			personalityType: TravelPersonalityTypeCode,
			foodPreference: FoodPreference,
		): TourApiProfileSignals =
			TourApiProfileSignals(
				themeSystems = travelThemes.flatMap { themeSystemCodes[it].orEmpty() }.toSet(),
				personalitySystems = personalitySystemCodes[personalityType].orEmpty(),
				foodPreference = foodPreference,
			)

		private val themeSystemCodes: Map<TravelThemeCode, Set<String>> = mapOf(
			TravelThemeCode.NATURE_SCENERY to setOf("NA"),
			TravelThemeCode.HISTORY_CULTURE to setOf("HS", "VE"),
			TravelThemeCode.SHOPPING to setOf("SH"),
			TravelThemeCode.ACTIVITY to setOf("LS"),
			TravelThemeCode.CULTURE_LIFE to setOf("VE", "EV"),
			TravelThemeCode.LANDMARK to setOf("VE", "HS", "NA"),
			TravelThemeCode.EXPERIENCE to setOf("EX"),
		)

		private val personalitySystemCodes: Map<TravelPersonalityTypeCode, Set<String>> = mapOf(
			TravelPersonalityTypeCode.URBAN_EXPLORER to setOf("SH", "VE", "FD"),
			TravelPersonalityTypeCode.CULTURE_STROLLER to setOf("VE", "FD"),
			TravelPersonalityTypeCode.HEALING_TRAVELER to setOf("NA", "FD"),
			TravelPersonalityTypeCode.HERITAGE_WALKER to setOf("HS", "NA", "FD"),
			TravelPersonalityTypeCode.ACTIVE_ADVENTURER to setOf("LS", "EX", "FD"),
			TravelPersonalityTypeCode.LOCAL_CHALLENGER to setOf("EX", "FD"),
		)
	}
}
