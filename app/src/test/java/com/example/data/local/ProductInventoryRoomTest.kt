package com.example.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.dao.ProductDao
import com.example.data.local.entity.ProductEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProductInventoryRoomTest {

    private lateinit var database: AppDatabase
    private lateinit var productDao: ProductDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        productDao = database.productDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertAndRetrieveProduct_persistsAllRequiredFields() = runBlocking {
        val product = ProductEntity(
            id = "prod-101",
            name = "San Miguel Pale Pilsen 330ml",
            sku = "SKU-BEV-SANMIG-330",
            price = 65.00,
            costPrice = 52.00,
            category = "BEVERAGES",
            stockQuantity = 24,
            lowStockThreshold = 5,
            barcode = "4800016644021",
            unit = "bottle"
        )

        productDao.insertProduct(product)

        val retrieved = productDao.getProductById("prod-101")
        assertNotNull(retrieved)
        assertEquals("San Miguel Pale Pilsen 330ml", retrieved?.name)
        assertEquals("SKU-BEV-SANMIG-330", retrieved?.sku)
        assertEquals(65.00, retrieved?.price ?: 0.0, 0.001)
        assertEquals("BEVERAGES", retrieved?.category)
        assertEquals(24, retrieved?.stockQuantity)
        assertEquals(24, retrieved?.stockCount)
        assertEquals(24, retrieved?.stockLevel)
    }

    @Test
    fun getProductBySkuAndBarcode_returnsMatchingEntity() = runBlocking {
        val product = ProductEntity(
            id = "prod-sku-test",
            name = "Coca-Cola 1.5L",
            sku = "SKU-COKE-1500",
            price = 72.00,
            costPrice = 58.00,
            category = "BEVERAGES",
            stockQuantity = 15,
            barcode = "4800016024999"
        )
        productDao.insertProduct(product)

        // Lookup by exact SKU
        val bySku = productDao.getProductBySku("SKU-COKE-1500")
        assertNotNull(bySku)
        assertEquals("Coca-Cola 1.5L", bySku?.name)
        assertEquals(15, bySku?.stockCount)

        // Lookup by Barcode
        val byBarcode = productDao.getProductByBarcode("4800016024999")
        assertNotNull(byBarcode)
        assertEquals("SKU-COKE-1500", byBarcode?.sku)

        // Lookup by either Barcode or SKU
        val byCode = productDao.getProductByBarcodeOrSku("SKU-COKE-1500")
        assertNotNull(byCode)
        assertEquals("prod-sku-test", byCode?.id)
    }

    @Test
    fun searchProducts_findsByNameAndSku() = runBlocking {
        val prod1 = ProductEntity(
            id = "prod-1",
            name = "Great Taste White Coffee",
            sku = "SKU-COF-GTW",
            price = 15.00,
            stockQuantity = 40
        )
        val prod2 = ProductEntity(
            id = "prod-2",
            name = "Nescafe 3-in-1 Original",
            sku = "SKU-COF-NES",
            price = 14.50,
            stockQuantity = 30
        )
        productDao.insertAll(listOf(prod1, prod2))

        // Search by query matching SKU
        val searchBySku = productDao.searchProducts("GTW").first()
        assertEquals(1, searchBySku.size)
        assertEquals("Great Taste White Coffee", searchBySku[0].name)

        // Search by query matching Name
        val searchByName = productDao.searchProducts("Nescafe").first()
        assertEquals(1, searchByName.size)
        assertEquals("SKU-COF-NES", searchByName[0].sku)
    }

    @Test
    fun stockAndPriceManagementBySku_updatesCorrectly() = runBlocking {
        val product = ProductEntity(
            id = "prod-sku-mgmt",
            name = "SkyFlakes Crackers",
            sku = "SKU-SNK-SKY",
            price = 8.00,
            stockQuantity = 20
        )
        productDao.insertProduct(product)

        // Adjust stock by SKU (+15 items restocked)
        productDao.adjustStockBySku("SKU-SNK-SKY", 15)
        var current = productDao.getProductBySku("SKU-SNK-SKY")
        assertEquals(35, current?.stockCount)

        // Update explicit stock count by SKU
        productDao.updateStockCountBySku("SKU-SNK-SKY", 50)
        current = productDao.getProductBySku("SKU-SNK-SKY")
        assertEquals(50, current?.stockCount)

        // Update price by SKU
        productDao.updatePriceBySku("SKU-SNK-SKY", 9.50)
        current = productDao.getProductBySku("SKU-SNK-SKY")
        assertEquals(9.50, current?.price ?: 0.0, 0.001)

        // Delete by SKU
        productDao.deleteProductBySku("SKU-SNK-SKY")
        assertNull(productDao.getProductBySku("SKU-SNK-SKY"))
    }

    @Test
    fun totalStockCountAndInventoryValue_aggregatesAccurately() = runBlocking {
        val prodA = ProductEntity(id = "a", name = "A", price = 10.0, stockQuantity = 5)
        val prodB = ProductEntity(id = "b", name = "B", price = 20.0, stockQuantity = 3)
        productDao.insertAll(listOf(prodA, prodB))

        val totalStock = productDao.getTotalStockCount().first()
        assertEquals(8, totalStock)

        val totalValue = productDao.getTotalInventoryValue().first()
        assertEquals(110.0, totalValue ?: 0.0, 0.001)
    }

    @Test
    fun updateStockAndAdjustStock_modifiesInventoryCorrectly() = runBlocking {
        val product = ProductEntity(
            id = "prod-102",
            name = "Lucky Me Pancit Canton",
            price = 18.00,
            costPrice = 14.00,
            category = "INSTANT_MEALS",
            stockQuantity = 50,
            lowStockThreshold = 10
        )

        productDao.insertProduct(product)

        // Adjust stock down (e.g. sale of 10 items)
        productDao.adjustStock("prod-102", -10)
        var updated = productDao.getProductById("prod-102")
        assertEquals(40, updated?.stockQuantity)
        assertEquals(40, updated?.stockLevel)

        // Set explicit new stock quantity
        productDao.updateStock("prod-102", 75)
        updated = productDao.getProductById("prod-102")
        assertEquals(75, updated?.stockQuantity)
        assertEquals(75, updated?.stockLevel)
    }

    @Test
    fun getLowStockProducts_returnsOnlyProductsAtOrBelowThreshold() = runBlocking {
        val prodHealthy = ProductEntity(
            id = "prod-healthy",
            name = "Cooking Oil 1L",
            price = 90.0,
            category = "HOUSEHOLD",
            stockQuantity = 30,
            lowStockThreshold = 5
        )
        val prodLow = ProductEntity(
            id = "prod-low",
            name = "Soy Sauce 500ml",
            price = 25.0,
            category = "CANNED_GOODS",
            stockQuantity = 3,
            lowStockThreshold = 5
        )
        val prodOut = ProductEntity(
            id = "prod-out",
            name = "Vinegar 500ml",
            price = 22.0,
            category = "CANNED_GOODS",
            stockQuantity = 0,
            lowStockThreshold = 5
        )

        productDao.insertAll(listOf(prodHealthy, prodLow, prodOut))

        val lowStockList = productDao.getLowStockProducts().first()
        assertEquals(2, lowStockList.size)
        assertTrue(lowStockList.any { it.id == "prod-low" })
        assertTrue(lowStockList.any { it.id == "prod-out" })
        assertFalse(lowStockList.any { it.id == "prod-healthy" })
    }

    @Test
    fun deleteProduct_removesItemFromDatabase() = runBlocking {
        val product = ProductEntity(
            id = "prod-del",
            name = "Expiring Bread",
            price = 45.0,
            category = "SNACKS",
            stockQuantity = 2
        )

        productDao.insertProduct(product)
        assertEquals(1, productDao.getProductCount())

        productDao.deleteProductById("prod-del")
        assertEquals(0, productDao.getProductCount())
        assertNull(productDao.getProductById("prod-del"))
    }
}
