package com.example.student_wallet.feature.security.audit

import com.example.student_wallet.core.domain.model.PaymentMethod
import com.example.student_wallet.core.domain.model.Transaction
import com.example.student_wallet.core.domain.model.TransactionType
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Pure Kotlin analyzer computing the monthly Financial Discipline Score (`0..100`) and a
 * 12-week GitHub-style Heatmap Calendar grid ending at the selected [YearMonth].
 */
object AuditJournalAnalyzer {

    private const val MIN_BASELINE_DAILY_EXPENSE = 50_000L
    private const val HEATMAP_WEEK_COLUMNS = 12

    fun analyzeMonth(
        transactions: List<Transaction>,
        targetMonth: YearMonth,
        today: LocalDate = LocalDate.now(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): MonthlyAuditSummary {
        val activeTransactions = transactions.filter { !it.isDeleted }
        val groupedByDate = activeTransactions.groupBy { tx ->
            Instant.ofEpochMilli(tx.timestampEpochMillis).atZone(zoneId).toLocalDate()
        }

        val monthStart = targetMonth.atDay(1)
        val monthEnd = targetMonth.atEndOfMonth()
        val monthDates = groupedByDate.keys.filter { !it.isBefore(monthStart) && !it.isAfter(monthEnd) }
        val monthTransactions = monthDates.flatMap { groupedByDate[it].orEmpty() }

        val totalIncome = monthTransactions
            .filter { it.type == TransactionType.INCOME }
            .sumOf { it.amount }
        val totalExpense = monthTransactions
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.amount }

        val dailyExpenseByDate = groupedByDate.mapValues { (_, dayTxs) ->
            dayTxs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        }
        val dailyIncomeByDate = groupedByDate.mapValues { (_, dayTxs) ->
            dayTxs.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        }

        val monthSpendDays = (1..targetMonth.lengthOfMonth())
            .map { targetMonth.atDay(it) }
            .mapNotNull { date -> dailyExpenseByDate[date]?.takeIf { it > 0L } }

        val avgActiveExpense = if (monthSpendDays.isEmpty()) {
            0L
        } else {
            monthSpendDays.sum() / monthSpendDays.size
        }
        val baseline = max(avgActiveExpense, MIN_BASELINE_DAILY_EXPENSE)

        val heatmapWeeks = buildHeatmapWeeks(
            targetMonth = targetMonth,
            dailyExpenseByDate = dailyExpenseByDate,
            dailyIncomeByDate = dailyIncomeByDate,
            groupedByDate = groupedByDate,
            baseline = baseline,
        )

        if (monthTransactions.isEmpty()) {
            return MonthlyAuditSummary(
                month = targetMonth,
                hasData = false,
                score = 0,
                grade = AuditGrade.NO_DATA,
                breakdown = ScoreBreakdown(0, 0, 0, 0),
                totalIncome = 0L,
                totalExpense = 0L,
                savingsRatePercent = 0,
                activeDaysCount = 0,
                noSpendDaysCount = 0,
                longestLoggingStreak = 0,
                averageActiveDayExpense = 0L,
                insightText = "Chưa có giao dịch trong tháng ${targetMonth.monthValue}/${targetMonth.year}. Ghi chép giao dịch hoặc bật dữ liệu mẫu để xem Heatmap.",
                heatmapWeeks = heatmapWeeks,
            )
        }

        val currentMonth = YearMonth.from(today)
        val maxTxDay = monthDates.maxOfOrNull { it.dayOfMonth } ?: 1
        val effectiveDays = when {
            targetMonth.isBefore(currentMonth) -> targetMonth.lengthOfMonth()
            targetMonth == currentMonth -> max(today.dayOfMonth, maxTxDay)
            else -> maxTxDay
        }.coerceIn(1, targetMonth.lengthOfMonth())

        val noSpendDays = (1..effectiveDays).count { day ->
            (dailyExpenseByDate[targetMonth.atDay(day)] ?: 0L) == 0L
        }
        val longestStreak = computeLongestStreak(targetMonth, effectiveDays, groupedByDate.keys)
        val highCount = (1..effectiveDays).count { day ->
            heatLevelFor(dailyExpenseByDate[targetMonth.atDay(day)] ?: 0L, baseline) == HeatLevel.HIGH
        }
        val peakCount = (1..effectiveDays).count { day ->
            heatLevelFor(dailyExpenseByDate[targetMonth.atDay(day)] ?: 0L, baseline) == HeatLevel.PEAK
        }

        val savingsRate = if (totalIncome > 0L) {
            (((totalIncome - totalExpense).toDouble() / totalIncome.toDouble()) * 100.0).roundToInt()
        } else {
            0
        }

        val breakdown = computeScoreBreakdown(
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            activeDays = monthDates.size,
            longestStreak = longestStreak,
            highCount = highCount,
            peakCount = peakCount,
        )
        val score = breakdown.total
        val grade = gradeForScore(score)
        val insight = buildInsight(
            grade = grade,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            savingsRate = savingsRate,
            peakCount = peakCount,
            noSpendDays = noSpendDays,
        )

        return MonthlyAuditSummary(
            month = targetMonth,
            hasData = true,
            score = score,
            grade = grade,
            breakdown = breakdown,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            savingsRatePercent = savingsRate,
            activeDaysCount = monthDates.size,
            noSpendDaysCount = noSpendDays,
            longestLoggingStreak = longestStreak,
            averageActiveDayExpense = avgActiveExpense,
            insightText = insight,
            heatmapWeeks = heatmapWeeks,
        )
    }

