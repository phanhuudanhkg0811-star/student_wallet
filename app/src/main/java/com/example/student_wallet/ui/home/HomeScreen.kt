package com.example.student_wallet.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.student_wallet.core.domain.model.CurrencyOption
import com.example.student_wallet.core.ui.components.BalanceCard
import com.example.student_wallet.core.ui.components.BudgetProgressBar
import com.example.student_wallet.core.ui.components.QuickActionTile
import com.example.student_wallet.core.ui.components.SectionHeader
import com.example.student_wallet.core.ui.components.TransactionRow
import com.example.student_wallet.core.ui.theme.Student_walletTheme
import com.example.student_wallet.core.ui.theme.financeColors

private data class SampleTx(
    val emoji: String,
    val title: String,
    val subtitle: String,
    val amount: Long,
    val isIncome: Boolean,
)

@Composable
fun HomeScreen(
    onOpenScanner: () -> Unit,
    onOpenGroup: () -> Unit,
    onOpenBudget: () -> Unit,
    onOpenTransaction: () -> Unit,
    modifier: Modifier = Modifier,
    currency: CurrencyOption = CurrencyOption.VND,
) {
    // TODO: thay dữ liệu mock bên dưới bằng TransactionRepository & BudgetRepository khi team ghép module
    val sampleTransactions = listOf(
        SampleTx("🍜", "Bún bò Huế", "Ăn uống · MoMo · Hôm nay", 45_000L, false),
        SampleTx("🚌", "Vé xe buýt tháng", "Di chuyển · Tiền mặt · Hôm qua", 140_000L, false),
        SampleTx("💸", "Trợ cấp gia đình", "Thu nhập · Chuyển khoản · 01/11", 5_000_000L, true),
        SampleTx("🛒", "WinMart (OCR hóa đơn)", "Đi chợ · Tiền mặt · 29/10", 186_500L, false),
    )

    val finance = MaterialTheme.financeColors

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            TopGreetingBar()
        }

        item {
            BalanceCard(
                balance = 2_450_000L,
                monthlyIncome = 5_000_000L,
                monthlyExpense = 2_550_000L,
                periodLabel = "Số dư hiện tại · Tháng 11",
                currency = currency,
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeader(title = "Lối tắt nhanh")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    QuickActionTile(
                        label = "Quét hóa đơn",
                        icon = Icons.Filled.DocumentScanner,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        onClick = onOpenScanner,
                        modifier = Modifier.weight(1f),
                    )
                    QuickActionTile(
                        label = "Quét QR",
                        icon = Icons.Filled.QrCodeScanner,
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        onClick = onOpenScanner,
                        modifier = Modifier.weight(1f),
                    )
                    QuickActionTile(
                        label = "Chia tiền nhóm",
                        icon = Icons.Filled.Groups,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        onClick = onOpenGroup,
                        modifier = Modifier.weight(1f),
                    )
                    QuickActionTile(
                        label = "Hũ tiết kiệm",
                        icon = Icons.Filled.Savings,
                        containerColor = finance.piggyActionContainer,
                        contentColor = finance.onPiggyActionContainer,
                        onClick = onOpenBudget,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeader(
                    title = "Ngân sách tháng này",
                    actionText = "Xem tất cả",
                    onActionClick = onOpenBudget,
                )
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                    shadowElevation = 2.dp,
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom,
                        ) {
                            Column {
                                Text(
                                    text = "Có thể chi an toàn mỗi ngày",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = "118.000 đ",
                                    fontSize = 21.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(99.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                            ) {
                                Text(
                                    text = "Còn 19 ngày",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                        }

                        BudgetProgressBar(
                            title = "🍜 Ăn uống",
                            spentLabel = "1.100k / 2.000k",
                            spent = 1_100_000L,
                            limit = 2_000_000L,
                        )
                        BudgetProgressBar(
                            title = "🏠 Tiền trọ & điện nước",
                            spentLabel = "1.150k / 1.500k",
                            spent = 1_150_000L,
                            limit = 1_500_000L,
                        )
                        BudgetProgressBar(
                            title = "🎮 Giải trí",
                            spentLabel = "420k / 400k",
                            spent = 420_000L,
                            limit = 400_000L,
                        )
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeader(title = "Mục tiêu tiết kiệm")
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                    shadowElevation = 2.dp,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        GoalProgressRing(percent = 0.68f)
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "💻 Mua laptop mới",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = "6.800.000 / 10.000.000 đ",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = "Dự kiến đạt mục tiêu: 15/02",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeader(
                    title = "Giao dịch gần đây",
                    actionText = "Tìm kiếm",
                    onActionClick = onOpenTransaction,
                )
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                    shadowElevation = 2.dp,
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        sampleTransactions.forEachIndexed { index, tx ->
                            TransactionRow(
                                emoji = tx.emoji,
                                title = tx.title,
                                subtitle = tx.subtitle,
                                amount = tx.amount,
                                isIncome = tx.isIncome,
                                currency = currency,
                            )
                            if (index < sampleTransactions.lastIndex) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopGreetingBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = "Chào buổi tối 👋",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Danh ơi, tháng này ổn!",
                style = MaterialTheme.typography.headlineSmall,
            )
        }
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.tertiary,
                        ),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "D",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
            )
        }
    }
}

@Composable
private fun GoalProgressRing(percent: Float) {
    val primary = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.surfaceVariant
    Box(
        modifier = Modifier.size(62.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round)
            drawArc(
                color = track,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = stroke,
            )
            drawArc(
                color = primary,
                startAngle = -90f,
                sweepAngle = 360f * percent.coerceIn(0f, 1f),
                useCenter = false,
                style = stroke,
            )
        }
        Text(
            text = "${(percent * 100).toInt()}%",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    Student_walletTheme {
        HomeScreen(
            onOpenScanner = {},
            onOpenGroup = {},
            onOpenBudget = {},
            onOpenTransaction = {},
        )
    }
}
