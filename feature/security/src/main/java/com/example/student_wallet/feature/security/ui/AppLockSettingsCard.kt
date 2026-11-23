package com.example.student_wallet.feature.security.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.student_wallet.core.ui.components.SectionHeader
import com.example.student_wallet.feature.security.R
import com.example.student_wallet.feature.security.SecurityIntent
import com.example.student_wallet.feature.security.biometric.BiometricAvailability
import com.example.student_wallet.feature.security.lock.AppLockState

private val autoLockTimeoutOptions = listOf(
    0 to "Ngay lập tức",
    30 to "30 giây",
    60 to "1 phút",
)

@Composable
internal fun AppLockSettingsCard(
    lockState: AppLockState,
    biometricAvailability: BiometricAvailability,
    onIntent: (SecurityIntent) -> Unit,
    cardContainer: @Composable (@Composable ColumnScope.() -> Unit) -> Unit,
    badgeIcon: @Composable (ImageVector, Color, Color) -> Unit,
    selectablePill: @Composable (String, Boolean, () -> Unit, Modifier) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader(title = stringResource(R.string.security_applock_section))
        cardContainer {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    badgeIcon(
                        Icons.Filled.Lock,
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Column {
                        Text(
                            text = "Khóa bằng mã PIN 6 số",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        )
                        Text(
                            text = if (lockState.isPinEnabled) {
                                "Đã bảo vệ bằng PBKDF2 & Android Keystore"
                            } else {
                                "Bật để bảo vệ số dư và lịch sử giao dịch"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = lockState.isPinEnabled,
                    onCheckedChange = { onIntent(SecurityIntent.RequestTogglePinLock) },
                )
            }

            if (lockState.isPinEnabled) {
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        badgeIcon(
                            Icons.Filled.Fingerprint,
                            MaterialTheme.colorScheme.tertiaryContainer,
                            MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                        Column {
                            Text(
                                text = "Mở khóa bằng Sinh trắc học",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                            )
                            Text(
                                text = when (biometricAvailability) {
                                    BiometricAvailability.AVAILABLE -> "Vân tay hoặc khuôn mặt qua BiometricPrompt"
                                    BiometricAvailability.NOT_ENROLLED -> "Thiết bị chưa cài đặt vân tay / khuôn mặt"
                                    BiometricAvailability.UNAVAILABLE -> "Thiết bị không hỗ trợ phần cứng sinh trắc học"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = lockState.isBiometricEnabled,
                        enabled = biometricAvailability == BiometricAvailability.AVAILABLE,
                        onCheckedChange = { onIntent(SecurityIntent.ToggleBiometricLock(it)) },
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Timer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = "Tự động khóa khi ẩn ứng dụng",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    autoLockTimeoutOptions.forEach { (seconds, label) ->
                        selectablePill(
                            label,
                            lockState.autoLockSeconds == seconds,
                            { onIntent(SecurityIntent.SelectAutoLockSeconds(seconds)) },
                            Modifier.weight(1f),
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = { onIntent(SecurityIntent.RequestChangePin) }) {
                        Icon(
                            imageVector = Icons.Filled.Password,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Đổi mã PIN")
                    }
                    OutlinedButton(
                        onClick = { onIntent(SecurityIntent.LockNowClick) },
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Khóa ngay")
                    }
                }
            }
        }
    }
}
