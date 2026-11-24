package com.example.student_wallet.feature.security.widget

import com.example.student_wallet.core.domain.budget.BudgetLevel
import com.example.student_wallet.core.domain.budget.budgetLevel
import com.example.student_wallet.core.domain.budget.budgetProgress
import com.example.student_wallet.core.domain.model.CurrencyOption
import com.example.student_wallet.core.domain.model.Transaction
import com.example.student_wallet.core.domain.model.TransactionType
import com.example.student_wallet.core.domain.model.UserSettings
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import kotlin.math.roundToInt

data class WidgetSummary(
    val currentBalance: Long,
    val todayExpense: Long,
    val monthIncome: Long,
    val monthExpense: Long,
    val budgetLimit: Long,
    val isReferenceBudget: Boolean,
    val budgetProgressFraction: Float,
    val budgetPercent: Int,
    val budgetLevel: BudgetLevel,
    val currency: CurrencyOption,
)

/** Pure Kotlin calculator for the Home Screen Glance Widget metrics and budget alert tier. */
object WidgetSummaryCalculator {

    const val DEFAULT_MONTHLY_REFERENCE_LIMIT = 2_000_000L

    fun compute(
        transactions: List<Transaction>,
        settings: UserSettings,
        monthlyBudgetOverride: Long? = null,
        today: LocalDate = LocalDate.now(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): WidgetSummary {
        val active = transactions.filter { !it.isDeleted }
        val targetMonth = YearMonth.from(today)

        var totalIncome = 0L
        var totalExpense = 0L
        var todayExpense = 0L
        var monthIncome = 0L
        var monthExpense = 0L

        for (tx in active) {
            val txDate = Instant.ofEpochMilli(tx.timestampEpochMillis).atZone(zoneId).toLocalDate()
            val txMonth = YearMonth.from(txDate)
            when (tx.type) {
                TransactionType.INCOME -> {
                    totalIncome += tx.amount
                    if (txMonth == targetMonth) {
                        monthIncome += tx.amount
                    }
                }
                TransactionType.EXPENSE -> {
                    totalExpense += tx.amount
                    if (txDate == today) {
                        todayExpense += tx.amount
                    }
                    if (txMonth == targetMonth) {
                        monthExpense += tx.amount
                    }
                }
            }
        }

        val (effectiveLimit, isReference) = when {
            monthlyBudgetOverride != null && monthlyBudgetOverride > 0L -> monthlyBudgetOverride to false
            monthIncome > 0L -> monthIncome to true
            else -> DEFAULT_MONTHLY_REFERENCE_LIMIT to true
        }

        val progress = budgetProgress(monthExpense, effectiveLimit)
        val level = budgetLevel(monthExpense, effectiveLimit)
        val percent = ((monthExpense.toDouble() / effectiveLimit.toDouble()) * 100.0)
            .roundToInt()
            .coerceAtLeast(0)

        return WidgetSummary(
            currentBalance = totalIncome - totalExpense,
            todayExpense = todayExpense,
            monthIncome = monthIncome,
            monthExpense = monthExpense,
            budgetLimit = effectiveLimit,
            isReferenceBudget = isReference,
            budgetProgressFraction = progress,
            budgetPercent = percent,
            budgetLevel = level,
            currency = settings.currency,
        )
    }

    fun budgetStatusLabel(level: BudgetLevel): String = when (level) {
        BudgetLevel.OK -> "Ngân sách ổn"
        BudgetLevel.WARN -> "Sắp chạm hạn mức"
        BudgetLevel.BAD -> "Vượt cảnh báo"
    }
}
