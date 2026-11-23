package com.example.student_wallet.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.student_wallet.core.domain.budget.BudgetLevel
import com.example.student_wallet.core.domain.budget.budgetLevel
import com.example.student_wallet.core.domain.budget.budgetProgress
import com.example.student_wallet.core.ui.theme.Student_walletTheme
import com.example.student_wallet.core.ui.theme.financeColors

@Composable
fun BudgetProgressBar(
    title: String,
    spentLabel: String,
    spent: Long,
    limit: Long,
    modifier: Modifier = Modifier,
) {
    val progress = budgetProgress(spent, limit)
    val level = budgetLevel(spent, limit)
    val finance = MaterialTheme.financeColors

    val targetColor = when (level) {
        BudgetLevel.OK -> finance.budgetOk
        BudgetLevel.WARN -> finance.budgetWarn
        BudgetLevel.BAD -> finance.budgetBad
    }
    val barColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 400),
        label = "budgetBarColor",
    )

    val alpha = if (level == BudgetLevel.BAD) {
        val transition = rememberInfiniteTransition(label = "budgetPulse")
        val animatedAlpha by transition.animateFloat(
            initialValue = 1f,
            targetValue = 0.52f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 700),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "budgetPulseAlpha",
        )
        animatedAlpha
    } else {
        1f
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = spentLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(9.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = progress)
                    .fillMaxHeight()
                    .alpha(alpha)
                    .clip(RoundedCornerShape(99.dp))
                    .background(barColor),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BudgetProgressBarPreview() {
    Student_walletTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BudgetProgressBar("🍜 Ăn uống", "1.100k / 2.000k", 1_100_000L, 2_000_000L)
            BudgetProgressBar("🏠 Tiền trọ & điện nước", "1.150k / 1.500k", 1_150_000L, 1_500_000L)
            BudgetProgressBar("🎮 Giải trí", "420k / 400k", 420_000L, 400_000L)
        }
    }
}
