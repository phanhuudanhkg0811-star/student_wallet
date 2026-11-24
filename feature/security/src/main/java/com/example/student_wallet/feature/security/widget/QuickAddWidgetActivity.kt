package com.example.student_wallet.feature.security.widget

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.student_wallet.core.domain.format.MoneyFormatter
import com.example.student_wallet.core.domain.model.PaymentMethod
import com.example.student_wallet.core.domain.model.ThemeMode
import com.example.student_wallet.core.domain.model.TransactionType
import com.example.student_wallet.core.domain.model.UserSettings
import com.example.student_wallet.core.domain.repository.SettingsRepository
import com.example.student_wallet.core.ui.theme.Student_walletTheme
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

private val presetAmounts = listOf(
    20_000L to "+20k",
    35_000L to "+35k",
    50_000L to "+50k",
    100_000L to "+100k",
)

private val paymentOptions = listOf(
    PaymentMethod.CASH to "Tiền mặt",
    PaymentMethod.MOMO to "MoMo",
    PaymentMethod.BANK_TRANSFER to "Ngân hàng",
)

/** Translucent floating dialog launched from the Home Screen Glance Widget Quick Add button. */
class QuickAddWidgetActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val settingsRepository: SettingsRepository = koinInject()
            val settings by settingsRepository.observeSettings()
                .collectAsStateWithLifecycle(initialValue = UserSettings())
            val viewModel: QuickAddWidgetViewModel = koinViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            val darkTheme = when (settings.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            Student_walletTheme(
                darkTheme = darkTheme,
                dynamicColor = settings.dynamicColor,
            ) {
                QuickAddDialogContent(
                    uiState = uiState,
                    onDismiss = ::finish,
                    onAmountChange = viewModel::onAmountInputChange,
                    onAddPreset = viewModel::onAddQuickPreset,
                    onTypeChange = viewModel::onTypeChange,
                    onCategorySelect = viewModel::onCategorySelected,
                    onNoteChange = viewModel::onNoteChange,
                    onPaymentSelect = viewModel::onPaymentMethodSelected,
                    onSubmit = {
                        viewModel.submit { savedAmount, currency ->
                            withContext(NonCancellable) {
                                WalletWidgetUpdater.refreshAll(applicationContext)
                            }
                            Toast.makeText(
                                applicationContext,
                                "Đã lưu ${MoneyFormatter.format(savedAmount, currency)} · Đã cập nhật Widget",
                                Toast.LENGTH_SHORT,
                            ).show()
                            finish()
                        }
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickAddDialogContent(
    uiState: QuickAddUiState,
    onDismiss: () -> Unit,
    onAmountChange: (String) -> Unit,
    onAddPreset: (Long) -> Unit,
    onTypeChange: (TransactionType) -> Unit,
    onCategorySelect: (Long) -> Unit,
    onNoteChange: (String) -> Unit,
    onPaymentSelect: (PaymentMethod) -> Unit,
    onSubmit: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.42f))
            .imePadding()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            )
            .padding(horizontal = 18.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
            shadowElevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = "Ghi nhanh từ Widget",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "Lưu vào ví & tự cập nhật Widget màn hình chính",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Filled.Close, contentDescription = "Đóng")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    QuickChip(
                        label = "Chi tiêu",
                        selected = uiState.type == TransactionType.EXPENSE,
                        onClick = { onTypeChange(TransactionType.EXPENSE) },
                        modifier = Modifier.weight(1f),
                    )
                    QuickChip(
                        label = "Thu nhập",
                        selected = uiState.type == TransactionType.INCOME,
                        onClick = { onTypeChange(TransactionType.INCOME) },
                        modifier = Modifier.weight(1f),
                    )
                }

                OutlinedTextField(
                    value = uiState.amountDigits,
                    onValueChange = onAmountChange,
                    label = { Text("Số tiền (${uiState.currency.symbol})") },
                    placeholder = { Text("Ví dụ: 35000") },
                    supportingText = {
                        val preview = uiState.amountDigits.toLongOrNull()?.let {
                            MoneyFormatter.format(it, uiState.currency)
                        }
                        if (preview != null) {
                            Text("Hiển thị: $preview")
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    presetAmounts.forEach { (delta, label) ->
                        QuickChip(
                            label = label,
                            selected = false,
                            onClick = { onAddPreset(delta) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                if (uiState.availableCategories.isNotEmpty()) {
                    Text(
                        text = "Danh mục",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        uiState.availableCategories.forEach { category ->
                            QuickChip(
                                label = "${category.emoji} ${category.name}",
                                selected = uiState.selectedCategoryId == category.id,
                                onClick = { onCategorySelect(category.id) },
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    paymentOptions.forEach { (method, label) ->
                        QuickChip(
                            label = label,
                            selected = uiState.paymentMethod == method,
                            onClick = { onPaymentSelect(method) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                OutlinedTextField(
                    value = uiState.note,
                    onValueChange = onNoteChange,
                    label = { Text("Ghi chú ngắn (tùy chọn)") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                )

                if (uiState.errorMessage != null) {
                    Text(
                        text = uiState.errorMessage,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                Button(
                    onClick = onSubmit,
                    enabled = !uiState.isSaving,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("Lưu & Cập nhật Widget")
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(12.dp)
    val bg = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.background
    val border = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        shape = shape,
        color = bg,
        border = BorderStroke(1.dp, border),
        modifier = modifier
            .clip(shape)
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = fg,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
