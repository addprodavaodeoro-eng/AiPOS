package com.example.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.presentation.common.ThemeToggleIconButton
import com.example.presentation.components.NetworkStatusIndicator
import com.example.presentation.dashboard.charts.DynamicSalesChartCard
import com.example.ui.theme.*

@Composable
fun DashboardScreen(
    onNavigateToPOS: () -> Unit = {},
    onNavigateToInventory: () -> Unit = {},
    onNavigateToMore: () -> Unit = {},
    onNavigateToSales: () -> Unit = {},
    viewModel: DashboardViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { DashboardTopBar() },
        bottomBar = { 
            DashboardBottomNav(
                onNavigateToPOS = onNavigateToPOS,
                onNavigateToInventory = onNavigateToInventory,
                onNavigateToMore = onNavigateToMore
            ) 
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item { Spacer(modifier = Modifier.height(2.dp)) }
            item {
                DynamicSalesChartCard(
                    dailySummary = uiState.dailyTrend,
                    weeklySummary = uiState.weeklyTrend
                )
            }
            item {
                StatsRow(
                    transactionsCount = uiState.todayTransactionsCount,
                    netProfitFormatted = "₱ ${String.format(java.util.Locale.US, "%,.2f", uiState.todayNetProfit)}"
                )
            }
            item { ActionRow(onNewSale = onNavigateToPOS) }
            item { RecentTransactionsList(onSeeAll = onNavigateToSales) }
            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}

@Composable
fun DashboardTopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .border(width = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "AiPOS",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = (-0.5).sp
            )
            Text(
                text = "ALING NENA'S SARI-SARI",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Slate400,
                letterSpacing = 1.sp
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            NetworkStatusIndicator()
            ThemeToggleIconButton()
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "AN",
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun MainBalanceCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(28.dp))
            .border(1.dp, TealCardBorder, RoundedCornerShape(28.dp))
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "BENTA NGAYON",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 0.5.sp
            )
            Box(
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.5f), CircleShape)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "Live",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            text = "₱ 3,842.50",
            fontSize = 36.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Box(
            modifier = Modifier
                .padding(top = 8.dp)
                .background(Color.White, CircleShape)
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = "+12% vs kahapon",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun StatsRow(
    transactionsCount: Int = 42,
    netProfitFormatted: String = "₱ 1,120.00"
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        StatCard(
            modifier = Modifier.weight(1f),
            label = "TRANSAKSYON",
            value = "$transactionsCount",
            valueColor = Slate900
        )
        StatCard(
            modifier = Modifier.weight(1f),
            label = "NET PROFIT",
            value = netProfitFormatted,
            valueColor = Emerald600
        )
    }
}

@Composable
fun StatCard(modifier: Modifier = Modifier, label: String, value: String, valueColor: Color) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(24.dp))
            .padding(16.dp)
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Slate400,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = if (valueColor == Slate900) MaterialTheme.colorScheme.onSurface else valueColor
        )
    }
}

@Composable
fun ActionRow(onNewSale: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Button(
            onClick = onNewSale,
            modifier = Modifier
                .weight(1f)
                .height(64.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "New Sale")
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "New Sale", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                .border(1.dp, Slate200, RoundedCornerShape(16.dp))
                .clickable { /* TODO */ },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan", tint = Slate400)
        }
    }
}

@Composable
fun RecentTransactionsList(onSeeAll: () -> Unit = {}) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "MGA HULING BENTA",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                letterSpacing = (-0.5).sp
            )
            Text(
                text = "See All",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onSeeAll() }
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TransactionItem("Cash Sale", "10:42 AM • 3 items", "₱ 124.00", "C", Orange100, Orange600, Slate900, onClick = onSeeAll)
            TransactionItem("GCash Payment", "10:15 AM • 1 item", "₱ 450.00", "G", Blue100, Blue600, Blue600, onClick = onSeeAll)
            TransactionItem("Utang (Credit)", "09:50 AM • Mang Kanor", "₱ 52.50", "U", Red100, Red600, Red500, onClick = onSeeAll)
        }
    }
}

@Composable
fun TransactionItem(
    title: String, subtitle: String, amount: String,
    iconLetter: String, iconBg: Color, iconColor: Color, amountColor: Color,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(iconBg, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = iconLetter, color = iconColor, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(text = subtitle, fontSize = 10.sp, color = Slate400)
            }
        }
        Text(text = amount, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = amountColor)
    }
}

@Composable
fun DashboardBottomNav(
    onNavigateToPOS: () -> Unit,
    onNavigateToInventory: () -> Unit,
    onNavigateToMore: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        NavItem("Dashboard", Icons.Default.Dashboard, isSelected = true) { }
        NavItem("POS", Icons.Default.PointOfSale, isSelected = false, onClick = onNavigateToPOS)
        NavItem("Stocks", Icons.Default.Inventory, isSelected = false, onClick = onNavigateToInventory)
        NavItem("More", Icons.Default.MoreHoriz, isSelected = false, onClick = onNavigateToMore)
    }
}

@Composable
fun NavItem(label: String, icon: ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .height(32.dp)
                .width(48.dp)
                .background(
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else Slate400,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else Slate400
        )
    }
}
