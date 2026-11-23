package com.example.student_wallet.feature.security

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.student_wallet.core.domain.export.ExportFormat
import com.example.student_wallet.core.domain.format.MoneyFormatter
import com.example.student_wallet.core.domain.model.CurrencyOption
import com.example.student_wallet.core.domain.model.ThemeMode
import com.example.student_wallet.core.domain.model.UserSettings
import com.example.student_wallet.core.ui.components.SectionHeader
import com.example.student_wallet.core.ui.theme.Student_walletTheme
import com.example.student_wallet.core.ui.theme.financeColors
import com.example.student_wallet.feature.security.biometric.BiometricAvailability
import com.example.student_wallet.feature.security.biometric.BiometricPromptHelper
import com.example.student_wallet.feature.security.ui.AppLockSettingsCard
import com.example.student_wallet.feature.security.ui.PinSetupSheet
import org.koin.compose.viewmodel.koinViewModel
import java.io.File

@Composable
fun SecurityScreen(
    modifier: Modifier = Modifier,
    viewModel: SecurityViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val biometricAvailability = remember(context) {
        BiometricPromptHelper.checkAvailability(context)
    }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = CreateExportDocumentContract,
    ) { uri ->
        if (uri != null) {
            viewModel.onIntent(
                SecurityIntent.ExportToStream { context.contentResolver.openOutputStream(uri) },
            )
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is SecurityEffect.LaunchCreateDocument -> {
                    createDocumentLauncher.launch(effect)
                }
                is SecurityEffect.ShareExportedFile -> {
                    shareExportFile(context, effect.file, effect.mimeType, effect.title)
                }
                is SecurityEffect.ShowMessage -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
            }
        }
    }

    SecurityContent(
        uiState = uiState,
        biometricAvailability = biometricAvailability,
        onIntent = viewModel::onIntent,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )

    uiState.activePinDialog?.let { mode ->
        PinSetupSheet(
            mode = mode,
            externalError = uiState.pinDialogError,
            isProcessing = uiState.isProcessingPin,
            onDismiss = { viewModel.onIntent(SecurityIntent.DismissPinDialog) },
            onSubmitCreate = { newPin -> viewModel.onIntent(SecurityIntent.SubmitCreatePin(newPin)) },
            onSubmitChange = { oldPin, newPin ->
                viewModel.onIntent(SecurityIntent.SubmitChangePin(oldPin, newPin))
            },
            onSubmitDisable = { currentPin ->
                viewModel.onIntent(SecurityIntent.SubmitDisablePin(currentPin))
            },
        )
    }
}

@Composable
internal fun SecurityContent(
    uiState: SecurityUiState,
    biometricAvailability: BiometricAvailability,
    onIntent: (SecurityIntent) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.security_title),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = stringResource(R.string.security_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item {
                AppearanceSettingsCard(
                    settings = uiState.settings,
                    onSelectTheme = { onIntent(SecurityIntent.SelectTheme(it)) },
                    onToggleDynamicColor = { onIntent(SecurityIntent.ToggleDynamicColor(it)) },
                )
            }

            item {
                CurrencySettingsCard(
                    selectedCurrency = uiState.settings.currency,
                    onSelectCurrency = { onIntent(SecurityIntent.SelectCurrency(it)) },
                )
            }

            item {
                AppLockSettingsCard(
                    lockState = uiState.lockState,
                    biometricAvailability = biometricAvailability,
                    onIntent = onIntent,
                    cardContainer = { SettingsCard(content = it) },
                    badgeIcon = { icon, container, content -> IconBadge(icon, container, content) },
                    selectablePill = { label, selected, onClick, itemMod ->
                        SelectablePill(label = label, selected = selected, onClick = onClick, modifier = itemMod)
                    },
                )
            }

            item {
                ExportReportCard(
                    uiState = uiState,
                    onIntent = onIntent,
                )
            }

            item {
                SecurityRoadmapCard()
            }
        }
    }
}

