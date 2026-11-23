package com.example.student_wallet.feature.security

import com.example.student_wallet.core.domain.export.ExportFormat
import com.example.student_wallet.core.domain.export.ReportDocument
import com.example.student_wallet.core.domain.export.ReportExporter
import com.example.student_wallet.core.domain.model.Category
import com.example.student_wallet.core.domain.model.CurrencyOption
import com.example.student_wallet.core.domain.model.PaymentMethod
import com.example.student_wallet.core.domain.model.ThemeMode
import com.example.student_wallet.core.domain.model.Transaction
import com.example.student_wallet.core.domain.model.TransactionType
import com.example.student_wallet.core.domain.model.UserSettings
import com.example.student_wallet.core.domain.repository.SettingsRepository
import com.example.student_wallet.core.domain.repository.TransactionRepository
import com.example.student_wallet.feature.security.crypto.PinHasher
import com.example.student_wallet.feature.security.lock.AppLockManager
import com.example.student_wallet.feature.security.lock.FakeSecretProtector
import com.example.student_wallet.feature.security.lock.FakeSecurityLockStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStream
import java.nio.charset.StandardCharsets

@OptIn(ExperimentalCoroutinesApi::class)
class SecurityViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var lockScope: CoroutineScope

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        lockScope = CoroutineScope(SupervisorJob() + testDispatcher)
    }

    @After
    fun tearDown() {
        lockScope.cancel()
        Dispatchers.resetMain()
    }

    private fun createAppLockManager(): AppLockManager = AppLockManager(
        lockStore = FakeSecurityLockStore(),
        pinHasher = PinHasher(defaultIterations = 1_000),
        secretProtector = FakeSecretProtector(),
        scope = lockScope,
        elapsedRealtimeMillis = { 0L },
        computeDispatcher = testDispatcher,
    )

    @Test
    fun `theme and currency intents update SettingsRepository and state`() = runTest(testDispatcher) {
        val settingsRepo = FakeSettingsRepository()
        val txRepo = FakeTransactionRepository()
        val exporter = FakeReportExporter()
        val viewModel = SecurityViewModel(settingsRepo, txRepo, exporter, createAppLockManager())

        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onIntent(SecurityIntent.SelectTheme(ThemeMode.DARK))
        viewModel.onIntent(SecurityIntent.ToggleDynamicColor(true))
        viewModel.onIntent(SecurityIntent.SelectCurrency(CurrencyOption.USD))
        advanceUntilIdle()

        assertEquals(ThemeMode.DARK, viewModel.uiState.value.settings.themeMode)
        assertTrue(viewModel.uiState.value.settings.dynamicColor)
        assertEquals(CurrencyOption.USD, viewModel.uiState.value.settings.currency)
        collectJob.cancel()
    }

    @Test
    fun `pin create and disable flow updates lockState and closes dialog`() = runTest(testDispatcher) {
        val viewModel = SecurityViewModel(
            FakeSettingsRepository(),
            FakeTransactionRepository(),
            FakeReportExporter(),
            createAppLockManager(),
        )

        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onIntent(SecurityIntent.RequestTogglePinLock)
        advanceUntilIdle()
        assertEquals(PinDialogMode.CREATE, viewModel.uiState.value.activePinDialog)

        viewModel.onIntent(SecurityIntent.SubmitCreatePin("135790"))
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.lockState.isPinEnabled)
        assertNull(viewModel.uiState.value.activePinDialog)

        viewModel.onIntent(SecurityIntent.RequestTogglePinLock)
        advanceUntilIdle()
        assertEquals(PinDialogMode.DISABLE, viewModel.uiState.value.activePinDialog)

        viewModel.onIntent(SecurityIntent.SubmitDisablePin("135790"))
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.lockState.isPinEnabled)
        assertNull(viewModel.uiState.value.activePinDialog)
        collectJob.cancel()
    }

    @Test
    fun `requestSaveExport emits document effect and ExportToStream writes report`() = runTest(testDispatcher) {
        val settingsRepo = FakeSettingsRepository()
        val txRepo = FakeTransactionRepository(
            listOf(
                Transaction(
                    id = 1L,
                    amount = 45_000L,
                    type = TransactionType.EXPENSE,
                    categoryId = 1L,
                    categoryName = "Ăn uống",
                    categoryEmoji = "🍜",
                    note = "Phở",
                    paymentMethod = PaymentMethod.CASH,
                    timestampEpochMillis = System.currentTimeMillis(),
                ),
            ),
        )
        val exporter = FakeReportExporter()
        val viewModel = SecurityViewModel(settingsRepo, txRepo, exporter, createAppLockManager())

        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onIntent(SecurityIntent.SelectFormat(ExportFormat.CSV))
        advanceUntilIdle()

        viewModel.onIntent(SecurityIntent.RequestSaveExport)
        val createEffect = viewModel.effects.first() as SecurityEffect.LaunchCreateDocument
        assertEquals(ExportFormat.CSV.mimeType, createEffect.mimeType)
        assertTrue(createEffect.suggestedFileName.endsWith(".csv"))

        val buffer = ByteArrayOutputStream()
        viewModel.onIntent(SecurityIntent.ExportToStream { buffer })
        advanceUntilIdle()

        val messageEffect = viewModel.effects.first() as SecurityEffect.ShowMessage
        assertTrue(messageEffect.message.contains("thành công"))
        assertEquals("CSV:Báo cáo", String(buffer.toByteArray(), StandardCharsets.UTF_8))
        collectJob.cancel()
    }

    private class FakeSettingsRepository : SettingsRepository {
        private val flow = MutableStateFlow(UserSettings())
        override fun observeSettings(): Flow<UserSettings> = flow
        override suspend fun setThemeMode(themeMode: ThemeMode) {
            flow.value = flow.value.copy(themeMode = themeMode)
        }
        override suspend fun setCurrency(currency: CurrencyOption) {
            flow.value = flow.value.copy(currency = currency)
        }
        override suspend fun setDynamicColor(enabled: Boolean) {
            flow.value = flow.value.copy(dynamicColor = enabled)
        }
    }

    private class FakeTransactionRepository(
        initial: List<Transaction> = emptyList(),
    ) : TransactionRepository {
        private val flow = MutableStateFlow(initial)
        override fun observeTransactions(): Flow<List<Transaction>> = flow
        override fun observeCategories(): Flow<List<Category>> = MutableStateFlow(emptyList())
        override suspend fun addTransaction(transaction: Transaction): Long = 1L
    }

    private class FakeReportExporter : ReportExporter {
        override suspend fun exportToStream(
            document: ReportDocument,
            format: ExportFormat,
            outputStream: OutputStream,
        ) {
            outputStream.write("${format.name}:Báo cáo".toByteArray(StandardCharsets.UTF_8))
        }

        override suspend fun exportToCacheFile(
            document: ReportDocument,
            format: ExportFormat,
            fileNamePrefix: String,
        ): File = File("${fileNamePrefix}.${format.extension}")
    }
}
