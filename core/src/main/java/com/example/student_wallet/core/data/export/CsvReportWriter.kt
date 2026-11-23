package com.example.student_wallet.core.data.export

import com.example.student_wallet.core.domain.export.ReportDocument
import java.io.BufferedWriter
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

/** Writes a [ReportDocument] as RFC 4180 CSV with UTF-8 BOM for desktop Excel compatibility. */
object CsvReportWriter {

    private const val CRLF = "\r\n"

    fun write(document: ReportDocument, outputStream: OutputStream) {
        // UTF-8 BOM ensures Microsoft Excel on Windows detects Vietnamese UTF-8 automatically.
        outputStream.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
        val writer = BufferedWriter(OutputStreamWriter(outputStream, StandardCharsets.UTF_8))

        writer.appendRow(listOf(document.title))
        if (document.subtitle.isNotBlank()) {
            writer.appendRow(listOf(document.subtitle))
        }
        writer.append(CRLF)

        if (document.headers.isNotEmpty()) {
            writer.appendRow(document.headers)
        }
        document.rows.forEach { row ->
            writer.appendRow(row)
        }

        if (document.summaryRows.isNotEmpty()) {
            writer.append(CRLF)
            document.summaryRows.forEach { summary ->
                writer.appendRow(listOf(summary.label, summary.value))
            }
        }
        writer.flush()
    }

    internal fun escapeCell(raw: String): String {
        val needsQuotes = raw.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        if (!needsQuotes) return raw
        val escaped = raw.replace("\"", "\"\"")
        return "\"$escaped\""
    }

    private fun BufferedWriter.appendRow(cells: List<String>) {
        append(cells.joinToString(separator = ",") { escapeCell(it) })
        append(CRLF)
    }
}
