package com.example.student_wallet.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.student_wallet.core.domain.model.Category
import com.example.student_wallet.core.domain.model.TransactionType

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val emoji: String,
    val type: String,
    val colorHex: Long,
)

fun CategoryEntity.toDomain(): Category = Category(
    id = id,
    name = name,
    emoji = emoji,
    type = TransactionType.valueOf(type),
    colorHex = colorHex,
)
