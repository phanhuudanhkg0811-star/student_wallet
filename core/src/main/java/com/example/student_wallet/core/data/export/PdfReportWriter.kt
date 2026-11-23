package com.example.student_wallet.core.data.export

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.TextPaint
import android.text.TextUtils
import com.example.student_wallet.core.domain.export.ReportDocument
import java.io.OutputStream

/** Renders a [ReportDocument] onto multi-page A4 PDF sheets using Android's native [PdfDocument]. */
object PdfReportWriter {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN_H = 36f
    private const val MARGIN_TOP = 40f
    private const val MARGIN_BOTTOM = 44f
    private const val USABLE_WIDTH = PAGE_WIDTH - MARGIN_H * 2
    private const val ROW_HEIGHT = 22f

    fun write(document: ReportDocument, outputStream: OutputStream) {
        val pdf = PdfDocument()
        val paints = PdfPaints()
        val columnCount = maxOf(document.headers.size, document.rows.maxOfOrNull { it.size } ?: 0, 1)
        val colWidths = FloatArray(columnCount) { USABLE_WIDTH / columnCount }

        var pageNumber = 1
        var page = pdf.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
        var y = drawDocumentHeader(page.canvas, document, paints)
        y = drawTableHeader(page.canvas, document.headers, colWidths, y, paints)

        document.rows.forEachIndexed { index, row ->
            if (y + ROW_HEIGHT > PAGE_HEIGHT - MARGIN_BOTTOM) {
                drawFooter(page.canvas, pageNumber, paints)
                pdf.finishPage(page)
                pageNumber++
                page = pdf.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
                y = MARGIN_TOP
                y = drawTableHeader(page.canvas, document.headers, colWidths, y, paints)
            }
            drawDataRow(page.canvas, row, colWidths, y, isAlternate = index % 2 == 1, paints = paints)
            y += ROW_HEIGHT
        }

        if (document.summaryRows.isNotEmpty()) {
            val summaryHeight = 20f + document.summaryRows.size * 20f + 12f
            if (y + summaryHeight > PAGE_HEIGHT - MARGIN_BOTTOM) {
                drawFooter(page.canvas, pageNumber, paints)
                pdf.finishPage(page)
                pageNumber++
                page = pdf.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
                y = MARGIN_TOP
            }
            drawSummaryBox(page.canvas, document, y + 12f, paints)
        }

        drawFooter(page.canvas, pageNumber, paints)
        pdf.finishPage(page)

        try {
            pdf.writeTo(outputStream)
            outputStream.flush()
        } finally {
            pdf.close()
        }
    }

    private fun drawDocumentHeader(
        canvas: Canvas,
        document: ReportDocument,
        paints: PdfPaints,
    ): Float {
        var y = MARGIN_TOP
        val title = TextUtils.ellipsize(document.title, paints.title, USABLE_WIDTH, TextUtils.TruncateAt.END)
        canvas.drawText(title.toString(), MARGIN_H, y + 14f, paints.title)
        y += 24f

        if (document.subtitle.isNotBlank()) {
            val subtitle = TextUtils.ellipsize(document.subtitle, paints.subtitle, USABLE_WIDTH, TextUtils.TruncateAt.END)
            canvas.drawText(subtitle.toString(), MARGIN_H, y + 10f, paints.subtitle)
            y += 18f
        }
        canvas.drawLine(MARGIN_H, y + 4f, PAGE_WIDTH - MARGIN_H, y + 4f, paints.divider)
        return y + 14f
    }

    private fun drawTableHeader(
        canvas: Canvas,
        headers: List<String>,
        colWidths: FloatArray,
        startY: Float,
        paints: PdfPaints,
    ): Float {
        if (headers.isEmpty()) return startY
        canvas.drawRect(MARGIN_H, startY, PAGE_WIDTH - MARGIN_H, startY + ROW_HEIGHT, paints.headerFill)
        var x = MARGIN_H
        headers.forEachIndexed { idx, header ->
            val width = colWidths[idx]
            val text = TextUtils.ellipsize(header, paints.headerText, width - 8f, TextUtils.TruncateAt.END)
            canvas.drawText(text.toString(), x + 4f, startY + 15f, paints.headerText)
            x += width
        }
        return startY + ROW_HEIGHT
    }

    private fun drawDataRow(
        canvas: Canvas,
        row: List<String>,
        colWidths: FloatArray,
        startY: Float,
        isAlternate: Boolean,
        paints: PdfPaints,
    ) {
        if (isAlternate) {
            canvas.drawRect(MARGIN_H, startY, PAGE_WIDTH - MARGIN_H, startY + ROW_HEIGHT, paints.zebraFill)
        }
        var x = MARGIN_H
        colWidths.forEachIndexed { idx, width ->
            val cell = row.getOrNull(idx).orEmpty()
            val text = TextUtils.ellipsize(cell, paints.cellText, width - 8f, TextUtils.TruncateAt.END)
            canvas.drawText(text.toString(), x + 4f, startY + 14.5f, paints.cellText)
            x += width
        }
        canvas.drawLine(MARGIN_H, startY + ROW_HEIGHT, PAGE_WIDTH - MARGIN_H, startY + ROW_HEIGHT, paints.rowDivider)
    }

    private fun drawSummaryBox(
        canvas: Canvas,
        document: ReportDocument,
        startY: Float,
        paints: PdfPaints,
    ) {
        val boxHeight = 16f + document.summaryRows.size * 20f
        val box = RectF(MARGIN_H, startY, PAGE_WIDTH - MARGIN_H, startY + boxHeight)
        canvas.drawRoundRect(box, 8f, 8f, paints.headerFill)

        var y = startY + 20f
        document.summaryRows.forEach { summary ->
            canvas.drawText(summary.label, MARGIN_H + 12f, y, paints.headerText)
            val valWidth = paints.headerText.measureText(summary.value)
            canvas.drawText(summary.value, PAGE_WIDTH - MARGIN_H - 12f - valWidth, y, paints.headerText)
            y += 20f
        }
    }

    private fun drawFooter(canvas: Canvas, pageNumber: Int, paints: PdfPaints) {
        val text = "StudentWallet · Trang $pageNumber"
        val width = paints.subtitle.measureText(text)
        canvas.drawText(text, PAGE_WIDTH - MARGIN_H - width, PAGE_HEIGHT - 18f, paints.subtitle)
    }

    private class PdfPaints {
        val title = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF006C51.toInt()
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subtitle = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF404944.toInt()
            textSize = 9.5f
        }
        val headerText = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF002117.toInt()
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val cellText = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF171D1A.toInt()
            textSize = 9f
        }
        val headerFill = Paint().apply {
            color = 0xFFE0F2EC.toInt()
            style = Paint.Style.FILL
        }
        val zebraFill = Paint().apply {
            color = 0xFFF5FBF7.toInt()
            style = Paint.Style.FILL
        }
        val divider = Paint().apply {
            color = 0xFF006C51.toInt()
            strokeWidth = 1.2f
        }
        val rowDivider = Paint().apply {
            color = 0xFFDCE5DF.toInt()
            strokeWidth = 0.6f
        }
    }
}
