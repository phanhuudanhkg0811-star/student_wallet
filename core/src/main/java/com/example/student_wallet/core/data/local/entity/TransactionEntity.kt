package com.example.student_wallet.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.student_wallet.core.domain.model.PaymentMethod
import com.example.student_wallet.core.domain.model.Transaction
import com.example.student_wallet.core.domain.model.TransactionType

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val amount: Long,
    val type: String,
    val categoryId: Long,
    val categoryName: String,
    val categoryEmoji: String,
    val note: String,
    val paymentMethod: String,
    val timestampEpochMillis: Long,
    val receiptImagePath: String? = null,
    val isDeleted: Boolean = false,
)

fun TransactionEntity.toDomain(): Transaction = Transaction(
    id = id,
    amount = amount,
    type = TransactionType.valueOf(type),
    categoryId = categoryId,
    categoryName = categoryName,
    categoryEmoji = categoryEmoji,
    note = note,
    paymentMethod = PaymentMethod.valueOf(paymentMethod),
    timestampEpochMillis = timestampEpochMillis,
    receiptImagePath = receiptImagePath,
    isDeleted = isDeleted,
)

fun Transaction.toEntity(): TransactionEntity = TransactionEntity(
    id = id,
    amount = amount,
    type = type.name,
    categoryId = categoryId,
    categoryName = categoryName,
    categoryEmoji = categoryEmoji,
    note = note,
    paymentMethod = paymentMethod.name,
    timestampEpochMillis = timestampEpochMillis,
    receiptImagePath = receiptImagePath,
    isDeleted = isDeleted,
)
