package com.example.student_wallet.feature.security

import com.example.student_wallet.core.domain.model.CurrencyOption
import com.example.student_wallet.core.domain.model.PaymentMethod
import com.example.student_wallet.core.domain.model.Transaction
import com.example.student_wallet.core.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class PersonalReportBuilderTest {

    private val zone: ZoneId = ZoneId.of("Asia/Ho_Chi_Minh")
    private val now: LocalDate = LocalDate.of(2026, 11, 23)

    private fun txAt(
        date: LocalDate,
        amount: Long,
        type: TransactionType,
        note: String = "",
        isDeleted: Boolean = false,
    ): Transaction {
        val epoch = date.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
        return Transaction(
            id = 1L,
            amount = amount,
            type = type,
            categoryId = 1L,
            categoryName = "Ăn uống",
            categoryEmoji = "🍜",
            note = note,
            paymentMethod = PaymentMethod.MOMO,
            timestampEpochMillis = epoch,
            isDeleted = isDeleted,
        )
    }

    @Test
    fun `filterByPeriod selects matching month and excludes soft-deleted items`() {
        val txs = listOf(
            txAt(LocalDate.of(2026, 11, 20), 45_000L, TransactionType.EXPENSE),
            txAt(LocalDate.of(2026, 11, 10), 5_000_000L, TransactionType.INCOME),
            txAt(LocalDate.of(2026, 11, 5), 99_000L, TransactionType.EXPENSE, isDeleted = true),
            txAt(LocalDate.of(2026, 10, 15), 200_000L, TransactionType.EXPENSE),
        )

        val thisMonth = PersonalReportBuilder.filterByPeriod(txs, ReportPeriod.THIS_MONTH, now, zone)
        assertEquals(2, thisMonth.size)

        val lastMonth = PersonalReportBuilder.filterByPeriod(txs, ReportPeriod.LAST_MONTH, now, zone)
        assertEquals(1, lastMonth.size)

        val allTime = PersonalReportBuilder.filterByPeriod(txs, ReportPeriod.ALL_TIME, now, zone)
        assertEquals(3, allTime.size)
    }

    @Test
    fun `buildDocument computes totals and formats rows accurately`() {
        val txs = listOf(
            txAt(LocalDate.of(2026, 11, 1), 5_000_000L, TransactionType.INCOME, note = "Trợ cấp"),
            txAt(LocalDate.of(2026, 11, 20), 45_000L, TransactionType.EXPENSE, note = "Bún bò"),
        )

        val doc = PersonalReportBuilder.buildDocument(
            transactions = txs,
            period = ReportPeriod.THIS_MONTH,
            currency = CurrencyOption.VND,
            now = now,
            zoneId = zone,
        )

        assertEquals(2, doc.rows.size)
        assertTrue(doc.title.contains("11/2026"))
        assertEquals("5.000.000 đ", doc.summaryRows.first { it.label == "Tổng thu" }.value)
        assertEquals("45.000 đ", doc.summaryRows.first { it.label == "Tổng chi" }.value)
        assertEquals("4.955.000 đ", doc.summaryRows.first { it.label == "Chênh lệch (Thu - Chi)" }.value)
    }
}
