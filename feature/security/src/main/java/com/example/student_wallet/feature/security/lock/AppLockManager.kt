package com.example.student_wallet.feature.security.lock

import com.example.student_wallet.feature.security.crypto.PinHasher
import com.example.student_wallet.feature.security.crypto.SecretProtector
import com.example.student_wallet.feature.security.data.SecurityLockPrefs
import com.example.student_wallet.feature.security.data.SecurityLockStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.ceil

data class AppLockState(
    val isInitialized: Boolean = false,
    val isLocked: Boolean = false,
    val isPinEnabled: Boolean = false,
    val isBiometricEnabled: Boolean = false,
    val autoLockSeconds: Int = SecurityLockPrefs.DEFAULT_AUTO_LOCK_SECONDS,
    val failedAttempts: Int = 0,
    val lockoutUntilEpochMillis: Long = 0L,
)

sealed interface PinVerifyResult {
    data object Success : PinVerifyResult
    data class Incorrect(val remainingAttempts: Int) : PinVerifyResult
    data class LockedOut(val remainingSeconds: Long) : PinVerifyResult
    data object CorruptedKeystore : PinVerifyResult
}

/**
 * Coordinates PIN verification, biometric unlock, and 30-second background auto-lock across
 * `:app` and `:feature:security`.
 */
