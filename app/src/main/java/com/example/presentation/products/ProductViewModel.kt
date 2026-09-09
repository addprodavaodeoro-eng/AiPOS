package com.example.presentation.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.domain.barcode.BarcodeUtils
import com.example.domain.model.ProductCategory
import com.example.domain.model.ProductItem
import com.example.domain.repository.IProductRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * UI State representing the product inventory screen.
 */
data class ProductListUiState(
    val products: List<ProductItem> = emptyList(),
    val filteredProducts: List<ProductItem> = emptyList(),
    val lowStockProducts: List<ProductItem> = emptyList(),
    val selectedCategory: ProductCategory = ProductCategory.ALL,
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val totalProductCount: Int = 0,
    val lowStockCount: Int = 0,
    val outOfStockCount: Int = 0,
    val totalInventoryValue: Double = 0.0,
    val userMessage: String? = null,
    val error: String? = null
)

/**
 * ViewModel for managing product inventory state and handling CRUD operations.
 */
class ProductViewModel(
    private val productRepository: IProductRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow(ProductCategory.ALL)
    val selectedCategory: StateFlow<ProductCategory> = _selectedCategory.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val uiState: StateFlow<ProductListUiState> = combine(
        productRepository.allProducts,
        productRepository.lowStockProducts,
        _searchQuery,
        _selectedCategory,
        combine(_userMessage, _error) { msg, err -> Pair(msg, err) }
    ) { allProducts, lowStockList, query, category, (msg, err) ->
        val filtered = allProducts.filter { item ->
            val matchesCategory = (category == ProductCategory.ALL) || (item.category == category)
            val matchesQuery = query.isBlank() ||
                    item.name.contains(query, ignoreCase = true) ||
                    (item.barcode != null && item.barcode.contains(query, ignoreCase = true))
            matchesCategory && matchesQuery
        }

        val lowStock = allProducts.count { it.isLowStock }
        val outOfStock = allProducts.count { it.isOutOfStock }
        val inventoryValue = allProducts.sumOf { it.price * it.stockQuantity }

        ProductListUiState(
            products = allProducts,
            filteredProducts = filtered,
            lowStockProducts = lowStockList,
            selectedCategory = category,
            searchQuery = query,
            isLoading = false,
            totalProductCount = allProducts.size,
            lowStockCount = lowStock,
            outOfStockCount = outOfStock,
            totalInventoryValue = inventoryValue,
            userMessage = msg,
            error = err
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ProductListUiState(isLoading = true)
    )

    init {
        viewModelScope.launch {
            productRepository.seedIfEmpty()
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onCategorySelected(category: ProductCategory) {
        _selectedCategory.value = category
    }

    /**
     * Add a new inventory product.
     */
    fun addProduct(
        name: String,
        price: Double,
        costPrice: Double = 0.0,
        category: ProductCategory = ProductCategory.SNACKS,
        stockQuantity: Int = 0,
        lowStockThreshold: Int = 5,
        barcode: String? = null,
        unit: String = "pc"
    ) {
        if (name.isBlank()) {
            _error.value = "Product name cannot be empty"
            return
        }
        if (price < 0) {
            _error.value = "Price cannot be negative"
            return
        }

        viewModelScope.launch {
            try {
                val newProduct = ProductItem(
                    id = "prod_${UUID.randomUUID()}",
                    name = name.trim(),
                    price = price,
                    costPrice = costPrice,
                    category = category,
                    stockQuantity = stockQuantity,
                    lowStockThreshold = lowStockThreshold,
                    barcode = barcode?.trim()?.ifBlank { null } ?: BarcodeUtils.generateRandomBarcode(),
                    unit = unit.trim().ifBlank { "pc" }
                )
                productRepository.addProduct(newProduct)
                _userMessage.value = "Product '${newProduct.name}' added successfully"
            } catch (e: Exception) {
                _error.value = "Failed to add product: ${e.localizedMessage}"
            }
        }
    }

    /**
     * Add an existing domain ProductItem.
     */
    fun addProductItem(product: ProductItem) {
        viewModelScope.launch {
            try {
                productRepository.addProduct(product)
                _userMessage.value = "Product '${product.name}' added successfully"
            } catch (e: Exception) {
                _error.value = "Failed to add product: ${e.localizedMessage}"
            }
        }
    }

    /**
     * Update an existing product.
     */
    fun updateProduct(product: ProductItem) {
        viewModelScope.launch {
            try {
                productRepository.updateProduct(product)
                _userMessage.value = "Product '${product.name}' updated successfully"
            } catch (e: Exception) {
                _error.value = "Failed to update product: ${e.localizedMessage}"
            }
        }
    }

    /**
     * Update the exact stock level for a product.
     */
    fun updateStockLevel(productId: String, newQuantity: Int) {
        viewModelScope.launch {
            try {
                productRepository.updateStock(productId, maxOf(0, newQuantity))
                _userMessage.value = "Stock updated successfully"
            } catch (e: Exception) {
                _error.value = "Failed to update stock: ${e.localizedMessage}"
            }
        }
    }

    /**
     * Increment or decrement stock quantity for a product.
     */
    fun adjustStock(productId: String, delta: Int) {
        viewModelScope.launch {
            try {
                productRepository.adjustStock(productId, delta)
            } catch (e: Exception) {
                _error.value = "Failed to adjust stock: ${e.localizedMessage}"
            }
        }
    }

    /**
     * Delete a product from inventory.
     */
    fun deleteProduct(productId: String) {
        viewModelScope.launch {
            try {
                productRepository.deleteProduct(productId)
                _userMessage.value = "Product deleted successfully"
            } catch (e: Exception) {
                _error.value = "Failed to delete product: ${e.localizedMessage}"
            }
        }
    }

    /**
     * Clear active message or error notification.
     */
    fun dismissMessage() {
        _userMessage.value = null
        _error.value = null
    }
}

/**
 * Factory for creating instances of ProductViewModel.
 */
class ProductViewModelFactory(
    private val productRepository: IProductRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProductViewModel::class.java)) {
            return ProductViewModel(productRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
