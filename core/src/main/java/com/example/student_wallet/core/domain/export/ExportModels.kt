package com.example.student_wallet.core.domain.export

/** Supported document formats for shared report exports (Personal & Group). */
enum class ExportFormat(
    val extension: String,
    val mimeType: String,
    val displayLabel: String,
) {
    PDF(
        extension = "pdf",
        mimeType = "application/pdf",
        displayLabel = "PDF",
    ),
    XLSX(
        extension = "xlsx",
        mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        displayLabel = "Excel (.xlsx)",
    ),
    CSV(
        extension = "csv",
        mimeType = "text/csv",
        displayLabel = "CSV",
    ),
}

/** Key-value summary row rendered below the main report table. */
data class ReportSummaryRow(
    val label: String,
    val value: String,
)

/** Tabular report payload shared between `:feature:security` (30) and `:feature:group` (23). */
data class ReportDocument(
    val title: String,
    val subtitle: String,
    val headers: List<String>,
    val rows: List<List<String>>,
    val summaryRows: List<ReportSummaryRow> = emptyList(),
    val generatedAtMillis: Long = System.currentTimeMillis(),
)
