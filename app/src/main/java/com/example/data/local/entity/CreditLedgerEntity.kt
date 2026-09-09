package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class CreditEntryType {
    CREDIT_SALE,
    PAYMENT,
    ADJUSTMENT,
    REVERSAL
}

@Entity(tableName = "credit_ledger")
data class CreditLedgerEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val customerId: String,
    val storeId: String,
    val entryType: CreditEntryType,
    val referenceId: String?, // ID of the sale or payment
    val debitAmount: Long,  // Increases Utang (stored in centavos)
    val creditAmount: Long, // Decreases Utang (stored in centavos)
    val notes: String?,
    val occurredAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)
