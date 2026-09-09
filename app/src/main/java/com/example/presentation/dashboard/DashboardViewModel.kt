package com.example.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.ChartPoint
import com.example.domain.model.SalesTrendSummary
import com.example.domain.model.TrendTimeframe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DashboardUiState(
    val todayRevenue: Double = 3842.50,
    val todayNetProfit: Double = 1120.00,
    val todayTransactionsCount: Int = 42,
    val dailyTrend: SalesTrendSummary = sampleDailyTrend(),
    val weeklyTrend: SalesTrendSummary = sampleWeeklyTrend()
)

class DashboardViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadSalesData()
    }

    private fun loadSalesData() {
        viewModelScope.launch {
            val daily = sampleDailyTrend()
            val weekly = sampleWeeklyTrend()
            _uiState.value = DashboardUiState(
                todayRevenue = daily.totalRevenue,
                todayNetProfit = daily.totalProfit,
                todayTransactionsCount = daily.totalOrders,
                dailyTrend = daily,
                weeklyTrend = weekly
            )
        }
    }
}

fun sampleDailyTrend(): SalesTrendSummary {
    val points = listOf(
        ChartPoint("h1", "6 AM", "6:00 AM - Opening", 210.00, 3),
        ChartPoint("h2", "8 AM", "8:00 AM - Breakfast Rush", 480.50, 7),
        ChartPoint("h3", "10 AM", "10:00 AM - Mid Morning", 320.00, 4),
        ChartPoint("h4", "12 PM", "12:00 PM - Lunch Peak", 890.00, 11),
        ChartPoint("h5", "2 PM", "2:00 PM - Afternoon Merienda", 540.00, 6),
        ChartPoint("h6", "4 PM", "4:00 PM - School Dismissal", 762.00, 9),
        ChartPoint("h7", "6 PM", "6:00 PM - Dinner Rush", 640.00, 8)
    )
    val totalRevenue = points.sumOf { it.value }
    val totalOrders = points.sumOf { it.orderCount }
    val totalProfit = points.sumOf { it.profit }
    val aov = if (totalOrders > 0) totalRevenue / totalOrders else 0.0
    val highest = points.maxByOrNull { it.value }
    val lowest = points.minByOrNull { it.value }

    return SalesTrendSummary(
        timeframe = TrendTimeframe.DAILY,
        totalRevenue = totalRevenue,
        totalOrders = totalOrders,
        totalProfit = totalProfit,
        averageOrderValue = aov,
        highestPoint = highest,
        lowestPoint = lowest,
        growthPercentage = 12.4,
        points = points
    )
}

fun sampleWeeklyTrend(): SalesTrendSummary {
    val points = listOf(
        ChartPoint("d1", "Mon", "Monday, Aug 31", 3420.00, 38),
        ChartPoint("d2", "Tue", "Tuesday, Sep 01", 3890.50, 41),
        ChartPoint("d3", "Wed", "Wednesday, Sep 02", 3150.00, 34),
        ChartPoint("d4", "Thu", "Thursday, Sep 03", 4210.00, 46),
        ChartPoint("d5", "Fri", "Friday, Sep 04", 5100.00, 58),
        ChartPoint("d6", "Sat", "Saturday, Sep 05 - Weekend Peak", 6450.00, 72),
        ChartPoint("d7", "Sun", "Sunday, Sep 06 - Today", 3842.50, 42)
    )
    val totalRevenue = points.sumOf { it.value }
    val totalOrders = points.sumOf { it.orderCount }
    val totalProfit = points.sumOf { it.profit }
    val aov = if (totalOrders > 0) totalRevenue / totalOrders else 0.0
    val highest = points.maxByOrNull { it.value }
    val lowest = points.minByOrNull { it.value }

    return SalesTrendSummary(
        timeframe = TrendTimeframe.WEEKLY,
        totalRevenue = totalRevenue,
        totalOrders = totalOrders,
        totalProfit = totalProfit,
        averageOrderValue = aov,
        highestPoint = highest,
        lowestPoint = lowest,
        growthPercentage = 18.2,
        points = points
    )
}
