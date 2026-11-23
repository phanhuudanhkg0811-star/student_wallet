package com.example.student_wallet.feature.security.audit

import java.time.LocalDate
import java.time.YearMonth

enum class HeatLevel(val label: String) {
    NONE("Không chi"),
    LOW("Chi nhẹ"),
    MEDIUM("Vừa phải"),
    HIGH("Chi cao"),
    PEAK("Đột biến"),
}

enum class AuditGrade(val label: String) {
    NO_DATA("Chưa có dữ liệu"),
    EXCELLENT("Kỷ luật xuất sắc"),
    GOOD("Kỷ luật tốt"),
    FAIR("Cần chú ý"),
    NEEDS_IMPROVEMENT("Cảnh báo chi tiêu"),
}

data class DailyAuditCell(
    val date: LocalDate,
    val expenseAmount: Long,
    val incomeAmount: Long,
    val transactionCount: Int,
    val level: HeatLevel,
    val isInTargetMonth: Boolean,
)

data class ScoreBreakdown(
    val savingsScore: Int,
    val consistencyScore: Int,
    val stabilityScore: Int,
    val streakScore: Int,
) {
    val total: Int
        get() = (savingsScore + consistencyScore + stabilityScore + streakScore).coerceIn(0, 100)
}

data class MonthlyAuditSummary(
    val month: YearMonth,
    val hasData: Boolean,
    val score: Int,
    val grade: AuditGrade,
    val breakdown: ScoreBreakdown,
    val totalIncome: Long,
    val totalExpense: Long,
    val savingsRatePercent: Int,
    val activeDaysCount: Int,
    val noSpendDaysCount: Int,
    val longestLoggingStreak: Int,
    val averageActiveDayExpense: Long,
    val insightText: String,
    val heatmapWeeks: List<List<DailyAuditCell?>>,
)
