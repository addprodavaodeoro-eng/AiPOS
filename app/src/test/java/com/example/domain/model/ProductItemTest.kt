package com.example.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductItemTest {

    @Test
    fun formattedPrice_displaysPhilippinePesoWithDecimals() {
        val product = ProductItem(
            id = "test_1",
            name = "Test Beverage",
            price = 38.50,
            category = ProductCategory.BEVERAGES
        )

        assertEquals("₱38.50", product.formattedPrice)
    }

    @Test
    fun stockStatus_flagsLowStockAndOutOfStockCorrectly() {
        val inStockItem = ProductItem("1", "Item A", 10.0, ProductCategory.SNACKS, stockQuantity = 20)
        val lowStockItem = ProductItem("2", "Item B", 10.0, ProductCategory.SNACKS, stockQuantity = 3)
        val outOfStockItem = ProductItem("3", "Item C", 10.0, ProductCategory.SNACKS, stockQuantity = 0)

        assertFalse(inStockItem.isLowStock)
        assertFalse(inStockItem.isOutOfStock)

        assertTrue(lowStockItem.isLowStock)
        assertFalse(lowStockItem.isOutOfStock)

        assertFalse(outOfStockItem.isLowStock)
        assertTrue(outOfStockItem.isOutOfStock)
    }

    @Test
    fun cartItem_calculatesSubtotalCorrectly() {
        val product = ProductItem("1", "Piattos", 42.50, ProductCategory.SNACKS)
        val cartItem = CartItem(product = product, quantity = 3)

        assertEquals(127.50, cartItem.subtotal, 0.001)
        assertEquals("₱127.50", cartItem.formattedSubtotal)
    }

    @Test
    fun sampleCatalog_containsCategoriesAndValidPrices() {
        val catalog = SampleProductCatalog.items
        assertTrue("Catalog should not be empty", catalog.isNotEmpty())

        val beverageCount = catalog.count { it.category == ProductCategory.BEVERAGES }
        assertTrue("Should have beverages in sample catalog", beverageCount > 0)

        catalog.forEach {
            assertTrue("Price must be greater than zero for ${it.name}", it.price > 0)
            assertTrue("Product must have an id", it.id.isNotBlank())
        }
    }

    @Test
    fun profitMargin_calculatesCorrectAmountAndPercentage() {
        val item = ProductItem(
            id = "test_profit",
            name = "Test Item",
            price = 50.0,
            costPrice = 40.0,
            category = ProductCategory.SNACKS
        )

        assertEquals(10.0, item.profitMargin, 0.001)
        assertEquals(25.0, item.profitMarginPercent, 0.001)
        assertEquals("₱10.00", item.formattedProfitMargin)
        assertEquals("₱40.00", item.formattedCostPrice)
    }
}