    internal fun heatLevelFor(expenseAmount: Long, baseline: Long): HeatLevel {
        if (expenseAmount <= 0L) return HeatLevel.NONE
        val safeBaseline = max(baseline, MIN_BASELINE_DAILY_EXPENSE).toDouble()
        val ratio = expenseAmount.toDouble() / safeBaseline
        return when {
            ratio <= 0.6 -> HeatLevel.LOW
            ratio <= 1.15 -> HeatLevel.MEDIUM
            ratio <= 1.8 -> HeatLevel.HIGH
            else -> HeatLevel.PEAK
        }
    }

    internal fun sampleTransactionsForMonth(
        month: YearMonth,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): List<Transaction> {
        val entries = mutableListOf<Transaction>()
        var nextId = 10_000L

        fun addSample(dayOffset: Int, amount: Long, type: TransactionType, category: String, emoji: String, note: String) {
            val targetDate = month.atDay(1).plusDays(dayOffset.toLong())
            if (YearMonth.from(targetDate) != month) return
            val epoch = targetDate.atTime(12, 30).atZone(zoneId).toInstant().toEpochMilli()
            entries += Transaction(
                id = nextId++,
                amount = amount,
                type = type,
                categoryId = 1L,
                categoryName = category,
                categoryEmoji = emoji,
                note = note,
                paymentMethod = PaymentMethod.MOMO,
                timestampEpochMillis = epoch,
            )
        }

        // Pull in a couple of prior-month transactions so the 12-week heatmap looks alive.
        val prevMonth = month.minusMonths(1)
        listOf(10, 16, 22, 27).forEachIndexed { idx, day ->
            val date = prevMonth.atDay(day.coerceAtMost(prevMonth.lengthOfMonth()))
            val epoch = date.atTime(18, 0).atZone(zoneId).toInstant().toEpochMilli()
            entries += Transaction(
                id = nextId++,
                amount = 45_000L + idx * 35_000L,
                type = TransactionType.EXPENSE,
                categoryId = 1L,
                categoryName = "Ăn uống",
                categoryEmoji = "🍜",
                note = "Chi tiêu tháng trước",
                paymentMethod = PaymentMethod.CASH,
                timestampEpochMillis = epoch,
            )
        }

        addSample(0, 5_200_000L, TransactionType.INCOME, "Trợ cấp / Lương", "💸", "Trợ cấp & làm thêm")
        val dailyPattern = listOf(
            0 to 45_000L,
            1 to 55_000L,
            2 to 40_000L,
            4 to 140_000L,
            5 to 85_000L,
            6 to 280_000L,
            8 to 48_000L,
            9 to 52_000L,
            10 to 65_000L,
            12 to 190_000L,
            14 to 42_000L,
            15 to 60_000L,
            17 to 360_000L,
            19 to 55_000L,
            20 to 48_000L,
            22 to 110_000L,
            24 to 45_000L,
            26 to 75_000L,
        )
        dailyPattern.forEach { (dayOffset, amount) ->
            addSample(dayOffset, amount, TransactionType.EXPENSE, "Chi tiêu hàng ngày", "🍜", "Giao dịch mẫu")
        }
        return entries
    }

