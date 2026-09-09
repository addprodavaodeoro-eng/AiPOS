package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class UserRole {
    OWNER,
    MANAGER,
    CASHIER
}

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val storeId: String,
    val username: String, // Stored lowercase for case-insensitivity
    val passwordHash: String,
    val passwordSalt: String,
    val role: UserRole,
    val pin: String? = null,
    val requirePasswordChange: Boolean = true,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
