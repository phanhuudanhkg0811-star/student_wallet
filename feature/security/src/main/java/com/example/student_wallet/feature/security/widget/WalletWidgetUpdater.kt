package com.example.student_wallet.feature.security.widget

import android.content.Context
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicLong

object WalletWidgetUpdater {

    val KEY_REFRESH_NONCE = longPreferencesKey("wallet_widget_refresh_nonce")
    private val nonceGenerator = AtomicLong(0L)
    private val updateMutex = Mutex()

    suspend fun refreshAll(context: Context) {
        val appContext = context.applicationContext
        val nextNonce = nonceGenerator.incrementAndGet() * 1_000_000L + (System.nanoTime() and 0xFFFFFL)
        updateMutex.withLock {
            runCatching {
                val manager = GlanceAppWidgetManager(appContext)
                val glanceIds = manager.getGlanceIds(WalletGlanceWidget::class.java)
                if (glanceIds.isEmpty()) return@runCatching

                val widget = WalletGlanceWidget()
                glanceIds.forEach { glanceId ->
                    updateAppWidgetState(appContext, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                        prefs.toMutablePreferences().apply {
                            this[KEY_REFRESH_NONCE] = nextNonce
                        }
                    }
                    widget.update(appContext, glanceId)
                }
            }
        }
    }
}
