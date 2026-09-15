package com.msdragon.backend.trip.support

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TourApiTextFormatterTest {
	@Test
	fun `원문이 나눠둔 문단은 그대로 두고 빈 줄로 띄운다`() {
		val text = "경복궁은 조선왕조 제일의 법궁이다.\n경복궁은 1592년 임진왜란으로 소실되었다. \n\n\n현재는 복원되었다."

		assertEquals(
			"경복궁은 조선왕조 제일의 법궁이다.\n\n경복궁은 1592년 임진왜란으로 소실되었다.\n\n현재는 복원되었다.",
			TourApiTextFormatter.readable(text),
		)
	}

	@Test
	fun `줄바꿈이 없는 긴 글은 세 문장씩 문단으로 나눈다`() {
		// 실제 TourAPI 개요에는 800자가 넘는 글이 줄바꿈 하나 없이 오는 경우가 많다.
		val text = (1..7).joinToString(" ") { "${it}번째 문장이며 길이를 채우기 위해 적당히 늘려 쓴 설명이다." }

		val paragraphs = requireNotNull(TourApiTextFormatter.readable(text)).split("\n\n")

		assertEquals(3, paragraphs.size)
		assertTrue(paragraphs[0].startsWith("1번째"))
		assertTrue(paragraphs[1].startsWith("4번째"))
		assertTrue(paragraphs[2].startsWith("7번째"))
	}

	@Test
	fun `짧은 글은 한 덩어리로 둔다`() {
		val text = "작은 마을에 있는 조용한 정원이다. 사계절 내내 개방한다."

		assertEquals(text, TourApiTextFormatter.readable(text))
	}

	@Test
	fun `HTML 태그와 엔티티를 정리한다`() {
		val text = "첫 문단입니다.<br />둘째 문단입니다.&nbsp;<b>굵게</b> 표시된 부분&amp;기호입니다."

		assertEquals(
			"첫 문단입니다.\n\n둘째 문단입니다. 굵게 표시된 부분&기호입니다.",
			TourApiTextFormatter.readable(text),
		)
	}

	@Test
	fun `소수점은 문장 경계로 보지 않는다`() {
		// 150자를 넘겨야 문단 분리가 동작한다.
		val text = "평점 4.5점을 받은 곳이며 사계절 내내 방문객이 늘 많은 편이어서 주말에는 특히 붐빈다. " +
			"입장료는 3.000원이고 주차장은 무료로 운영하고 있어 차를 가져와도 부담이 없는 편이다. " +
			"연중무휴로 개방하며 반려동물 동반은 제한하고 있으니 미리 확인하고 방문하는 것이 좋다. " +
			"주변에 산책로가 잘 조성되어 있어 천천히 걷기에 좋고 중간중간 앉아 쉴 곳도 마련되어 있다."

		val paragraphs = requireNotNull(TourApiTextFormatter.readable(text)).split("\n\n")

		assertEquals(2, paragraphs.size)
		assertTrue(paragraphs[0].contains("4.5점"))
		assertTrue(paragraphs[0].contains("3.000원"))
	}

	@Test
	fun `비어 있으면 null을 돌려준다`() {
		assertNull(TourApiTextFormatter.readable(null))
		assertNull(TourApiTextFormatter.readable("   "))
	}
}
