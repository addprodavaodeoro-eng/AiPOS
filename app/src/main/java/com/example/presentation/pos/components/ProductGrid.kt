package com.example.presentation.pos.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.ProductCategory
import com.example.domain.model.ProductItem
import com.example.ui.theme.*

@Composable
fun ProductGrid(
    products: List<ProductItem>,
    cartItemCounts: Map<String, Int> = emptyMap(),
    onAddToCart: (ProductItem) -> Unit,
    onRemoveFromCart: (ProductItem) -> Unit = {},
    onScanBarcode: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    initialCategory: ProductCategory = ProductCategory.ALL,
    initialStockThreshold: Int? = null,
    headerContent: (@Composable () -> Unit)? = null
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(initialCategory) }
    var filterOnlyLowStock by remember { mutableStateOf(false) }
    var customStockThreshold by remember { mutableStateOf<Int?>(initialStockThreshold) }
    var showThresholdDialog by remember { mutableStateOf(false) }

    // Helper to calculate threshold for a given product
    val effectiveThresholdFor: (ProductItem) -> Int = { product ->
        customStockThreshold ?: product.lowStockThreshold
    }

    // Low stock count across all products
    val lowStockCount = remember(products, customStockThreshold) {
        products.count { product ->
            val thresh = effectiveThresholdFor(product)
            product.stockQuantity in 1..thresh
        }
    }

    // Filter products based on search, category, and low-stock filter
    val filteredProducts = remember(products, searchQuery, selectedCategory, filterOnlyLowStock, customStockThreshold) {
        products.filter { product ->
            val matchesCategory = selectedCategory == ProductCategory.ALL || product.category == selectedCategory
            val matchesSearch = searchQuery.isBlank() ||
                    product.name.contains(searchQuery, ignoreCase = true) ||
                    (product.barcode != null && product.barcode.contains(searchQuery, ignoreCase = true))
            val matchesLowStock = !filterOnlyLowStock || (product.stockQuantity in 1..effectiveThresholdFor(product))
            matchesCategory && matchesSearch && matchesLowStock
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val minCellSize = if (maxWidth > 600.dp) 170.dp else 150.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Optional custom header (e.g. POS top bar)
            headerContent?.invoke()

            // Search Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(14.dp),
                shadowElevation = 1.dp
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("product_search_input"),
                    placeholder = {
                        Text(
                            text = "Search items, brands, barcodes…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Slate400
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search icon",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AnimatedVisibility(
                                visible = searchQuery.isNotEmpty(),
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                IconButton(
                                    onClick = { searchQuery = "" },
                                    modifier = Modifier.testTag("clear_search_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear search",
                                        tint = Slate400
                                    )
                                }
                            }
                            if (onScanBarcode != null) {
                                IconButton(
                                    onClick = onScanBarcode,
                                    modifier = Modifier.testTag("search_bar_scan_barcode_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.QrCodeScanner,
                                        contentDescription = "Scan Barcode",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Slate200,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }

            // Category Filter Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ProductCategory.values().forEach { category ->
                    val isSelected = selectedCategory == category
                    val count = if (category == ProductCategory.ALL) {
                        products.size
                    } else {
                        products.count { it.category == category }
                    }

                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = category },
                        label = {
                            Text(
                                text = "${category.displayName} ($count)",
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            containerColor = MaterialTheme.colorScheme.surface,
                            labelColor = Slate800
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Slate200
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("category_chip_${category.name}")
                    )
                }
            }

            // Stock Alert & Threshold Control Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Low Stock Filter Toggle Chip
                FilterChip(
                    selected = filterOnlyLowStock,
                    onClick = { filterOnlyLowStock = !filterOnlyLowStock },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (filterOnlyLowStock) Color.White else Amber600,
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "Low Stock Alert ($lowStockCount)",
                            fontSize = 12.sp,
                            fontWeight = if (filterOnlyLowStock) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Amber600,
                        selectedLabelColor = Color.White,
                        containerColor = Amber50,
                        labelColor = Amber700
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = filterOnlyLowStock,
                        borderColor = if (filterOnlyLowStock) Amber600 else Amber200
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("filter_low_stock_chip")
                )

                // Stock Alert Threshold Selector Chip
                AssistChip(
                    onClick = { showThresholdDialog = true },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Configure threshold",
                            tint = Slate600,
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    label = {
                        Text(
                            text = if (customStockThreshold != null) {
                                "Threshold: ≤$customStockThreshold units"
                            } else {
                                "Threshold: Presets (≤5-15)"
                            },
                            fontSize = 12.sp,
                            color = Slate700,
                            fontWeight = FontWeight.Medium
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = Slate100
                    ),
                    border = AssistChipDefaults.assistChipBorder(
                        enabled = true,
                        borderColor = Slate200
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("stock_threshold_selector_chip")
                )
            }

            // Low Stock Visual Banner (when items exist below threshold and user hasn't filtered yet)
            if (lowStockCount > 0 && !filterOnlyLowStock && searchQuery.isEmpty()) {
                Surface(
                    color = Amber50,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Amber200),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .testTag("low_stock_alert_banner")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Low stock alert",
                                tint = Amber600,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "$lowStockCount item(s) are below the stock threshold (highlighted with amber badge and border)",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                color = Amber700,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        TextButton(
                            onClick = { filterOnlyLowStock = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.testTag("banner_view_low_stock_button")
                        ) {
                            Text(
                                text = "View",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Amber700
                            )
                        }
                    }
                }
            }

            // Product Count / Status summary
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${filteredProducts.size} items found" +
                            if (filterOnlyLowStock) " (low stock only)" else "",
                    style = MaterialTheme.typography.labelMedium,
                    color = Slate400,
                    fontWeight = FontWeight.SemiBold
                )

                if (searchQuery.isNotEmpty() || selectedCategory != ProductCategory.ALL || filterOnlyLowStock) {
                    TextButton(
                        onClick = {
                            searchQuery = ""
                            selectedCategory = ProductCategory.ALL
                            filterOnlyLowStock = false
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.testTag("reset_filters_button")
                    ) {
                        Text(
                            text = "Reset Filters",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Responsive Product Grid or Empty State
            if (filteredProducts.isEmpty()) {
                EmptyProductsState(
                    searchQuery = searchQuery,
                    onReset = {
                        searchQuery = ""
                        selectedCategory = ProductCategory.ALL
                        filterOnlyLowStock = false
                    },
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = minCellSize),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("product_grid_view"),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 80.dp // Leave breathing room for cart bar or system navigation
                    ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(
                        items = filteredProducts,
                        key = { it.id }
                    ) { product ->
                        ProductCard(
                            product = product,
                            cartQuantity = cartItemCounts[product.id] ?: 0,
                            thresholdOverride = customStockThreshold,
                            onAddToCart = onAddToCart,
                            onRemoveFromCart = onRemoveFromCart
                        )
                    }
                }
            }
        }
    }

    if (showThresholdDialog) {
        ThresholdConfigDialog(
            currentThreshold = customStockThreshold,
            onDismiss = { showThresholdDialog = false },
            onApplyThreshold = { newThreshold ->
                customStockThreshold = newThreshold
                showThresholdDialog = false
            }
        )
    }
}

@Composable
private fun EmptyProductsState(
    searchQuery: String,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                color = Slate100,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.SearchOff,
                        contentDescription = null,
                        tint = Slate400,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Text(
                text = "No products found",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Slate800
            )

            Text(
                text = if (searchQuery.isNotEmpty()) {
                    "No items match \"$searchQuery\". Check the spelling or try another keyword."
                } else {
                    "There are no items currently available in this category."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = Slate400,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 280.dp)
            )

            Button(
                onClick = onReset,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Show All Products")
            }
        }
    }
}

