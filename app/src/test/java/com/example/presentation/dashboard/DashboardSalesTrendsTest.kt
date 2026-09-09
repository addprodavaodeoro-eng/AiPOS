package com.example.presentation.dashboard

import com.example.domain.model.TrendTimeframe
import org.junit.Assert.*
import org.junit.Test

class DashboardSalesTrendsTest {

    @Test
    fun sampleDailyTrend_containsExpectedHourlyPoints() {
        val daily = sampleDailyTrend()
        assertEquals(TrendTimeframe.DAILY, daily.timeframe)
        assertEquals(7, daily.points.size)
        assertTrue(daily.totalRevenue > 0.0)
        assertTrue(daily.totalOrders > 0)
        assertTrue(daily.averageOrderValue > 0.0)
        assertNotNull(daily.highestPoint)
        assertEquals("12 PM", daily.highestPoint?.label)
    }

    @Test
    fun sampleWeeklyTrend_containsExpected7DayPoints() {
        val weekly = sampleWeeklyTrend()
        assertEquals(TrendTimeframe.WEEKLY, weekly.timeframe)
        assertEquals(7, weekly.points.size)
        assertTrue(weekly.totalRevenue > dailyRevenueSum())
        assertNotNull(weekly.highestPoint)
        assertEquals("Sat", weekly.highestPoint?.label)
    }

    @Test
    fun chartPoint_formatsValuesAccurately() {
        val daily = sampleDailyTrend()
        val lunchPeak = daily.highestPoint
        assertNotNull(lunchPeak)
        assertEquals("₱890.00", lunchPeak?.formattedValue)
        assertEquals("₱890", lunchPeak?.formattedCompactValue)
    }

    private fun dailyRevenueSum(): Double {
        return sampleDailyTrend().totalRevenue
    }
}
