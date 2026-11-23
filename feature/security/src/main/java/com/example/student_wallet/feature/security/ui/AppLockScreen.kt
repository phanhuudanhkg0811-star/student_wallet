package com.example.student_wallet.feature.security.ui

import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.student_wallet.core.ui.theme.Student_walletTheme
import com.example.student_wallet.feature.security.biometric.BiometricAvailability
import com.example.student_wallet.feature.security.biometric.BiometricPromptHelper
import com.example.student_wallet.feature.security.crypto.PinHasher
import com.example.student_wallet.feature.security.lock.AppLockManager
import com.example.student_wallet.feature.security.lock.PinVerifyResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import kotlin.math.ceil

@Composable
fun AppLockOverlayRoute(
    modifier: Modifier = Modifier,
    appLockManager: AppLockManager = koinInject(),
) {
    val state by appLockManager.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val canUseBiometric = remember(context, state.isBiometricEnabled) {
        state.isBiometricEnabled &&
            BiometricPromptHelper.checkAvailability(context) == BiometricAvailability.AVAILABLE &&
            context.findFragmentActivity() != null
    }

    var pinInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isVerifying by remember { mutableStateOf(false) }
    var showCorruptedReset by remember { mutableStateOf(false) }
    var autoPromptedBiometric by rememberSaveable { mutableStateOf(false) }
    var lockoutRemainingSec by remember { mutableLongStateOf(0L) }

    LaunchedEffect(state.lockoutUntilEpochMillis) {
        while (true) {
            val remainingMs = state.lockoutUntilEpochMillis - System.currentTimeMillis()
            if (remainingMs <= 0L) {
                lockoutRemainingSec = 0L
                break
            }
            lockoutRemainingSec = ceil(remainingMs / 1000.0).toLong().coerceAtLeast(1L)
            delay(1_000L)
        }
    }

    val launchBiometric: () -> Unit = {
        val activity = context.findFragmentActivity()
        if (activity != null) {
            BiometricPromptHelper.authenticate(
                activity = activity,
                onSuccess = {
                    errorMessage = null
                    pinInput = ""
                    appLockManager.unlockWithBiometric()
                },
                onError = { message ->
                    errorMessage = message
                },
            )
        }
    }

    LaunchedEffect(state.isLocked, canUseBiometric) {
        if (state.isLocked && canUseBiometric && !autoPromptedBiometric) {
            autoPromptedBiometric = true
            launchBiometric()
        } else if (!state.isLocked) {
            autoPromptedBiometric = false
        }
    }

    AppLockScreen(
        pinLength = pinInput.length,
        isBiometricVisible = canUseBiometric,
        isVerifying = isVerifying,
        lockoutRemainingSeconds = lockoutRemainingSec,
        errorMessage = errorMessage,
        showCorruptedReset = showCorruptedReset,
        onDigitClick = { digit ->
            if (isVerifying || lockoutRemainingSec > 0L || pinInput.length >= PinHasher.PIN_LENGTH) {
                return@AppLockScreen
            }
            errorMessage = null
            val updated = pinInput + digit
            pinInput = updated
            if (updated.length == PinHasher.PIN_LENGTH) {
                isVerifying = true
                coroutineScope.launch {
                    val result = appLockManager.verifyPinToUnlock(updated)
                    isVerifying = false
                    pinInput = ""
                    when (result) {
                        PinVerifyResult.Success -> Unit
                        is PinVerifyResult.Incorrect -> {
                            errorMessage = "Mã PIN không đúng (còn ${result.remainingAttempts} lần thử)"
                        }
                        is PinVerifyResult.LockedOut -> {
                            errorMessage = null
                        }
                        PinVerifyResult.CorruptedKeystore -> {
                            showCorruptedReset = true
                            errorMessage = "Khóa bảo mật thiết bị đã thay đổi. Vui lòng đặt lại khóa PIN."
                        }
                    }
                }
            }
        },
        onBackspaceClick = {
            if (!isVerifying && pinInput.isNotEmpty()) {
                pinInput = pinInput.dropLast(1)
            }
        },
        onBiometricClick = launchBiometric,
        onResetCorruptedClick = {
            coroutineScope.launch {
                appLockManager.resetAfterCorruptedKeystore()
                showCorruptedReset = false
                errorMessage = null
            }
        },
        modifier = modifier,
    )
}

@Composable
internal fun AppLockScreen(
    pinLength: Int,
    isBiometricVisible: Boolean,
    isVerifying: Boolean,
    lockoutRemainingSeconds: Long,
    errorMessage: String?,
    showCorruptedReset: Boolean,
    onDigitClick: (Char) -> Unit,
    onBackspaceClick: () -> Unit,
    onBiometricClick: () -> Unit,
    onResetCorruptedClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(32.dp),
                    )
                }
                Text(
                    text = "Mở khóa StudentWallet",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "Nhập mã PIN 6 số để tiếp tục truy cập ví của bạn",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(14.dp))
                PinDotsIndicator(
                    pinLength = pinLength,
                    isError = errorMessage != null || lockoutRemainingSeconds > 0L,
                )

                Spacer(modifier = Modifier.height(8.dp))
                when {
                    lockoutRemainingSeconds > 0L -> {
                        Text(
                            text = "Nhập sai quá 5 lần. Thử lại sau ${lockoutRemainingSeconds}s",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                        )
                    }
                    errorMessage != null -> {
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                        )
                    }
                    else -> {
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }

                if (showCorruptedReset) {
                    TextButton(onClick = onResetCorruptedClick) {
                        Text("Đặt lại khóa ứng dụng")
                    }
                }
            }

            NumericPinKeypad(
                onDigitClick = onDigitClick,
                onBackspaceClick = onBackspaceClick,
                enabled = !isVerifying && lockoutRemainingSeconds <= 0L,
                showBiometricKey = isBiometricVisible,
                onBiometricClick = onBiometricClick,
            )
        }
    }
}

private tailrec fun Context.findFragmentActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findFragmentActivity()
    else -> null
}

@Preview(showBackground = true)
@Composable
private fun AppLockScreenPreview() {
    Student_walletTheme {
        AppLockScreen(
            pinLength = 4,
            isBiometricVisible = true,
            isVerifying = false,
            lockoutRemainingSeconds = 0L,
            errorMessage = null,
            showCorruptedReset = false,
            onDigitClick = {},
            onBackspaceClick = {},
            onBiometricClick = {},
            onResetCorruptedClick = {},
        )
    }
}
