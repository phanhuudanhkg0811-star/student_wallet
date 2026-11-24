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
import com.example.student_wallet.feature.security.backup.BackupPreviewInfo
import com.example.student_wallet.feature.security.backup.BackupStats
import com.example.student_wallet.feature.security.backup.RestoreSummary
import com.example.student_wallet.feature.security.backup.WalletBackupCoordinator
import com.example.student_wallet.feature.security.backup.WalletBackupErrorReason
import com.example.student_wallet.feature.security.backup.WalletBackupException
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
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
        val viewModel = SecurityViewModel(
            settingsRepo,
            txRepo,
            exporter,
            createAppLockManager(),
            FakeWalletBackupCoordinator(),
        )

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
            FakeWalletBackupCoordinator(),
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
        val viewModel = SecurityViewModel(
            settingsRepo,
            txRepo,
            exporter,
            createAppLockManager(),
            FakeWalletBackupCoordinator(),
        )

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

    @Test
    fun `backup save flow launches wallet document and writes encrypted bytes`() = runTest(testDispatcher) {
        val backupCoordinator = FakeWalletBackupCoordinator()
        val viewModel = SecurityViewModel(
            FakeSettingsRepository(),
            FakeTransactionRepository(),
            FakeReportExporter(),
            createAppLockManager(),
            backupCoordinator,
        )

        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onIntent(SecurityIntent.OpenCreateBackupSheet)
        advanceUntilIdle()
        assertEquals(BackupDialogState.CreatePassphrase, viewModel.uiState.value.activeBackupDialog)

        viewModel.onIntent(SecurityIntent.RequestSaveBackup("Secret#2026"))
        val createEffect = viewModel.effects.first() as SecurityEffect.LaunchCreateBackupDocument
        assertTrue(createEffect.suggestedFileName.endsWith(".wallet"))

        val buffer = ByteArrayOutputStream()
        viewModel.onIntent(SecurityIntent.BackupToStream { buffer })
        advanceUntilIdle()

        val msg = viewModel.effects.first() as SecurityEffect.ShowMessage
        assertTrue(msg.message.contains("thành công"))
        assertNull(viewModel.uiState.value.activeBackupDialog)
        assertEquals("WALLET:Secret#2026", String(buffer.toByteArray(), StandardCharsets.UTF_8))
        collectJob.cancel()
    }

    @Test
    fun `two-step restore verifies passphrase shows preview and confirms replacement`() = runTest(testDispatcher) {
        val backupCoordinator = FakeWalletBackupCoordinator(expectedPassphrase = "Pass@999")
        val viewModel = SecurityViewModel(
            FakeSettingsRepository(),
            FakeTransactionRepository(),
            FakeReportExporter(),
            createAppLockManager(),
            backupCoordinator,
        )

        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onIntent(
            SecurityIntent.BackupFilePicked(
                fileName = "backup_nov.wallet",
                openStream = { ByteArrayInputStream("encrypted".toByteArray()) },
            ),
        )
        advanceUntilIdle()

        val initialDialog = viewModel.uiState.value.activeBackupDialog as BackupDialogState.RestoreFlow
        assertEquals("backup_nov.wallet", initialDialog.fileName)
        assertNull(initialDialog.preview)

        viewModel.onIntent(SecurityIntent.InspectRestoreWithPassphrase("WrongPass"))
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.backupDialogError)
        assertNull((viewModel.uiState.value.activeBackupDialog as BackupDialogState.RestoreFlow).preview)

        viewModel.onIntent(SecurityIntent.InspectRestoreWithPassphrase("Pass@999"))
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.backupDialogError)
        val previewDialog = viewModel.uiState.value.activeBackupDialog as BackupDialogState.RestoreFlow
        assertEquals(18, previewDialog.preview?.transactionCount)

        viewModel.onIntent(SecurityIntent.ConfirmRestoreBackup)
        advanceUntilIdle()
        assertTrue(backupCoordinator.confirmedRestore)
        assertNull(viewModel.uiState.value.activeBackupDialog)
        val msg = viewModel.effects.first() as SecurityEffect.ShowMessage
        assertTrue(msg.message.contains("18 giao dịch"))
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

    private class FakeWalletBackupCoordinator(
        private val expectedPassphrase: String = "Secret#2026",
    ) : WalletBackupCoordinator {
        var confirmedRestore = false
        private var hasStaged = false

        override suspend fun getCurrentBackupStats(): BackupStats = BackupStats(18, 7, 2)

        override suspend fun createBackupToStream(passphrase: String, outputStream: OutputStream) {
            outputStream.write("WALLET:$passphrase".toByteArray(StandardCharsets.UTF_8))
        }

        override suspend fun createBackupCacheFile(passphrase: String, suggestedFileName: String): File =
            File(suggestedFileName)

        override suspend fun inspectBackupFile(
            fileName: String,
            passphrase: String,
            openInputStream: () -> InputStream?,
        ): BackupPreviewInfo {
            if (passphrase != expectedPassphrase) {
                throw WalletBackupException(
                    WalletBackupErrorReason.WRONG_PASSWORD,
                    "Mật khẩu giải mã không đúng.",
                )
            }
            hasStaged = true
            return BackupPreviewInfo(
                fileName = fileName,
                createdAtEpochMillis = 1_764_000_000_000L,
                dbVersion = 1,
                transactionCount = 18,
                categoryCount = 7,
                receiptCount = 2,
                checksumShortHex = "abcdef123456",
                settings = UserSettings(),
            )
        }

        override suspend fun confirmStagedRestore(): RestoreSummary {
            check(hasStaged)
            confirmedRestore = true
            hasStaged = false
            return RestoreSummary(18, 7, 2)
        }

        override fun discardStagedRestore() {
            hasStaged = false
        }
    }
}
