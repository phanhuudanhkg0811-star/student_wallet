package com.example.student_wallet.feature.security.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.student_wallet.core.domain.model.ThemeMode
import com.example.student_wallet.core.ui.theme.financeColors
import com.example.student_wallet.feature.security.backup.BackupPreviewInfo
import com.example.student_wallet.feature.security.backup.WalletCrypto
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val backupTimestampFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BackupCreateSheet(
    isBusy: Boolean,
    externalError: String?,
    onDismiss: () -> Unit,
    onSaveToDevice: (passphrase: String) -> Unit,
    onShareFile: (passphrase: String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var passphrase by remember { mutableStateOf("") }
    var confirmPassphrase by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }

    val displayedError = externalError ?: localError

    fun validate(): String? {
        if (passphrase.length < WalletCrypto.MIN_PASSPHRASE_LENGTH) {
            localError = "Mật khẩu sao lưu cần tối thiểu ${WalletCrypto.MIN_PASSPHRASE_LENGTH} ký tự."
            return null
        }
        if (passphrase != confirmPassphrase) {
            localError = "Mật khẩu xác nhận chưa trùng khớp."
            return null
        }
        localError = null
        return passphrase
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Đặt mật khẩu tệp .wallet",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = "Dùng PBKDF2 + AES-256 CBC và gắn checksum SHA-256. Hãy ghi nhớ mật khẩu vì không thể khôi phục nếu quên.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            PassphraseField(
                value = passphrase,
                onValueChange = {
                    passphrase = it
                    localError = null
                },
                label = "Mật khẩu sao lưu (>= 6 ký tự)",
                showPassword = showPassword,
                onToggleVisibility = { showPassword = !showPassword },
                enabled = !isBusy,
            )

            PassphraseField(
                value = confirmPassphrase,
                onValueChange = {
                    confirmPassphrase = it
                    localError = null
                },
                label = "Nhập lại mật khẩu",
                showPassword = showPassword,
                onToggleVisibility = { showPassword = !showPassword },
                enabled = !isBusy,
            )

            if (displayedError != null) {
                Text(
                    text = displayedError,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = { validate()?.let(onSaveToDevice) },
                    enabled = !isBusy,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Download,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Lưu về máy", maxLines = 1)
                }

                Button(
                    onClick = { validate()?.let(onShareFile) },
                    enabled = !isBusy,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    if (isBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Chia sẻ", maxLines = 1)
                    }
                }
            }

            TextButton(
                onClick = onDismiss,
                enabled = !isBusy,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text("Hủy")
            }
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BackupRestoreSheet(
    fileName: String,
    preview: BackupPreviewInfo?,
    isBusy: Boolean,
    externalError: String?,
    onDismiss: () -> Unit,
    onInspectWithPassphrase: (passphrase: String) -> Unit,
    onConfirmRestore: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var passphrase by remember(fileName) { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var localError by remember(fileName) { mutableStateOf<String?>(null) }

    val displayedError = externalError ?: localError

    ModalBottomSheet(
        onDismissRequest = { if (!isBusy) onDismiss() },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (preview == null) {
                Text(
                    text = "Bước 1/2 · Giải mã & Kiểm tra tệp",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "Tệp đã chọn: $fileName",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "Ứng dụng sẽ xác thực SHA-256 checksum và giải mã AES-256 để bạn xem trước nội dung trước khi ghi đè dữ liệu.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                PassphraseField(
                    value = passphrase,
                    onValueChange = {
                        passphrase = it
                        localError = null
                    },
                    label = "Mật khẩu bản sao lưu",
                    showPassword = showPassword,
                    onToggleVisibility = { showPassword = !showPassword },
                    enabled = !isBusy,
                )

                if (displayedError != null) {
                    Text(
                        text = displayedError,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        enabled = !isBusy,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Hủy")
                    }
                    Button(
                        onClick = {
                            if (passphrase.length < WalletCrypto.MIN_PASSPHRASE_LENGTH) {
                                localError = "Vui lòng nhập mật khẩu tối thiểu ${WalletCrypto.MIN_PASSPHRASE_LENGTH} ký tự."
                            } else {
                                localError = null
                                onInspectWithPassphrase(passphrase)
                            }
                        },
                        enabled = !isBusy,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1.4f),
                    ) {
                        if (isBusy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        } else {
                            Text("Kiểm tra & Xem trước", maxLines = 1)
                        }
                    }
                }
            } else {
                val finance = MaterialTheme.financeColors
                val createdLabel = if (preview.createdAtEpochMillis > 0L) {
                    Instant.ofEpochMilli(preview.createdAtEpochMillis)
                        .atZone(ZoneId.systemDefault())
                        .format(backupTimestampFormatter)
                } else {
                    "Không rõ thời gian"
                }

                Text(
                    text = "Bước 2/2 · Xem trước bản sao lưu",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = finance.budgetOk.copy(alpha = 0.12f),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.VerifiedUser,
                            contentDescription = null,
                            tint = finance.budgetOk,
                        )
                        Column {
                            Text(
                                text = "Đã xác thực AES-256 & SHA-256",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = finance.budgetOk,
                            )
                            Text(
                                text = "Checksum: ${preview.checksumShortHex}… · DB v${preview.dbVersion}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        PreviewInfoRow("Tệp", preview.fileName)
                        PreviewInfoRow("Ngày tạo", createdLabel)
                        PreviewInfoRow("Giao dịch", "${preview.transactionCount} giao dịch")
                        PreviewInfoRow("Danh mục", "${preview.categoryCount} danh mục")
                        PreviewInfoRow("Ảnh hóa đơn", "${preview.receiptCount} ảnh")
                        PreviewInfoRow(
                            "Cài đặt kèm theo",
                            "${preview.settings.currency.name} (${preview.settings.currency.symbol}) · ${themeLabel(preview.settings.themeMode)}",
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.WarningAmber,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                        )
                        Text(
                            text = "Xác nhận khôi phục sẽ thay thế toàn bộ dữ liệu giao dịch, danh mục, ảnh hóa đơn và giao diện/tiền tệ hiện tại. Khóa PIN trên máy không bị thay đổi.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                if (displayedError != null) {
                    Text(
                        text = displayedError,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        enabled = !isBusy,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Hủy")
                    }
                    Button(
                        onClick = onConfirmRestore,
                        enabled = !isBusy,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1.4f),
                    ) {
                        if (isBusy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onError,
                            )
                        } else {
                            Text("Xác nhận khôi phục", maxLines = 1)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

@Composable
private fun PassphraseField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    showPassword: Boolean,
    onToggleVisibility: () -> Unit,
    enabled: Boolean,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        enabled = enabled,
        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            IconButton(onClick = onToggleVisibility) {
                Icon(
                    imageVector = if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = if (showPassword) "Ẩn mật khẩu" else "Hiện mật khẩu",
                )
            }
        },
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun PreviewInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun themeLabel(mode: ThemeMode): String = when (mode) {
    ThemeMode.SYSTEM -> "Theo máy"
    ThemeMode.LIGHT -> "Sáng"
    ThemeMode.DARK -> "Tối"
}
