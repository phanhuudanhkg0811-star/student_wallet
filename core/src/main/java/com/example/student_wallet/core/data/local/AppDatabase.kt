package com.example.student_wallet.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.student_wallet.core.data.local.dao.CategoryDao
import com.example.student_wallet.core.data.local.dao.TransactionDao
import com.example.student_wallet.core.data.local.entity.CategoryEntity
import com.example.student_wallet.core.data.local.entity.TransactionEntity

@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
}

class DatabaseSeedCallback : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        val defaults: List<Array<Any>> = listOf(
            arrayOf("Ăn uống", "🍜", "EXPENSE", 0xFF006C51L),
            arrayOf("Tiền trọ & điện nước", "🏠", "EXPENSE", 0xFF406376L),
            arrayOf("Di chuyển", "🚌", "EXPENSE", 0xFF4C6359L),
            arrayOf("Đi chợ / Mua sắm", "🛒", "EXPENSE", 0xFFE08A00L),
            arrayOf("Giải trí", "🎮", "EXPENSE", 0xFFBA1A1AL),
            arrayOf("Học tập", "📚", "EXPENSE", 0xFF274B5DL),
            arrayOf("Trợ cấp / Lương", "💸", "INCOME", 0xFF00855FL),
        )
        defaults.forEach { row ->
            db.execSQL(
                "INSERT INTO categories (name, emoji, type, colorHex) VALUES (?, ?, ?, ?)",
                row,
            )
        }
    }
}
