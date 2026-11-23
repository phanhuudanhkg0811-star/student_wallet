package com.example.student_wallet.feature.security.audit

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.student_wallet.core.domain.format.MoneyFormatter
import com.example.student_wallet.core.domain.model.CurrencyOption
import com.example.student_wallet.core.ui.components.SectionHeader
import com.example.student_wallet.core.ui.theme.Student_walletTheme
import com.example.student_wallet.core.ui.theme.financeColors
import org.koin.compose.viewmodel.koinViewModel
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private val dayAxisLabels = listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")
private val fullDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

@Composable
fun AuditJournalScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AuditJournalViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AuditJournalContent(
        uiState = uiState,
        onBack = onBack,
        onPreviousMonth = viewModel::previousMonth,
        onNextMonth = viewModel::nextMonth,
        onToggleSampleData = viewModel::toggleSampleData,
        onSelectCell = viewModel::selectCell,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AuditJournalContent(
    uiState: AuditJournalUiState,
    onBack: () -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onToggleSampleData: (Boolean) -> Unit,
    onSelectCell: (DailyAuditCell) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Nhật ký kỷ luật tài chính", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại",
                        )
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                MonthSelectorCard(
                    month = uiState.selectedMonth,
                    canGoNext = uiState.canGoNextMonth,
                    useSampleData = uiState.useSampleData,
                    onPreviousMonth = onPreviousMonth,
                    onNextMonth = onNextMonth,
                    onToggleSampleData = onToggleSampleData,
                )
            }

            item {
                FinancialScoreCard(summary = uiState.summary)
            }

            item {
                HeatmapCalendarCard(
                    summary = uiState.summary,
                    currency = uiState.currency,
                    selectedCell = uiState.selectedCell,
                    onSelectCell = onSelectCell,
                )
            }

            item {
                MonthlyMetricsCard(
                    summary = uiState.summary,
                    currency = uiState.currency,
                )
            }
        }
    }
}

@Composable
private fun MonthSelectorCard(
    month: YearMonth,
    canGoNext: Boolean,
    useSampleData: Boolean,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onToggleSampleData: (Boolean) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shadowElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onPreviousMonth) {
                    Icon(imageVector = Icons.Filled.ChevronLeft, contentDescription = "Tháng trước")
                }
                Text(
                    text = "Tháng ${month.monthValue.toString().padStart(2, '0')}/${month.year}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                IconButton(onClick = onNextMonth, enabled = canGoNext) {
                    Icon(imageVector = Icons.Filled.ChevronRight, contentDescription = "Tháng sau")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Xem dữ liệu mẫu minh họa",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Switch(checked = useSampleData, onCheckedChange = onToggleSampleData)
            }
        }
    }
}

@Composable
private fun FinancialScoreCard(summary: MonthlyAuditSummary) {
    val finance = MaterialTheme.financeColors
    val scoreColor = when (summary.grade) {
        AuditGrade.EXCELLENT, AuditGrade.GOOD -> finance.budgetOk
        AuditGrade.FAIR -> finance.budgetWarn
        AuditGrade.NEEDS_IMPROVEMENT -> finance.budgetBad
        AuditGrade.NO_DATA -> MaterialTheme.colorScheme.outline
    }
    val animatedProgress by animateFloatAsState(
        targetValue = (summary.score / 100f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 600),
        label = "financialScoreSweep",
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader(title = "Điểm kỷ luật tài chính")
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            shadowElevation = 1.dp,
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val trackColor = MaterialTheme.colorScheme.surfaceVariant
                    Box(
                        modifier = Modifier.size(92.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val stroke = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                            drawArc(
                                color = trackColor,
                                startAngle = 135f,
                                sweepAngle = 270f,
                                useCenter = false,
                                style = stroke,
                            )
                            drawArc(
                                color = scoreColor,
                                startAngle = 135f,
                                sweepAngle = 270f * animatedProgress,
                                useCenter = false,
                                style = stroke,
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${summary.score}",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = scoreColor,
                            )
                            Text(
                                text = "/ 100",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = scoreColor.copy(alpha = 0.14f),
                        ) {
                            Text(
                                text = summary.grade.label,
                                style = MaterialTheme.typography.labelMedium,
                                color = scoreColor,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            )
                        }
                        Text(
                            text = summary.insightText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    BreakdownRow("Cân đối Thu - Chi", summary.breakdown.savingsScore, 40, scoreColor)
                    BreakdownRow("Ghi chép đều đặn", summary.breakdown.consistencyScore, 30, scoreColor)
                    BreakdownRow("Chi tiêu ổn định", summary.breakdown.stabilityScore, 20, scoreColor)
                    BreakdownRow("Chuỗi ngày liên tiếp", summary.breakdown.streakScore, 10, scoreColor)
                }
            }
        }
    }
}

@Composable
private fun BreakdownRow(
    label: String,
    points: Int,
    maxPoints: Int,
    barColor: Color,
) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "$points / $maxPoints đ",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LinearProgressIndicator(
            progress = { (points.toFloat() / maxPoints.toFloat()).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(99.dp)),
            color = barColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}

