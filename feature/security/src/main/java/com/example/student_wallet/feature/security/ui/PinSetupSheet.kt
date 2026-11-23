package com.example.student_wallet.feature.security.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.student_wallet.feature.security.PinDialogMode
import com.example.student_wallet.feature.security.crypto.PinHasher

private enum class PinFlowStep {
    VERIFY_CURRENT,
    ENTER_NEW,
    CONFIRM_NEW,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PinSetupSheet(
    mode: PinDialogMode,
    externalError: String?,
    isProcessing: Boolean,
    onDismiss: () -> Unit,
    onSubmitCreate: (newPin: String) -> Unit,
    onSubmitChange: (oldPin: String, newPin: String) -> Unit,
    onSubmitDisable: (currentPin: String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var step by remember(mode) {
        mutableStateOf(
            when (mode) {
                PinDialogMode.CREATE -> PinFlowStep.ENTER_NEW
                PinDialogMode.CHANGE, PinDialogMode.DISABLE -> PinFlowStep.VERIFY_CURRENT
            },
        )
    }
    var currentPinBuffer by remember(mode) { mutableStateOf("") }
    var newPinBuffer by remember(mode) { mutableStateOf("") }
    var activeInput by remember(mode) { mutableStateOf("") }
    var localError by remember(mode) { mutableStateOf<String?>(null) }

    val displayedError = externalError ?: localError

    val (title, subtitle) = when (step) {
        PinFlowStep.VERIFY_CURRENT -> when (mode) {
            PinDialogMode.DISABLE -> "Tắt khóa PIN 6 số" to "Nhập mã PIN hiện tại để xác nhận tắt khóa"
            else -> "Đổi mã PIN 6 số" to "Bước 1/3 · Nhập mã PIN hiện tại"
        }
        PinFlowStep.ENTER_NEW -> {
            val stepPrefix = if (mode == PinDialogMode.CHANGE) "Bước 2/3 · " else "Bước 1/2 · "
            "Thiết lập mã PIN mới" to "${stepPrefix}Nhập mã PIN gồm 6 chữ số"
        }
        PinFlowStep.CONFIRM_NEW -> {
            val stepPrefix = if (mode == PinDialogMode.CHANGE) "Bước 3/3 · " else "Bước 2/2 · "
            "Xác nhận mã PIN" to "${stepPrefix}Nhập lại đúng mã PIN vừa đặt"
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(6.dp))
            PinDotsIndicator(
                pinLength = activeInput.length,
                isError = displayedError != null,
            )

            if (displayedError != null) {
                Text(
                    text = displayedError,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
            } else {
                Spacer(modifier = Modifier.height(18.dp))
            }

            NumericPinKeypad(
                enabled = !isProcessing,
                onDigitClick = { digit ->
                    if (isProcessing || activeInput.length >= PinHasher.PIN_LENGTH) return@NumericPinKeypad
                    localError = null
                    val next = activeInput + digit
                    activeInput = next
                    if (next.length == PinHasher.PIN_LENGTH) {
                        when (step) {
                            PinFlowStep.VERIFY_CURRENT -> {
                                if (mode == PinDialogMode.DISABLE) {
                                    activeInput = ""
                                    onSubmitDisable(next)
                                } else {
                                    currentPinBuffer = next
                                    activeInput = ""
                                    step = PinFlowStep.ENTER_NEW
                                }
                            }
                            PinFlowStep.ENTER_NEW -> {
                                newPinBuffer = next
                                activeInput = ""
                                step = PinFlowStep.CONFIRM_NEW
                            }
                            PinFlowStep.CONFIRM_NEW -> {
                                if (next != newPinBuffer) {
                                    localError = "Mã PIN nhập lại chưa khớp. Vui lòng thử lại."
                                    activeInput = ""
                                } else {
                                    activeInput = ""
                                    if (mode == PinDialogMode.CREATE) {
                                        onSubmitCreate(newPinBuffer)
                                    } else {
                                        onSubmitChange(currentPinBuffer, newPinBuffer)
                                    }
                                }
                            }
                        }
                    }
                },
                onBackspaceClick = {
                    if (activeInput.isNotEmpty()) {
                        activeInput = activeInput.dropLast(1)
                    }
                },
            )

            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
