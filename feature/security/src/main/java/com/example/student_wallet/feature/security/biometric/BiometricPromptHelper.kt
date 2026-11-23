package com.example.student_wallet.feature.security.biometric

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

enum class BiometricAvailability {
    AVAILABLE,
    NOT_ENROLLED,
    UNAVAILABLE,
}

/** Wraps AndroidX [BiometricPrompt] and capability checks for biometric unlock. */
object BiometricPromptHelper {

    private const val ALLOWED_AUTHENTICATORS =
        BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.BIOMETRIC_WEAK

    fun checkAvailability(context: Context): BiometricAvailability {
        val status = BiometricManager.from(context).canAuthenticate(ALLOWED_AUTHENTICATORS)
        return when (status) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricAvailability.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricAvailability.NOT_ENROLLED
            else -> BiometricAvailability.UNAVAILABLE
        }
    }

    fun authenticate(
        activity: FragmentActivity,
        title: String = "Mở khóa StudentWallet",
        subtitle: String = "Xác thực sinh trắc học để tiếp tục",
        negativeButtonText: String = "Dùng mã PIN",
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                val isUserCancel = errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                    errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                    errorCode == BiometricPrompt.ERROR_CANCELED
                if (!isUserCancel) {
                    onError(errString.toString())
                }
            }
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(negativeButtonText)
            .setAllowedAuthenticators(ALLOWED_AUTHENTICATORS)
            .build()

        BiometricPrompt(activity, executor, callback).authenticate(promptInfo)
    }
}
