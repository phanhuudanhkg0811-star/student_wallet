package com.example.student_wallet.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class FinanceColors(
    val income: Color,
    val expense: Color,
    val budgetOk: Color,
    val budgetWarn: Color,
    val budgetBad: Color,
    val balanceGradientStart: Color,
    val balanceGradientMid: Color,
    val balanceGradientEnd: Color,
    val piggyActionContainer: Color,
    val onPiggyActionContainer: Color,
)

val LightFinanceColors = FinanceColors(
    income = Color(0xFF00855F),
    expense = Color(0xFFBA1A1A),
    budgetOk = Color(0xFF1FA971),
    budgetWarn = Color(0xFFE08A00),
    budgetBad = Color(0xFFBA1A1A),
    balanceGradientStart = Color(0xFF006C51),
    balanceGradientMid = Color(0xFF0A8F6B),
    balanceGradientEnd = Color(0xFF406376),
    piggyActionContainer = Color(0xFFFFE2B8),
    onPiggyActionContainer = Color(0xFF8A5300),
)

val DarkFinanceColors = FinanceColors(
    income = Color(0xFF5EDDB1),
    expense = Color(0xFFFFB4AB),
    budgetOk = Color(0xFF5EDDB1),
    budgetWarn = Color(0xFFFFB547),
    budgetBad = Color(0xFFFFB4AB),
    balanceGradientStart = Color(0xFF00513C),
    balanceGradientMid = Color(0xFF0B6E54),
    balanceGradientEnd = Color(0xFF274B5D),
    piggyActionContainer = Color(0xFF4A3410),
    onPiggyActionContainer = Color(0xFFFFB547),
)

val LocalFinanceColors = staticCompositionLocalOf { LightFinanceColors }

val MaterialTheme.financeColors: FinanceColors
    @Composable
    @ReadOnlyComposable
    get() = LocalFinanceColors.current