    private fun buildHeatmapWeeks(
        targetMonth: YearMonth,
        dailyExpenseByDate: Map<LocalDate, Long>,
        dailyIncomeByDate: Map<LocalDate, Long>,
        groupedByDate: Map<LocalDate, List<Transaction>>,
        baseline: Long,
    ): List<List<DailyAuditCell?>> {
        val monthEnd = targetMonth.atEndOfMonth()
        val lastWeekMonday = monthEnd.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val firstWeekMonday = lastWeekMonday.minusWeeks((HEATMAP_WEEK_COLUMNS - 1).toLong())

        return (0 until HEATMAP_WEEK_COLUMNS).map { weekIndex ->
            val weekStart = firstWeekMonday.plusWeeks(weekIndex.toLong())
            (0 until 7).map { dayOffset ->
                val date = weekStart.plusDays(dayOffset.toLong())
                if (date.isAfter(monthEnd)) {
                    null
                } else {
                    val expense = dailyExpenseByDate[date] ?: 0L
                    val income = dailyIncomeByDate[date] ?: 0L
                    DailyAuditCell(
                        date = date,
                        expenseAmount = expense,
                        incomeAmount = income,
                        transactionCount = groupedByDate[date]?.size ?: 0,
                        level = heatLevelFor(expense, baseline),
                        isInTargetMonth = YearMonth.from(date) == targetMonth,
                    )
                }
            }
        }
    }

    private fun computeLongestStreak(
        targetMonth: YearMonth,
        effectiveDays: Int,
        activeDates: Set<LocalDate>,
    ): Int {
        var best = 0
        var current = 0
        for (day in 1..effectiveDays) {
            if (targetMonth.atDay(day) in activeDates) {
                current++
                if (current > best) best = current
            } else {
                current = 0
            }
        }
        return best
    }

    private fun computeScoreBreakdown(
        totalIncome: Long,
        totalExpense: Long,
        activeDays: Int,
        longestStreak: Int,
        highCount: Int,
        peakCount: Int,
    ): ScoreBreakdown {
        val savingsScore = if (totalIncome > 0L) {
            val rate = (totalIncome - totalExpense).toDouble() / totalIncome.toDouble()
            when {
                rate <= -0.20 -> 0
                rate < 0.0 -> ((rate + 0.20) / 0.20 * 14.0).roundToInt()
                rate < 0.30 -> (16.0 + (rate / 0.30) * 24.0).roundToInt()
                else -> 40
            }.coerceIn(0, 40)
        } else {
            if (peakCount == 0) 20 else 12
        }

        val consistencyScore = (activeDays * 2).coerceIn(0, 30)
        val stabilityPenalty = peakCount * 6 + highCount * 2
        val stabilityScore = (20 - stabilityPenalty).coerceIn(0, 20)
        val streakScore = (longestStreak * 2).coerceIn(0, 10)

        return ScoreBreakdown(
            savingsScore = savingsScore,
            consistencyScore = consistencyScore,
            stabilityScore = stabilityScore,
            streakScore = streakScore,
        )
    }

    private fun gradeForScore(score: Int): AuditGrade = when {
        score >= 85 -> AuditGrade.EXCELLENT
        score >= 70 -> AuditGrade.GOOD
        score >= 50 -> AuditGrade.FAIR
        else -> AuditGrade.NEEDS_IMPROVEMENT
    }

    private fun buildInsight(
        grade: AuditGrade,
        totalIncome: Long,
        totalExpense: Long,
        savingsRate: Int,
        peakCount: Int,
        noSpendDays: Int,
    ): String = when {
        totalIncome > 0L && totalExpense > totalIncome ->
            "Chi tiêu tháng đang vượt thu nhập. Cần rà lại $peakCount ngày chi đột biến để cân đối ví."
        grade == AuditGrade.EXCELLENT ->
            "Tuyệt vời! Bạn giữ được $noSpendDays ngày không chi tiêu và tiết kiệm khoảng $savingsRate% thu nhập."
        grade == AuditGrade.GOOD ->
            "Kỷ luật ổn định với $noSpendDays ngày không chi. Kiểm soát tốt hơn ở $peakCount ngày chi cao sẽ chạm mốc 85+."
        peakCount > 0 ->
            "Có $peakCount ngày chi vượt ngưỡng 1.8x trung bình ngày. Hãy chia nhỏ hạn mức tuần để ổn định hơn."
        else ->
            "Duy trì ghi chép đều đặn hàng ngày để tăng điểm kỷ luật và theo dõi dòng tiền sát hơn."
    }
}
