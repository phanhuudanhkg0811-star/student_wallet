package com.example.student_wallet.core.domain.model

data class Category(
    val id: Long = 0L,
    val name: String,
    val emoji: String,
    val type: TransactionType,
    val colorHex: Long,
)

data class Transaction(
    val id: Long = 0L,
    val amount: Long,
    val type: TransactionType,
    val categoryId: Long,
    val categoryName: String,
    val categoryEmoji: String,
    val note: String,
    val paymentMethod: PaymentMethod,
    val timestampEpochMillis: Long,
    val receiptImagePath: String? = null,
    val isDeleted: Boolean = false,
)
