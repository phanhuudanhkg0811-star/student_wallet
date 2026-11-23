package com.example.student_wallet.core.data.export

import com.example.student_wallet.core.domain.export.ReportDocument
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.math.max
import kotlin.math.min

/**
 * Pure-Kotlin OOXML `.xlsx` writer backed by [ZipOutputStream] so no heavyweight JVM Excel
 * libraries (e.g. Apache POI) are needed in the Android APK.
 */
object XlsxReportWriter {

    fun write(document: ReportDocument, outputStream: OutputStream) {
        val zip = ZipOutputStream(outputStream, StandardCharsets.UTF_8)
        zip.writeTextEntry("[Content_Types].xml", CONTENT_TYPES_XML)
        zip.writeTextEntry("_rels/.rels", ROOT_RELS_XML)
        zip.writeTextEntry("xl/workbook.xml", WORKBOOK_XML)
        zip.writeTextEntry("xl/_rels/workbook.xml.rels", WORKBOOK_RELS_XML)
        zip.writeTextEntry("xl/styles.xml", STYLES_XML)
        zip.writeTextEntry("xl/worksheets/sheet1.xml", buildSheetXml(document))
        // finish() finalizes the ZIP central directory without closing the caller's SAF stream.
        zip.finish()
        zip.flush()
    }

    internal fun columnName(zeroBasedIndex: Int): String {
        require(zeroBasedIndex >= 0) { "Column index must be non-negative: $zeroBasedIndex" }
        val builder = StringBuilder()
        var current = zeroBasedIndex
        while (current >= 0) {
            val remainder = current % 26
            builder.append(('A'.code + remainder).toChar())
            current = (current / 26) - 1
        }
        return builder.reverse().toString()
    }

    internal fun escapeXml(raw: String): String {
        val out = StringBuilder(raw.length)
        for (ch in raw) {
            when (ch) {
                '&' -> out.append("&amp;")
                '<' -> out.append("&lt;")
                '>' -> out.append("&gt;")
                '"' -> out.append("&quot;")
                '\'' -> out.append("&apos;")
                else -> {
                    // Strip XML 1.0 disallowed control characters while keeping tab, LF, CR.
                    if (ch.code >= 0x20 || ch == '\t' || ch == '\n' || ch == '\r') {
                        out.append(ch)
                    }
                }
            }
        }
        return out.toString()
    }

    private fun buildSheetXml(document: ReportDocument): String {
        val columnCount = max(
            document.headers.size,
            max(document.rows.maxOfOrNull { it.size } ?: 0, 2),
        )
        val colWidths = IntArray(columnCount) { colIdx ->
            val headerLen = document.headers.getOrNull(colIdx)?.length ?: 0
            val maxDataLen = document.rows.maxOfOrNull { row -> row.getOrNull(colIdx)?.length ?: 0 } ?: 0
            val summaryLen = when (colIdx) {
                0 -> document.summaryRows.maxOfOrNull { it.label.length } ?: 0
                1 -> document.summaryRows.maxOfOrNull { it.value.length } ?: 0
                else -> 0
            }
            min(42, max(14, max(headerLen, max(maxDataLen, summaryLen)) + 4))
        }

        val sb = StringBuilder(4096)
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append(
            """<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""",
        )
        sb.append("<cols>")
        colWidths.forEachIndexed { idx, width ->
            val colNumber = idx + 1
            sb.append("""<col min="$colNumber" max="$colNumber" width="$width" customWidth="1"/>""")
        }
        sb.append("</cols>")
        sb.append("<sheetData>")

        var rowNumber = 1
        sb.appendRow(rowNumber++, listOf(StyledCell(document.title, styleIndex = 1)))
        if (document.subtitle.isNotBlank()) {
            sb.appendRow(rowNumber++, listOf(StyledCell(document.subtitle, styleIndex = 0)))
        }
        rowNumber++ // visual spacing row before table

        if (document.headers.isNotEmpty()) {
            sb.appendRow(
                rowNumber++,
                document.headers.map { StyledCell(it, styleIndex = 2) },
            )
        }
        document.rows.forEach { row ->
            sb.appendRow(
                rowNumber++,
                row.map { StyledCell(it, styleIndex = 0) },
            )
        }
        if (document.summaryRows.isNotEmpty()) {
            rowNumber++
            document.summaryRows.forEach { summary ->
                sb.appendRow(
                    rowNumber++,
                    listOf(
                        StyledCell(summary.label, styleIndex = 1),
                        StyledCell(summary.value, styleIndex = 1),
                    ),
                )
            }
        }

        sb.append("</sheetData></worksheet>")
        return sb.toString()
    }

    private fun StringBuilder.appendRow(rowNumber: Int, cells: List<StyledCell>) {
        append("""<row r="$rowNumber">""")
        cells.forEachIndexed { colIndex, cell ->
            val ref = "${columnName(colIndex)}$rowNumber"
            append("""<c r="$ref" t="inlineStr" s="${cell.styleIndex}">""")
            append("""<is><t xml:space="preserve">${escapeXml(cell.value)}</t></is>""")
            append("</c>")
        }
        append("</row>")
    }

    private fun ZipOutputStream.writeTextEntry(path: String, content: String) {
        putNextEntry(ZipEntry(path))
        write(content.toByteArray(StandardCharsets.UTF_8))
        closeEntry()
    }

    private data class StyledCell(
        val value: String,
        val styleIndex: Int,
    )

    private const val CONTENT_TYPES_XML =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" +
            """<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">""" +
            """<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>""" +
            """<Default Extension="xml" ContentType="application/xml"/>""" +
            """<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>""" +
            """<Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>""" +
            """<Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>""" +
            """</Types>"""

    private const val ROOT_RELS_XML =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" +
            """<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""" +
            """<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>""" +
            """</Relationships>"""

    private const val WORKBOOK_XML =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" +
            """<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" """ +
            """xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">""" +
            """<sheets><sheet name="Report" sheetId="1" r:id="rId1"/></sheets>""" +
            """</workbook>"""

    private const val WORKBOOK_RELS_XML =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" +
            """<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""" +
            """<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>""" +
            """<Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>""" +
            """</Relationships>"""

    // OOXML requires fillId 0 = none and fillId 1 = gray125 before custom header fills (fillId 2).
    private const val STYLES_XML =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" +
            """<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""" +
            """<fonts count="2">""" +
            """<font><sz val="11"/><name val="Calibri"/></font>""" +
            """<font><b/><sz val="11"/><name val="Calibri"/></font>""" +
            """</fonts>""" +
            """<fills count="3">""" +
            """<fill><patternFill patternType="none"/></fill>""" +
            """<fill><patternFill patternType="gray125"/></fill>""" +
            """<fill><patternFill patternType="solid"><fgColor rgb="FFE0F2EC"/><bgColor indexed="64"/></patternFill></fill>""" +
            """</fills>""" +
            """<borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders>""" +
            """<cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>""" +
            """<cellXfs count="3">""" +
            """<xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>""" +
            """<xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0" applyFont="1"/>""" +
            """<xf numFmtId="0" fontId="1" fillId="2" borderId="0" xfId="0" applyFont="1" applyFill="1"/>""" +
            """</cellXfs>""" +
            """</styleSheet>"""
}
