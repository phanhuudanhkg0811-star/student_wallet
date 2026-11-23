package com.example.student_wallet.core.domain.export

import java.io.File
import java.io.OutputStream

/** Exports a [ReportDocument] to an output stream or a shareable cache file. */
interface ReportExporter {
    suspend fun exportToStream(
        document: ReportDocument,
        format: ExportFormat,
        outputStream: OutputStream,
    )

    suspend fun exportToCacheFile(
        document: ReportDocument,
        format: ExportFormat,
        fileNamePrefix: String,
    ): File
}
