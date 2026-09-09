package com.example.presentation.more

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.presentation.common.AdminResetConfirmationDialog
import com.example.presentation.common.ResetScope
import com.example.presentation.common.ShiftModeTogglePill
import com.example.presentation.common.ThemeToggleIconButton
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreMenuScreen(
    onNavigateBack: () -> Unit,
    onNavigateToUtang: () -> Unit,
    onNavigateToBackup: () -> Unit,
    onNavigateToInventory: () -> Unit,
    onNavigateToPOS: () -> Unit,
    onNavigateToSales: () -> Unit = {},
    onNavigateToStaff: () -> Unit = {},
    onPerformReset: ((ResetScope, Boolean, String, (Boolean, String?) -> Unit) -> Unit)? = null
) {
    val themeState = LocalThemeState.current
    val isDark = themeState.isDarkTheme
    var showResetDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    if (showResetDialog && onPerformReset != null) {
        AdminResetConfirmationDialog(
            initialScope = ResetScope.ALL_PRODUCTS_AND_SALES,
            onDismiss = { showResetDialog = false },
            onPerformReset = { scope, reseedSampleCatalog, password, onResult ->
                onPerformReset(scope, reseedSampleCatalog, password) { success, error ->
                    if (success) {
                        showResetDialog = false
                        coroutineScope.launch {
                            val scopeName = when (scope) {
                                ResetScope.ALL_PRODUCTS_AND_SALES -> "All products and transactions"
                                ResetScope.PRODUCTS_ONLY -> "All inventory products"
                                ResetScope.SALES_AND_TRANSACTIONS_ONLY -> "All sales history and transactions"
                            }
                            snackbarHostState.showSnackbar(
                                message = "$scopeName successfully deleted & reset.",
                                duration = SnackbarDuration.Short
                            )
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
                    Text(
                        "Store Management",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("more_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                actions = {
                    ThemeToggleIconButton(modifier = Modifier.padding(end = 8.dp))
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
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header store summary
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Storefront, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(26.dp))
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text("Aling Nena's Sari-Sari Store", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                                Text("Offline-First POS & Ledger v1.0", fontSize = 12.sp, color = Slate400)
                            }
                        }
                    }
                }
            }

            // Section: Display & Shift Mode Preference
            item {
                Text(
                    "DISPLAY & SHIFT VISIBILITY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate400,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 4.dp, top = 6.dp)
                )
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { themeState.toggleTheme() }
                        .testTag("menu_theme_toggle_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(
                                        if (isDark) Amber500.copy(alpha = 0.2f) else TealLight,
                                        RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isDark) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                                    contentDescription = null,
                                    tint = if (isDark) Amber500 else TealPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    if (isDark) "Night Shift (Dark Mode)" else "Day Shift (Light Mode)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    if (isDark) "High-contrast dark canvas for evening cashier shifts" else "Crisp bright canvas for daytime storefront visibility",
                                    fontSize = 12.sp,
                                    color = Slate400,
                                    lineHeight = 16.sp
                                )
                            }
                        }

                        Switch(
                            checked = isDark,
                            onCheckedChange = { themeState.setThemeMode(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Amber500,
                                checkedTrackColor = TealContainerDark,
                                uncheckedThumbColor = TealPrimary,
                                uncheckedTrackColor = TealLight
                            ),
                            modifier = Modifier.testTag("switch_dark_mode")
                        )
                    }
                }
            }

            // Section: Financial & Credit
            item {
                Text(
                    "FINANCES & CUSTOMERS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate400,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 4.dp, top = 6.dp)
                )
            }

            item {
                MoreMenuCard(
                    title = "Customers & Utang Ledger",
                    subtitle = "Track customer credit, pay balances & reminders",
                    icon = Icons.Default.PeopleAlt,
                    iconBg = Color(0xFFE0F2FE),
                    iconColor = Color(0xFF0284C7),
                    testTag = "menu_utang_button",
                    onClick = onNavigateToUtang
                )
            }

            // Section: Staff & Access Control
            item {
                Text(
                    "STAFF & ACCESS CONTROL",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate400,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 4.dp, top = 6.dp)
                )
            }

            item {
                MoreMenuCard(
                    title = "Staff Management & 4-Digit PINs",
                    subtitle = "Manage staff profiles, PIN accountability & auto-lockout",
                    icon = Icons.Default.Badge,
                    iconBg = Color(0xFFFEF3C7),
                    iconColor = Color(0xFFD97706),
                    badge = "Security",
                    badgeColor = Color(0xFFD97706),
                    testTag = "menu_staff_mgmt_button",
                    onClick = onNavigateToStaff
                )
            }

            // Section: Data Maintenance & Safety
            item {
                Text(
                    "DATA SECURITY & BACKUP",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate400,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 4.dp, top = 6.dp)
                )
            }

            item {
                MoreMenuCard(
                    title = "Database Backup & Snapshot",
                    subtitle = "Manual export & import snapshots to local storage",
                    icon = Icons.Default.Backup,
                    iconBg = Color(0xFFDCFCE7),
                    iconColor = Color(0xFF16A34A),
                    badge = "Recovery",
                    badgeColor = Color(0xFF16A34A),
                    testTag = "menu_backup_restore_button",
                    onClick = onNavigateToBackup
                )
            }

            // Section: Store Operations
            item {
                Text(
                    "STORE OPERATIONS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate400,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 4.dp, top = 6.dp)
                )
            }

            item {
                MoreMenuCard(
                    title = "Daily Sales & Ledger",
                    subtitle = "Review today's gross sales, hourly breakdown, and receipt logs",
                    icon = Icons.Default.Assessment,
                    iconBg = Color(0xFFCCFBF1),
                    iconColor = Color(0xFF0F766E),
                    badge = "Daily",
                    badgeColor = Color(0xFF0F766E),
                    testTag = "menu_daily_sales_button",
                    onClick = onNavigateToSales
                )
            }

            item {
                MoreMenuCard(
                    title = "Inventory & Stocks",
                    subtitle = "Manage product catalog, prices, and stock reordering",
                    icon = Icons.Default.Inventory2,
                    iconBg = Color(0xFFFEF3C7),
                    iconColor = Color(0xFFD97706),
                    testTag = "menu_inventory_button",
                    onClick = onNavigateToInventory
                )
            }

            item {
                MoreMenuCard(
                    title = "Point of Sale (POS)",
                    subtitle = "Open barcode scanner and fast checkout register",
                    icon = Icons.Default.PointOfSale,
                    iconBg = Color(0xFFF3E8FF),
                    iconColor = Color(0xFF9333EA),
                    testTag = "menu_pos_button",
                    onClick = onNavigateToPOS
                )
            }

            // Section: Danger Zone
            item {
                Text(
                    "DANGER ZONE & DATA PURGE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFDC2626),
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 4.dp, top = 6.dp)
                )
            }

            item {
                MoreMenuCard(
                    title = "Reset Products & Sales Data",
                    subtitle = "Wipe products catalog, sales records & logs (Admin required)",
                    icon = Icons.Default.DeleteForever,
                    iconBg = Color(0xFFFEE2E2),
                    iconColor = Color(0xFFDC2626),
                    badge = "Admin Lock",
                    badgeColor = Color(0xFFDC2626),
                    testTag = "menu_reset_data_button",
                    onClick = { showResetDialog = true }
                )
            }
        }
    }
}

@Composable
fun MoreMenuCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconBg: Color,
    iconColor: Color,
    badge: String? = null,
    badgeColor: Color = Color(0xFF16A34A),
    testTag: String,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(iconBg, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(title, fontWeight = FontWeight.Bold, fontSize = 14.5.sp, color = MaterialTheme.colorScheme.onSurface)
                        if (badge != null) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = badgeColor.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = badge,
                                    color = badgeColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(subtitle, fontSize = 12.sp, color = Slate400, lineHeight = 16.sp)
                }
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = Slate400,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
