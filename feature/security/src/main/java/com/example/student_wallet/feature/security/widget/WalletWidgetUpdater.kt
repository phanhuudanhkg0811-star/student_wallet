package com.example.student_wallet.feature.security.widget

import android.content.Context
import androidx.glance.appwidget.updateAll

object WalletWidgetUpdater {
    suspend fun refreshAll(context: Context) {
        runCatching {
            WalletGlanceWidget().updateAll(context.applicationContext)
        }
    }
}
