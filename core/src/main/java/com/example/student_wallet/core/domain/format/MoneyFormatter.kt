package com.example.student_wallet.core.domain.format

import com.example.student_wallet.core.domain.model.CurrencyOption
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs

object MoneyFormatter {

    fun format(amount: Long, currency: CurrencyOption = CurrencyOption.VND): String {
        val sign = if (amount < 0L) "−" else ""
        val absValue = abs(amount)
        return when (currency) {
            CurrencyOption.VND -> {
                val symbols = DecimalFormatSymbols(Locale.forLanguageTag("vi-VN")).apply {
                    groupingSeparator = '.'
                }
                val formatted = DecimalFormat("#,###", symbols).format(absValue)
                "$sign$formatted ${currency.symbol}"
            }
            CurrencyOption.USD, CurrencyOption.EUR -> {
                val symbols = DecimalFormatSymbols(Locale.US).apply {
                    groupingSeparator = ','
                }
                val formatted = DecimalFormat("#,###", symbols).format(absValue)
                "$sign${currency.symbol}$formatted"
            }
        }
    }

    fun formatSigned(
        amount: Long,
        isIncome: Boolean,
        currency: CurrencyOption = CurrencyOption.VND,
    ): String {
        val prefix = if (isIncome) "+" else "−"
        return "$prefix${format(abs(amount), currency)}"
    }
}
