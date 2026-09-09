package com.example.domain.model

import java.util.Locale

enum class TrendTimeframe(val label: String, val title: String) {
    DAILY("Daily", "Hourly Sales Today"),
    WEEKLY("Weekly", "7-Day Sales Trend")
}

data class ChartPoint(
    val id: String,
    val label: String,
    val fullLabel: String,
    val value: Double,
    val orderCount: Int,
    val profit: Double = value * 0.28 // estimated profit margin
) {
    val formattedValue: String
        get() = "₱${String.format(Locale.US, "%,.2f", value)}"

    val formattedCompactValue: String
        get() = if (value >= 1000) "₱${String.format(Locale.US, "%.1fk", value / 1000)}" else "₱${value.toInt()}"
}

data class SalesTrendSummary(
    val timeframe: TrendTimeframe,
    val totalRevenue: Double,
    val totalOrders: Int,
    val totalProfit: Double,
    val averageOrderValue: Double,
    val highestPoint: ChartPoint?,
    val lowestPoint: ChartPoint?,
    val growthPercentage: Double,
    val points: List<ChartPoint>
) {
    val formattedRevenue: String
        get() = "₱${String.format(Locale.US, "%,.2f", totalRevenue)}"

    val formattedProfit: String
        get() = "₱${String.format(Locale.US, "%,.2f", totalProfit)}"

    val formattedAov: String
        get() = "₱${String.format(Locale.US, "%,.2f", averageOrderValue)}"
}
