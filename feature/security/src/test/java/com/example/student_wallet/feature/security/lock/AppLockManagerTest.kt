package com.example.student_wallet.feature.security.lock

import com.example.student_wallet.feature.security.crypto.PinHasher
import com.example.student_wallet.feature.security.crypto.SecretProtector
import com.example.student_wallet.feature.security.data.SecurityLockPrefs
import com.example.student_wallet.feature.security.data.SecurityLockStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppLockManagerTest {

    @Test
    fun `cold start locks when PIN is already enabled and stays unlocked when disabled`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val hasher = PinHasher(defaultIterations = 1_000)
        val protector = FakeSecretProtector()

        val disabledScope = CoroutineScope(SupervisorJob() + dispatcher)
        val disabledManager = AppLockManager(
            lockStore = FakeSecurityLockStore(SecurityLockPrefs(isPinEnabled = false)),
            pinHasher = hasher,
            secretProtector = protector,
            scope = disabledScope,
            elapsedRealtimeMillis = { 0L },
            computeDispatcher = dispatcher,
        )
        advanceUntilIdle()
        assertTrue(disabledManager.state.value.isInitialized)
        assertFalse(disabledManager.state.value.isLocked)
        disabledScope.cancel()

        val enabledScope = CoroutineScope(SupervisorJob() + dispatcher)
        val enabledManager = AppLockManager(
            lockStore = FakeSecurityLockStore(
                SecurityLockPrefs(
                    encryptedPinBlob = protector.encrypt(hasher.hash("123456")),
                    isPinEnabled = true,
                ),
            ),
            pinHasher = hasher,
            secretProtector = protector,
            scope = enabledScope,
            elapsedRealtimeMillis = { 0L },
            computeDispatcher = dispatcher,
        )
        advanceUntilIdle()
        assertTrue(enabledManager.state.value.isInitialized)
        assertTrue(enabledManager.state.value.isLocked)
        enabledScope.cancel()
    }

    @Test
    fun `background auto-lock triggers only at or after 30 seconds`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val hasher = PinHasher(defaultIterations = 1_000)
        val protector = FakeSecretProtector()
        var elapsedMs = 1_000L
        val scope = CoroutineScope(SupervisorJob() + dispatcher)
        val manager = AppLockManager(
            lockStore = FakeSecurityLockStore(
                SecurityLockPrefs(
                    encryptedPinBlob = protector.encrypt(hasher.hash("654321")),
                    isPinEnabled = true,
                    autoLockSeconds = 30,
                ),
            ),
            pinHasher = hasher,
            secretProtector = protector,
            scope = scope,
            elapsedRealtimeMillis = { elapsedMs },
            computeDispatcher = dispatcher,
        )
        advanceUntilIdle()
        assertTrue(manager.verifyPinToUnlock("654321") is PinVerifyResult.Success)
        assertFalse(manager.state.value.isLocked)

        manager.onAppBackgrounded()
        elapsedMs += 29_999L
        manager.onAppForegrounded()
        assertFalse(manager.state.value.isLocked)

        manager.onAppBackgrounded()
        elapsedMs += 30_000L
        manager.onAppForegrounded()
        assertTrue(manager.state.value.isLocked)
        scope.cancel()
    }

    @Test
    fun `5 failed attempts trigger 30 second lockout and reset after valid unlock`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val hasher = PinHasher(defaultIterations = 1_000)
        val protector = FakeSecretProtector()
        var wallMs = 10_000L
        val scope = CoroutineScope(SupervisorJob() + dispatcher)
        val manager = AppLockManager(
            lockStore = FakeSecurityLockStore(
                SecurityLockPrefs(
                    encryptedPinBlob = protector.encrypt(hasher.hash("112233")),
                    isPinEnabled = true,
                ),
            ),
            pinHasher = hasher,
            secretProtector = protector,
            scope = scope,
            elapsedRealtimeMillis = { wallMs },
            wallClockEpochMillis = { wallMs },
            computeDispatcher = dispatcher,
        )
        advanceUntilIdle()

        repeat(4) { attempt ->
            val result = manager.verifyPinToUnlock("000000")
            assertEquals(PinVerifyResult.Incorrect(remainingAttempts = 4 - attempt), result)
        }

        val fifthResult = manager.verifyPinToUnlock("000000")
        assertTrue(fifthResult is PinVerifyResult.LockedOut)

        val blockedEvenWithRightPin = manager.verifyPinToUnlock("112233")
        assertTrue(blockedEvenWithRightPin is PinVerifyResult.LockedOut)

        wallMs += 30_001L
        val unlocked = manager.verifyPinToUnlock("112233")
        assertEquals(PinVerifyResult.Success, unlocked)
        assertFalse(manager.state.value.isLocked)
        scope.cancel()
    }
}

internal class FakeSecretProtector : SecretProtector {
    override fun encrypt(plaintext: String): String = "enc:$plaintext"
    override fun decrypt(payload: String): String? =
        if (payload.startsWith("enc:")) payload.removePrefix("enc:") else null
}

internal class FakeSecurityLockStore(
    initial: SecurityLockPrefs = SecurityLockPrefs(),
) : SecurityLockStore {
    private val flow = MutableStateFlow(initial)
    override val lockPrefsFlow: Flow<SecurityLockPrefs> = flow

    override suspend fun savePin(encryptedPinBlob: String) {
        flow.value = flow.value.copy(
            encryptedPinBlob = encryptedPinBlob,
            isPinEnabled = true,
            failedAttempts = 0,
            lockoutUntilEpochMillis = 0L,
        )
    }

    override suspend fun clearPinAndDisableLock() {
        flow.value = SecurityLockPrefs(autoLockSeconds = flow.value.autoLockSeconds)
    }

    override suspend fun setBiometricEnabled(enabled: Boolean) {
        flow.value = flow.value.copy(isBiometricEnabled = enabled)
    }

    override suspend fun setAutoLockSeconds(seconds: Int) {
        flow.value = flow.value.copy(autoLockSeconds = seconds)
    }

    override suspend fun recordFailedAttempt(failedAttempts: Int, lockoutUntilEpochMillis: Long) {
        flow.value = flow.value.copy(
            failedAttempts = failedAttempts,
            lockoutUntilEpochMillis = lockoutUntilEpochMillis,
        )
    }

    override suspend fun resetFailedAttempts() {
        flow.value = flow.value.copy(failedAttempts = 0, lockoutUntilEpochMillis = 0L)
    }
}
