package com.example.student_wallet.core.data.repository

import com.example.student_wallet.core.data.local.dao.CategoryDao
import com.example.student_wallet.core.data.local.dao.TransactionDao
import com.example.student_wallet.core.data.local.entity.toDomain
import com.example.student_wallet.core.data.local.entity.toEntity
import com.example.student_wallet.core.domain.model.Category
import com.example.student_wallet.core.domain.model.Transaction
import com.example.student_wallet.core.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OfflineTransactionRepository(
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
) : TransactionRepository {

    override fun observeTransactions(): Flow<List<Transaction>> =
        transactionDao.observeAll().map { entities ->
            entities.map { it.toDomain() }
        }

    override fun observeCategories(): Flow<List<Category>> =
        categoryDao.observeAll().map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun addTransaction(transaction: Transaction): Long =
        transactionDao.insert(transaction.toEntity())
}
