package com.msdragon.backend.pledge.service

import com.openhtmltopdf.extend.FSSupplier
import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder.FontStyle
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder
import com.msdragon.backend.auth.entity.UserRole
import com.msdragon.backend.common.exception.InternalServerException
import org.jsoup.Jsoup
import org.jsoup.helper.W3CDom
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.Entities
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Base64

@Component
class TripPledgePdfRenderer {
	private val templateResource = ClassPathResource(TEMPLATE_PATH)
	private val regularFontResource = ClassPathResource(REGULAR_FONT_PATH)
	private val boldFontResource = ClassPathResource(BOLD_FONT_PATH)

	fun render(data: TripPledgePdfData): ByteArray =
		try {
			val document = loadTemplate()
			document.slot("document-title").text(data.documentTitle)
			document.slot("trip-title").text(data.tripTitle)
			document.slot("trip-period").text(formatPeriod(data.startDate, data.endDate))
			appendItems(document.slot("pledge-items"), data.items)
			appendSignatures(document.slot("signature-list"), data.signatures)
			renderPdf(document)
		} catch (exception: Exception) {
			logger.error("여행 10계명 PDF 생성 실패: tripId={}", data.tripId, exception)
			throw InternalServerException("여행 10계명 PDF 생성에 실패했습니다.")
		}

	private fun loadTemplate(): Document =
		templateResource.inputStream.bufferedReader(StandardCharsets.UTF_8).use { reader ->
			Jsoup.parse(reader.readText()).also { document ->
				document.outputSettings()
					.syntax(Document.OutputSettings.Syntax.xml)
					.escapeMode(Entities.EscapeMode.xhtml)
					.charset(StandardCharsets.UTF_8)
			}
		}

	private fun appendItems(container: Element, items: List<String>) {
		container.empty()
		items.forEach { content ->
			container.appendElement("li").text(content)
		}
	}

	private fun appendSignatures(container: Element, signatures: List<TripPledgePdfSignature>) {
		container.empty()
		signatures.forEach { signature ->
			val card = container.appendElement("div").addClass("signature-card")
			card.appendElement("div")
				.addClass("signature-image-frame")
				.appendElement("img")
				.addClass("signature-image")
				.attr("src", "data:${signature.mimeType};base64,${Base64.getEncoder().encodeToString(signature.imageData)}")
				.attr("alt", "${signature.displayName} 서명")
			card.appendElement("div").addClass("signature-name").text(signature.displayName)
			card.appendElement("div").addClass("signature-role").text(signature.role.displayName())
			card.appendElement("div").addClass("signature-date").text(signature.signedAt.format(SIGNATURE_DATE_FORMAT))
		}
	}

	private fun renderPdf(document: Document): ByteArray =
		ByteArrayOutputStream().use { outputStream ->
			PdfRendererBuilder()
				.withW3cDocument(W3CDom().fromJsoup(document), null)
				.useFont(fontSupplier(regularFontResource), FONT_FAMILY, 400, FontStyle.NORMAL, true)
				.useFont(fontSupplier(boldFontResource), FONT_FAMILY, 700, FontStyle.NORMAL, true)
				.toStream(outputStream)
				.run()
			outputStream.toByteArray()
		}

	private fun fontSupplier(resource: ClassPathResource): FSSupplier<InputStream> =
		FSSupplier { resource.inputStream }

	private fun formatPeriod(startDate: LocalDate, endDate: LocalDate): String =
		if (startDate == endDate) {
			startDate.format(DATE_FORMAT)
		} else {
			"${startDate.format(DATE_FORMAT)} - ${endDate.format(DATE_FORMAT)}"
		}

	private fun Document.slot(id: String): Element =
		getElementById(id) ?: throw IllegalStateException("PDF 템플릿 슬롯을 찾을 수 없습니다: $id")

	private fun UserRole.displayName(): String =
		when (this) {
			UserRole.CHILD -> "자녀"
			UserRole.PARENT -> "부모"
		}

	companion object {
		private val logger = LoggerFactory.getLogger(TripPledgePdfRenderer::class.java)
		private val DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd")
		private val SIGNATURE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm")
		private const val TEMPLATE_PATH = "templates/pledge/trip-pledge.html"
		private const val REGULAR_FONT_PATH = "fonts/NanumGothic-Regular.ttf"
		private const val BOLD_FONT_PATH = "fonts/NanumGothic-Bold.ttf"
		private const val FONT_FAMILY = "NanumGothic"
	}
}

data class TripPledgePdfData(
	val tripId: Long,
	val documentTitle: String,
	val tripTitle: String,
	val startDate: LocalDate,
	val endDate: LocalDate,
	val items: List<String>,
	val signatures: List<TripPledgePdfSignature>,
)

data class TripPledgePdfSignature(
	val role: UserRole,
	val displayName: String,
	val mimeType: String,
	val imageData: ByteArray,
	val signedAt: LocalDateTime,
)

data class TripPledgePdfFile(
	val fileName: String,
	val content: ByteArray,
)
