package com.example.student_wallet.core.data.export

import android.content.Context
import com.example.student_wallet.core.domain.export.ExportFormat
import com.example.student_wallet.core.domain.export.ReportDocument
import com.example.student_wallet.core.domain.export.ReportExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

/** Default [ReportExporter] implementation dispatching heavy I/O and rendering to [Dispatchers.IO]. */
class AndroidReportExporter(
    private val context: Context,
) : ReportExporter {

    override suspend fun exportToStream(
        document: ReportDocument,
        format: ExportFormat,
        outputStream: OutputStream,
    ) {
        withContext(Dispatchers.IO) {
            when (format) {
                ExportFormat.CSV -> CsvReportWriter.write(document, outputStream)
                ExportFormat.XLSX -> XlsxReportWriter.write(document, outputStream)
                ExportFormat.PDF -> PdfReportWriter.write(document, outputStream)
            }
        }
    }

    override suspend fun exportToCacheFile(
        document: ReportDocument,
        format: ExportFormat,
        fileNamePrefix: String,
    ): File = withContext(Dispatchers.IO) {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        pruneOldExports(exportDir)
        val safePrefix = fileNamePrefix.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val file = File(exportDir, "${safePrefix}.${format.extension}")
        FileOutputStream(file).use { stream ->
            exportToStream(document, format, stream)
        }
        file
    }

    private fun pruneOldExports(exportDir: File) {
        val cutoffMillis = System.currentTimeMillis() - EXPORT_CACHE_TTL_MILLIS
        exportDir.listFiles()?.forEach { file ->
            if (file.lastModified() < cutoffMillis) {
                file.delete()
            }
        }
    }

    private companion object {
        const val EXPORT_CACHE_TTL_MILLIS = 24L * 60L * 60L * 1000L
    }
}
