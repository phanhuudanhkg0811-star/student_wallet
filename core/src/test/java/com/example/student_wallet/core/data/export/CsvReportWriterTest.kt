package com.example.student_wallet.core.data.export

import com.example.student_wallet.core.domain.export.ReportDocument
import com.example.student_wallet.core.domain.export.ReportSummaryRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

class CsvReportWriterTest {

    @Test
    fun `write outputs UTF-8 BOM, escaped cells, and summary rows`() {
        val doc = ReportDocument(
            title = "Báo cáo chi tiêu",
            subtitle = "Tháng 11/2026",
            headers = listOf("Ngày", "Danh mục", "Ghi chú", "Số tiền"),
            rows = listOf(
                listOf("23/11/2026", "Ăn uống", "Cơm tấm, \"trà đá\"", "-45.000 đ"),
            ),
            summaryRows = listOf(
                ReportSummaryRow("Tổng chi", "45.000 đ"),
            ),
        )
        val out = ByteArrayOutputStream()

        CsvReportWriter.write(doc, out)

        val bytes = out.toByteArray()
        assertEquals(0xEF.toByte(), bytes[0])
        assertEquals(0xBB.toByte(), bytes[1])
        assertEquals(0xBF.toByte(), bytes[2])

        val text = String(bytes, 3, bytes.size - 3, StandardCharsets.UTF_8)
        assertTrue(text.contains("Báo cáo chi tiêu"))
        assertTrue(text.contains("\"Cơm tấm, \"\"trà đá\"\"\""))
        assertTrue(text.contains("Tổng chi,45.000 đ"))
    }
}
