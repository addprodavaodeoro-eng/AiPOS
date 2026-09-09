package com.example.presentation.sales

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.TransactionLogEntity
import com.example.domain.model.HourlySalesBucket
import com.example.domain.model.PaymentBreakdown
import com.example.presentation.common.AdminResetConfirmationDialog
import com.example.presentation.common.ResetScope
import com.example.presentation.common.ThemeToggleIconButton
import com.example.ui.theme.*
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailySalesSummaryScreen(
    viewModel: DailySalesViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToPos: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    var showResetDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    if (showResetDialog) {
        AdminResetConfirmationDialog(
            initialScope = ResetScope.SALES_AND_TRANSACTIONS_ONLY,
            onDismiss = { showResetDialog = false },
            onPerformReset = { scope, reseedSampleCatalog, password, onResult ->
                viewModel.executeReset(scope, reseedSampleCatalog, password) { success, error ->
                    if (success) {
                        showResetDialog = false
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Sales history successfully purged.", duration = SnackbarDuration.Short)
                        }
                    }
                    onResult(success, error)
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Daily Sales Summary",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Arawang Ulat at Transaksyon",
                            fontSize = 11.sp,
                            color = Slate400
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("btn_daily_sales_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showResetDialog = true },
                        modifier = Modifier.testTag("btn_daily_sales_reset")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteForever,
                            contentDescription = "Purge Logs",
                            tint = Color(0xFFDC2626)
                        )
                    }
                    ThemeToggleIconButton(modifier = Modifier.padding(end = 4.dp))
                    IconButton(
                        onClick = onNavigateToPos,
                        modifier = Modifier.testTag("btn_daily_sales_pos_shortcut")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PointOfSale,
                            contentDescription = "Open POS",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                DateNavigatorCard(
                    displayDate = uiState.summary.displayDate,
                    isToday = uiState.isToday,
                    onPrevDay = { viewModel.goToPreviousDay() },
                    onNextDay = { viewModel.goToNextDay() },
                    onToday = { viewModel.goToToday() }
                )
            }

            // Hero Sales KPI Card
            item {
                HeroDailySalesCard(
                    grossSalesFormatted = uiState.summary.formattedGrossSales,
                    totalTransactions = uiState.summary.totalTransactions,
                    totalItemsSold = uiState.summary.totalItemsSold,
                    avgTicketFormatted = uiState.summary.formattedAverageTicket,
                    isToday = uiState.isToday
                )
            }

            // Payment Methods Breakdown
            if (uiState.summary.paymentBreakdowns.isNotEmpty()) {
                item {
                    PaymentBreakdownCard(breakdowns = uiState.summary.paymentBreakdowns)
                }
            }

            // Hourly Distribution
            if (uiState.summary.totalTransactions > 0) {
                item {
                    HourlySalesDistributionCard(
                        hourlyList = uiState.summary.hourlyBreakdowns,
                        totalSales = uiState.summary.totalGrossSales
                    )
                }
            }

            // Transaction Filter & Search Header
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "COMPLETED TRANSACTIONS (${uiState.filteredTransactions.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate400,
                            letterSpacing = 0.8.sp
                        )
                        if (uiState.searchQuery.isNotEmpty() || uiState.selectedPaymentFilter != "ALL") {
                            Text(
                                text = "Reset Filter",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable {
                                    viewModel.setSearchQuery("")
                                    viewModel.setPaymentFilter("ALL")
                                }
                            )
                        }
                    }

                    // Search text field
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text("Search by receipt # or product...", fontSize = 13.sp, color = Slate400) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = Slate400, modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = Slate400, modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_transaction_search")
                    )

                    // Payment Filter Chips
                    val paymentFilters = listOf("ALL", "Cash", "GCash", "Maya", "Utang")
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(paymentFilters) { filter ->
                            val isSelected = uiState.selectedPaymentFilter.equals(filter, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setPaymentFilter(filter) },
                                label = {
                                    Text(
                                        text = filter,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 12.sp
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                    selectedBorderColor = MaterialTheme.colorScheme.primary,
                                    enabled = true,
                                    selected = isSelected
                                ),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }
            }

            // Transactions list or empty state
            if (uiState.filteredTransactions.isEmpty()) {
                item {
                    EmptyTransactionsCard(
                        isSearch = uiState.searchQuery.isNotEmpty() || uiState.selectedPaymentFilter != "ALL",
                        onOpenPos = onNavigateToPos
                    )
                }
            } else {
                items(uiState.filteredTransactions, key = { it.id }) { tx ->
                    TransactionLogCard(
                        transaction = tx,
                        onClick = { viewModel.showTransactionDetail(tx) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Detail Dialog
    uiState.selectedTransactionForDetail?.let { selectedTx ->
        TransactionDetailDialog(
            transaction = selectedTx,
            onDismiss = { viewModel.dismissTransactionDetail() },
            onDelete = {
                viewModel.deleteTransaction(selectedTx.id)
            }
        )
    }
}

@Composable
fun DateNavigatorCard(
    displayDate: String,
    isToday: Boolean,
    onPrevDay: () -> Unit,
    onNextDay: () -> Unit,
    onToday: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onPrevDay,
                modifier = Modifier
                    .size(36.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    .testTag("btn_prev_day")
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Previous Day",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onToday() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = displayDate,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                if (isToday) {
                    Surface(
                        color = Emerald500.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.padding(top = 3.dp)
                    ) {
                        Text(
                            text = "TODAY'S SHIFT",
                            color = Emerald600,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                        )
                    }
                } else {
                    Text(
                        text = "Tap to jump to Today",
                        color = Slate400,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            IconButton(
                onClick = onNextDay,
                modifier = Modifier
                    .size(36.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    .testTag("btn_next_day")
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Next Day",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun HeroDailySalesCard(
    grossSalesFormatted: String,
    totalTransactions: Int,
    totalItemsSold: Int,
    avgTicketFormatted: String,
    isToday: Boolean
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_hero_daily_sales")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isToday) "TOTAL GROSS SALES TODAY" else "DAILY GROSS SALES",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    letterSpacing = 0.8.sp
                )

                Surface(
                    shape = CircleShape,
                    color = if (isToday) Emerald500.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(if (isToday) Emerald600 else Slate400, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (isToday) "Live Ledger" else "Archived",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = grossSalesFormatted,
                fontSize = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(18.dp))

            HorizontalDivider(
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f),
                thickness = 1.dp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Sub Stats (Transactions, Items Sold, Avg Ticket)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummarySubStat(
                    label = "TRANSACTIONS",
                    value = "$totalTransactions sales",
                    icon = Icons.Default.ReceiptLong
                )
                SummarySubStat(
                    label = "ITEMS SOLD",
                    value = "$totalItemsSold pcs",
                    icon = Icons.Default.ShoppingBag
                )
                SummarySubStat(
                    label = "AVG TICKET",
                    value = avgTicketFormatted,
                    icon = Icons.Default.TrendingUp
                )
            }
        }
    }
}

@Composable
fun SummarySubStat(
    label: String,
    value: String,
    icon: ImageVector
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                letterSpacing = 0.5.sp
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
fun PaymentBreakdownCard(breakdowns: List<PaymentBreakdown>) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PAYMENT CHANNELS",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate400,
                    letterSpacing = 0.8.sp
                )
                Text(
                    text = "${breakdowns.size} Methods",
                    fontSize = 11.sp,
                    color = Slate400
                )
            }

            // Stacked Bar Indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                breakdowns.forEach { item ->
                    val color = when (item.paymentMethod.lowercase(Locale.ROOT)) {
                        "cash" -> Emerald500
                        "gcash" -> Color(0xFF0284C7)
                        "maya" -> Color(0xFF10B981)
                        "utang" -> Red500
                        else -> Amber500
                    }
                    val weight = item.percentage.coerceAtLeast(1f)
                    Box(
                        modifier = Modifier
                            .weight(weight)
                            .fillMaxHeight()
                            .background(color)
                    )
                }
            }

            // Breakdown list items
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                breakdowns.forEach { item ->
                    val color = when (item.paymentMethod.lowercase(Locale.ROOT)) {
                        "cash" -> Emerald500
                        "gcash" -> Color(0xFF0284C7)
                        "maya" -> Color(0xFF10B981)
                        "utang" -> Red500
                        else -> Amber500
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(color, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = item.paymentMethod,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "(${item.count} txs)",
                                fontSize = 11.sp,
                                color = Slate400
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = item.formattedTotal,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = color.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = "${String.format(Locale.US, "%.0f", item.percentage)}%",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = color,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HourlySalesDistributionCard(
    hourlyList: List<HourlySalesBucket>,
    totalSales: Double
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "HOURLY SALES ACTIVITY",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate400,
                    letterSpacing = 0.8.sp
                )
                Text(
                    text = "Peak Store Hours",
                    fontSize = 11.sp,
                    color = Slate400
                )
            }

            hourlyList.forEach { bucket ->
                val maxSales = hourlyList.maxOfOrNull { it.totalAmount }?.coerceAtLeast(1.0) ?: 1.0
                val progress = (bucket.totalAmount / maxSales).toFloat().coerceIn(0f, 1f)
                val isPeak = bucket.totalAmount > 0 && bucket.totalAmount == maxSales

                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = bucket.hourLabel,
                                fontSize = 12.sp,
                                fontWeight = if (isPeak) FontWeight.Bold else FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (isPeak && bucket.totalAmount > 0) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Amber500.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "PEAK",
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Amber500,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = if (bucket.totalAmount > 0) "₱${String.format(Locale.US, "%,.2f", bucket.totalAmount)} (${bucket.count})" else "—",
                            fontSize = 12.sp,
                            fontWeight = if (bucket.totalAmount > 0) FontWeight.Bold else FontWeight.Normal,
                            color = if (bucket.totalAmount > 0) MaterialTheme.colorScheme.onSurface else Slate400
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { if (bucket.totalAmount > 0) progress else 0.02f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (isPeak) Amber500 else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun TransactionLogCard(
    transaction: TransactionLogEntity,
    onClick: () -> Unit
) {
    val methodColor = when (transaction.paymentMethod.lowercase(Locale.ROOT)) {
        "cash" -> Emerald600
        "gcash" -> Color(0xFF0284C7)
        "maya" -> Color(0xFF10B981)
        "utang" -> Red500
        else -> Amber500
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("card_transaction_${transaction.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(methodColor.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (transaction.paymentMethod.lowercase(Locale.ROOT)) {
                            "cash" -> Icons.Default.AttachMoney
                            "gcash", "maya" -> Icons.Default.QrCode
                            "utang" -> Icons.Default.Person
                            else -> Icons.Default.ReceiptLong
                        },
                        contentDescription = null,
                        tint = methodColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = transaction.receiptNumber,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = methodColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = transaction.paymentMethod,
                                color = methodColor,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = transaction.itemsSummary.ifBlank { "${transaction.totalItemCount} items sold" },
                        fontSize = 11.5.sp,
                        color = Slate400,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "${transaction.formattedTime} • ${transaction.cashierName}",
                        fontSize = 10.5.sp,
                        color = Slate400
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text(
                    text = transaction.formattedTotal,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (transaction.discountAmount > 0) {
                    Text(
                        text = "-₱${String.format(Locale.US, "%.2f", transaction.discountAmount)} disc",
                        fontSize = 10.sp,
                        color = Emerald600,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyTransactionsCard(
    isSearch: Boolean,
    onOpenPos: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isSearch) Icons.Default.SearchOff else Icons.Default.ReceiptLong,
                    contentDescription = null,
                    tint = Slate400,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = if (isSearch) "No Matching Transactions" else "No Transactions Recorded",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (isSearch) "Try adjusting your search query or payment channel filter." else "No sales recorded yet for this date. Completed POS transactions will automatically log here.",
                fontSize = 12.sp,
                color = Slate400,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )

            if (!isSearch) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onOpenPos,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Go to POS Register", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun TransactionDetailDialog(
    transaction: TransactionLogEntity,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Transaction Details",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = transaction.receiptNumber,
                            fontSize = 12.sp,
                            color = Slate400
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Slate400)
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Date & Cashier Info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("DATE & TIME", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Slate400)
                        Text(transaction.formattedDateTime, fontSize = 12.5.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("CASHIER", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Slate400)
                        Text(transaction.cashierName, fontSize = 12.5.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                    }
                }

                // Purchased Items list
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "PURCHASED ITEMS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate400,
                        letterSpacing = 0.5.sp
                    )

                    if (transaction.itemsSummary.isNotBlank()) {
                        val items = transaction.itemsSummary.split(", ")
                        items.forEach { line ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(line, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
                            }
                        }
                    } else {
                        Text("${transaction.totalItemCount} items recorded", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }

                // Payment and Total Breakdown
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ReceiptRow(label = "Subtotal", value = "₱${String.format(Locale.US, "%,.2f", transaction.subtotal)}")
                    if (transaction.discountAmount > 0) {
                        ReceiptRow(label = "Discount", value = "-₱${String.format(Locale.US, "%,.2f", transaction.discountAmount)}", valueColor = Emerald600)
                    }
                    ReceiptRow(label = "Payment Method", value = transaction.paymentMethod, isBold = true)
                    ReceiptRow(label = "Amount Tendered", value = "₱${String.format(Locale.US, "%,.2f", transaction.amountTendered)}")
                    if (transaction.changeAmount > 0) {
                        ReceiptRow(label = "Change", value = "₱${String.format(Locale.US, "%,.2f", transaction.changeAmount)}")
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = 4.dp))
                    ReceiptRow(label = "TOTAL PAID", value = transaction.formattedTotal, isBold = true, fontSize = 17.sp, valueColor = MaterialTheme.colorScheme.primary)
                }

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { showDeleteConfirm = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Red500),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete Log", fontSize = 12.sp)
                    }

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Close", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Transaction Log?") },
            text = { Text("Are you sure you want to remove receipt ${transaction.receiptNumber}? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    }
                ) {
                    Text("Delete", color = Red500, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ReceiptRow(
    label: String,
    value: String,
    isBold: Boolean = false,
    fontSize: androidx.compose.ui.unit.TextUnit = 13.sp,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = fontSize,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = if (isBold) MaterialTheme.colorScheme.onSurface else Slate400
        )
        Text(
            text = value,
            fontSize = fontSize,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
            color = valueColor
        )
    }
}
