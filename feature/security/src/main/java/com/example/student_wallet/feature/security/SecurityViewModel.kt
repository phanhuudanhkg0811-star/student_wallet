package com.example.student_wallet.feature.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.student_wallet.core.domain.export.ExportFormat
import com.example.student_wallet.core.domain.export.ReportDocument
import com.example.student_wallet.core.domain.export.ReportExporter
import com.example.student_wallet.core.domain.model.Transaction
import com.example.student_wallet.core.domain.model.UserSettings
import com.example.student_wallet.core.domain.repository.SettingsRepository
import com.example.student_wallet.core.domain.repository.TransactionRepository
import com.example.student_wallet.feature.security.backup.WalletBackupCoordinator
import com.example.student_wallet.feature.security.lock.AppLockManager
import com.example.student_wallet.feature.security.lock.AppLockState
import com.example.student_wallet.feature.security.lock.PinVerifyResult
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class SecurityViewModel(
    private val settingsRepository: SettingsRepository,
    transactionRepository: TransactionRepository,
    private val reportExporter: ReportExporter,
    private val appLockManager: AppLockManager,
    private val backupCoordinator: WalletBackupCoordinator,
) : ViewModel() {

    private val localState = MutableStateFlow(LocalSecurityState())
    private val latestTransactions = MutableStateFlow<List<Transaction>>(emptyList())
    private var pendingBackupPassphrase: String? = null
    private var pendingRestoreOpenStream: (() -> InputStream?)? = null

    private val _effects = Channel<SecurityEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    val uiState: StateFlow<SecurityUiState> = combine(
        settingsRepository.observeSettings(),
        transactionRepository.observeTransactions(),
        appLockManager.state,
        localState,
    ) { settings, transactions, lockState, local ->
        latestTransactions.value = transactions
        val filtered = PersonalReportBuilder.filterByPeriod(transactions, local.selectedPeriod)
        SecurityUiState(
            settings = settings,
            lockState = lockState,
            activePinDialog = local.activePinDialog,
            pinDialogError = local.pinDialogError,
            isProcessingPin = local.isProcessingPin,
            selectedPeriod = local.selectedPeriod,
            selectedFormat = local.selectedFormat,
            periodStats = PersonalReportBuilder.summarize(filtered),
            isExporting = local.isExporting,
            totalTransactionCount = transactions.count { !it.isDeleted },
            activeBackupDialog = local.activeBackupDialog,
            backupDialogError = local.backupDialogError,
            isBackupBusy = local.isBackupBusy,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SecurityUiState(),
    )

    fun onIntent(intent: SecurityIntent) {
        when (intent) {
            is SecurityIntent.SelectTheme -> launchBlock {
                settingsRepository.setThemeMode(intent.themeMode)
            }
            is SecurityIntent.ToggleDynamicColor -> launchBlock {
                settingsRepository.setDynamicColor(intent.enabled)
            }
            is SecurityIntent.SelectCurrency -> launchBlock {
                settingsRepository.setCurrency(intent.currency)
                _effects.send(SecurityEffect.RefreshWidgets)
            }
            SecurityIntent.RequestTogglePinLock -> openTogglePinDialog()
            SecurityIntent.RequestChangePin -> localState.update {
                it.copy(activePinDialog = PinDialogMode.CHANGE, pinDialogError = null)
            }
            SecurityIntent.DismissPinDialog -> localState.update {
                it.copy(activePinDialog = null, pinDialogError = null, isProcessingPin = false)
            }
            is SecurityIntent.SubmitCreatePin -> submitCreatePin(intent.newPin)
            is SecurityIntent.SubmitChangePin -> submitChangePin(intent.oldPin, intent.newPin)
            is SecurityIntent.SubmitDisablePin -> submitDisablePin(intent.currentPin)
            is SecurityIntent.ToggleBiometricLock -> launchBlock {
                appLockManager.setBiometricEnabled(intent.enabled)
            }
            is SecurityIntent.SelectAutoLockSeconds -> launchBlock {
                appLockManager.setAutoLockSeconds(intent.seconds)
            }
            SecurityIntent.LockNowClick -> appLockManager.lockNow()
            is SecurityIntent.SelectPeriod -> localState.update { it.copy(selectedPeriod = intent.period) }
            is SecurityIntent.SelectFormat -> localState.update { it.copy(selectedFormat = intent.format) }
            SecurityIntent.RequestSaveExport -> requestSaveExport()
            SecurityIntent.RequestShareExport -> requestShareExport()
            is SecurityIntent.ExportToStream -> exportToStream(intent.openStream)
            SecurityIntent.OpenCreateBackupSheet -> openCreateBackupSheet()
            SecurityIntent.RequestOpenBackupPicker -> launchBlock {
                _effects.send(SecurityEffect.LaunchOpenBackupDocument)
            }
            is SecurityIntent.RequestSaveBackup -> requestSaveBackup(intent.passphrase)
            is SecurityIntent.RequestShareBackup -> requestShareBackup(intent.passphrase)
            is SecurityIntent.BackupToStream -> backupToStream(intent.openStream)
            is SecurityIntent.BackupFilePicked -> onBackupFilePicked(intent.fileName, intent.openStream)
            is SecurityIntent.InspectRestoreWithPassphrase -> inspectRestoreBackup(intent.passphrase)
            SecurityIntent.ConfirmRestoreBackup -> confirmRestoreBackup()
            SecurityIntent.DismissBackupDialog -> dismissBackupDialog()
        }
    }

    override fun onCleared() {
        pendingBackupPassphrase = null
        pendingRestoreOpenStream = null
        backupCoordinator.discardStagedRestore()
    }

    private fun launchBlock(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    private fun openTogglePinDialog() {
        val nextMode = if (uiState.value.lockState.isPinEnabled) {
            PinDialogMode.DISABLE
        } else {
            PinDialogMode.CREATE
        }
        localState.update { it.copy(activePinDialog = nextMode, pinDialogError = null) }
    }

    private fun submitCreatePin(newPin: String) {
        viewModelScope.launch {
            localState.update { it.copy(isProcessingPin = true, pinDialogError = null) }
            runCatching { appLockManager.enablePin(newPin) }
                .onSuccess {
                    localState.update {
                        it.copy(activePinDialog = null, isProcessingPin = false, pinDialogError = null)
                    }
                    _effects.send(SecurityEffect.ShowMessage("Đã bật khóa ứng dụng bằng PIN 6 số."))
                }
                .onFailure { error ->
                    localState.update {
                        it.copy(
                            isProcessingPin = false,
                            pinDialogError = "Không thể lưu mã PIN: ${error.message ?: "Lỗi bảo mật"}",
                        )
                    }
                }
        }
    }

    private fun submitChangePin(oldPin: String, newPin: String) {
        handlePinVerificationFlow(
            successMessage = "Đã đổi mã PIN thành công.",
            action = { appLockManager.changePin(oldPin, newPin) },
        )
    }

    private fun submitDisablePin(currentPin: String) {
        handlePinVerificationFlow(
            successMessage = "Đã tắt khóa ứng dụng.",
            action = { appLockManager.disablePin(currentPin) },
        )
    }

    private fun handlePinVerificationFlow(
        successMessage: String,
        action: suspend () -> PinVerifyResult,
    ) {
        viewModelScope.launch {
            localState.update { it.copy(isProcessingPin = true, pinDialogError = null) }
            when (val result = action()) {
                PinVerifyResult.Success -> {
                    localState.update {
                        it.copy(activePinDialog = null, isProcessingPin = false, pinDialogError = null)
                    }
                    _effects.send(SecurityEffect.ShowMessage(successMessage))
                }
                is PinVerifyResult.Incorrect -> {
                    localState.update {
                        it.copy(
                            isProcessingPin = false,
                            pinDialogError = "Mã PIN hiện tại không đúng (còn ${result.remainingAttempts} lần thử).",
                        )
                    }
                }
                is PinVerifyResult.LockedOut -> {
                    localState.update {
                        it.copy(activePinDialog = null, isProcessingPin = false, pinDialogError = null)
                    }
                    _effects.send(
                        SecurityEffect.ShowMessage("Nhập sai quá nhiều lần. Vui lòng thử lại sau ${result.remainingSeconds}s."),
                    )
                }
                PinVerifyResult.CorruptedKeystore -> {
                    appLockManager.resetAfterCorruptedKeystore()
                    localState.update {
                        it.copy(activePinDialog = null, isProcessingPin = false, pinDialogError = null)
                    }
                    _effects.send(SecurityEffect.ShowMessage("Khóa thiết bị thay đổi nên khóa PIN cũ đã được đặt lại."))
                }
            }
        }
    }

    private fun requestSaveExport() {
        val state = uiState.value
        val fileName = "${PersonalReportBuilder.suggestedFileNamePrefix(state.selectedPeriod)}.${state.selectedFormat.extension}"
        viewModelScope.launch {
            _effects.send(
                SecurityEffect.LaunchCreateDocument(
                    suggestedFileName = fileName,
                    mimeType = state.selectedFormat.mimeType,
                ),
            )
        }
    }

    private fun exportToStream(openStream: () -> java.io.OutputStream?) {
        val state = uiState.value
        val doc = buildReport(state.settings, state.selectedPeriod)
        viewModelScope.launch {
            localState.update { it.copy(isExporting = true) }
            try {
                val stream = openStream()
                if (stream == null) {
                    _effects.send(SecurityEffect.ShowMessage("Không mở được tệp đích để lưu."))
                    return@launch
                }
                stream.use { output ->
                    reportExporter.exportToStream(doc, state.selectedFormat, output)
                }
                _effects.send(SecurityEffect.ShowMessage("Đã xuất báo cáo ${state.selectedFormat.displayLabel} thành công."))
            } catch (e: Exception) {
                _effects.send(SecurityEffect.ShowMessage("Xuất báo cáo thất bại: ${e.message ?: "Lỗi không xác định"}"))
            } finally {
                localState.update { it.copy(isExporting = false) }
            }
        }
    }

    private fun requestShareExport() {
        val state = uiState.value
        val doc = buildReport(state.settings, state.selectedPeriod)
        val prefix = PersonalReportBuilder.suggestedFileNamePrefix(state.selectedPeriod)
        viewModelScope.launch {
            localState.update { it.copy(isExporting = true) }
            try {
                val file = reportExporter.exportToCacheFile(doc, state.selectedFormat, prefix)
                _effects.send(
                    SecurityEffect.ShareExportedFile(
                        file = file,
                        mimeType = state.selectedFormat.mimeType,
                        title = doc.title,
                    ),
                )
            } catch (e: Exception) {
                _effects.send(SecurityEffect.ShowMessage("Không thể tạo tệp chia sẻ: ${e.message ?: "Lỗi không xác định"}"))
            } finally {
                localState.update { it.copy(isExporting = false) }
            }
        }
    }

    private fun buildReport(settings: UserSettings, period: ReportPeriod): ReportDocument =
        PersonalReportBuilder.buildDocument(
            transactions = latestTransactions.value,
            period = period,
            currency = settings.currency,
        )

    private fun openCreateBackupSheet() {
        pendingBackupPassphrase = null
        pendingRestoreOpenStream = null
        backupCoordinator.discardStagedRestore()
        localState.update {
            it.copy(
                activeBackupDialog = BackupDialogState.CreatePassphrase,
                backupDialogError = null,
                isBackupBusy = false,
            )
        }
    }

    private fun requestSaveBackup(passphrase: String) {
        pendingBackupPassphrase = passphrase
        viewModelScope.launch {
            _effects.send(
                SecurityEffect.LaunchCreateBackupDocument(
                    suggestedFileName = suggestedBackupFileName(),
                ),
            )
        }
    }

    private fun backupToStream(openStream: () -> OutputStream?) {
        val passphrase = pendingBackupPassphrase
        pendingBackupPassphrase = null
        if (passphrase == null) {
            localState.update {
                it.copy(backupDialogError = "Phiên đặt mật khẩu đã hết hạn, vui lòng thử lại.")
            }
            return
        }
        viewModelScope.launch {
            localState.update { it.copy(isBackupBusy = true, backupDialogError = null) }
            try {
                val stream = openStream()
                if (stream == null) {
                    localState.update {
                        it.copy(isBackupBusy = false, backupDialogError = "Không mở được tệp đích để lưu bản sao lưu.")
                    }
                    return@launch
                }
                stream.use { output ->
                    backupCoordinator.createBackupToStream(passphrase, output)
                }
                localState.update {
                    it.copy(activeBackupDialog = null, backupDialogError = null, isBackupBusy = false)
                }
                _effects.send(SecurityEffect.ShowMessage("Đã tạo bản sao lưu mã hóa .wallet thành công."))
            } catch (e: Exception) {
                localState.update {
                    it.copy(
                        isBackupBusy = false,
                        backupDialogError = "Sao lưu thất bại: ${e.message ?: "Lỗi không xác định"}",
                    )
                }
            }
        }
    }

    private fun requestShareBackup(passphrase: String) {
        pendingBackupPassphrase = null
        viewModelScope.launch {
            localState.update { it.copy(isBackupBusy = true, backupDialogError = null) }
            try {
                val file = backupCoordinator.createBackupCacheFile(
                    passphrase = passphrase,
                    suggestedFileName = suggestedBackupFileName(),
                )
                localState.update {
                    it.copy(activeBackupDialog = null, backupDialogError = null, isBackupBusy = false)
                }
                _effects.send(
                    SecurityEffect.ShareExportedFile(
                        file = file,
                        mimeType = "application/octet-stream",
                        title = "Bản sao lưu StudentWallet (.wallet)",
                    ),
                )
            } catch (e: Exception) {
                localState.update {
                    it.copy(
                        isBackupBusy = false,
                        backupDialogError = "Không thể tạo tệp chia sẻ: ${e.message ?: "Lỗi không xác định"}",
                    )
                }
            }
        }
    }

    private fun onBackupFilePicked(fileName: String, openStream: () -> InputStream?) {
        pendingBackupPassphrase = null
        pendingRestoreOpenStream = openStream
        backupCoordinator.discardStagedRestore()
        localState.update {
            it.copy(
                activeBackupDialog = BackupDialogState.RestoreFlow(fileName = fileName, preview = null),
                backupDialogError = null,
                isBackupBusy = false,
            )
        }
    }

    private fun inspectRestoreBackup(passphrase: String) {
        val currentRestore = uiState.value.activeBackupDialog as? BackupDialogState.RestoreFlow ?: return
        val openStream = pendingRestoreOpenStream
        if (openStream == null) {
            localState.update {
                it.copy(backupDialogError = "Không truy cập được tệp .wallet đã chọn, vui lòng chọn lại.")
            }
            return
        }
        viewModelScope.launch {
            localState.update { it.copy(isBackupBusy = true, backupDialogError = null) }
            try {
                val preview = backupCoordinator.inspectBackupFile(
                    fileName = currentRestore.fileName,
                    passphrase = passphrase,
                    openInputStream = openStream,
                )
                localState.update {
                    it.copy(
                        activeBackupDialog = currentRestore.copy(preview = preview),
                        backupDialogError = null,
                        isBackupBusy = false,
                    )
                }
            } catch (e: Exception) {
                localState.update {
                    it.copy(
                        isBackupBusy = false,
                        backupDialogError = e.message ?: "Không thể giải mã tệp .wallet.",
                    )
                }
            }
        }
    }

    private fun confirmRestoreBackup() {
        viewModelScope.launch {
            localState.update { it.copy(isBackupBusy = true, backupDialogError = null) }
            try {
                val summary = backupCoordinator.confirmStagedRestore()
                pendingRestoreOpenStream = null
                localState.update {
                    it.copy(activeBackupDialog = null, backupDialogError = null, isBackupBusy = false)
                }
                _effects.send(
                    SecurityEffect.ShowMessage(
                        "Khôi phục thành công ${summary.transactionCount} giao dịch và ${summary.receiptCount} ảnh hóa đơn.",
                    ),
                )
            } catch (e: Exception) {
                localState.update {
                    it.copy(
                        isBackupBusy = false,
                        backupDialogError = e.message ?: "Khôi phục dữ liệu thất bại.",
                    )
                }
            }
        }
    }

    private fun dismissBackupDialog() {
        pendingBackupPassphrase = null
        pendingRestoreOpenStream = null
        backupCoordinator.discardStagedRestore()
        localState.update {
            it.copy(activeBackupDialog = null, backupDialogError = null, isBackupBusy = false)
        }
    }

    private fun suggestedBackupFileName(): String =
        "student_wallet_${LocalDateTime.now().format(BACKUP_FILE_TIME_FORMATTER)}.wallet"

    private data class LocalSecurityState(
        val activePinDialog: PinDialogMode? = null,
        val pinDialogError: String? = null,
        val isProcessingPin: Boolean = false,
        val selectedPeriod: ReportPeriod = ReportPeriod.THIS_MONTH,
        val selectedFormat: ExportFormat = ExportFormat.PDF,
        val isExporting: Boolean = false,
        val activeBackupDialog: BackupDialogState? = null,
        val backupDialogError: String? = null,
        val isBackupBusy: Boolean = false,
    )

    private companion object {
        val BACKUP_FILE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")
    }
}
