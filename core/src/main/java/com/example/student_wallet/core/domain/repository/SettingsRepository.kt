package com.example.student_wallet.core.domain.repository

import com.example.student_wallet.core.domain.model.CurrencyOption
import com.example.student_wallet.core.domain.model.ThemeMode
import com.example.student_wallet.core.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun observeSettings(): Flow<UserSettings>
    suspend fun setThemeMode(themeMode: ThemeMode)
    suspend fun setCurrency(currency: CurrencyOption)
    suspend fun setDynamicColor(enabled: Boolean)
}
