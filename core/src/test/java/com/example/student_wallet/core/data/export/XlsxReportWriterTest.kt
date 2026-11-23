package com.example.student_wallet.core.data.export

import com.example.student_wallet.core.domain.export.ReportDocument
import com.example.student_wallet.core.domain.export.ReportSummaryRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipInputStream

class XlsxReportWriterTest {

    @Test
    fun `columnName converts zero-based indexes to Excel letters`() {
        assertEquals("A", XlsxReportWriter.columnName(0))
        assertEquals("Z", XlsxReportWriter.columnName(25))
        assertEquals("AA", XlsxReportWriter.columnName(26))
    }

    @Test
    fun `write produces valid OOXML zip entries with XML-escaped Vietnamese text`() {
        val doc = ReportDocument(
            title = "Chi tiêu <Tháng 11>",
            subtitle = "SV HCMUS & Bạn bè",
            headers = listOf("Danh mục", "Số tiền"),
            rows = listOf(listOf("Ăn uống", "45.000 đ")),
            summaryRows = listOf(ReportSummaryRow("Tổng", "45.000 đ")),
        )
        val out = ByteArrayOutputStream()

        XlsxReportWriter.write(doc, out)

        val entries = mutableMapOf<String, String>()
        ZipInputStream(ByteArrayInputStream(out.toByteArray())).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                entries[entry.name] = String(zip.readBytes(), StandardCharsets.UTF_8)
                entry = zip.nextEntry
            }
        }

        assertTrue(entries.containsKey("[Content_Types].xml"))
        assertTrue(entries.containsKey("_rels/.rels"))
        assertTrue(entries.containsKey("xl/workbook.xml"))
        assertTrue(entries.containsKey("xl/_rels/workbook.xml.rels"))
        assertTrue(entries.containsKey("xl/styles.xml"))
        val sheetXml = entries.getValue("xl/worksheets/sheet1.xml")
        assertTrue(sheetXml.contains("Chi tiêu &lt;Tháng 11&gt;"))
        assertTrue(sheetXml.contains("SV HCMUS &amp; Bạn bè"))
        assertTrue(sheetXml.contains("Ăn uống"))
    }
}
