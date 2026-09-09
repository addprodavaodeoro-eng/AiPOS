package com.example.domain.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ReceiptItem(
    val id: String,
    val name: String,
    val unitPrice: Double,
    val quantity: Int,
    val subtotal: Double,
    val unit: String = "pc"
) {
    val formattedUnitPrice: String
        get() = "₱${String.format(Locale.US, "%,.2f", unitPrice)}"

    val formattedSubtotal: String
        get() = "₱${String.format(Locale.US, "%,.2f", subtotal)}"
}

data class Receipt(
    val receiptNumber: String,
    val timestamp: Long = System.currentTimeMillis(),
    val items: List<ReceiptItem>,
    val subtotal: Double,
    val discount: Double = 0.0,
    val tax: Double = 0.0,
    val totalAmount: Double,
    val paymentMethod: String,
    val amountTendered: Double,
    val change: Double,
    val cashierName: String = "Admin / Cashier",
    val storeName: String = "AiPOS Store",
    val storeAddress: String = "Poblacion, Nabunturan, Davao de Oro",
    val storeContact: String = "Tel: (082) 555-0199"
) {
    val formattedDate: String
        get() = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.US).format(Date(timestamp))

    val formattedSubtotal: String
        get() = "₱${String.format(Locale.US, "%,.2f", subtotal)}"

    val formattedDiscount: String
        get() = "₱${String.format(Locale.US, "%,.2f", discount)}"

    val formattedTax: String
        get() = "₱${String.format(Locale.US, "%,.2f", tax)}"

    val formattedTotal: String
        get() = "₱${String.format(Locale.US, "%,.2f", totalAmount)}"

    val formattedTendered: String
        get() = "₱${String.format(Locale.US, "%,.2f", amountTendered)}"

    val formattedChange: String
        get() = "₱${String.format(Locale.US, "%,.2f", change)}"

    val totalItemCount: Int
        get() = items.sumOf { it.quantity }
}
