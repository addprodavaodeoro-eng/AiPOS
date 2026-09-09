package com.example.domain.backup

import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.ProductEntity
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.*
import org.junit.Test

class DatabaseBackupTest {

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    @Test
    fun databaseSnapshot_serializesAndDeserializesCorrectly() {
        val sampleProduct = ProductEntity(
            id = "prod_001",
            name = "Coca Cola 1.5L",
            price = 75.0,
            costPrice = 62.0,
            category = "Beverages",
            stockQuantity = 24,
            lowStockThreshold = 5
        )

        val sampleCustomer = CustomerEntity(
            id = "cust_001",
            storeId = "store_main",
            name = "Juan Dela Cruz",
            mobileNumber = "09171234567",
            address = "Poblacion",
            creditEnabled = true,
            creditLimitCentavos = 500000L
        )

        val originalSnapshot = DatabaseSnapshot(
            version = 1,
            exportedAt = 1788771600000L,
            appVersion = "1.0.0",
            storeName = "Aling Nena's Sari-Sari Store",
            products = listOf(sampleProduct),
            customers = listOf(sampleCustomer),
            creditLedger = emptyList(),
            expenses = emptyList(),
            users = emptyList()
        )

        val adapter = moshi.adapter(DatabaseSnapshot::class.java)
        val json = adapter.toJson(originalSnapshot)

        assertNotNull(json)
        assertTrue(json.contains("Coca Cola 1.5L"))
        assertTrue(json.contains("Juan Dela Cruz"))

        val deserialized = adapter.fromJson(json)
        assertNotNull(deserialized)
        assertEquals(1, deserialized?.products?.size)
        assertEquals("Coca Cola 1.5L", deserialized?.products?.first()?.name)
        assertEquals(1, deserialized?.customers?.size)
        assertEquals("Juan Dela Cruz", deserialized?.customers?.first()?.name)
        assertEquals(2, deserialized?.totalRecordsCount)
    }
}
