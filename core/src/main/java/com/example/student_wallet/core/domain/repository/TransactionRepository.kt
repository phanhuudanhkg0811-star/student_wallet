package com.example.student_wallet.core.domain.repository

import com.example.student_wallet.core.domain.model.Category
import com.example.student_wallet.core.domain.model.Transaction
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun observeTransactions(): Flow<List<Transaction>>
    fun observeCategories(): Flow<List<Category>>
    suspend fun addTransaction(transaction: Transaction): Long
}
