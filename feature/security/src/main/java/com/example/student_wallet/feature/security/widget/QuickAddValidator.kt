package com.example.student_wallet.feature.security.widget

import com.example.student_wallet.core.domain.model.Category
import com.example.student_wallet.core.domain.model.PaymentMethod
import com.example.student_wallet.core.domain.model.Transaction
import com.example.student_wallet.core.domain.model.TransactionType

internal object QuickAddValidator {

    const val MAX_AMOUNT = 999_000_000L
    private const val MAX_INPUT_DIGITS = 11

    fun sanitizeDigits(input: String): String =
        input.filter { it in '0'..'9' }.take(MAX_INPUT_DIGITS)

    fun parseAndValidateAmount(digits: String): Long? {
        val parsed = digits.toLongOrNull() ?: return null
        return parsed.takeIf { it in 1L..MAX_AMOUNT }
    }

    fun buildTransaction(
        amount: Long,
        type: TransactionType,
        category: Category?,
        note: String,
        paymentMethod: PaymentMethod,
        timestampEpochMillis: Long = System.currentTimeMillis(),
    ): Transaction {
        val resolvedCategoryId = category?.id ?: if (type == TransactionType.EXPENSE) 1L else 7L
        val resolvedCategoryName = category?.name ?: if (type == TransactionType.EXPENSE) "Ăn uống" else "Trợ cấp / Lương"
        val resolvedCategoryEmoji = category?.emoji ?: if (type == TransactionType.EXPENSE) "🍜" else "💸"
        val resolvedNote = note.trim().ifEmpty {
            if (type == TransactionType.EXPENSE) "Chi nhanh từ Widget" else "Thu nhanh từ Widget"
        }

        return Transaction(
            amount = amount,
            type = type,
            categoryId = resolvedCategoryId,
            categoryName = resolvedCategoryName,
            categoryEmoji = resolvedCategoryEmoji,
            note = resolvedNote,
            paymentMethod = paymentMethod,
            timestampEpochMillis = timestampEpochMillis,
        )
    }
}
