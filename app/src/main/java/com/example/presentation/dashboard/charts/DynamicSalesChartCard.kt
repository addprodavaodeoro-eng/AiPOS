package com.example.presentation.dashboard.charts

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.ChartPoint
import com.example.domain.model.SalesTrendSummary
import com.example.domain.model.TrendTimeframe
import com.example.ui.theme.*
import java.util.Locale
import kotlin.math.max

enum class ChartDisplayType {
    AREA_SPLINE,
    ROUNDED_BAR
}

/**
 * Dynamic Interactive Sales Chart Component for Daily & Weekly Trends.
 * Includes smooth bezier curve area fill, interactive touch inspection tooltips,
 * animated transitions, benchmark average lines, and time-frame toggle.
 */
@Composable
fun DynamicSalesChartCard(
    dailySummary: SalesTrendSummary,
    weeklySummary: SalesTrendSummary,
    modifier: Modifier = Modifier
) {
    var selectedTimeframe by remember { mutableStateOf(TrendTimeframe.DAILY) }
    var chartType by remember { mutableStateOf(ChartDisplayType.AREA_SPLINE) }
    var selectedPointIndex by remember { mutableStateOf<Int?>(null) }

    val activeSummary = when (selectedTimeframe) {
        TrendTimeframe.DAILY -> dailySummary
        TrendTimeframe.WEEKLY -> weeklySummary
    }

    // Reset selected point when timeframe switches
    LaunchedEffect(selectedTimeframe) {
        selectedPointIndex = null
    }

    val selectedPoint = selectedPointIndex?.let { idx ->
        activeSummary.points.getOrNull(idx)
    }

    // Animation progress for chart entry & changes
    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(selectedTimeframe, chartType) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
        )
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("dynamic_sales_chart_card"),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row: Title & Timeframe Selector Tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = TealLight,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.ShowChart,
                                    contentDescription = null,
                                    tint = TealPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        Text(
                            text = "SALES TRENDS",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp,
                            color = TealPrimary
                        )
                    }
                    Text(
                        text = activeSummary.timeframe.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                }

                // Timeframe Segmented Control (Daily vs Weekly)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Slate100,
                    modifier = Modifier.testTag("timeframe_selector")
                ) {
                    Row(modifier = Modifier.padding(3.dp)) {
                        TrendTimeframe.entries.forEach { tf ->
                            val isSelected = tf == selectedTimeframe
                            Surface(
                                shape = RoundedCornerShape(9.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
                                shadowElevation = if (isSelected) 2.dp else 0.dp,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(9.dp))
                                    .clickable { selectedTimeframe = tf }
                                    .testTag("tab_${tf.name.lowercase()}")
                            ) {
                                Text(
                                    text = tf.label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) TealPrimary else Slate600,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Metric Highlights Bar (Total, Trend Growth %, Chart Style Switch)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = activeSummary.formattedRevenue,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Slate900,
                        letterSpacing = (-0.5).sp
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val isPositive = activeSummary.growthPercentage >= 0
                        Surface(
                            color = if (isPositive) Emerald100 else Red100,
                            shape = CircleShape
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPositive) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                    contentDescription = null,
                                    tint = if (isPositive) Emerald700 else Red600,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "${if (isPositive) "+" else ""}${String.format(Locale.US, "%.1f", activeSummary.growthPercentage)}%",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPositive) Emerald700 else Red600
                                )
                            }
                        }
                        Text(
                            text = if (selectedTimeframe == TrendTimeframe.DAILY) "vs yesterday" else "vs previous week",
                            fontSize = 11.sp,
                            color = Slate500
                        )
                    }
                }

                // Chart Style Switcher (Spline vs Bar)
                Row(
                    modifier = Modifier
                        .background(Slate100, RoundedCornerShape(10.dp))
                        .padding(2.dp)
                ) {
                    IconButton(
                        onClick = { chartType = ChartDisplayType.AREA_SPLINE },
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                if (chartType == ChartDisplayType.AREA_SPLINE) MaterialTheme.colorScheme.surface else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .testTag("chart_type_line")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShowChart,
                            contentDescription = "Line Chart",
                            tint = if (chartType == ChartDisplayType.AREA_SPLINE) TealPrimary else Slate400,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = { chartType = ChartDisplayType.ROUNDED_BAR },
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                if (chartType == ChartDisplayType.ROUNDED_BAR) MaterialTheme.colorScheme.surface else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .testTag("chart_type_bar")
                    ) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = "Bar Chart",
                            tint = if (chartType == ChartDisplayType.ROUNDED_BAR) TealPrimary else Slate400,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Interactive Tooltip / Detail Panel
            if (selectedPoint != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Slate900,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("chart_inspection_tooltip")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = selectedPoint.fullLabel,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Slate400
                            )
                            Text(
                                text = selectedPoint.formattedValue,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Orders", fontSize = 10.sp, color = Slate400)
                                Text("${selectedPoint.orderCount}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Est. Profit", fontSize = 10.sp, color = Slate400)
                                Text("₱${String.format(Locale.US, "%,.2f", selectedPoint.profit)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Emerald200)
                            }
                        }
                    }
                }
            }

            // Canvas Chart Canvas
            val textMeasurer = rememberTextMeasurer()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .testTag("canvas_chart_area")
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(activeSummary.points) {
                            detectTapGestures { offset ->
                                val count = activeSummary.points.size
                                if (count > 0) {
                                    val paddingLeft = 32.dp.toPx()
                                    val paddingRight = 16.dp.toPx()
                                    val chartWidth = size.width - paddingLeft - paddingRight
                                    val stepX = chartWidth / (count - 1).coerceAtLeast(1)

                                    val tappedIndex = if (count == 1) 0 else {
                                        val rawIdx = ((offset.x - paddingLeft + stepX / 2) / stepX).toInt()
                                        rawIdx.coerceIn(0, count - 1)
                                    }
                                    selectedPointIndex = if (selectedPointIndex == tappedIndex) null else tappedIndex
                                }
                            }
                        }
                ) {
                    val points = activeSummary.points
                    if (points.isEmpty()) return@Canvas

                    val progress = animationProgress.value
                    val paddingLeft = 36.dp.toPx()
                    val paddingRight = 16.dp.toPx()
                    val paddingTop = 20.dp.toPx()
                    val paddingBottom = 30.dp.toPx()

                    val chartWidth = size.width - paddingLeft - paddingRight
                    val chartHeight = size.height - paddingTop - paddingBottom

                    val maxValue = max(points.maxOfOrNull { it.value } ?: 100.0, 100.0) * 1.15
                    val stepX = chartWidth / (points.size - 1).coerceAtLeast(1)

                    // Draw Horizontal Grid Lines & Y-Axis Reference Labels
                    val gridSteps = 3
                    for (i in 0..gridSteps) {
                        val gridY = paddingTop + (chartHeight / gridSteps) * i
                        val gridValue = maxValue - (maxValue / gridSteps) * i

                        // Dotted grid line
                        drawLine(
                            color = Slate200,
                            start = Offset(paddingLeft, gridY),
                            end = Offset(size.width - paddingRight, gridY),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                        )

                        // Y-axis label
                        val labelText = if (gridValue >= 1000) "${(gridValue / 1000).toInt()}k" else "${gridValue.toInt()}"
                        val textResult = textMeasurer.measure(
                            text = labelText,
                            style = TextStyle(fontSize = 9.sp, color = Slate400, fontWeight = FontWeight.Normal)
                        )
                        drawText(
                            textLayoutResult = textResult,
                            topLeft = Offset(paddingLeft - textResult.size.width - 6.dp.toPx(), gridY - textResult.size.height / 2)
                        )
                    }

                    if (chartType == ChartDisplayType.AREA_SPLINE) {
                        drawSplineChart(
                            points = points,
                            maxValue = maxValue,
                            chartWidth = chartWidth,
                            chartHeight = chartHeight,
                            paddingLeft = paddingLeft,
                            paddingTop = paddingTop,
                            stepX = stepX,
                            progress = progress,
                            selectedIndex = selectedPointIndex,
                            textMeasurer = textMeasurer
                        )
                    } else {
                        drawBarChart(
                            points = points,
                            maxValue = maxValue,
                            chartWidth = chartWidth,
                            chartHeight = chartHeight,
                            paddingLeft = paddingLeft,
                            paddingTop = paddingTop,
                            stepX = stepX,
                            progress = progress,
                            selectedIndex = selectedPointIndex,
                            textMeasurer = textMeasurer
                        )
                    }

                    // Draw X-Axis Labels
                    points.forEachIndexed { index, point ->
                        val x = paddingLeft + index * stepX
                        val textResult = textMeasurer.measure(
                            text = point.label,
                            style = TextStyle(
                                fontSize = 10.sp,
                                color = if (selectedPointIndex == index) TealPrimary else Slate500,
                                fontWeight = if (selectedPointIndex == index) FontWeight.Bold else FontWeight.Medium
                            )
                        )
                        drawText(
                            textLayoutResult = textResult,
                            topLeft = Offset(x - textResult.size.width / 2, size.height - paddingBottom + 8.dp.toPx())
                        )
                    }
                }
            }

            // Bottom Quick Stats Row: Peak Period, Average Transaction, Best Selling Day
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                QuickMetricItem(
                    label = "Average Sale",
                    value = activeSummary.formattedAov
                )
                QuickMetricItem(
                    label = "Total Orders",
                    value = "${activeSummary.totalOrders}"
                )
                QuickMetricItem(
                    label = "Peak Period",
                    value = activeSummary.highestPoint?.label ?: "N/A"
                )
                QuickMetricItem(
                    label = "Est. Profit",
                    value = activeSummary.formattedProfit,
                    valueColor = Emerald700
                )
            }
        }
    }
}

