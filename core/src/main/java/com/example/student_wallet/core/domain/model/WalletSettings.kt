package com.example.student_wallet.core.domain.model

enum class TransactionType {
    EXPENSE,
    INCOME,
}

enum class PaymentMethod {
    CASH,
    BANK_TRANSFER,
    MOMO,
    OTHER,
}

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

data class UserSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val currency: CurrencyOption = CurrencyOption.VND,
    val dynamicColor: Boolean = false,
)
