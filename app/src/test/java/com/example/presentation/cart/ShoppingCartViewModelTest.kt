package com.example.presentation.cart

import com.example.data.local.dao.ProductDao
import com.example.data.local.entity.ProductEntity
import com.example.domain.model.CartItem
import com.example.domain.model.ProductCategory
import com.example.domain.model.ProductItem
import com.example.domain.repository.IProductRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeProductRepository : IProductRepository {
    private val productsFlow = MutableStateFlow<List<ProductItem>>(emptyList())
    var deductedItems: List<CartItem> = emptyList()

    fun setProducts(items: List<ProductItem>) {
        productsFlow.value = items
    }

    override val allProducts: Flow<List<ProductItem>> = productsFlow
    override val lowStockProducts: Flow<List<ProductItem>> = flowOf(emptyList())

    override suspend fun getProductById(productId: String): ProductItem? =
        productsFlow.value.find { it.id == productId }

    fun getProductByBarcode(barcode: String): ProductItem? =
        productsFlow.value.find { it.barcode == barcode }

    override suspend fun searchProducts(query: String, category: ProductCategory): Flow<List<ProductItem>> =
        flowOf(emptyList())

    override suspend fun getProductCount(): Int = productsFlow.value.size

    override suspend fun seedIfEmpty() {}

    override suspend fun addProduct(product: ProductItem) {
        productsFlow.value = productsFlow.value + product
    }

    override suspend fun addProducts(products: List<ProductItem>) {
        productsFlow.value = productsFlow.value + products
    }

    override suspend fun updateProduct(product: ProductItem) {
        productsFlow.value = productsFlow.value.map { if (it.id == product.id) product else it }
    }

    override suspend fun updateStock(productId: String, newQuantity: Int) {
        productsFlow.value = productsFlow.value.map {
            if (it.id == productId) it.copy(stockQuantity = newQuantity) else it
        }
    }

    override suspend fun adjustStock(productId: String, delta: Int) {
        productsFlow.value = productsFlow.value.map {
            if (it.id == productId) it.copy(stockQuantity = (it.stockQuantity + delta).coerceAtLeast(0)) else it
        }
    }

    fun deductStock(id: String, quantitySold: Int) {
        productsFlow.value = productsFlow.value.map {
            if (it.id == id) it.copy(stockQuantity = (it.stockQuantity - quantitySold).coerceAtLeast(0)) else it
        }
    }

    override suspend fun deductStockForSale(cartItems: List<CartItem>) {
        deductedItems = cartItems
        cartItems.forEach { item ->
            deductStock(item.product.id, item.quantity)
        }
    }

    override suspend fun deleteProduct(productId: String) {
        productsFlow.value = productsFlow.value.filterNot { it.id == productId }
    }

    override suspend fun deleteAllProducts() {
        productsFlow.value = emptyList()
    }
}

class ShoppingCartViewModelTest {

    private lateinit var fakeRepository: FakeProductRepository
    private lateinit var viewModel: ShoppingCartViewModel

    private val sampleProduct1 = ProductItem(
        id = "prod-1",
        name = "Kopiko Blanca 30g",
        price = 15.0,
        category = ProductCategory.BEVERAGES,
        stockQuantity = 20,
        lowStockThreshold = 5
    )

    private val sampleProduct2 = ProductItem(
        id = "prod-2",
        name = "Piattos Cheese 85g",
        price = 38.0,
        category = ProductCategory.SNACKS,
        stockQuantity = 10,
        lowStockThreshold = 3
    )

    private val outOfStockProduct = ProductItem(
        id = "prod-3",
        name = "Lucky Me Pancit Canton",
        price = 20.0,
        category = ProductCategory.INSTANT_MEALS,
        stockQuantity = 0,
        lowStockThreshold = 5
    )

    @Before
    fun setup() {
        fakeRepository = FakeProductRepository()
        fakeRepository.setProducts(listOf(sampleProduct1, sampleProduct2, outOfStockProduct))
        viewModel = ShoppingCartViewModel(fakeRepository)
    }

    @Test
    fun addProductToCart_incrementsQuantityAndCalculatesTotal() {
        viewModel.addProductToCart(sampleProduct1, 2)

        val state = viewModel.uiState.value
        assertEquals(1, state.cartItems.size)
        assertEquals(2, state.totalItemCount)
        assertEquals(30.0, state.subtotal, 0.001)
        assertEquals(30.0, state.grandTotal, 0.001)
    }

    @Test
    fun addProductToCart_outOfStock_setsErrorMessage() {
        viewModel.addProductToCart(outOfStockProduct, 1)

        val state = viewModel.uiState.value
        assertTrue(state.isCartEmpty)
        assertNotNull(state.errorMessage)
    }

    @Test
    fun addProductToCart_exceedingStock_blocksExcessAddition() {
        viewModel.addProductToCart(sampleProduct2, 10)
        assertEquals(10, viewModel.uiState.value.totalItemCount)

        // Try adding 1 more beyond stock of 10
        viewModel.addProductToCart(sampleProduct2, 1)
        assertEquals(10, viewModel.uiState.value.totalItemCount)
        assertNotNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun updateQuantity_decrementAndRemove() {
        viewModel.addProductToCart(sampleProduct1, 2)
        viewModel.decrementItem(sampleProduct1.id)

        assertEquals(1, viewModel.uiState.value.totalItemCount)

        viewModel.decrementItem(sampleProduct1.id)
        assertTrue(viewModel.uiState.value.isCartEmpty)
    }

    @Test
    fun discountCalculation_appliesPercentageAccurately() {
        viewModel.addProductToCart(sampleProduct1, 2) // 30.0
        viewModel.addProductToCart(sampleProduct2, 1) // 38.0 -> total 68.0

        viewModel.setDiscountPercent(10.0)

        val state = viewModel.uiState.value
        assertEquals(68.0, state.subtotal, 0.001)
        assertEquals(6.80, state.discountAmount, 0.001)
        assertEquals(61.20, state.grandTotal, 0.001)
    }

    @Test
    fun finalizeSale_deductsStockAndGeneratesReceipt() = runTest {
        viewModel.addProductToCart(sampleProduct1, 3) // 45.0
        viewModel.setPaymentAmountReceived(50.0)

        var completedReceiptId: String? = null
        viewModel.finalizeSale(
            storeName = "Test Store",
            paymentMethod = "Cash",
            onSaleCompleted = { receipt ->
                completedReceiptId = receipt.receiptNumber
            }
        )

        val state = viewModel.uiState.value
        assertTrue(state.isCartEmpty)
        assertNotNull(state.completedReceipt)
        assertNotNull(completedReceiptId)
        assertEquals(1, fakeRepository.deductedItems.size)
        assertEquals(3, fakeRepository.deductedItems.first().quantity)
    }
}
