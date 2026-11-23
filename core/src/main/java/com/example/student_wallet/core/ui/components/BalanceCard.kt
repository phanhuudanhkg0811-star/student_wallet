package com.example.student_wallet.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.student_wallet.core.domain.format.MoneyFormatter
import com.example.student_wallet.core.domain.model.CurrencyOption
import com.example.student_wallet.core.ui.theme.Student_walletTheme
import com.example.student_wallet.core.ui.theme.financeColors

@Composable
fun BalanceCard(
    balance: Long,
    monthlyIncome: Long,
    monthlyExpense: Long,
    periodLabel: String,
    modifier: Modifier = Modifier,
    currency: CurrencyOption = CurrencyOption.VND,
) {
    val finance = MaterialTheme.financeColors
    val gradient = Brush.linearGradient(
        colors = listOf(
            finance.balanceGradientStart,
            finance.balanceGradientMid,
            finance.balanceGradientEnd,
        ),
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(gradient),
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(
                color = Color.White.copy(alpha = 0.10f),
                radius = 85.dp.toPx(),
                center = Offset(size.width - 20.dp.toPx(), 10.dp.toPx()),
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.07f),
                radius = 70.dp.toPx(),
                center = Offset(size.width - 80.dp.toPx(), size.height + 10.dp.toPx()),
            )
        }

        Column(modifier = Modifier.padding(horizontal = 22.dp, vertical = 20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.AccountBalanceWallet,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.88f),
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = periodLabel,
                    color = Color.White.copy(alpha = 0.88f),
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = MoneyFormatter.format(balance, currency),
                color = Color.White,
                style = MaterialTheme.typography.displaySmall,
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                BalanceStatPill(
                    label = "↓ Thu nhập",
                    value = MoneyFormatter.format(monthlyIncome, currency),
                    modifier = Modifier.weight(1f),
                )
                BalanceStatPill(
                    label = "↑ Chi tiêu",
                    value = MoneyFormatter.format(monthlyExpense, currency),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun BalanceStatPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.16f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 11.sp,
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BalanceCardPreview() {
    Student_walletTheme {
        BalanceCard(
            balance = 2_450_000L,
            monthlyIncome = 5_000_000L,
            monthlyExpense = 2_550_000L,
            periodLabel = "Số dư hiện tại · Tháng 11",
            modifier = Modifier.padding(16.dp),
        )
    }
}