@Composable
private fun HeatmapCalendarCard(
    summary: MonthlyAuditSummary,
    currency: CurrencyOption,
    selectedCell: DailyAuditCell?,
    onSelectCell: (DailyAuditCell) -> Unit,
) {
    val darkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader(title = "Heatmap kỷ luật chi tiêu (12 tuần)")
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            shadowElevation = 1.dp,
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Left weekday axis (T2..CN)
                    Column(
                        modifier = Modifier.width(22.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        dayAxisLabels.forEach { dayLabel ->
                            Box(
                                modifier = Modifier.height(18.dp),
                                contentAlignment = Alignment.CenterStart,
                            ) {
                                Text(
                                    text = dayLabel,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    summary.heatmapWeeks.forEach { weekColumn ->
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            weekColumn.forEach { cell ->
                                if (cell == null) {
                                    Spacer(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(1f),
                                    )
                                } else {
                                    val isSelected = selectedCell?.date == cell.date
                                    val fillColor = heatCellColor(cell.level, darkTheme)
                                    val border = if (isSelected) {
                                        BorderStroke(1.5.dp, MaterialTheme.colorScheme.onSurface)
                                    } else {
                                        null
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = fillColor,
                                        border = border,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(1f)
                                            .alpha(if (cell.isInTargetMonth) 1f else 0.45f)
                                            .clip(RoundedCornerShape(4.dp))
                                            .clickable { onSelectCell(cell) },
                                    ) {}
                                }
                            }
                        }
                    }
                }

                // Legend
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    HeatLevel.entries.forEach { level ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(heatCellColor(level, darkTheme)),
                            )
                            Text(
                                text = level.label,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                if (selectedCell != null) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.background,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(14.dp),
                            ),
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = "Ngày ${selectedCell.date.format(fullDateFormatter)} · ${selectedCell.level.label}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = "Chi: ${MoneyFormatter.format(selectedCell.expenseAmount, currency)} · Thu: ${MoneyFormatter.format(selectedCell.incomeAmount, currency)} (${selectedCell.transactionCount} GD)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                } else {
                    Text(
                        text = "Chạm vào bất kỳ ô ngày nào trên biểu đồ để xem chi tiết Thu/Chi.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthlyMetricsCard(
    summary: MonthlyAuditSummary,
    currency: CurrencyOption,
) {
    val finance = MaterialTheme.financeColors

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader(title = "Thống kê kỷ luật trong tháng")
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            shadowElevation = 1.dp,
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    MetricBox(
                        label = "Tổng thu",
                        value = MoneyFormatter.format(summary.totalIncome, currency),
                        valueColor = finance.income,
                        modifier = Modifier.weight(1f),
                    )
                    MetricBox(
                        label = "Tổng chi",
                        value = MoneyFormatter.format(summary.totalExpense, currency),
                        valueColor = finance.expense,
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    MetricBox(
                        label = "Ngày không chi (No-Spend)",
                        value = "${summary.noSpendDaysCount} ngày",
                        valueColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                    )
                    MetricBox(
                        label = "Chuỗi ghi chép dài nhất",
                        value = "${summary.longestLoggingStreak} ngày",
                        valueColor = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    MetricBox(
                        label = "Tỷ lệ tiết kiệm",
                        value = "${summary.savingsRatePercent}%",
                        valueColor = if (summary.savingsRatePercent >= 20) finance.income else finance.budgetWarn,
                        modifier = Modifier.weight(1f),
                    )
                    MetricBox(
                        label = "TB ngày có chi",
                        value = MoneyFormatter.format(summary.averageActiveDayExpense, currency),
                        valueColor = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricBox(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.background,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = valueColor,
            )
        }
    }
}

@Composable
private fun heatCellColor(level: HeatLevel, darkTheme: Boolean): Color = when (level) {
    HeatLevel.NONE -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    HeatLevel.LOW -> if (darkTheme) Color(0xFF145340) else Color(0xFF9BF2D2)
    HeatLevel.MEDIUM -> if (darkTheme) Color(0xFF0E805E) else Color(0xFF28B886)
    HeatLevel.HIGH -> MaterialTheme.financeColors.budgetWarn
    HeatLevel.PEAK -> MaterialTheme.financeColors.budgetBad
}

@Preview(showBackground = true)
@Composable
private fun AuditJournalPreview() {
    val month = YearMonth.of(2026, 11)
    val sample = AuditJournalAnalyzer.sampleTransactionsForMonth(month)
    Student_walletTheme {
        AuditJournalContent(
            uiState = AuditJournalUiState(
                selectedMonth = month,
                useSampleData = true,
                summary = AuditJournalAnalyzer.analyzeMonth(sample, month),
            ),
            onBack = {},
            onPreviousMonth = {},
            onNextMonth = {},
            onToggleSampleData = {},
            onSelectCell = {},
        )
    }
}
