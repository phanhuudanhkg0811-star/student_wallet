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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SecurityViewModel(
    private val settingsRepository: SettingsRepository,
    transactionRepository: TransactionRepository,
    private val reportExporter: ReportExporter,
) : ViewModel() {

    private val selectedPeriod = MutableStateFlow(ReportPeriod.THIS_MONTH)
    private val selectedFormat = MutableStateFlow(ExportFormat.PDF)
    private val isExporting = MutableStateFlow(false)
    private val latestTransactions = MutableStateFlow<List<Transaction>>(emptyList())

    private val _effects = Channel<SecurityEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    val uiState: StateFlow<SecurityUiState> = combine(
        settingsRepository.observeSettings(),
        transactionRepository.observeTransactions(),
        selectedPeriod,
        selectedFormat,
        isExporting,
    ) { settings, transactions, period, format, exporting ->
        latestTransactions.value = transactions
        val filtered = PersonalReportBuilder.filterByPeriod(transactions, period)
        SecurityUiState(
            settings = settings,
            selectedPeriod = period,
            selectedFormat = format,
            periodStats = PersonalReportBuilder.summarize(filtered),
            isExporting = exporting,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SecurityUiState(),
    )

    fun onIntent(intent: SecurityIntent) {
        when (intent) {
            is SecurityIntent.SelectTheme -> launchSettingUpdate {
                settingsRepository.setThemeMode(intent.themeMode)
            }
            is SecurityIntent.ToggleDynamicColor -> launchSettingUpdate {
                settingsRepository.setDynamicColor(intent.enabled)
            }
            is SecurityIntent.SelectCurrency -> launchSettingUpdate {
                settingsRepository.setCurrency(intent.currency)
            }
            is SecurityIntent.SelectPeriod -> selectedPeriod.value = intent.period
            is SecurityIntent.SelectFormat -> selectedFormat.value = intent.format
            SecurityIntent.RequestSaveExport -> requestSaveExport()
            SecurityIntent.RequestShareExport -> requestShareExport()
            is SecurityIntent.ExportToStream -> exportToStream(intent.openStream)
        }
    }

    private fun launchSettingUpdate(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
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
            isExporting.value = true
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
                isExporting.value = false
            }
        }
    }

    private fun requestShareExport() {
        val state = uiState.value
        val doc = buildReport(state.settings, state.selectedPeriod)
        val prefix = PersonalReportBuilder.suggestedFileNamePrefix(state.selectedPeriod)
        viewModelScope.launch {
            isExporting.value = true
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
                isExporting.value = false
            }
        }
    }

    private fun buildReport(settings: UserSettings, period: ReportPeriod): ReportDocument =
        PersonalReportBuilder.buildDocument(
            transactions = latestTransactions.value,
            period = period,
            currency = settings.currency,
        )
}