@Composable
private fun AppearanceSettingsCard(
    settings: UserSettings,
    onSelectTheme: (ThemeMode) -> Unit,
    onToggleDynamicColor: (Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader(title = stringResource(R.string.security_appearance_section))
        SettingsCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ThemeOptionChip(
                    label = "Theo máy",
                    icon = Icons.Filled.PhoneAndroid,
                    selected = settings.themeMode == ThemeMode.SYSTEM,
                    onClick = { onSelectTheme(ThemeMode.SYSTEM) },
                    modifier = Modifier.weight(1f),
                )
                ThemeOptionChip(
                    label = "Sáng",
                    icon = Icons.Filled.LightMode,
                    selected = settings.themeMode == ThemeMode.LIGHT,
                    onClick = { onSelectTheme(ThemeMode.LIGHT) },
                    modifier = Modifier.weight(1f),
                )
                ThemeOptionChip(
                    label = "Tối",
                    icon = Icons.Filled.DarkMode,
                    selected = settings.themeMode == ThemeMode.DARK,
                    onClick = { onSelectTheme(ThemeMode.DARK) },
                    modifier = Modifier.weight(1f),
                )
            }

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
                    IconBadge(
                        icon = Icons.Filled.Palette,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    Column {
                        Text(
                            text = "Material You (Dynamic Color)",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "Dùng bảng màu theo hình nền Android 12+",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = settings.dynamicColor,
                    onCheckedChange = onToggleDynamicColor,
                )
            }
        }
    }
}

