package com.example.student_wallet.core.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.student_wallet.core.domain.model.CurrencyOption
import com.example.student_wallet.core.domain.model.ThemeMode
import com.example.student_wallet.core.domain.model.UserSettings
import com.example.student_wallet.core.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "student_wallet_settings",
)

class DataStoreSettingsRepository(
    private val context: Context,
) : SettingsRepository {

    override fun observeSettings(): Flow<UserSettings> =
        context.settingsDataStore.data.map { prefs ->
            val theme = prefs[KEY_THEME_MODE]
                ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM
            val currency = prefs[KEY_CURRENCY]
                ?.let { runCatching { CurrencyOption.valueOf(it) }.getOrNull() }
                ?: CurrencyOption.VND
            val dynamicColor = prefs[KEY_DYNAMIC_COLOR] ?: false
            UserSettings(
                themeMode = theme,
                currency = currency,
                dynamicColor = dynamicColor,
            )
        }

    override suspend fun setThemeMode(themeMode: ThemeMode) {
        context.settingsDataStore.edit { prefs ->
            prefs[KEY_THEME_MODE] = themeMode.name
        }
    }

    override suspend fun setCurrency(currency: CurrencyOption) {
        context.settingsDataStore.edit { prefs ->
            prefs[KEY_CURRENCY] = currency.name
        }
    }

    override suspend fun setDynamicColor(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[KEY_DYNAMIC_COLOR] = enabled
        }
    }

    private companion object {
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_CURRENCY = stringPreferencesKey("currency")
        val KEY_DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
    }
}
