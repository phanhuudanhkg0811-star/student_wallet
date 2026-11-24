package com.example.student_wallet.feature.security.widget

import com.example.student_wallet.core.domain.budget.BudgetLevel
import com.example.student_wallet.core.domain.model.CurrencyOption
import com.example.student_wallet.core.domain.model.PaymentMethod
import com.example.student_wallet.core.domain.model.Transaction
import com.example.student_wallet.core.domain.model.TransactionType
import com.example.student_wallet.core.domain.model.UserSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class WidgetSummaryCalculatorTest {

    private val zoneId = ZoneId.of("Asia/Ho_Chi_Minh")
    private val today = LocalDate.of(2026, 11, 24)

    @Test
    fun `empty transactions produce zero metrics and reference budget limit`() {
        val summary = WidgetSummaryCalculator.compute(
            transactions = emptyList(),
            settings = UserSettings(currency = CurrencyOption.VND),
            today = today,
            zoneId = zoneId,
        )

        assertEquals(0L, summary.currentBalance)
        assertEquals(0L, summary.todayExpense)
        assertEquals(0L, summary.monthExpense)
        assertEquals(WidgetSummaryCalculator.DEFAULT_MONTHLY_REFERENCE_LIMIT, summary.budgetLimit)
        assertTrue(summary.isReferenceBudget)
        assertEquals(BudgetLevel.OK, summary.budgetLevel)
    }

    @Test
    fun `compute filters deleted rows and separates today vs month vs balance and levels`() {
        val txs = listOf(
            tx(id = 1L, amount = 4_000_000L, type = TransactionType.INCOME, date = LocalDate.of(2026, 11, 1)),
            tx(id = 2L, amount = 1_500_000L, type = TransactionType.EXPENSE, date = LocalDate.of(2026, 11, 10)),
            tx(id = 3L, amount = 120_000L, type = TransactionType.EXPENSE, date = today),
            tx(id = 4L, amount = 300_000L, type = TransactionType.INCOME, date = today),
            tx(id = 5L, amount = 900_000L, type = TransactionType.EXPENSE, date = today, isDeleted = true),
            tx(id = 6L, amount = 500_000L, type = TransactionType.EXPENSE, date = LocalDate.of(2026, 10, 20)),
        )

        val okSummary = WidgetSummaryCalculator.compute(
            transactions = txs,
            settings = UserSettings(currency = CurrencyOption.VND),
            today = today,
            zoneId = zoneId,
        )

        // All active income = 4.3M; active expense = 1.5M + 120k + 500k = 2.12M -> balance = 2.18M
        assertEquals(2_180_000L, okSummary.currentBalance)
        assertEquals(120_000L, okSummary.todayExpense)
        assertEquals(4_300_000L, okSummary.monthIncome)
        assertEquals(1_620_000L, okSummary.monthExpense)
        assertEquals(BudgetLevel.OK, okSummary.budgetLevel)

        val warnSummary = WidgetSummaryCalculator.compute(
            transactions = txs,
            settings = UserSettings(),
            monthlyBudgetOverride = 2_000_000L,
            today = today,
            zoneId = zoneId,
        )
        assertFalse(warnSummary.isReferenceBudget)
        assertEquals(81, warnSummary.budgetPercent)
        assertEquals(BudgetLevel.WARN, warnSummary.budgetLevel)

        val badSummary = WidgetSummaryCalculator.compute(
            transactions = txs,
            settings = UserSettings(),
            monthlyBudgetOverride = 1_800_000L,
            today = today,
            zoneId = zoneId,
        )
        assertEquals(90, badSummary.budgetPercent)
        assertEquals(BudgetLevel.BAD, badSummary.budgetLevel)
    }

    private fun tx(
        id: Long,
        amount: Long,
        type: TransactionType,
        date: LocalDate,
        isDeleted: Boolean = false,
    ): Transaction = Transaction(
        id = id,
        amount = amount,
        type = type,
        categoryId = 1L,
        categoryName = "Ăn uống",
        categoryEmoji = "🍜",
        note = "Test",
        paymentMethod = PaymentMethod.CASH,
        timestampEpochMillis = date.atTime(10, 0).atZone(zoneId).toInstant().toEpochMilli(),
        isDeleted = isDeleted,
    )
}
