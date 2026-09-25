package com.example.domain.repository

import com.example.data.local.dao.ProductDao
import com.example.data.local.entity.ProductEntity
import com.example.domain.model.CartItem
import com.example.domain.model.ProductCategory
import com.example.domain.model.ProductItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class FakeProductDao : ProductDao {
    val storage = mutableMapOf<String, ProductEntity>()

    override fun getAllProducts(): Flow<List<ProductEntity>> {
        return flowOf(storage.values.sortedBy { it.name })
    }

    override suspend fun getAllProductsList(): List<ProductEntity> {
        return storage.values.toList()
    }

    override suspend fun getProductById(id: String): ProductEntity? {
        return storage[id]
    }

    override fun getLowStockProducts(): Flow<List<ProductEntity>> {
        return flowOf(storage.values.filter { it.stockQuantity <= it.lowStockThreshold })
    }

    override suspend fun getProductCount(): Int {
        return storage.size
    }

    override suspend fun insertProduct(product: ProductEntity) {
        storage[product.id] = product
    }

    override suspend fun insertAll(products: List<ProductEntity>) {
        products.forEach { storage[it.id] = it }
    }

    override suspend fun updateProduct(product: ProductEntity) {
        storage[product.id] = product
    }

    override suspend fun updateStock(id: String, newQuantity: Int, updatedAt: Long) {
        storage[id]?.let {
            storage[id] = it.copy(stockQuantity = newQuantity, updatedAt = updatedAt)
        }
    }

    override suspend fun adjustStock(id: String, delta: Int, updatedAt: Long) {
        storage[id]?.let {
            val newQty = maxOf(0, it.stockQuantity + delta)
            storage[id] = it.copy(stockQuantity = newQty, updatedAt = updatedAt)
        }
    }

    override suspend fun getProductBySku(sku: String): ProductEntity? {
        return storage.values.find { it.sku == sku }
    }

    override suspend fun getProductByBarcode(barcode: String): ProductEntity? {
        return storage.values.find { it.barcode == barcode }
    }

    override suspend fun getProductByBarcodeOrSku(code: String): ProductEntity? {
        return storage.values.find { it.barcode == code || it.sku == code }
    }

    override fun searchProducts(query: String): Flow<List<ProductEntity>> {
        return flowOf(storage.values.filter {
            it.name.contains(query, ignoreCase = true) ||
            (it.sku != null && it.sku.contains(query, ignoreCase = true)) ||
            (it.barcode != null && it.barcode.contains(query, ignoreCase = true))
        })
    }

    override fun getProductsByCategory(category: String): Flow<List<ProductEntity>> {
        return flowOf(storage.values.filter { it.category.equals(category, ignoreCase = true) })
    }

    override fun getProductsBelowStock(threshold: Int): Flow<List<ProductEntity>> {
        return flowOf(storage.values.filter { it.stockQuantity <= threshold })
    }

    override fun getTotalStockCount(): Flow<Int?> {
        return flowOf(storage.values.sumOf { it.stockQuantity })
    }

    override fun getTotalInventoryValue(): Flow<Double?> {
        return flowOf(storage.values.sumOf { it.price * it.stockQuantity })
    }

    override suspend fun updateStockCount(id: String, newStockCount: Int, updatedAt: Long) {
        storage[id]?.let {
            storage[id] = it.copy(stockQuantity = newStockCount, updatedAt = updatedAt)
        }
    }

    override suspend fun updateStockCountBySku(sku: String, newStockCount: Int, updatedAt: Long) {
        val prod = storage.values.find { it.sku == sku }
        if (prod != null) {
            storage[prod.id] = prod.copy(stockQuantity = newStockCount, updatedAt = updatedAt)
        }
    }

    override suspend fun adjustStockBySku(sku: String, delta: Int, updatedAt: Long) {
        val prod = storage.values.find { it.sku == sku }
        if (prod != null) {
            val newQty = maxOf(0, prod.stockQuantity + delta)
            storage[prod.id] = prod.copy(stockQuantity = newQty, updatedAt = updatedAt)
        }
    }

    override suspend fun updatePrice(id: String, newPrice: Double, updatedAt: Long) {
        storage[id]?.let {
            storage[id] = it.copy(price = newPrice, updatedAt = updatedAt)
        }
    }

    override suspend fun updatePriceBySku(sku: String, newPrice: Double, updatedAt: Long) {
        val prod = storage.values.find { it.sku == sku }
        if (prod != null) {
            storage[prod.id] = prod.copy(price = newPrice, updatedAt = updatedAt)
        }
    }

    override suspend fun deleteProduct(product: ProductEntity) {
        storage.remove(product.id)
    }

    override suspend fun deleteProductById(id: String) {
        storage.remove(id)
    }

    override suspend fun deleteProductBySku(sku: String) {
        val prod = storage.values.find { it.sku == sku }
        if (prod != null) {
            storage.remove(prod.id)
        }
    }

    override suspend fun deleteAllProducts() {
        storage.clear()
    }
}

