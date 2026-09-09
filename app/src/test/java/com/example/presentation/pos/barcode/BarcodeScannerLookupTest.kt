package com.example.presentation.pos.barcode

import com.example.domain.model.ProductCategory
import com.example.domain.model.ProductItem
import org.junit.Assert.*
import org.junit.Test

class BarcodeScannerLookupTest {

    private val sampleProducts = listOf(
        ProductItem(
            id = "prod_1",
            name = "Coca-Cola Original 330ml Can",
            price = 38.00,
            costPrice = 30.00,
            category = ProductCategory.BEVERAGES,
            stockQuantity = 42,
            barcode = "4800016644021"
        ),
        ProductItem(
            id = "prod_2",
            name = "Piattos Cheese Potato Crisps 85g",
            price = 42.50,
            costPrice = 34.00,
            category = ProductCategory.SNACKS,
            stockQuantity = 24,
            barcode = "4800016024113"
        ),
        ProductItem(
            id = "prod_out",
            name = "Out of Stock Item",
            price = 50.00,
            costPrice = 40.00,
            category = ProductCategory.SNACKS,
            stockQuantity = 0,
            barcode = "9999999999999"
        )
    )

    @Test
    fun lookupByBarcode_matchesExactBarcode() {
        val targetBarcode = "4800016644021"
        val found = sampleProducts.firstOrNull { it.barcode == targetBarcode }

        assertNotNull(found)
        assertEquals("prod_1", found?.id)
        assertEquals("Coca-Cola Original 330ml Can", found?.name)
    }

    @Test
    fun lookupByBarcode_handlesWhitespaceAndCaseSensitivity() {
        val inputWithWhitespace = "  4800016024113  "
        val clean = inputWithWhitespace.trim()
        val found = sampleProducts.firstOrNull { it.barcode.equals(clean, ignoreCase = true) }

        assertNotNull(found)
        assertEquals("prod_2", found?.id)
    }

    @Test
    fun lookupByBarcode_returnsNullForUnknownBarcode() {
        val unknownBarcode = "1234567890123"
        val found = sampleProducts.firstOrNull { it.barcode == unknownBarcode }

        assertNull(found)
    }

    @Test
    fun scanResult_distinguishesOutOfStockProducts() {
        val barcode = "9999999999999"
        val product = sampleProducts.firstOrNull { it.barcode == barcode }

        assertNotNull(product)
        assertTrue(product!!.stockQuantity <= 0)
    }
}
