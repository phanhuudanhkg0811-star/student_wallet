package com.example.student_wallet.feature.security.widget

import android.content.Context
import com.example.student_wallet.core.domain.repository.SettingsRepository
import com.example.student_wallet.core.domain.repository.TransactionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Observes Room transactions and DataStore user settings while the app process is alive
 * so pinned Glance widgets stay synchronized regardless of which feature module writes data.
 */
internal class WalletWidgetAutoSync(
    private val context: Context,
    private val transactionRepository: TransactionRepository,
    private val settingsRepository: SettingsRepository,
    private val scope: CoroutineScope,
) {
    private val started = AtomicBoolean(false)

    fun start() {
        if (!started.compareAndSet(false, true)) return
        scope.launch {
            combine(
                transactionRepository.observeTransactions(),
                settingsRepository.observeSettings(),
            ) { transactions, settings ->
                Triple(transactions.size, transactions.sumOf { it.amount }, settings)
            }
                .drop(1)
                .conflate()
                .collect {
                    WalletWidgetUpdater.refreshAll(context)
                }
        }
    }
}
