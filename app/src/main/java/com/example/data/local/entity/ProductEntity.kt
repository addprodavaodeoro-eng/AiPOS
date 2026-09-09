package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val price: Double,
    val costPrice: Double = 0.0,
    val category: String,
    val stockQuantity: Int = 0,
    val lowStockThreshold: Int = 5,
    val barcode: String? = null,
    val unit: String = "pc",
    val drawableResName: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val stockLevel: Int
        get() = stockQuantity
}
