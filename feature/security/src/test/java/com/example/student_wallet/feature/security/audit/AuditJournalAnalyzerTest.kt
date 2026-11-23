package com.example.student_wallet.feature.security.audit

import com.example.student_wallet.core.domain.model.PaymentMethod
import com.example.student_wallet.core.domain.model.Transaction
import com.example.student_wallet.core.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class AuditJournalAnalyzerTest {

    private val zone: ZoneId = ZoneId.of("Asia/Ho_Chi_Minh")
    private val month: YearMonth = YearMonth.of(2026, 11)
    private val today: LocalDate = LocalDate.of(2026, 11, 20)

    private fun tx(day: Int, amount: Long, type: TransactionType, deleted: Boolean = false): Transaction {
        val epoch = month.atDay(day).atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
        return Transaction(
            id = day.toLong(),
            amount = amount,
            type = type,
            categoryId = 1L,
            categoryName = "Ăn uống",
            categoryEmoji = "🍜",
            note = "",
            paymentMethod = PaymentMethod.CASH,
            timestampEpochMillis = epoch,
            isDeleted = deleted,
        )
    }

    @Test
    fun `empty month returns NO_DATA with zero score and full 12-week grid`() {
        val summary = AuditJournalAnalyzer.analyzeMonth(
            transactions = listOf(tx(5, 100_000L, TransactionType.EXPENSE, deleted = true)),
            targetMonth = month,
            today = today,
            zoneId = zone,
        )

        assertFalse(summary.hasData)
        assertEquals(0, summary.score)
        assertEquals(AuditGrade.NO_DATA, summary.grade)
        assertEquals(12, summary.heatmapWeeks.size)
        val novemberDays = summary.heatmapWeeks.flatten().filterNotNull().count { it.isInTargetMonth }
        assertEquals(30, novemberDays)
    }

    @Test
    fun `disciplined high-savings month scores higher than overspent spike month`() {
        val disciplined = mutableListOf(tx(1, 5_000_000L, TransactionType.INCOME))
        (1..15).forEach { day ->
            disciplined += tx(day, 60_000L, TransactionType.EXPENSE)
        }

        val disciplinedSummary = AuditJournalAnalyzer.analyzeMonth(disciplined, month, today, zone)
        assertTrue(disciplinedSummary.hasData)
        assertTrue("Expected >= 80 but got ${disciplinedSummary.score}", disciplinedSummary.score >= 80)
        assertEquals(15, disciplinedSummary.longestLoggingStreak)

        val overspent = listOf(
            tx(1, 1_000_000L, TransactionType.INCOME),
            tx(2, 50_000L, TransactionType.EXPENSE),
            tx(5, 60_000L, TransactionType.EXPENSE),
            tx(10, 1_500_000L, TransactionType.EXPENSE),
        )
        val overspentSummary = AuditJournalAnalyzer.analyzeMonth(overspent, month, today, zone)
        assertTrue(overspentSummary.score < disciplinedSummary.score)
        assertTrue(overspentSummary.totalExpense > overspentSummary.totalIncome)
    }

    @Test
    fun `heatLevelFor distinguishes none, controlled, and spike spending`() {
        assertEquals(HeatLevel.NONE, AuditJournalAnalyzer.heatLevelFor(0L, 100_000L))
        assertEquals(HeatLevel.LOW, AuditJournalAnalyzer.heatLevelFor(40_000L, 100_000L))
        assertEquals(HeatLevel.MEDIUM, AuditJournalAnalyzer.heatLevelFor(100_000L, 100_000L))
        assertEquals(HeatLevel.HIGH, AuditJournalAnalyzer.heatLevelFor(150_000L, 100_000L))
        assertEquals(HeatLevel.PEAK, AuditJournalAnalyzer.heatLevelFor(220_000L, 100_000L))
    }
}
