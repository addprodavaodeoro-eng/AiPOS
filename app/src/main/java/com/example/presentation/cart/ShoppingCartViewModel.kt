package com.example.presentation.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.domain.model.CartItem
import com.example.domain.model.ProductItem
import com.example.domain.model.Receipt
import com.example.domain.model.ReceiptItem
import com.example.domain.repository.IProductRepository
import com.example.domain.repository.ITransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * UI State representing the active Shopping Cart session.
 */
data class ShoppingCartUiState(
    val cartItems: Map<String, CartItem> = emptyMap(),
    val customerName: String = "",
    val discountPercent: Double = 0.0,
    val paymentAmountReceived: Double = 0.0,
    val isFinalizingSale: Boolean = false,
    val completedReceipt: Receipt? = null,
    val userMessage: String? = null,
    val errorMessage: String? = null
) {
    val itemsList: List<CartItem>
        get() = cartItems.values.toList()

    val totalItemCount: Int
        get() = cartItems.values.sumOf { it.quantity }

    val subtotal: Double
        get() = cartItems.values.sumOf { it.subtotal }

    val discountAmount: Double
        get() = (subtotal * (discountPercent / 100.0)).coerceAtLeast(0.0)

    val grandTotal: Double
        get() = (subtotal - discountAmount).coerceAtLeast(0.0)

    val changeAmount: Double
        get() = (paymentAmountReceived - grandTotal).coerceAtLeast(0.0)

    val isCartEmpty: Boolean
        get() = cartItems.isEmpty()

    val canCheckout: Boolean
        get() = cartItems.isNotEmpty() && paymentAmountReceived >= grandTotal
}

/**
 * ViewModel managing the active shopping cart session, stock validation, and sale finalization.
 */
