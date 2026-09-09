package com.example.domain.repository

import com.example.data.local.dao.ProductDao
import com.example.domain.model.CartItem
import com.example.domain.model.ProductCategory
import com.example.domain.model.ProductItem
import com.example.domain.model.SampleProductCatalog
import com.example.domain.model.toProductEntity
import com.example.domain.model.toProductItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Repository interface defining clean contract for product inventory data operations.
 */
interface IProductRepository {
    val allProducts: Flow<List<ProductItem>>
    val lowStockProducts: Flow<List<ProductItem>>
    suspend fun getProductById(productId: String): ProductItem?
    suspend fun searchProducts(query: String, category: ProductCategory = ProductCategory.ALL): Flow<List<ProductItem>>
    suspend fun getProductCount(): Int
    suspend fun seedIfEmpty()
    suspend fun addProduct(product: ProductItem)
    suspend fun addProducts(products: List<ProductItem>)
    suspend fun updateProduct(product: ProductItem)
    suspend fun updateStock(productId: String, newQuantity: Int)
    suspend fun adjustStock(productId: String, delta: Int)
    suspend fun deductStockForSale(cartItems: List<CartItem>)
    suspend fun deleteProduct(productId: String)
    suspend fun deleteAllProducts()
}

/**
 * Concrete ProductRepository implementing IProductRepository and abstracting Room DAO operations.
 */
class ProductRepository(
    private val productDao: ProductDao
) : IProductRepository {

    override val allProducts: Flow<List<ProductItem>> = productDao.getAllProducts()
        .map { list -> list.map { it.toProductItem() } }
        .flowOn(Dispatchers.IO)

    override val lowStockProducts: Flow<List<ProductItem>> = productDao.getLowStockProducts()
        .map { list -> list.map { it.toProductItem() } }
        .flowOn(Dispatchers.IO)

    override suspend fun getProductById(productId: String): ProductItem? = withContext(Dispatchers.IO) {
        productDao.getProductById(productId)?.toProductItem()
    }

    override suspend fun searchProducts(
        query: String,
        category: ProductCategory
    ): Flow<List<ProductItem>> = allProducts.map { list ->
        list.filter { item ->
            val matchesCategory = (category == ProductCategory.ALL) || (item.category == category)
            val matchesQuery = query.isBlank() ||
                    item.name.contains(query, ignoreCase = true) ||
                    (item.barcode != null && item.barcode.contains(query, ignoreCase = true))
            matchesCategory && matchesQuery
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun getProductCount(): Int = withContext(Dispatchers.IO) {
        productDao.getProductCount()
    }

    override suspend fun seedIfEmpty() = withContext(Dispatchers.IO) {
        if (productDao.getProductCount() == 0) {
            val entities = SampleProductCatalog.items.map { it.toProductEntity() }
            productDao.insertAll(entities)
        }
    }

    override suspend fun addProduct(product: ProductItem) = withContext(Dispatchers.IO) {
        productDao.insertProduct(product.toProductEntity())
    }

    override suspend fun addProducts(products: List<ProductItem>) = withContext(Dispatchers.IO) {
        productDao.insertAll(products.map { it.toProductEntity() })
    }

    override suspend fun updateProduct(product: ProductItem) = withContext(Dispatchers.IO) {
        productDao.updateProduct(product.toProductEntity())
    }

    override suspend fun updateStock(productId: String, newQuantity: Int) = withContext(Dispatchers.IO) {
        productDao.updateStock(productId, newQuantity)
    }

    override suspend fun adjustStock(productId: String, delta: Int) = withContext(Dispatchers.IO) {
        productDao.adjustStock(productId, delta)
    }

    override suspend fun deductStockForSale(cartItems: List<CartItem>) = withContext(Dispatchers.IO) {
        cartItems.forEach { item ->
            productDao.adjustStock(item.product.id, -item.quantity)
        }
    }

    override suspend fun deleteProduct(productId: String) = withContext(Dispatchers.IO) {
        productDao.deleteProductById(productId)
    }

    override suspend fun deleteAllProducts() = withContext(Dispatchers.IO) {
        productDao.deleteAllProducts()
    }
}

