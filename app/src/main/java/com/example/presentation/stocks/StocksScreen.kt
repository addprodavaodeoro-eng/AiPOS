package com.example.presentation.stocks

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.model.ProductCategory
import com.example.domain.model.ProductItem
import com.example.ui.theme.*
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StocksScreen(
    viewModel: StocksViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var productToEdit by remember { mutableStateOf<ProductItem?>(null) }
    var isAddProductOpen by remember { mutableStateOf(false) }
    var productToRestock by remember { mutableStateOf<ProductItem?>(null) }
    var productToDelete by remember { mutableStateOf<ProductItem?>(null) }

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Stocks & Inventory",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Slate900
                        )
                        Text(
                            text = "Imbentaryo at Pagsubaybay",
                            fontSize = 12.sp,
                            color = Slate400
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("stocks_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Slate800
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.toggleLowStockFilter() },
                        modifier = Modifier.testTag("stocks_toggle_low_stock")
                    ) {
                        Badge(
                            containerColor = if (uiState.kpi.lowStockCount > 0) Orange600 else Slate400,
                            contentColor = Color.White
                        ) {
                            Text(uiState.kpi.lowStockCount.toString())
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { isAddProductOpen = true },
                icon = { Icon(Icons.Default.Add, contentDescription = "Add Product") },
                text = { Text("Add Product", fontWeight = FontWeight.Bold) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("stocks_fab_add_product")
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Slate50),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. KPI Summary Cards
            item {
                StocksKpiSection(
                    kpi = uiState.kpi,
                    isLowStockFilterActive = uiState.filterLowStockOnly,
                    onLowStockClicked = { viewModel.toggleLowStockFilter() }
                )
            }

            // 2. Search Field
            item {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    placeholder = { Text("Search by name or barcode...", fontSize = 14.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = Slate400)
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Slate400)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Slate200
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("stocks_search_input")
                )
            }

            // 3. Category Filter Chips
            item {
                StocksCategoryChips(
                    selectedCategory = uiState.selectedCategory,
                    isLowStockSelected = uiState.filterLowStockOnly,
                    lowStockCount = uiState.kpi.lowStockCount,
                    onCategorySelected = {
                        if (uiState.filterLowStockOnly) viewModel.toggleLowStockFilter()
                        viewModel.onCategorySelected(it)
                    },
                    onToggleLowStock = { viewModel.toggleLowStockFilter() }
                )
            }

            // 4. Products List Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ITEMS LIST (${uiState.filteredProducts.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate800,
                        letterSpacing = 0.5.sp
                    )
                    if (uiState.searchQuery.isNotEmpty() || uiState.selectedCategory != ProductCategory.ALL || uiState.filterLowStockOnly) {
                        TextButton(
                            onClick = { viewModel.clearFilters() },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Reset Filters", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            // 5. Product Stock Cards
            if (uiState.filteredProducts.isEmpty()) {
                item {
                    EmptyStocksPlaceholder(
                        searchQuery = uiState.searchQuery,
                        onResetFilters = { viewModel.clearFilters() },
                        onAddProduct = { isAddProductOpen = true }
                    )
                }
            } else {
                items(
                    items = uiState.filteredProducts,
                    key = { it.id }
                ) { product ->
                    StockProductCard(
                        product = product,
                        onAdjustStock = { delta -> viewModel.adjustStock(product.id, delta) },
                        onQuickRestock = { productToRestock = product },
                        onEdit = { productToEdit = product },
                        onDelete = { productToDelete = product }
                    )
                }
            }

            // Bottom Spacing for FAB
            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }

    // Add Product Dialog
    if (isAddProductOpen) {
        AddEditProductDialog(
            product = null,
            onDismiss = { isAddProductOpen = false },
            onSave = { newProduct ->
                viewModel.saveProduct(newProduct)
                isAddProductOpen = false
            }
        )
    }

    // Edit Product Dialog
    productToEdit?.let { product ->
        AddEditProductDialog(
            product = product,
            onDismiss = { productToEdit = null },
            onSave = { updatedProduct ->
                viewModel.saveProduct(updatedProduct)
                productToEdit = null
            }
        )
    }

    // Restock Dialog
    productToRestock?.let { product ->
        QuickRestockDialog(
            product = product,
            onDismiss = { productToRestock = null },
            onConfirmRestock = { addQty ->
                viewModel.restockProduct(product.id, addQty)
                productToRestock = null
            }
        )
    }

    // Delete Confirmation Dialog
    productToDelete?.let { product ->
        AlertDialog(
            onDismissRequest = { productToDelete = null },
            title = { Text("Delete Product?") },
            text = {
                Text("Sigurado ka bang nais mong burahin ang \"${product.name}\"? Hindi na ito maibabalik.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteProduct(product.id)
                        productToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Red600)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { productToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun StocksKpiSection(
    kpi: StocksKpiSummary,
    isLowStockFilterActive: Boolean,
    onLowStockClicked: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Row 1: Total items & Low stock alert
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(90.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "TOTAL PRODUCTS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate400
                    )
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "${kpi.totalProductCount}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Slate900
                        )
                        Text(
                            text = "(${kpi.totalStockUnits} units)",
                            fontSize = 12.sp,
                            color = Slate400,
                            modifier = Modifier.padding(bottom = 3.dp)
                        )
                    }
                }
            }

            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(90.dp)
                    .clickable { onLowStockClicked() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isLowStockFilterActive) Orange100 else if (kpi.lowStockCount > 0) Orange100.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surface
                ),
                border = if (kpi.lowStockCount > 0) androidx.compose.foundation.BorderStroke(1.dp, Orange600.copy(alpha = 0.5f)) else null,
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "LOW STOCK ALERTS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (kpi.lowStockCount > 0) Orange600 else Slate400
                        )
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Alert",
                            tint = if (kpi.lowStockCount > 0) Orange600 else Slate400,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = "${kpi.lowStockCount} items",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (kpi.lowStockCount > 0) Orange600 else Slate800
                    )
                }
            }
        }

        // Row 2: Inventory Valuation Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "RETAIL INVENTORY VALUE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate400
                    )
                    Text(
                        text = kpi.formattedTotalRetailValue,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                }
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(36.dp)
                        .background(Slate200)
                )
                Column {
                    Text(
                        text = "COST (PUHUNAN)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate400
                    )
                    Text(
                        text = kpi.formattedTotalCostValue,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate800
                    )
                }
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(36.dp)
                        .background(Slate200)
                )
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "EST. PROFIT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Emerald600
                    )
                    Text(
                        text = kpi.formattedPotentialProfit,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Emerald600
                    )
                }
            }
        }
    }
}

