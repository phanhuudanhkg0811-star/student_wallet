package com.example.student_wallet.feature.security.audit

import com.example.student_wallet.core.domain.model.Category
import com.example.student_wallet.core.domain.model.CurrencyOption
import com.example.student_wallet.core.domain.model.ThemeMode
import com.example.student_wallet.core.domain.model.Transaction
import com.example.student_wallet.core.domain.model.UserSettings
import com.example.student_wallet.core.domain.repository.SettingsRepository
import com.example.student_wallet.core.domain.repository.TransactionRepository
import com.example.student_wallet.feature.security.di.securityModule
import com.example.student_wallet.feature.security.widget.QuickAddWidgetViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class AuditJournalViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val fixedMonth = YearMonth.of(2026, 11)
    private val fixedToday = LocalDate.of(2026, 11, 20)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `navigation between months and sample toggle update summary and selected cell`() = runTest(testDispatcher) {
        val viewModel = AuditJournalViewModel(
            transactionRepository = EmptyTransactionRepository(),
            settingsRepository = FakeSettingsRepository(),
            currentMonthProvider = { fixedMonth },
            todayProvider = { fixedToday },
        )

        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertEquals(fixedMonth, viewModel.uiState.value.selectedMonth)
        assertFalse(viewModel.uiState.value.canGoNextMonth)
        assertFalse(viewModel.uiState.value.summary.hasData)

        viewModel.toggleSampleData(true)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.summary.hasData)
        val firstCell = viewModel.uiState.value.summary.heatmapWeeks
            .flatten()
            .filterNotNull()
            .first { it.isInTargetMonth }
        viewModel.selectCell(firstCell)
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.selectedCell)

        viewModel.previousMonth()
        advanceUntilIdle()
        assertEquals(fixedMonth.minusMonths(1), viewModel.uiState.value.selectedMonth)
        assertTrue(viewModel.uiState.value.canGoNextMonth)

        viewModel.nextMonth()
        advanceUntilIdle()
        assertEquals(fixedMonth, viewModel.uiState.value.selectedMonth)
        collectJob.cancel()
    }

    @Test
    fun `securityModule resolves AuditJournalViewModel via Koin without missing defaults`() {
        val coreDepsModule = module {
            single<TransactionRepository> { EmptyTransactionRepository() }
            single<SettingsRepository> { FakeSettingsRepository() }
        }
        val koinApp = koinApplication {
            modules(coreDepsModule, securityModule)
        }
        try {
            val resolved = koinApp.koin.get<AuditJournalViewModel>()
            val resolvedQuickAdd = koinApp.koin.get<QuickAddWidgetViewModel>()
            assertEquals(YearMonth.now(), resolved.uiState.value.selectedMonth)
            assertEquals("", resolvedQuickAdd.uiState.value.amountDigits)
        } finally {
            koinApp.close()
        }
    }

    private class EmptyTransactionRepository : TransactionRepository {
        override fun observeTransactions(): Flow<List<Transaction>> = MutableStateFlow(emptyList())
        override fun observeCategories(): Flow<List<Category>> = MutableStateFlow(emptyList())
        override suspend fun addTransaction(transaction: Transaction): Long = 1L
    }

    private class FakeSettingsRepository : SettingsRepository {
        override fun observeSettings(): Flow<UserSettings> = MutableStateFlow(UserSettings())
        override suspend fun setThemeMode(themeMode: ThemeMode) = Unit
        override suspend fun setCurrency(currency: CurrencyOption) = Unit
        override suspend fun setDynamicColor(enabled: Boolean) = Unit
    }
}
