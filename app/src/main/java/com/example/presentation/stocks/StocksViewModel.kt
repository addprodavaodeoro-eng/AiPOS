package com.example.presentation.stocks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.domain.model.ProductCategory
import com.example.domain.model.ProductItem
import com.example.domain.repository.ProductRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class StocksKpiSummary(
    val totalProductCount: Int = 0,
    val totalStockUnits: Int = 0,
    val lowStockCount: Int = 0,
    val outOfStockCount: Int = 0,
    val totalRetailValue: Double = 0.0,
    val totalCostValue: Double = 0.0
) {
    val formattedTotalRetailValue: String
        get() = "₱${String.format("%,.2f", totalRetailValue)}"

    val formattedTotalCostValue: String
        get() = "₱${String.format("%,.2f", totalCostValue)}"

    val potentialProfit: Double
        get() = totalRetailValue - totalCostValue

    val formattedPotentialProfit: String
        get() = "₱${String.format("%,.2f", potentialProfit)}"
}

data class StocksUiState(
    val products: List<ProductItem> = emptyList(),
    val filteredProducts: List<ProductItem> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: ProductCategory = ProductCategory.ALL,
    val filterLowStockOnly: Boolean = false,
    val kpi: StocksKpiSummary = StocksKpiSummary(),
    val isLoading: Boolean = false,
    val userMessage: String? = null
)

class StocksViewModel(
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategory = MutableStateFlow(ProductCategory.ALL)
    private val _filterLowStockOnly = MutableStateFlow(false)
    private val _userMessage = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch {
            productRepository.seedIfEmpty()
        }
    }

    val uiState: StateFlow<StocksUiState> = combine(
        productRepository.allProducts,
        _searchQuery,
        _selectedCategory,
        _filterLowStockOnly,
        _userMessage
    ) { products, query, category, lowStockOnly, message ->
        val filtered = products.filter { item ->
            val matchesCategory = (category == ProductCategory.ALL) || (item.category == category)
            val matchesSearch = query.isBlank() ||
                    item.name.contains(query, ignoreCase = true) ||
                    (item.barcode != null && item.barcode.contains(query, ignoreCase = true))
            val matchesLowStock = !lowStockOnly || (item.isLowStock || item.isOutOfStock)

            matchesCategory && matchesSearch && matchesLowStock
        }

        val kpi = StocksKpiSummary(
            totalProductCount = products.size,
            totalStockUnits = products.sumOf { it.stockQuantity },
            lowStockCount = products.count { it.isLowStock },
            outOfStockCount = products.count { it.isOutOfStock },
            totalRetailValue = products.sumOf { it.price * it.stockQuantity },
            totalCostValue = products.sumOf { it.costPrice * it.stockQuantity }
        )

        StocksUiState(
            products = products,
            filteredProducts = filtered,
            searchQuery = query,
            selectedCategory = category,
            filterLowStockOnly = lowStockOnly,
            kpi = kpi,
            isLoading = false,
            userMessage = message
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = StocksUiState(isLoading = true)
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onCategorySelected(category: ProductCategory) {
        _selectedCategory.value = category
    }

    fun toggleLowStockFilter() {
        _filterLowStockOnly.value = !_filterLowStockOnly.value
    }

    fun clearFilters() {
        _searchQuery.value = ""
        _selectedCategory.value = ProductCategory.ALL
        _filterLowStockOnly.value = false
    }

    fun adjustStock(productId: String, delta: Int) {
        viewModelScope.launch {
            productRepository.adjustStock(productId, delta)
        }
    }

    fun restockProduct(productId: String, addQuantity: Int) {
        if (addQuantity <= 0) return
        viewModelScope.launch {
            productRepository.adjustStock(productId, addQuantity)
            _userMessage.value = "Stock updated (+ $addQuantity)"
        }
    }

    fun updateStockExact(productId: String, newQuantity: Int) {
        viewModelScope.launch {
            productRepository.updateStock(productId, maxOf(0, newQuantity))
            _userMessage.value = "Stock updated"
        }
    }

    fun saveProduct(product: ProductItem) {
        viewModelScope.launch {
            productRepository.addProduct(product)
            _userMessage.value = "${product.name} saved"
        }
    }

    fun deleteProduct(productId: String) {
        viewModelScope.launch {
            productRepository.deleteProduct(productId)
            _userMessage.value = "Product deleted"
        }
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }
}

class StocksViewModelFactory(
    private val productRepository: ProductRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StocksViewModel::class.java)) {
            return StocksViewModel(productRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