private fun DrawScope.drawSplineChart(
    points: List<ChartPoint>,
    maxValue: Double,
    chartWidth: Float,
    chartHeight: Float,
    paddingLeft: Float,
    paddingTop: Float,
    stepX: Float,
    progress: Float,
    selectedIndex: Int?,
    textMeasurer: TextMeasurer
) {
    if (points.isEmpty()) return

    val coordinates = points.mapIndexed { index, point ->
        val x = paddingLeft + index * stepX
        val normalizedValue = (point.value / maxValue).toFloat().coerceIn(0f, 1f)
        val y = paddingTop + chartHeight * (1f - normalizedValue * progress)
        Offset(x, y)
    }

    // Build Spline Path
    val path = Path().apply {
        moveTo(coordinates[0].x, coordinates[0].y)
        for (i in 0 until coordinates.size - 1) {
            val p0 = coordinates[i]
            val p1 = coordinates[i + 1]
            val controlPoint1 = Offset(p0.x + (p1.x - p0.x) / 2f, p0.y)
            val controlPoint2 = Offset(p0.x + (p1.x - p0.x) / 2f, p1.y)
            cubicTo(controlPoint1.x, controlPoint1.y, controlPoint2.x, controlPoint2.y, p1.x, p1.y)
        }
    }

    // Fill Gradient Area
    val fillPath = Path().apply {
        addPath(path)
        val lastPoint = coordinates.last()
        val firstPoint = coordinates.first()
        val groundY = paddingTop + chartHeight
        lineTo(lastPoint.x, groundY)
        lineTo(firstPoint.x, groundY)
        close()
    }

    drawPath(
        path = fillPath,
        brush = Brush.verticalGradient(
            colors = listOf(
                TealPrimary.copy(alpha = 0.35f),
                TealLight.copy(alpha = 0.15f),
                Color.Transparent
            ),
            startY = paddingTop,
            endY = paddingTop + chartHeight
        )
    )

    // Stroke the spline curve
    drawPath(
        path = path,
        color = TealPrimary,
        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
    )

    // Draw data points & selection indicators
    coordinates.forEachIndexed { index, offset ->
        val isSelected = selectedIndex == index
        val radius = if (isSelected) 6.dp.toPx() else 3.5.dp.toPx()

        if (isSelected) {
            // Selected Vertical Guide Line
            drawLine(
                color = TealPrimary.copy(alpha = 0.4f),
                start = Offset(offset.x, paddingTop),
                end = Offset(offset.x, paddingTop + chartHeight),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
            )

            // Outer highlight ring
            drawCircle(
                color = TealLight,
                radius = 11.dp.toPx(),
                center = offset
            )
        }

        // Inner solid dot
        drawCircle(
            color = if (isSelected) TealPrimary else Color.White,
            radius = radius,
            center = offset
        )
        drawCircle(
            color = TealPrimary,
            radius = radius,
            style = Stroke(width = 2.dp.toPx()),
            center = offset
        )
    }
}

