package com.example.student_wallet.feature.security

import com.example.student_wallet.core.domain.export.ExportFormat
import com.example.student_wallet.core.domain.model.CurrencyOption
import com.example.student_wallet.core.domain.model.ThemeMode
import com.example.student_wallet.core.domain.model.UserSettings
import com.example.student_wallet.feature.security.backup.BackupPreviewInfo
import com.example.student_wallet.feature.security.lock.AppLockState
import java.io.File
import java.io.InputStream
import java.io.OutputStream

enum class PinDialogMode {
    CREATE,
    CHANGE,
    DISABLE,
}

sealed interface BackupDialogState {
    data object CreatePassphrase : BackupDialogState

    data class RestoreFlow(
        val fileName: String,
        val preview: BackupPreviewInfo? = null,
    ) : BackupDialogState
}

data class SecurityUiState(
    val settings: UserSettings = UserSettings(),
    val lockState: AppLockState = AppLockState(),
    val activePinDialog: PinDialogMode? = null,
    val pinDialogError: String? = null,
    val isProcessingPin: Boolean = false,
    val selectedPeriod: ReportPeriod = ReportPeriod.THIS_MONTH,
    val selectedFormat: ExportFormat = ExportFormat.PDF,
    val periodStats: PeriodStats = PeriodStats(0, 0L, 0L, 0L),
    val isExporting: Boolean = false,
    val totalTransactionCount: Int = 0,
    val activeBackupDialog: BackupDialogState? = null,
    val backupDialogError: String? = null,
    val isBackupBusy: Boolean = false,
)

sealed interface SecurityIntent {
    data class SelectTheme(val themeMode: ThemeMode) : SecurityIntent
    data class ToggleDynamicColor(val enabled: Boolean) : SecurityIntent
    data class SelectCurrency(val currency: CurrencyOption) : SecurityIntent

    data object RequestTogglePinLock : SecurityIntent
    data object RequestChangePin : SecurityIntent
    data object DismissPinDialog : SecurityIntent
    data class SubmitCreatePin(val newPin: String) : SecurityIntent
    data class SubmitChangePin(val oldPin: String, val newPin: String) : SecurityIntent
    data class SubmitDisablePin(val currentPin: String) : SecurityIntent
    data class ToggleBiometricLock(val enabled: Boolean) : SecurityIntent
    data class SelectAutoLockSeconds(val seconds: Int) : SecurityIntent
    data object LockNowClick : SecurityIntent

    data class SelectPeriod(val period: ReportPeriod) : SecurityIntent
    data class SelectFormat(val format: ExportFormat) : SecurityIntent
    data object RequestSaveExport : SecurityIntent
    data object RequestShareExport : SecurityIntent
    data class ExportToStream(val openStream: () -> OutputStream?) : SecurityIntent

    data object OpenCreateBackupSheet : SecurityIntent
    data object RequestOpenBackupPicker : SecurityIntent
    data class RequestSaveBackup(val passphrase: String) : SecurityIntent
    data class RequestShareBackup(val passphrase: String) : SecurityIntent
    data class BackupToStream(val openStream: () -> OutputStream?) : SecurityIntent
    data class BackupFilePicked(
        val fileName: String,
        val openStream: () -> InputStream?,
    ) : SecurityIntent

    data class InspectRestoreWithPassphrase(val passphrase: String) : SecurityIntent
    data object ConfirmRestoreBackup : SecurityIntent
    data object DismissBackupDialog : SecurityIntent
}

sealed interface SecurityEffect {
    data class LaunchCreateDocument(
        val suggestedFileName: String,
        val mimeType: String,
    ) : SecurityEffect

    data class LaunchCreateBackupDocument(
        val suggestedFileName: String,
    ) : SecurityEffect

    data object LaunchOpenBackupDocument : SecurityEffect

    data class ShareExportedFile(
        val file: File,
        val mimeType: String,
        val title: String,
    ) : SecurityEffect

    data class ShowMessage(val message: String) : SecurityEffect
}