class ProductRepositoryTest {

    private lateinit var fakeDao: FakeProductDao
    private lateinit var repository: ProductRepository

    @Before
    fun setup() {
        fakeDao = FakeProductDao()
        repository = ProductRepository(fakeDao)
    }

    @Test
    fun addAndGetProduct_convertsAndRetrievesDomainModel() = runBlocking {
        val item = ProductItem(
            id = "prod-1",
            name = "Royal Tru-Orange 500ml",
            price = 35.0,
            costPrice = 28.0,
            category = ProductCategory.BEVERAGES,
            stockQuantity = 20,
            lowStockThreshold = 5,
            barcode = "4800016644038",
            unit = "bottle"
        )

        repository.addProduct(item)

        val retrieved = repository.getProductById("prod-1")
        assertNotNull(retrieved)
        assertEquals("Royal Tru-Orange 500ml", retrieved?.name)
        assertEquals(35.0, retrieved?.price ?: 0.0, 0.001)
        assertEquals(ProductCategory.BEVERAGES, retrieved?.category)
        assertEquals(20, retrieved?.stockQuantity)
    }

    @Test
    fun deductStockForSale_adjustsQuantitiesCorrectly() = runBlocking {
        val prod1 = ProductItem(id = "p1", name = "Soap", price = 25.0, costPrice = 18.0, category = ProductCategory.HOUSEHOLD, stockQuantity = 10)
        val prod2 = ProductItem(id = "p2", name = "Shampoo", price = 8.0, costPrice = 5.0, category = ProductCategory.HOUSEHOLD, stockQuantity = 15)
        repository.addProducts(listOf(prod1, prod2))

        val cart = listOf(
            CartItem(product = prod1, quantity = 3),
            CartItem(product = prod2, quantity = 5)
        )

        repository.deductStockForSale(cart)

        assertEquals(7, repository.getProductById("p1")?.stockQuantity)
        assertEquals(10, repository.getProductById("p2")?.stockQuantity)
    }

    @Test
    fun lowStockProducts_emitsOnlyItemsAtOrBelowThreshold() = runBlocking {
        val prod1 = ProductItem(id = "p1", name = "Chips", price = 15.0, costPrice = 10.0, category = ProductCategory.SNACKS, stockQuantity = 2, lowStockThreshold = 5)
        val prod2 = ProductItem(id = "p2", name = "Soda", price = 20.0, costPrice = 14.0, category = ProductCategory.BEVERAGES, stockQuantity = 20, lowStockThreshold = 5)
        repository.addProducts(listOf(prod1, prod2))

        val lowStock = repository.lowStockProducts.first()
        assertEquals(1, lowStock.size)
        assertEquals("Chips", lowStock.first().name)
    }

    @Test
    fun deleteProduct_removesItem() = runBlocking {
        val prod = ProductItem(id = "p1", name = "Coffee", price = 12.0, costPrice = 8.0, category = ProductCategory.BEVERAGES, stockQuantity = 10)
        repository.addProduct(prod)
        assertEquals(1, repository.getProductCount())

        repository.deleteProduct("p1")
        assertEquals(0, repository.getProductCount())
        assertNull(repository.getProductById("p1"))
    }
}
