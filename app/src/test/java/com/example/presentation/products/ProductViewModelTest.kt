package com.example.presentation.products

import com.example.domain.model.ProductCategory
import com.example.domain.model.ProductItem
import com.example.domain.repository.FakeProductDao
import com.example.domain.repository.ProductRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProductViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeDao: FakeProductDao
    private lateinit var repository: ProductRepository
    private lateinit var viewModel: ProductViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeDao = FakeProductDao()
        repository = ProductRepository(fakeDao)
        viewModel = ProductViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun addProduct_insertsItemAndEmitsInState() = runTest {
        testScheduler.advanceUntilIdle()

        viewModel.addProduct(
            name = "Piattos Cheese 85g",
            price = 38.0,
            costPrice = 30.0,
            category = ProductCategory.SNACKS,
            stockQuantity = 25,
            lowStockThreshold = 5
        )
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertEquals(1, state.products.size)
        assertEquals("Piattos Cheese 85g", state.products.first().name)
        assertEquals(38.0, state.products.first().price, 0.001)
        assertEquals(ProductCategory.SNACKS, state.products.first().category)
        assertEquals(25, state.products.first().stockQuantity)
    }

    @Test
    fun updateStockLevel_updatesInventoryCount() = runTest {
        val prod = ProductItem(
            id = "prod_1",
            name = "Bear Brand 33g",
            price = 15.0,
            costPrice = 11.0,
            category = ProductCategory.BEVERAGES,
            stockQuantity = 10
        )
        repository.addProduct(prod)
        testScheduler.advanceUntilIdle()

        viewModel.updateStockLevel("prod_1", 45)
        testScheduler.advanceUntilIdle()

        val updated = repository.getProductById("prod_1")
        assertEquals(45, updated?.stockQuantity)
    }

    @Test
    fun deleteProduct_removesItemFromState() = runTest {
        val prod = ProductItem(
            id = "prod_del",
            name = "Old Stock",
            price = 10.0,
            costPrice = 7.0,
            category = ProductCategory.HOUSEHOLD,
            stockQuantity = 5
        )
        repository.addProduct(prod)
        testScheduler.advanceUntilIdle()

        viewModel.deleteProduct("prod_del")
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertTrue(state.products.isEmpty())
        assertNull(repository.getProductById("prod_del"))
    }

    @Test
    fun searchAndFilter_updatesFilteredProductsCorrectly() = runTest {
        val p1 = ProductItem(id = "1", name = "Coca Cola 1.5L", price = 75.0, category = ProductCategory.BEVERAGES, stockQuantity = 20)
        val p2 = ProductItem(id = "2", name = "Royal Tru-Orange 1.5L", price = 70.0, category = ProductCategory.BEVERAGES, stockQuantity = 15)
        val p3 = ProductItem(id = "3", name = "Chippy BBQ 110g", price = 28.0, category = ProductCategory.SNACKS, stockQuantity = 30)
        repository.addProducts(listOf(p1, p2, p3))
        testScheduler.advanceUntilIdle()

        // Filter by Category
        viewModel.onCategorySelected(ProductCategory.BEVERAGES)
        testScheduler.advanceUntilIdle()

        var state = viewModel.uiState.first()
        assertEquals(2, state.filteredProducts.size)

        // Filter by Query
        viewModel.onSearchQueryChanged("Royal")
        testScheduler.advanceUntilIdle()

        state = viewModel.uiState.first()
        assertEquals(1, state.filteredProducts.size)
        assertEquals("Royal Tru-Orange 1.5L", state.filteredProducts.first().name)
    }
}
