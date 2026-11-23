package com.example.student_wallet.feature.security.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.securityLockDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "student_wallet_security",
)

data class SecurityLockPrefs(
    val encryptedPinBlob: String? = null,
    val isPinEnabled: Boolean = false,
    val isBiometricEnabled: Boolean = false,
    val autoLockSeconds: Int = DEFAULT_AUTO_LOCK_SECONDS,
    val failedAttempts: Int = 0,
    val lockoutUntilEpochMillis: Long = 0L,
) {
    companion object {
        const val DEFAULT_AUTO_LOCK_SECONDS = 30
    }
}

interface SecurityLockStore {
    val lockPrefsFlow: Flow<SecurityLockPrefs>
    suspend fun savePin(encryptedPinBlob: String)
    suspend fun clearPinAndDisableLock()
    suspend fun setBiometricEnabled(enabled: Boolean)
    suspend fun setAutoLockSeconds(seconds: Int)
    suspend fun recordFailedAttempt(failedAttempts: Int, lockoutUntilEpochMillis: Long)
    suspend fun resetFailedAttempts()
}

class DataStoreSecurityLockStore(
    private val context: Context,
) : SecurityLockStore {

    override val lockPrefsFlow: Flow<SecurityLockPrefs> =
        context.securityLockDataStore.data.map { prefs ->
            SecurityLockPrefs(
                encryptedPinBlob = prefs[KEY_ENCRYPTED_PIN_BLOB],
                isPinEnabled = prefs[KEY_PIN_ENABLED] ?: false,
                isBiometricEnabled = prefs[KEY_BIOMETRIC_ENABLED] ?: false,
                autoLockSeconds = prefs[KEY_AUTO_LOCK_SECONDS]
                    ?: SecurityLockPrefs.DEFAULT_AUTO_LOCK_SECONDS,
                failedAttempts = prefs[KEY_FAILED_ATTEMPTS] ?: 0,
                lockoutUntilEpochMillis = prefs[KEY_LOCKOUT_UNTIL] ?: 0L,
            )
        }

    override suspend fun savePin(encryptedPinBlob: String) {
        context.securityLockDataStore.edit { prefs ->
            prefs[KEY_ENCRYPTED_PIN_BLOB] = encryptedPinBlob
            prefs[KEY_PIN_ENABLED] = true
            prefs[KEY_FAILED_ATTEMPTS] = 0
            prefs[KEY_LOCKOUT_UNTIL] = 0L
        }
    }

    override suspend fun clearPinAndDisableLock() {
        context.securityLockDataStore.edit { prefs ->
            prefs.remove(KEY_ENCRYPTED_PIN_BLOB)
            prefs[KEY_PIN_ENABLED] = false
            prefs[KEY_BIOMETRIC_ENABLED] = false
            prefs[KEY_FAILED_ATTEMPTS] = 0
            prefs[KEY_LOCKOUT_UNTIL] = 0L
        }
    }

    override suspend fun setBiometricEnabled(enabled: Boolean) {
        context.securityLockDataStore.edit { prefs ->
            prefs[KEY_BIOMETRIC_ENABLED] = enabled
        }
    }

    override suspend fun setAutoLockSeconds(seconds: Int) {
        context.securityLockDataStore.edit { prefs ->
            prefs[KEY_AUTO_LOCK_SECONDS] = seconds.coerceAtLeast(0)
        }
    }

    override suspend fun recordFailedAttempt(
        failedAttempts: Int,
        lockoutUntilEpochMillis: Long,
    ) {
        context.securityLockDataStore.edit { prefs ->
            prefs[KEY_FAILED_ATTEMPTS] = failedAttempts
            prefs[KEY_LOCKOUT_UNTIL] = lockoutUntilEpochMillis
        }
    }

    override suspend fun resetFailedAttempts() {
        context.securityLockDataStore.edit { prefs ->
            prefs[KEY_FAILED_ATTEMPTS] = 0
            prefs[KEY_LOCKOUT_UNTIL] = 0L
        }
    }

    private companion object {
        val KEY_ENCRYPTED_PIN_BLOB = stringPreferencesKey("encrypted_pin_blob")
        val KEY_PIN_ENABLED = booleanPreferencesKey("is_pin_enabled")
        val KEY_BIOMETRIC_ENABLED = booleanPreferencesKey("is_biometric_enabled")
        val KEY_AUTO_LOCK_SECONDS = intPreferencesKey("auto_lock_seconds")
        val KEY_FAILED_ATTEMPTS = intPreferencesKey("failed_attempts")
        val KEY_LOCKOUT_UNTIL = longPreferencesKey("lockout_until_epoch_millis")
    }
}
