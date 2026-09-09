package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@Entity(tableName = "transaction_logs")
data class TransactionLogEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val receiptNumber: String,
    val timestamp: Long = System.currentTimeMillis(),
    val dateString: String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(timestamp)),
    val totalAmount: Double,
    val subtotal: Double,
    val discountAmount: Double = 0.0,
    val taxAmount: Double = 0.0,
    val paymentMethod: String = "Cash", // Cash, GCash, Maya, Utang
    val amountTendered: Double = totalAmount,
    val changeAmount: Double = 0.0,
    val totalItemCount: Int = 1,
    val itemsSummary: String = "", // e.g. "2x Lucky Me Pancit Canton, 1x Coca-Cola 1.5L"
    val itemsJson: String = "", // Detailed serialized items JSON
    val cashierName: String = "Admin / Cashier",
    val customerName: String? = null,
    val notes: String? = null
) {
    val formattedTotal: String
        get() = "₱${String.format(Locale.US, "%,.2f", totalAmount)}"

    val formattedTime: String
        get() = SimpleDateFormat("hh:mm a", Locale.US).format(Date(timestamp))

    val formattedDate: String
        get() = SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date(timestamp))

    val formattedDateTime: String
        get() = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.US).format(Date(timestamp))
}
