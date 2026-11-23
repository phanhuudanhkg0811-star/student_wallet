package com.example.student_wallet.feature.security

import com.example.student_wallet.core.domain.export.ReportDocument
import com.example.student_wallet.core.domain.export.ReportSummaryRow
import com.example.student_wallet.core.domain.format.MoneyFormatter
import com.example.student_wallet.core.domain.model.CurrencyOption
import com.example.student_wallet.core.domain.model.PaymentMethod
import com.example.student_wallet.core.domain.model.Transaction
import com.example.student_wallet.core.domain.model.TransactionType
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter

enum class ReportPeriod(val label: String) {
    THIS_MONTH("Tháng này"),
    LAST_MONTH("Tháng trước"),
    ALL_TIME("Tất cả"),
}

data class PeriodStats(
    val transactionCount: Int,
    val totalIncome: Long,
    val totalExpense: Long,
    val netBalance: Long,
)

/** Pure Kotlin builder transforming user transactions into a shared [ReportDocument]. */
object PersonalReportBuilder {

    private val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
    private val monthFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MM/yyyy")

    fun filterByPeriod(
        transactions: List<Transaction>,
        period: ReportPeriod,
        now: LocalDate = LocalDate.now(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): List<Transaction> {
        val active = transactions.filter { !it.isDeleted }
        val targetMonth: YearMonth? = when (period) {
            ReportPeriod.THIS_MONTH -> YearMonth.from(now)
            ReportPeriod.LAST_MONTH -> YearMonth.from(now).minusMonths(1)
            ReportPeriod.ALL_TIME -> null
        }
        val filtered = if (targetMonth == null) {
            active
        } else {
            active.filter { tx ->
                val txMonth = Instant.ofEpochMilli(tx.timestampEpochMillis)
                    .atZone(zoneId)
                    .let(YearMonth::from)
                txMonth == targetMonth
            }
        }
        return filtered.sortedByDescending { it.timestampEpochMillis }
    }

    fun summarize(transactions: List<Transaction>): PeriodStats {
        var income = 0L
        var expense = 0L
        transactions.forEach { tx ->
            when (tx.type) {
                TransactionType.INCOME -> income += tx.amount
                TransactionType.EXPENSE -> expense += tx.amount
            }
        }
        return PeriodStats(
            transactionCount = transactions.size,
            totalIncome = income,
            totalExpense = expense,
            netBalance = income - expense,
        )
    }

    fun buildDocument(
        transactions: List<Transaction>,
        period: ReportPeriod,
        currency: CurrencyOption,
        now: LocalDate = LocalDate.now(),
        zoneId: ZoneId = ZoneId.systemDefault(),
        generatedAtMillis: Long = System.currentTimeMillis(),
    ): ReportDocument {
        val filtered = filterByPeriod(transactions, period, now, zoneId)
        val stats = summarize(filtered)
        val periodLabel = periodDisplayLabel(period, now)
        val exportedAtText = Instant.ofEpochMilli(generatedAtMillis)
            .atZone(zoneId)
            .format(dateTimeFormatter)

        val rows = filtered.map { tx ->
            val timeText = Instant.ofEpochMilli(tx.timestampEpochMillis)
                .atZone(zoneId)
                .format(dateTimeFormatter)
            val isIncome = tx.type == TransactionType.INCOME
            listOf(
                timeText,
                if (isIncome) "Thu nhập" else "Chi tiêu",
                tx.categoryName,
                paymentMethodLabel(tx.paymentMethod),
                MoneyFormatter.formatSigned(tx.amount, isIncome = isIncome, currency = currency),
                tx.note.ifBlank { "-" },
            )
        }

        return ReportDocument(
            title = "Báo cáo tài chính cá nhân ($periodLabel)",
            subtitle = "Xuất lúc $exportedAtText · Đơn vị: ${currency.name} (${currency.symbol})",
            headers = listOf("Thời gian", "Loại", "Danh mục", "Phương thức", "Số tiền", "Ghi chú"),
            rows = rows,
            summaryRows = listOf(
                ReportSummaryRow("Số giao dịch", "${stats.transactionCount}"),
                ReportSummaryRow("Tổng thu", MoneyFormatter.format(stats.totalIncome, currency)),
                ReportSummaryRow("Tổng chi", MoneyFormatter.format(stats.totalExpense, currency)),
                ReportSummaryRow("Chênh lệch (Thu - Chi)", MoneyFormatter.format(stats.netBalance, currency)),
            ),
            generatedAtMillis = generatedAtMillis,
        )
    }

    fun suggestedFileNamePrefix(
        period: ReportPeriod,
        now: LocalDate = LocalDate.now(),
    ): String = when (period) {
        ReportPeriod.THIS_MONTH -> {
            val ym = YearMonth.from(now)
            "StudentWallet_${ym.year}_${ym.monthValue.toString().padStart(2, '0')}"
        }
        ReportPeriod.LAST_MONTH -> {
            val ym = YearMonth.from(now).minusMonths(1)
            "StudentWallet_${ym.year}_${ym.monthValue.toString().padStart(2, '0')}"
        }
        ReportPeriod.ALL_TIME -> "StudentWallet_TatCa"
    }

    private fun periodDisplayLabel(period: ReportPeriod, now: LocalDate): String = when (period) {
        ReportPeriod.THIS_MONTH -> "Tháng ${YearMonth.from(now).format(monthFormatter)}"
        ReportPeriod.LAST_MONTH -> "Tháng ${YearMonth.from(now).minusMonths(1).format(monthFormatter)}"
        ReportPeriod.ALL_TIME -> "Tất cả thời gian"
    }

    private fun paymentMethodLabel(method: PaymentMethod): String = when (method) {
        PaymentMethod.CASH -> "Tiền mặt"
        PaymentMethod.BANK_TRANSFER -> "Chuyển khoản"
        PaymentMethod.MOMO -> "MoMo"
        PaymentMethod.OTHER -> "Khác"
    }
}