class ShoppingCartViewModel(
    private val productRepository: IProductRepository,
    private val transactionRepository: ITransactionRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShoppingCartUiState())
    val uiState: StateFlow<ShoppingCartUiState> = _uiState.asStateFlow()

    /**
     * Add a product to the cart session or increment quantity if already present.
     */
    fun addProductToCart(product: ProductItem, quantity: Int = 1) {
        if (product.isOutOfStock) {
            _uiState.update { it.copy(errorMessage = "'${product.name}' is out of stock.") }
            return
        }

        _uiState.update { state ->
            val existing = state.cartItems[product.id]
            val currentQty = existing?.quantity ?: 0
            val requestedQty = currentQty + quantity

            if (requestedQty > product.stockQuantity) {
                state.copy(
                    errorMessage = "Cannot add more. Only ${product.stockQuantity} ${product.unit} available in stock."
                )
            } else {
                val updatedMap = state.cartItems.toMutableMap()
                updatedMap[product.id] = CartItem(product = product, quantity = requestedQty)
                state.copy(
                    cartItems = updatedMap,
                    userMessage = "Added ${product.name} to cart"
                )
            }
        }
    }

    /**
     * Update the exact quantity of an item in the cart.
     */
    fun updateQuantity(productId: String, quantity: Int) {
        _uiState.update { state ->
            val existing = state.cartItems[productId] ?: return@update state
            if (quantity <= 0) {
                val updatedMap = state.cartItems.toMutableMap()
                updatedMap.remove(productId)
                state.copy(cartItems = updatedMap)
            } else if (quantity > existing.product.stockQuantity) {
                state.copy(
                    errorMessage = "Only ${existing.product.stockQuantity} ${existing.product.unit} in stock."
                )
            } else {
                val updatedMap = state.cartItems.toMutableMap()
                updatedMap[productId] = existing.copy(quantity = quantity)
                state.copy(cartItems = updatedMap)
            }
        }
    }

    /**
     * Increment item quantity by 1.
     */
    fun incrementItem(productId: String) {
        val current = _uiState.value.cartItems[productId] ?: return
        addProductToCart(current.product, 1)
    }

    /**
     * Decrement item quantity by 1, removing if it reaches 0.
     */
    fun decrementItem(productId: String) {
        val current = _uiState.value.cartItems[productId] ?: return
        updateQuantity(productId, current.quantity - 1)
    }

    /**
     * Remove an item from the cart entirely.
     */
    fun removeItem(productId: String) {
        _uiState.update { state ->
            val updatedMap = state.cartItems.toMutableMap()
            val removed = updatedMap.remove(productId)
            state.copy(
                cartItems = updatedMap,
                userMessage = removed?.let { "Removed ${it.product.name}" }
            )
        }
    }

    /**
     * Clear all items in the active cart session.
     */
    fun clearCart() {
        _uiState.update {
            it.copy(
                cartItems = emptyMap(),
                paymentAmountReceived = 0.0,
                discountPercent = 0.0,
                customerName = "",
                errorMessage = null
            )
        }
    }

    /**
     * Update customer name for the receipt.
     */
    fun setCustomerName(name: String) {
        _uiState.update { it.copy(customerName = name) }
    }

    /**
     * Set a percentage discount (e.g. 5.0, 10.0, 20.0 for Senior/PWD).
     */
    fun setDiscountPercent(discount: Double) {
        _uiState.update { it.copy(discountPercent = discount.coerceIn(0.0, 100.0)) }
    }

    /**
     * Update cash/payment amount received from the customer.
     */
    fun setPaymentAmountReceived(amount: Double) {
        _uiState.update { it.copy(paymentAmountReceived = amount.coerceAtLeast(0.0)) }
    }

    /**
     * Finalize sale transaction:
     * 1. Deducts purchased quantities from local Room database inventory.
     * 2. Generates digital receipt.
     * 3. Clears temporary cart session.
     */
    fun finalizeSale(
        storeName: String = "Aling Nena's Sari-Sari Store",
        cashierName: String = "Admin / Cashier",
        paymentMethod: String = "Cash",
        onSaleCompleted: (Receipt) -> Unit = {}
    ) {
        val state = _uiState.value
        if (state.cartItems.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Cart is empty. Add products before finalizing.") }
            return
        }

        if (paymentMethod.equals("Cash", ignoreCase = true) && state.paymentAmountReceived < state.grandTotal) {
            _uiState.update { it.copy(errorMessage = "Cash received is less than total amount due.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isFinalizingSale = true) }
            try {
                // Deduct items from Room Inventory
                val soldItems = state.cartItems.values.toList()
                productRepository.deductStockForSale(soldItems)

                // Generate Digital Receipt
                val receiptNumber = "RCP-" + SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
                val receipt = Receipt(
                    receiptNumber = receiptNumber,
                    timestamp = System.currentTimeMillis(),
                    items = soldItems.map {
                        ReceiptItem(
                            id = it.product.id,
                            name = it.product.name,
                            unitPrice = it.product.price,
                            quantity = it.quantity,
                            subtotal = it.subtotal,
                            unit = it.product.unit
                        )
                    },
                    subtotal = state.subtotal,
                    discount = state.discountAmount,
                    tax = 0.0,
                    totalAmount = state.grandTotal,
                    paymentMethod = paymentMethod,
                    amountTendered = if (state.paymentAmountReceived > 0) state.paymentAmountReceived else state.grandTotal,
                    change = state.changeAmount,
                    cashierName = cashierName,
                    storeName = storeName
                )

                // Persist to Room Transaction Logs if repository provided
                transactionRepository?.recordTransaction(receipt)

                _uiState.update {
                    it.copy(
                        cartItems = emptyMap(),
                        paymentAmountReceived = 0.0,
                        discountPercent = 0.0,
                        customerName = "",
                        isFinalizingSale = false,
                        completedReceipt = receipt,
                        userMessage = "Sale finalized successfully!"
                    )
                }
                onSaleCompleted(receipt)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isFinalizingSale = false,
                        errorMessage = "Failed to complete sale: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    /**
     * Dismiss receipt dialog after review or print.
     */
    fun dismissReceipt() {
        _uiState.update { it.copy(completedReceipt = null) }
    }

    /**
     * Dismiss message banners.
     */
    fun dismissMessages() {
        _uiState.update { it.copy(userMessage = null, errorMessage = null) }
    }
}

/**
 * Factory for creating ShoppingCartViewModel.
 */
class ShoppingCartViewModelFactory(
    private val productRepository: IProductRepository,
    private val transactionRepository: ITransactionRepository? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ShoppingCartViewModel::class.java)) {
            return ShoppingCartViewModel(productRepository, transactionRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
