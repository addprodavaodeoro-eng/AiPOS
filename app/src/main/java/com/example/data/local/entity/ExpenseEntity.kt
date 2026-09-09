package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class ExpenseCategory {
    ELECTRICITY,
    WATER,
    RENT,
    TRANSPORTATION,
    SUPPLIES,
    EMPLOYEE_WAGES,
    MAINTENANCE,
    MISCELLANEOUS
}

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(), // Should migrate to UUIDv7
    val storeId: String,
    val amountCentavos: Long,
    val category: ExpenseCategory,
    val description: String?,
    val recordedByUserId: String,
    val receiptImageUri: String?,
    val isVoided: Boolean = false,
    val occurredAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)
