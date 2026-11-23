package com.example.student_wallet.feature.security

import com.example.student_wallet.core.domain.export.ExportFormat
import com.example.student_wallet.core.domain.model.CurrencyOption
import com.example.student_wallet.core.domain.model.ThemeMode
import com.example.student_wallet.core.domain.model.UserSettings
import java.io.File
import java.io.OutputStream

data class SecurityUiState(
    val settings: UserSettings = UserSettings(),
    val selectedPeriod: ReportPeriod = ReportPeriod.THIS_MONTH,
    val selectedFormat: ExportFormat = ExportFormat.PDF,
    val periodStats: PeriodStats = PeriodStats(0, 0L, 0L, 0L),
    val isExporting: Boolean = false,
)

sealed interface SecurityIntent {
    data class SelectTheme(val themeMode: ThemeMode) : SecurityIntent
    data class ToggleDynamicColor(val enabled: Boolean) : SecurityIntent
    data class SelectCurrency(val currency: CurrencyOption) : SecurityIntent
    data class SelectPeriod(val period: ReportPeriod) : SecurityIntent
    data class SelectFormat(val format: ExportFormat) : SecurityIntent
    data object RequestSaveExport : SecurityIntent
    data object RequestShareExport : SecurityIntent
    data class ExportToStream(val openStream: () -> OutputStream?) : SecurityIntent
}

sealed interface SecurityEffect {
    data class LaunchCreateDocument(
        val suggestedFileName: String,
        val mimeType: String,
    ) : SecurityEffect

    data class ShareExportedFile(
        val file: File,
        val mimeType: String,
        val title: String,
    ) : SecurityEffect

    data class ShowMessage(val message: String) : SecurityEffect
}