private fun DrawScope.drawBarChart(
    points: List<ChartPoint>,
    maxValue: Double,
    chartWidth: Float,
    chartHeight: Float,
    paddingLeft: Float,
    paddingTop: Float,
    stepX: Float,
    progress: Float,
    selectedIndex: Int?,
    textMeasurer: TextMeasurer
) {
    val barWidth = (stepX * 0.52f).coerceIn(10.dp.toPx(), 28.dp.toPx())

    points.forEachIndexed { index, point ->
        val centerX = paddingLeft + index * stepX
        val normalizedValue = (point.value / maxValue).toFloat().coerceIn(0f, 1f)
        val barHeight = chartHeight * normalizedValue * progress
        val topY = paddingTop + chartHeight - barHeight
        val isSelected = selectedIndex == index

        // Background track
        drawRoundRect(
            color = Slate100,
            topLeft = Offset(centerX - barWidth / 2, paddingTop),
            size = Size(barWidth, chartHeight),
            cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
        )

        // Active Bar
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = if (isSelected) {
                    listOf(TealDark, TealPrimary)
                } else {
                    listOf(TealPrimary, TealLight)
                },
                startY = topY,
                endY = paddingTop + chartHeight
            ),
            topLeft = Offset(centerX - barWidth / 2, topY),
            size = Size(barWidth, barHeight.coerceAtLeast(4.dp.toPx())),
            cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
        )
    }
}

@Composable
private fun QuickMetricItem(
    label: String,
    value: String,
    valueColor: Color = Slate800
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 10.sp,
            color = Slate500,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}
