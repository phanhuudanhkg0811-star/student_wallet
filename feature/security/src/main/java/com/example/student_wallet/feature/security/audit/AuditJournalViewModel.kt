package com.example.student_wallet.feature.security.audit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.student_wallet.core.domain.model.CurrencyOption
import com.example.student_wallet.core.domain.repository.SettingsRepository
import com.example.student_wallet.core.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.YearMonth

data class AuditJournalUiState(
    val selectedMonth: YearMonth = YearMonth.now(),
    val canGoNextMonth: Boolean = false,
    val currency: CurrencyOption = CurrencyOption.VND,
    val useSampleData: Boolean = false,
    val summary: MonthlyAuditSummary = AuditJournalAnalyzer.analyzeMonth(emptyList(), YearMonth.now()),
    val selectedCell: DailyAuditCell? = null,
)

class AuditJournalViewModel(
    transactionRepository: TransactionRepository,
    settingsRepository: SettingsRepository,
    private val currentMonthProvider: () -> YearMonth = { YearMonth.now() },
    private val todayProvider: () -> LocalDate = { LocalDate.now() },
) : ViewModel() {

    private val selectedMonth = MutableStateFlow(currentMonthProvider())
    private val useSampleData = MutableStateFlow(false)
    private val selectedDate = MutableStateFlow<LocalDate?>(null)

    val uiState: StateFlow<AuditJournalUiState> = combine(
        transactionRepository.observeTransactions(),
        settingsRepository.observeSettings(),
        selectedMonth,
        useSampleData,
        selectedDate,
    ) { transactions, settings, month, sampleEnabled, pickedDate ->
        val sourceTransactions = if (sampleEnabled) {
            AuditJournalAnalyzer.sampleTransactionsForMonth(month)
        } else {
            transactions
        }
        val summary = AuditJournalAnalyzer.analyzeMonth(
            transactions = sourceTransactions,
            targetMonth = month,
            today = todayProvider(),
        )
        val resolvedSelectedCell = pickedDate?.let { targetDate ->
            summary.heatmapWeeks.flatten().filterNotNull().firstOrNull { it.date == targetDate }
        }
        AuditJournalUiState(
            selectedMonth = month,
            canGoNextMonth = month.isBefore(currentMonthProvider()),
            currency = settings.currency,
            useSampleData = sampleEnabled,
            summary = summary,
            selectedCell = resolvedSelectedCell,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AuditJournalUiState(selectedMonth = currentMonthProvider()),
    )

    fun previousMonth() {
        selectedMonth.update { it.minusMonths(1) }
        selectedDate.value = null
    }

    fun nextMonth() {
        val currentMonth = currentMonthProvider()
        selectedMonth.update { month ->
            if (month.isBefore(currentMonth)) month.plusMonths(1) else month
        }
        selectedDate.value = null
    }

    fun toggleSampleData(enabled: Boolean) {
        useSampleData.value = enabled
        selectedDate.value = null
    }

    fun selectCell(cell: DailyAuditCell) {
        selectedDate.update { current -> if (current == cell.date) null else cell.date }
    }
}