@Composable
fun StocksCategoryChips(
    selectedCategory: ProductCategory,
    isLowStockSelected: Boolean,
    lowStockCount: Int,
    onCategorySelected: (ProductCategory) -> Unit,
    onToggleLowStock: () -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Low stock quick chip
        item {
            FilterChip(
                selected = isLowStockSelected,
                onClick = onToggleLowStock,
                label = { Text("⚠️ Low Stock ($lowStockCount)") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Orange600,
                    selectedLabelColor = Color.White
                )
            )
        }

        items(ProductCategory.values()) { category ->
            FilterChip(
                selected = !isLowStockSelected && selectedCategory == category,
                onClick = { onCategorySelected(category) },
                label = { Text(category.displayName) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    }
}

@Composable
fun StockProductCard(
    product: ProductItem,
    onAdjustStock: (Int) -> Unit,
    onQuickRestock: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("stock_card_${product.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top row: Thumbnail, Name, Category, Edit & Delete actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Product Thumbnail
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Slate100),
                    contentAlignment = Alignment.Center
                ) {
                    if (product.imageResId != null) {
                        Image(
                            painter = painterResource(id = product.imageResId),
                            contentDescription = product.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = product.name,
                            tint = Slate400,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Name, category, barcode
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "${product.category.displayName} • ${product.unit}",
                            fontSize = 11.sp,
                            color = Slate400
                        )
                        if (!product.barcode.isNullOrBlank()) {
                            Box(
                                modifier = Modifier
                                    .background(Slate100, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = product.barcode,
                                    fontSize = 10.sp,
                                    color = Slate800,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // Edit & Delete buttons
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit Product",
                        tint = Slate400,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "Delete Product",
                        tint = Red500.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            HorizontalDivider(color = Slate100, thickness = 1.dp)

            // Middle row: Pricing and margins
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Selling Price: ${product.formattedPrice}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                    Text(
                        text = "Cost: ${product.formattedCostPrice}  •  Margin: +${product.formattedProfitMargin}",
                        fontSize = 11.sp,
                        color = Emerald600,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Status Badge
                StockStatusBadge(
                    quantity = product.stockQuantity,
                    threshold = product.lowStockThreshold
                )
            }

            HorizontalDivider(color = Slate100, thickness = 1.dp)

            // Bottom row: Direct Stock Controls (- / + / Quick Restock)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Decrement & Increment Quick Stepper
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledIconButton(
                        onClick = { onAdjustStock(-1) },
                        enabled = product.stockQuantity > 0,
                        modifier = Modifier.size(36.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Slate100,
                            contentColor = Slate800,
                            disabledContainerColor = Slate50,
                            disabledContentColor = Slate300
                        )
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease Stock", modifier = Modifier.size(16.dp))
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.widthIn(min = 48.dp)
                    ) {
                        Text(
                            text = "${product.stockQuantity}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (product.isOutOfStock) Red600 else if (product.isLowStock) Orange600 else Slate900
                        )
                        Text(
                            text = product.unit,
                            fontSize = 10.sp,
                            color = Slate400
                        )
                    }

                    FilledIconButton(
                        onClick = { onAdjustStock(1) },
                        modifier = Modifier.size(36.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Slate100,
                            contentColor = Slate800
                        )
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Increase Stock", modifier = Modifier.size(16.dp))
                    }
                }

                // Quick Restock Button
                Button(
                    onClick = onQuickRestock,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TealLight,
                        contentColor = TealText
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Restock", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun StockStatusBadge(quantity: Int, threshold: Int) {
    val (bgColor, textColor, text) = when {
        quantity <= 0 -> Triple(Red100, Red600, "Out of Stock (0)")
        quantity <= threshold -> Triple(Orange100, Orange600, "Low Stock ($quantity left)")
        else -> Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), "In Stock ($quantity)")
    }

    Box(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

@Composable
fun EmptyStocksPlaceholder(
    searchQuery: String,
    onResetFilters: () -> Unit,
    onAddProduct: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.SearchOff,
            contentDescription = null,
            tint = Slate400,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (searchQuery.isNotEmpty()) "No matching products found" else "No products in this category",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Slate800
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Try adjusting your search terms or add a new stock item.",
            fontSize = 13.sp,
            color = Slate400
        )
        Spacer(modifier = Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onResetFilters) {
                Text("Clear Filters")
            }
            Button(
                onClick = onAddProduct,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add New Item")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditProductDialog(
    product: ProductItem?,
    onDismiss: () -> Unit,
    onSave: (ProductItem) -> Unit
) {
    val isEditing = product != null

    var name by remember { mutableStateOf(product?.name ?: "") }
    var priceText by remember { mutableStateOf(product?.price?.toString() ?: "") }
    var costPriceText by remember { mutableStateOf(product?.costPrice?.toString() ?: "") }
    var stockQuantityText by remember { mutableStateOf(product?.stockQuantity?.toString() ?: "10") }
    var lowStockThresholdText by remember { mutableStateOf(product?.lowStockThreshold?.toString() ?: "5") }
    var barcode by remember { mutableStateOf(product?.barcode ?: "") }
    var unit by remember { mutableStateOf(product?.unit ?: "pc") }
    var category by remember { mutableStateOf(product?.category ?: ProductCategory.SNACKS) }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEditing) "Edit Product Stock" else "Add New Stock Item",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                errorMessage?.let { error ->
                    Text(
                        text = error,
                        color = Red600,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Product Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Product Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Category Selector
                ExposedDropdownMenuBox(
                    expanded = categoryDropdownExpanded,
                    onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = category.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = categoryDropdownExpanded,
                        onDismissRequest = { categoryDropdownExpanded = false }
                    ) {
                        ProductCategory.values().filter { it != ProductCategory.ALL }.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.displayName) },
                                onClick = {
                                    category = cat
                                    categoryDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Selling Price & Cost Price
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it },
                        label = { Text("Selling (₱) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = costPriceText,
                        onValueChange = { costPriceText = it },
                        label = { Text("Cost/Puhunan (₱)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Stock Quantity, Threshold & Unit
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = stockQuantityText,
                        onValueChange = { stockQuantityText = it },
                        label = { Text("Stock Qty *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = lowStockThresholdText,
                        onValueChange = { lowStockThresholdText = it },
                        label = { Text("Low Alert") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Unit") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Barcode / SKU (Optional)
                OutlinedTextField(
                    value = barcode,
                    onValueChange = { barcode = it },
                    label = { Text("Barcode / SKU (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedPrice = priceText.toDoubleOrNull()
                    val parsedCost = costPriceText.toDoubleOrNull() ?: 0.0
                    val parsedStock = stockQuantityText.toIntOrNull()
                    val parsedThreshold = lowStockThresholdText.toIntOrNull() ?: 5

                    if (name.isBlank()) {
                        errorMessage = "Product name cannot be empty."
                        return@Button
                    }
                    if (parsedPrice == null || parsedPrice < 0) {
                        errorMessage = "Valid selling price is required."
                        return@Button
                    }
                    if (parsedStock == null || parsedStock < 0) {
                        errorMessage = "Valid stock quantity is required."
                        return@Button
                    }

                    val updatedItem = ProductItem(
                        id = product?.id ?: UUID.randomUUID().toString(),
                        name = name.trim(),
                        price = parsedPrice,
                        costPrice = parsedCost,
                        category = category,
                        stockQuantity = parsedStock,
                        lowStockThreshold = parsedThreshold,
                        barcode = barcode.ifBlank { null },
                        unit = unit.ifBlank { "pc" },
                        imageResId = product?.imageResId,
                        drawableResName = product?.drawableResName
                    )
                    onSave(updatedItem)
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(if (isEditing) "Save Changes" else "Add Item")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun QuickRestockDialog(
    product: ProductItem,
    onDismiss: () -> Unit,
    onConfirmRestock: (Int) -> Unit
) {
    var addedQtyText by remember { mutableStateOf("12") }
    val addedQty = addedQtyText.toIntOrNull() ?: 0
    val newTotalStock = product.stockQuantity + addedQty

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Quick Restock") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = product.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Slate900
                )

                // Current vs New Stock preview
                Card(
                    colors = CardDefaults.cardColors(containerColor = Slate50),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Current Stock", fontSize = 11.sp, color = Slate400)
                            Text("${product.stockQuantity} ${product.unit}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Slate400)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("New Total", fontSize = 11.sp, color = Emerald600)
                            Text("$newTotalStock ${product.unit}", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Emerald600)
                        }
                    }
                }

                // Preset Chips
                Text("Select restock batch:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate400)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf(6, 12, 24, 48).forEach { preset ->
                        SuggestionChip(
                            onClick = { addedQtyText = preset.toString() },
                            label = { Text("+$preset") },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = if (addedQty == preset) MaterialTheme.colorScheme.primaryContainer else Slate50
                            )
                        )
                    }
                }

                // Custom amount field
                OutlinedTextField(
                    value = addedQtyText,
                    onValueChange = { addedQtyText = it },
                    label = { Text("Quantity to Add (${product.unit})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (addedQty > 0) {
                        onConfirmRestock(addedQty)
                    }
                },
                enabled = addedQty > 0,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Confirm Restock (+$addedQty)")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
