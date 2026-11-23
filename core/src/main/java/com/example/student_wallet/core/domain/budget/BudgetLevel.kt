package com.example.student_wallet.core.domain.budget

enum class BudgetLevel {
    OK,
    WARN,
    BAD,
}

fun budgetProgress(spent: Long, limit: Long): Float {
    if (limit <= 0L) return if (spent > 0L) 1f else 0f
    if (spent <= 0L) return 0f
    return (spent.toDouble() / limit.toDouble()).toFloat().coerceIn(0f, 1f)
}

fun budgetLevel(percent: Float): BudgetLevel = when {
    percent > 0.85f -> BudgetLevel.BAD
    percent >= 0.60f -> BudgetLevel.WARN
    else -> BudgetLevel.OK
}

fun budgetLevel(spent: Long, limit: Long): BudgetLevel {
    if (limit <= 0L) return if (spent > 0L) BudgetLevel.BAD else BudgetLevel.OK
    val ratio = (spent.toDouble() / limit.toDouble()).toFloat()
    return budgetLevel(ratio)
}