class AppLockManager(
    private val lockStore: SecurityLockStore,
    private val pinHasher: PinHasher,
    private val secretProtector: SecretProtector,
    private val scope: CoroutineScope,
    private val elapsedRealtimeMillis: () -> Long,
    private val wallClockEpochMillis: () -> Long = { System.currentTimeMillis() },
    private val computeDispatcher: CoroutineDispatcher = Dispatchers.Default,
) {

    private val _state = MutableStateFlow(AppLockState())
    val state: StateFlow<AppLockState> = _state.asStateFlow()

    @Volatile
    private var latestPrefs = SecurityLockPrefs()

    @Volatile
    private var backgroundedElapsedMillis: Long? = null

    init {
        scope.launch {
            lockStore.lockPrefsFlow.collect { prefs ->
                latestPrefs = prefs
                _state.update { current ->
                    val shouldLock = when {
                        !prefs.isPinEnabled -> false
                        !current.isInitialized -> true // Lock immediately on cold start when PIN is enabled
                        else -> current.isLocked
                    }
                    current.copy(
                        isInitialized = true,
                        isLocked = shouldLock,
                        isPinEnabled = prefs.isPinEnabled,
                        isBiometricEnabled = prefs.isBiometricEnabled,
                        autoLockSeconds = prefs.autoLockSeconds,
                        failedAttempts = prefs.failedAttempts,
                        lockoutUntilEpochMillis = prefs.lockoutUntilEpochMillis,
                    )
                }
            }
        }
    }

    fun onAppBackgrounded() {
        if (backgroundedElapsedMillis == null) {
            backgroundedElapsedMillis = elapsedRealtimeMillis()
        }
    }

    fun onAppForegrounded() {
        val backgroundAt = backgroundedElapsedMillis ?: return
        backgroundedElapsedMillis = null
        val prefs = latestPrefs
        if (!prefs.isPinEnabled) return

        val elapsedMs = elapsedRealtimeMillis() - backgroundAt
        if (elapsedMs >= prefs.autoLockSeconds * 1000L) {
            _state.update { it.copy(isLocked = true) }
        }
    }

    fun lockNow() {
        if (latestPrefs.isPinEnabled) {
            backgroundedElapsedMillis = null
            _state.update { it.copy(isLocked = true) }
        }
    }

    fun unlockWithBiometric() {
        backgroundedElapsedMillis = null
        _state.update { it.copy(isLocked = false, failedAttempts = 0, lockoutUntilEpochMillis = 0L) }
        scope.launch { lockStore.resetFailedAttempts() }
    }

    suspend fun verifyPinToUnlock(pin: String): PinVerifyResult {
        val result = verifyStoredPin(pin)
        if (result is PinVerifyResult.Success) {
            backgroundedElapsedMillis = null
            _state.update { it.copy(isLocked = false, failedAttempts = 0, lockoutUntilEpochMillis = 0L) }
        }
        return result
    }

    suspend fun enablePin(newPin: String) {
        val encryptedBlob = withContext(computeDispatcher) {
            val hashed = pinHasher.hash(newPin)
            secretProtector.encrypt(hashed)
        }
        backgroundedElapsedMillis = null
        lockStore.savePin(encryptedBlob)
        _state.update { it.copy(isLocked = false, isPinEnabled = true) }
    }

    suspend fun changePin(oldPin: String, newPin: String): PinVerifyResult {
        val check = verifyStoredPin(oldPin)
        if (check is PinVerifyResult.Success) {
            enablePin(newPin)
        }
        return check
    }

    suspend fun disablePin(currentPin: String): PinVerifyResult {
        val check = verifyStoredPin(currentPin)
        if (check is PinVerifyResult.Success) {
            backgroundedElapsedMillis = null
            lockStore.clearPinAndDisableLock()
            _state.update {
                it.copy(
                    isLocked = false,
                    isPinEnabled = false,
                    isBiometricEnabled = false,
                    failedAttempts = 0,
                    lockoutUntilEpochMillis = 0L,
                )
            }
        }
        return check
    }

    suspend fun resetAfterCorruptedKeystore() {
        backgroundedElapsedMillis = null
        lockStore.clearPinAndDisableLock()
        _state.update {
            it.copy(
                isLocked = false,
                isPinEnabled = false,
                isBiometricEnabled = false,
                failedAttempts = 0,
                lockoutUntilEpochMillis = 0L,
            )
        }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        if (!latestPrefs.isPinEnabled && enabled) return
        lockStore.setBiometricEnabled(enabled)
    }

    suspend fun setAutoLockSeconds(seconds: Int) {
        lockStore.setAutoLockSeconds(seconds)
    }

    private suspend fun verifyStoredPin(pin: String): PinVerifyResult {
        val prefs = lockStore.lockPrefsFlow.first()
        val nowMillis = wallClockEpochMillis()

        if (prefs.lockoutUntilEpochMillis > nowMillis) {
            val remainingSec = ceil((prefs.lockoutUntilEpochMillis - nowMillis) / 1000.0).toLong()
            return PinVerifyResult.LockedOut(remainingSec.coerceAtLeast(1L))
        }

        val encryptedBlob = prefs.encryptedPinBlob ?: return PinVerifyResult.CorruptedKeystore
        val verified = withContext(computeDispatcher) {
            val decryptedHash = secretProtector.decrypt(encryptedBlob) ?: return@withContext null
            pinHasher.verify(pin, decryptedHash)
        } ?: return PinVerifyResult.CorruptedKeystore

        if (verified) {
            lockStore.resetFailedAttempts()
            return PinVerifyResult.Success
        }

        // Start a fresh attempt window if a prior temporary lockout already expired.
        val baselineAttempts = if (prefs.lockoutUntilEpochMillis > 0L) 0 else prefs.failedAttempts
        val newAttempts = baselineAttempts + 1
        return if (newAttempts >= MAX_FAILED_ATTEMPTS) {
            val lockoutUntil = nowMillis + LOCKOUT_DURATION_MS
            lockStore.recordFailedAttempt(newAttempts, lockoutUntil)
            PinVerifyResult.LockedOut(LOCKOUT_DURATION_MS / 1000L)
        } else {
            lockStore.recordFailedAttempt(newAttempts, 0L)
            PinVerifyResult.Incorrect(MAX_FAILED_ATTEMPTS - newAttempts)
        }
    }

    companion object {
        const val MAX_FAILED_ATTEMPTS = 5
        const val LOCKOUT_DURATION_MS = 30_000L
    }
}
