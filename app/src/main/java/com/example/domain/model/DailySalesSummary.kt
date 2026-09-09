package com.example.domain.model

import com.example.data.local.entity.TransactionLogEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PaymentBreakdown(
    val paymentMethod: String,
    val totalAmount: Double,
    val count: Int,
    val percentage: Float
) {
    val formattedTotal: String
        get() = "₱${String.format(Locale.US, "%,.2f", totalAmount)}"
}

data class HourlySalesBucket(
    val hourLabel: String, // e.g. "8 AM - 10 AM"
    val totalAmount: Double,
    val count: Int
)

data class DailySalesSummary(
    val dateString: String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()),
    val displayDate: String = SimpleDateFormat("MMMM dd, yyyy", Locale.US).format(Date()),
    val totalGrossSales: Double = 0.0,
    val totalNetSales: Double = 0.0,
    val totalDiscount: Double = 0.0,
    val totalTransactions: Int = 0,
    val totalItemsSold: Int = 0,
    val averageTicketSize: Double = 0.0,
    val paymentBreakdowns: List<PaymentBreakdown> = emptyList(),
    val hourlyBreakdowns: List<HourlySalesBucket> = emptyList(),
    val transactions: List<TransactionLogEntity> = emptyList()
) {
    val formattedGrossSales: String
        get() = "₱${String.format(Locale.US, "%,.2f", totalGrossSales)}"

    val formattedNetSales: String
        get() = "₱${String.format(Locale.US, "%,.2f", totalNetSales)}"

    val formattedDiscount: String
        get() = "₱${String.format(Locale.US, "%,.2f", totalDiscount)}"

    val formattedAverageTicket: String
        get() = "₱${String.format(Locale.US, "%,.2f", averageTicketSize)}"
}
