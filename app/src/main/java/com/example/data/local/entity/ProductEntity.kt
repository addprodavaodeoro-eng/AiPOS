package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "products",
    indices = [
        Index(value = ["sku"], unique = false),
        Index(value = ["barcode"], unique = false),
        Index(value = ["name"], unique = false)
    ]
)
data class ProductEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val sku: String? = null,
    val price: Double,
    val costPrice: Double = 0.0,
    val category: String = "ALL",
    val stockQuantity: Int = 0,
    val lowStockThreshold: Int = 5,
    val barcode: String? = null,
    val unit: String = "pc",
    val drawableResName: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Stock count alias representing available on-hand inventory count.
     */
    val stockCount: Int
        get() = stockQuantity

    val stockLevel: Int
        get() = stockQuantity

    val effectiveSku: String
        get() = sku ?: barcode ?: id
}
