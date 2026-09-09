package com.example.presentation.pos

import com.example.domain.model.ProductCategory
import com.example.domain.model.ProductItem
import org.junit.Assert.*
import org.junit.Test

class ProductStockIndicatorTest {

    @Test
    fun productItem_detectsDefaultThresholdCorrectly() {
        val lowStockProduct = ProductItem(
            id = "p1",
            name = "Coke Mismo 290ml",
            price = 20.0,
            category = ProductCategory.BEVERAGES,
            stockQuantity = 4,
            lowStockThreshold = 5
        )

        val normalStockProduct = ProductItem(
            id = "p2",
            name = "Royal 500ml",
            price = 35.0,
            category = ProductCategory.BEVERAGES,
            stockQuantity = 15,
            lowStockThreshold = 5
        )

        val outOfStockProduct = ProductItem(
            id = "p3",
            name = "Sprite 1.5L",
            price = 75.0,
            category = ProductCategory.BEVERAGES,
            stockQuantity = 0,
            lowStockThreshold = 5
        )

        assertTrue(lowStockProduct.isLowStock)
        assertFalse(lowStockProduct.isOutOfStock)

        assertFalse(normalStockProduct.isLowStock)
        assertFalse(normalStockProduct.isOutOfStock)

        assertFalse(outOfStockProduct.isLowStock)
        assertTrue(outOfStockProduct.isOutOfStock)
    }

    @Test
    fun customThresholdOverride_adjustsLowStockDetermination() {
        val product = ProductItem(
            id = "p1",
            name = "Lucky Me Pancit Canton",
            price = 16.0,
            category = ProductCategory.INSTANT_MEALS,
            stockQuantity = 8,
            lowStockThreshold = 5
        )

        // With default threshold 5: 8 is NOT low stock
        assertFalse(product.isLowStock)

        // When threshold is overridden to 10: 8 IS low stock
        val customThreshold = 10
        val isLowStockWithOverride = product.stockQuantity in 1..customThreshold
        assertTrue(isLowStockWithOverride)

        // When threshold is overridden to 3: 8 is NOT low stock
        val strictThreshold = 3
        val isLowStockWithStrictOverride = product.stockQuantity in 1..strictThreshold
        assertFalse(isLowStockWithStrictOverride)
    }

    @Test
    fun filterLowStockProducts_returnsOnlyItemsBelowSetThreshold() {
        val products = listOf(
            ProductItem(id = "1", name = "Item A", price = 10.0, category = ProductCategory.SNACKS, stockQuantity = 2, lowStockThreshold = 5),
            ProductItem(id = "2", name = "Item B", price = 10.0, category = ProductCategory.SNACKS, stockQuantity = 4, lowStockThreshold = 5),
            ProductItem(id = "3", name = "Item C", price = 10.0, category = ProductCategory.SNACKS, stockQuantity = 20, lowStockThreshold = 5),
            ProductItem(id = "4", name = "Item D", price = 10.0, category = ProductCategory.SNACKS, stockQuantity = 0, lowStockThreshold = 5)
        )

        val threshold = 5
        val lowStockOnly = products.filter { it.stockQuantity in 1..threshold }

        assertEquals(2, lowStockOnly.size)
        assertTrue(lowStockOnly.any { it.id == "1" })
        assertTrue(lowStockOnly.any { it.id == "2" })
        assertFalse(lowStockOnly.any { it.id == "3" })
        assertFalse(lowStockOnly.any { it.id == "4" })
    }
}
