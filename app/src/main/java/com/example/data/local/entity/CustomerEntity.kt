package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(), // Should migrate to UUIDv7
    val storeId: String,
    val name: String,
    val mobileNumber: String?,
    val address: String?,
    val creditEnabled: Boolean = true,
    val creditLimitCentavos: Long? = null, // Enforces optional safe limit
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
