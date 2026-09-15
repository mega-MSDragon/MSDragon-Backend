package com.msdragon.backend.trip.support

import org.jsoup.Jsoup

/**
 * TourAPI 개요 텍스트를 읽기 좋게 다듬는다.
 *
 * 원문은 콘텐츠마다 편차가 크다. 줄바꿈이 문단 단위로 들어 있기도 하고, 800자가 넘는 글이
 * 줄바꿈 하나 없이 한 덩어리로 오기도 한다. 부모님이 읽는 화면이라 그대로 내보내면 읽기 어렵다.
 */
object TourApiTextFormatter {
	fun readable(value: String?): String? {
		val text = value?.takeIf(String::isNotBlank) ?: return null

		val paragraphs = stripMarkup(text)
			.replace("\r\n", "\n")
			.replace('\r', '\n')
			.split('\n')
			.map { it.replace(WHITESPACE, " ").trim() }
			.filter(String::isNotEmpty)

		val normalized = when {
			paragraphs.isEmpty() -> return null
			// 줄바꿈이 전혀 없는 긴 글만 문장 단위로 끊는다. 원문이 문단을 나눠뒀으면 그대로 존중한다.
			paragraphs.size == 1 && paragraphs.first().length > MIN_SPLIT_LENGTH ->
				splitIntoParagraphs(paragraphs.first())
			else -> paragraphs
		}

		return normalized.joinToString("\n\n")
	}

	/** `<br>`과 문단 태그는 줄바꿈으로 바꾼 뒤 나머지 태그를 걷어내고 HTML 엔티티를 되돌린다. */
	private fun stripMarkup(value: String): String =
		Jsoup.parse(value.replace(LINE_BREAK_TAG, "\n")).wholeText()

	/**
	 * ponytail: 마침표 뒤 공백을 문장 경계로 보는 휴리스틱이다. 소수점(`4.5`)은 뒤에 공백이 없어
	 * 걸리지 않는다. 더 정확히 나누려면 형태소 분석이 필요한데 개요 가독성에는 과하다.
	 */
	private fun splitIntoParagraphs(text: String): List<String> =
		text.split(SENTENCE_BOUNDARY)
			.filter(String::isNotBlank)
			.chunked(SENTENCES_PER_PARAGRAPH)
			.map { it.joinToString(" ") }

	private val LINE_BREAK_TAG = Regex("(?i)<br\\s*/?>|</p\\s*>|</div\\s*>|</li\\s*>")
	private val WHITESPACE = Regex("[ \\t\\u00A0]+")
	private val SENTENCE_BOUNDARY = Regex("(?<=[.!?])\\s+")
	private const val MIN_SPLIT_LENGTH = 150
	private const val SENTENCES_PER_PARAGRAPH = 3
}
