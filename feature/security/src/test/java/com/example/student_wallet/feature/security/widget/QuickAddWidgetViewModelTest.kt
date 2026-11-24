package com.example.student_wallet.feature.security.widget

import com.example.student_wallet.core.domain.model.Category
import com.example.student_wallet.core.domain.model.CurrencyOption
import com.example.student_wallet.core.domain.model.PaymentMethod
import com.example.student_wallet.core.domain.model.ThemeMode
import com.example.student_wallet.core.domain.model.Transaction
import com.example.student_wallet.core.domain.model.TransactionType
import com.example.student_wallet.core.domain.model.UserSettings
import com.example.student_wallet.core.domain.repository.SettingsRepository
import com.example.student_wallet.core.domain.repository.TransactionRepository
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class QuickAddWidgetViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `submit validates amount, switches category by type, and saves transaction`() = runTest(testDispatcher) {
        val repo = RecordingTransactionRepository()
        val viewModel = QuickAddWidgetViewModel(repo, FakeSettingsRepository())

        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        // Invalid empty submit
        var callbackInvoked = false
        viewModel.submit { _, _ -> callbackInvoked = true }
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.errorMessage)
        assertTrue(repo.addedTransactions.isEmpty())

        // Switch to INCOME and back to EXPENSE, accumulate presets
        viewModel.onTypeChange(TransactionType.INCOME)
        advanceUntilIdle()
        assertEquals(7L, viewModel.uiState.value.selectedCategoryId)

        viewModel.onTypeChange(TransactionType.EXPENSE)
        viewModel.onAddQuickPreset(20_000L)
        viewModel.onAddQuickPreset(35_000L)
        viewModel.onPaymentMethodSelected(PaymentMethod.MOMO)
        viewModel.onNoteChange("Cơm trưa")
        advanceUntilIdle()

        assertEquals("55000", viewModel.uiState.value.amountDigits)
        assertNull(viewModel.uiState.value.errorMessage)

        viewModel.submit { amount, currency ->
            callbackInvoked = true
            assertEquals(55_000L, amount)
            assertEquals(CurrencyOption.VND, currency)
        }
        advanceUntilIdle()

        assertTrue(callbackInvoked)
        assertEquals(1, repo.addedTransactions.size)
        val saved = repo.addedTransactions.single()
        assertEquals(55_000L, saved.amount)
        assertEquals("Cơm trưa", saved.note)
        assertEquals(PaymentMethod.MOMO, saved.paymentMethod)
        collectJob.cancel()
    }

    private class RecordingTransactionRepository : TransactionRepository {
        val addedTransactions = mutableListOf<Transaction>()
        private val categories = MutableStateFlow(
            listOf(
                Category(1L, "Ăn uống", "🍜", TransactionType.EXPENSE, 0xFF006C51L),
                Category(7L, "Trợ cấp / Lương", "💸", TransactionType.INCOME, 0xFF00855FL),
            ),
        )

        override fun observeTransactions(): Flow<List<Transaction>> = MutableStateFlow(addedTransactions)
        override fun observeCategories(): Flow<List<Category>> = categories
        override suspend fun addTransaction(transaction: Transaction): Long {
            addedTransactions += transaction
            return addedTransactions.size.toLong()
        }
    }

    private class FakeSettingsRepository : SettingsRepository {
        override fun observeSettings(): Flow<UserSettings> = MutableStateFlow(UserSettings())
        override suspend fun setThemeMode(themeMode: ThemeMode) = Unit
        override suspend fun setCurrency(currency: CurrencyOption) = Unit
        override suspend fun setDynamicColor(enabled: Boolean) = Unit
    }
}
