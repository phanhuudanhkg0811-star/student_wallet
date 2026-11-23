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

class SecurityViewModel(
    private val settingsRepository: SettingsRepository,
    transactionRepository: TransactionRepository,
    private val reportExporter: ReportExporter,
    private val appLockManager: AppLockManager,
) : ViewModel() {

    private val localState = MutableStateFlow(LocalSecurityState())
    private val latestTransactions = MutableStateFlow<List<Transaction>>(emptyList())

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
        }
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

    private data class LocalSecurityState(
        val activePinDialog: PinDialogMode? = null,
        val pinDialogError: String? = null,
        val isProcessingPin: Boolean = false,
        val selectedPeriod: ReportPeriod = ReportPeriod.THIS_MONTH,
        val selectedFormat: ExportFormat = ExportFormat.PDF,
        val isExporting: Boolean = false,
    )
}
