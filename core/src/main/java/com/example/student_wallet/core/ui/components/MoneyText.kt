package com.example.student_wallet.core.ui.components

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import com.example.student_wallet.core.domain.format.MoneyFormatter
import com.example.student_wallet.core.domain.model.CurrencyOption
import com.example.student_wallet.core.ui.theme.financeColors

@Composable
fun MoneyText(
    amount: Long,
    modifier: Modifier = Modifier,
    currency: CurrencyOption = CurrencyOption.VND,
    isIncome: Boolean? = null,
    style: TextStyle = LocalTextStyle.current,
    fontWeight: FontWeight = FontWeight.Bold,
) {
    val text = when (isIncome) {
        null -> MoneyFormatter.format(amount, currency)
        true -> MoneyFormatter.formatSigned(amount, isIncome = true, currency = currency)
        false -> MoneyFormatter.formatSigned(amount, isIncome = false, currency = currency)
    }
    val color = when (isIncome) {
        true -> MaterialTheme.financeColors.income
        false -> MaterialTheme.financeColors.expense
        null -> Color.Unspecified
    }
    Text(
        text = text,
        color = color,
        style = style.copy(fontWeight = fontWeight),
        modifier = modifier,
    )
}
