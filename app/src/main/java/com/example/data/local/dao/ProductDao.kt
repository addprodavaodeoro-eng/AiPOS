package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for storing and managing product inventory items in Room.
 * Provides reactive Flow streams and suspend functions for inventory operations.
 */
@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products")
    suspend fun getAllProductsList(): List<ProductEntity>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: String): ProductEntity?

    @Query("SELECT * FROM products WHERE sku = :sku LIMIT 1")
    suspend fun getProductBySku(sku: String): ProductEntity?

    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    suspend fun getProductByBarcode(barcode: String): ProductEntity?

    @Query("SELECT * FROM products WHERE barcode = :code OR sku = :code LIMIT 1")
    suspend fun getProductByBarcodeOrSku(code: String): ProductEntity?

    @Query("SELECT * FROM products WHERE name LIKE '%' || :query || '%' OR sku LIKE '%' || :query || '%' OR barcode LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchProducts(query: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE category = :category ORDER BY name ASC")
    fun getProductsByCategory(category: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE stockQuantity <= lowStockThreshold ORDER BY stockQuantity ASC")
    fun getLowStockProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE stockQuantity <= :threshold ORDER BY stockQuantity ASC")
    fun getProductsBelowStock(threshold: Int): Flow<List<ProductEntity>>

    @Query("SELECT COUNT(*) FROM products")
    suspend fun getProductCount(): Int

    @Query("SELECT SUM(stockQuantity) FROM products")
    fun getTotalStockCount(): Flow<Int?>

    @Query("SELECT SUM(price * stockQuantity) FROM products")
    fun getTotalInventoryValue(): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<ProductEntity>)

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Query("UPDATE products SET stockQuantity = :newQuantity, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStock(id: String, newQuantity: Int, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE products SET stockQuantity = :newStockCount, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStockCount(id: String, newStockCount: Int, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE products SET stockQuantity = :newStockCount, updatedAt = :updatedAt WHERE sku = :sku")
    suspend fun updateStockCountBySku(sku: String, newStockCount: Int, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE products SET stockQuantity = MAX(0, stockQuantity + :delta), updatedAt = :updatedAt WHERE id = :id")
    suspend fun adjustStock(id: String, delta: Int, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE products SET stockQuantity = MAX(0, stockQuantity + :delta), updatedAt = :updatedAt WHERE sku = :sku")
    suspend fun adjustStockBySku(sku: String, delta: Int, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE products SET price = :newPrice, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updatePrice(id: String, newPrice: Double, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE products SET price = :newPrice, updatedAt = :updatedAt WHERE sku = :sku")
    suspend fun updatePriceBySku(sku: String, newPrice: Double, updatedAt: Long = System.currentTimeMillis())

    @Delete
    suspend fun deleteProduct(product: ProductEntity)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteProductById(id: String)

    @Query("DELETE FROM products WHERE sku = :sku")
    suspend fun deleteProductBySku(sku: String)

    @Query("DELETE FROM products")
    suspend fun deleteAllProducts()
}