@Composable
private fun ThresholdConfigDialog(
    currentThreshold: Int?,
    onDismiss: () -> Unit,
    onApplyThreshold: (Int?) -> Unit
) {
    var selectedPreset by remember { mutableStateOf(currentThreshold) }
    var isCustomSelected by remember {
        mutableStateOf(currentThreshold != null && currentThreshold !in listOf(3, 5, 10, 15))
    }
    var customText by remember {
        mutableStateOf(if (isCustomSelected) (currentThreshold?.toString() ?: "") else "")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Stock Alert Threshold",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Slate900
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Products with remaining stock at or below this threshold are highlighted in the grid with an amber border, badge, and level gauge.",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp),
                    color = Slate600
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Default / Per-product option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selectedPreset == null && !isCustomSelected) TealCard else Slate50)
                        .border(
                            1.dp,
                            if (selectedPreset == null && !isCustomSelected) MaterialTheme.colorScheme.primary else Slate200,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable {
                            selectedPreset = null
                            isCustomSelected = false
                        }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Use Per-Product Presets",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = Slate800
                        )
                        Text(
                            text = "Uses individual item thresholds (default 5-15)",
                            fontSize = 11.sp,
                            color = Slate500
                        )
                    }
                    RadioButton(
                        selected = selectedPreset == null && !isCustomSelected,
                        onClick = {
                            selectedPreset = null
                            isCustomSelected = false
                        }
                    )
                }

                // Standard Presets
                Text(
                    text = "Global Threshold Presets:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Slate700
                )

                listOf(
                    3 to "≤ 3 units (Critical)",
                    5 to "≤ 5 units (Standard)",
                    10 to "≤ 10 units (Early Warning)",
                    15 to "≤ 15 units (High Volume)"
                ).forEach { (value, label) ->
                    val isSelected = !isCustomSelected && selectedPreset == value
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) TealCard else Slate50)
                            .border(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else Slate200,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                selectedPreset = value
                                isCustomSelected = false
                                customText = value.toString()
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = label,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Slate800
                        )
                        RadioButton(
                            selected = isSelected,
                            onClick = {
                                selectedPreset = value
                                isCustomSelected = false
                                customText = value.toString()
                            }
                        )
                    }
                }

                // Custom Threshold Input
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isCustomSelected) TealCard else Slate50)
                        .border(
                            1.dp,
                            if (isCustomSelected) MaterialTheme.colorScheme.primary else Slate200,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { isCustomSelected = true }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Custom Number…",
                        fontSize = 13.sp,
                        fontWeight = if (isCustomSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isCustomSelected) MaterialTheme.colorScheme.primary else Slate800
                    )
                    RadioButton(
                        selected = isCustomSelected,
                        onClick = { isCustomSelected = true }
                    )
                }

                if (isCustomSelected) {
                    OutlinedTextField(
                        value = customText,
                        onValueChange = { input ->
                            if (input.isEmpty() || input.all { it.isDigit() }) {
                                customText = input
                            }
                        },
                        label = { Text("Threshold quantity") },
                        placeholder = { Text("e.g. 8") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("threshold_input_field")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalThreshold = if (isCustomSelected) {
                        customText.toIntOrNull() ?: 5
                    } else {
                        selectedPreset
                    }
                    onApplyThreshold(finalThreshold)
                },
                modifier = Modifier.testTag("apply_threshold_button")
            ) {
                Text("Apply Threshold")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_threshold_button")
            ) {
                Text("Cancel")
            }
        }
    )
}