@Composable
private fun CurrencySettingsCard(
    selectedCurrency: CurrencyOption,
    onSelectCurrency: (CurrencyOption) -> Unit,
) {
    val options = listOf(
        Triple(CurrencyOption.VND, "VND (đ)", "Ví dụ: ${MoneyFormatter.format(45_000L, CurrencyOption.VND)}"),
        Triple(CurrencyOption.USD, "USD ($)", "Ví dụ: ${MoneyFormatter.format(45_000L, CurrencyOption.USD)}"),
        Triple(CurrencyOption.EUR, "EUR (€)", "Ví dụ: ${MoneyFormatter.format(45_000L, CurrencyOption.EUR)}"),
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader(title = stringResource(R.string.security_currency_section))
        SettingsCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                IconBadge(
                    icon = Icons.Filled.Payments,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Column {
                    Text(
                        text = "Đơn vị hiển thị mặc định",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    )
                    Text(
                        text = "Đổi ký hiệu & định dạng số trên toàn ứng dụng",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                options.forEach { (option, label, _) ->
                    SelectablePill(
                        label = label,
                        selected = selectedCurrency == option,
                        onClick = { onSelectCurrency(option) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            val sample = options.first { it.first == selectedCurrency }.third
            Text(
                text = sample,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun ExportReportCard(
    uiState: SecurityUiState,
    onIntent: (SecurityIntent) -> Unit,
) {
    val currency = uiState.settings.currency
    val finance = MaterialTheme.financeColors

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader(title = stringResource(R.string.security_export_section))
        SettingsCard {
            Text(
                text = "Khoảng thời gian",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ReportPeriod.entries.forEach { period ->
                    SelectablePill(
                        label = period.label,
                        selected = uiState.selectedPeriod == period,
                        onClick = { onIntent(SecurityIntent.SelectPeriod(period)) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.background,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    StatMiniColumn(
                        label = "Giao dịch",
                        value = "${uiState.periodStats.transactionCount}",
                        valueColor = MaterialTheme.colorScheme.onSurface,
                    )
                    StatMiniColumn(
                        label = "Tổng thu",
                        value = MoneyFormatter.format(uiState.periodStats.totalIncome, currency),
                        valueColor = finance.income,
                    )
                    StatMiniColumn(
                        label = "Tổng chi",
                        value = MoneyFormatter.format(uiState.periodStats.totalExpense, currency),
                        valueColor = finance.expense,
                    )
                }
            }

            Text(
                text = "Định dạng tệp",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ExportFormat.entries.forEach { format ->
                    SelectablePill(
                        label = format.displayLabel,
                        selected = uiState.selectedFormat == format,
                        onClick = { onIntent(SecurityIntent.SelectFormat(format)) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            if (uiState.periodStats.transactionCount == 0) {
                Text(
                    text = "Kỳ này chưa có giao dịch trong cơ sở dữ liệu — vẫn có thể xuất tệp báo cáo trống để xem mẫu.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = { onIntent(SecurityIntent.RequestSaveExport) },
                    enabled = !uiState.isExporting,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Download,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = stringResource(R.string.security_save_file), maxLines = 1)
                }

                Button(
                    onClick = { onIntent(SecurityIntent.RequestShareExport) },
                    enabled = !uiState.isExporting,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    if (uiState.isExporting) {
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
                        Text(text = stringResource(R.string.security_share_file), maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun SecurityRoadmapCard() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader(title = stringResource(R.string.security_roadmap_section))
        SettingsCard {
            RoadmapFeatureRow(
                icon = Icons.Filled.CloudUpload,
                title = "Sao lưu & Khôi phục mã hóa (.wallet)",
                subtitle = "Feature 29 · AES-256 CBC + SHA-256 checksum",
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
            RoadmapFeatureRow(
                icon = Icons.Filled.Insights,
                title = "Nhật ký kỷ luật tài chính",
                subtitle = "Feature 12 · Financial Score 0–100 & Heatmap Calendar",
            )
        }
    }
}

@Composable
private fun RoadmapFeatureRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconBadge(
            icon = icon,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
        ) {
            Text(
                text = "Sắp có",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

@Composable
private fun SelectableOptionSurface(
    selected: Boolean,
    onClick: () -> Unit,
    cornerRadius: Dp,
    modifier: Modifier = Modifier,
    content: @Composable (contentColor: Color) -> Unit,
) {
    val shape = RoundedCornerShape(cornerRadius)
    val container = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.background
    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant

    Surface(
        shape = shape,
        color = container,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
            .clip(shape)
            .clickable(onClick = onClick),
    ) {
        content(contentColor)
    }
}

@Composable
private fun ThemeOptionChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SelectableOptionSurface(
        selected = selected,
        onClick = onClick,
        cornerRadius = 14.dp,
        modifier = modifier,
    ) { contentColor ->
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(20.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SelectablePill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SelectableOptionSurface(
        selected = selected,
        onClick = onClick,
        cornerRadius = 12.dp,
        modifier = modifier,
    ) { contentColor ->
        Box(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private object CreateExportDocumentContract :
    ActivityResultContract<SecurityEffect.LaunchCreateDocument, Uri?>() {
    override fun createIntent(
        context: Context,
        input: SecurityEffect.LaunchCreateDocument,
    ): Intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        type = input.mimeType
        putExtra(Intent.EXTRA_TITLE, input.suggestedFileName)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? =
        if (resultCode == Activity.RESULT_OK) intent?.data else null
}

@Composable
private fun StatMiniColumn(
    label: String,
    value: String,
    valueColor: Color,
) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = valueColor,
            maxLines = 1,
        )
    }
}

@Composable
private fun IconBadge(
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor),
        contentAlignment = Alignment.Center,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(20.dp))
    }
}

private fun shareExportFile(
    context: Context,
    file: File,
    mimeType: String,
    title: String,
) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, title)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(shareIntent, "Chia sẻ báo cáo"))
}

@Preview(showBackground = true)
@Composable
private fun SecurityScreenPreview() {
    Student_walletTheme {
        SecurityContent(
            uiState = SecurityUiState(
                periodStats = PeriodStats(
                    transactionCount = 12,
                    totalIncome = 5_000_000L,
                    totalExpense = 2_450_000L,
                    netBalance = 2_550_000L,
                ),
            ),
            biometricAvailability = BiometricAvailability.AVAILABLE,
            onIntent = {},
            snackbarHostState = SnackbarHostState(),
        )
    }
}
