package com.example.presentation.products

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.ProductCategory
import com.example.domain.model.ProductItem
import com.example.presentation.components.NetworkStatusIndicator
import com.example.domain.model.Receipt
import com.example.presentation.cart.CartStickyBottomBar
import com.example.presentation.cart.CheckoutFinalizeDialog
import com.example.presentation.cart.ShoppingCartSheet
import com.example.presentation.cart.ShoppingCartViewModel
import com.example.presentation.common.ThemeToggleIconButton
import com.example.presentation.pos.DigitalReceiptDialog
import com.example.presentation.pos.barcode.BarcodeScannerDialog
import com.example.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductListScreen(
    viewModel: ProductViewModel,
    cartViewModel: ShoppingCartViewModel? = null,
    onNavigateBack: () -> Unit = {},
    onProductClick: (ProductItem) -> Unit = {},
    onNavigateToPos: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val cartUiState by cartViewModel?.uiState?.collectAsState() ?: remember { mutableStateOf(null) }

    val focusManager = LocalFocusManager.current
    var showAddDialog by remember { mutableStateOf(false) }
    var showQuickStockDialog by remember { mutableStateOf<ProductItem?>(null) }
    var showCartSheet by remember { mutableStateOf(false) }
    var showCheckoutDialog by remember { mutableStateOf(false) }
    var showBarcodeScanner by remember { mutableStateOf(false) }
    var selectedProductForDetail by remember { mutableStateOf<ProductItem?>(null) }
    var showBluetoothPrinterForProduct by remember { mutableStateOf<ProductItem?>(null) }
    var completedReceipt by remember { mutableStateOf<Receipt?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    val currencyFormatter = remember {
        NumberFormat.getCurrencyInstance(Locale("en", "PH")).apply {
            maximumFractionDigits = 2
        }
    }

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissMessage()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { err ->
            snackbarHostState.showSnackbar(message = err, duration = SnackbarDuration.Short)
            viewModel.dismissMessage()
        }
    }

    LaunchedEffect(cartUiState?.userMessage) {
        cartUiState?.userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            cartViewModel?.dismissMessages()
        }
    }

    LaunchedEffect(cartUiState?.errorMessage) {
        cartUiState?.errorMessage?.let { err ->
            snackbarHostState.showSnackbar(message = err, duration = SnackbarDuration.Short)
            cartViewModel?.dismissMessages()
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("product_list_screen"),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(bottom = 12.dp)
                ) {
                    // Header Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onNavigateBack,
                                modifier = Modifier.testTag("btn_back")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Slate800
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Column {
                                Text(
                                    text = "Product Inventory",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 19.sp,
                                    color = Slate900,
                                    letterSpacing = (-0.3).sp
                                )
                                Text(
                                    text = "${uiState.totalProductCount} items in Room Database",
                                    fontSize = 12.sp,
                                    color = Slate500,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            NetworkStatusIndicator()
                            // Cart Badge Button (if CartViewModel provided)
                            if (cartViewModel != null && cartUiState != null) {
                                IconButton(
                                    onClick = { showCartSheet = true },
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(TealCard, CircleShape)
                                        .testTag("btn_header_cart")
                                ) {
                                    BadgedBox(
                                        badge = {
                                            if ((cartUiState?.totalItemCount ?: 0) > 0) {
                                                Badge(
                                                    containerColor = Amber500,
                                                    contentColor = Slate900
                                                ) {
                                                    Text(
                                                        "${cartUiState?.totalItemCount}",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 10.5.sp
                                                    )
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ShoppingCart,
                                            contentDescription = "Cart",
                                            tint = TealPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            // Barcode Scanner Action Button
                            IconButton(
                                onClick = { showBarcodeScanner = true },
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                    .testTag("btn_top_barcode_scanner")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = "Scan Barcode with Camera",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Theme Toggle Button
                            ThemeToggleIconButton()

                            // Right Action: Add Product Button
                            FilledTonalButton(
                                onClick = { showAddDialog = true },
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = TealPrimary,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                modifier = Modifier.testTag("btn_add_product")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Item", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    // Search Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = { viewModel.onSearchQueryChanged(it) },
                            placeholder = {
                                Text(
                                    "Search product name, barcode...",
                                    fontSize = 14.sp,
                                    color = Slate400
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = TealPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            trailingIcon = {
                                if (uiState.searchQuery.isNotBlank()) {
                                    IconButton(
                                        onClick = { viewModel.onSearchQueryChanged("") },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear Search",
                                            tint = Slate400,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Slate50,
                                unfocusedContainerColor = Slate50,
                                focusedBorderColor = TealPrimary,
                                unfocusedBorderColor = Slate200,
                                cursorColor = TealPrimary
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("product_search_input")
                        )
                    }

                    // Category Pill Filter Bar
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        items(ProductCategory.values()) { category ->
                            val isSelected = uiState.selectedCategory == category
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.onCategorySelected(category) },
                                label = {
                                    Text(
                                        text = category.displayName,
                                        fontSize = 12.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = TealPrimary,
                                    selectedLabelColor = Color.White,
                                    containerColor = Slate100,
                                    labelColor = Slate700
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = if (isSelected) TealPrimary else Slate200
                                ),
                                modifier = Modifier.testTag("chip_category_${category.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            if (cartViewModel != null) {
                CartStickyBottomBar(
                    viewModel = cartViewModel,
                    onViewCartClick = { showCartSheet = true },
                    onCheckoutClick = { showCheckoutDialog = true }
                )
            }
        },
        floatingActionButton = {
            if (cartViewModel == null || (cartUiState?.isCartEmpty == true)) {
                ExtendedFloatingActionButton(
                    onClick = onNavigateToPos,
                    containerColor = TealPrimary,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                    icon = { Icon(Icons.Default.Storefront, contentDescription = null) },
                    text = { Text("Open POS Register", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                    modifier = Modifier.testTag("fab_open_pos")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Slate50)
        ) {
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = TealPrimary)
                }
            } else if (uiState.filteredProducts.isEmpty()) {
                // Empty State
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(TealCard, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Inventory2,
                            contentDescription = null,
                            tint = TealPrimary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (uiState.searchQuery.isNotBlank()) "No products matching \"${uiState.searchQuery}\"" else "No products found",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate800
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (uiState.searchQuery.isNotBlank()) "Try checking the spelling or resetting the category filter." else "Get started by adding items to your store inventory.",
                        fontSize = 13.5.sp,
                        color = Slate500,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = {
                            if (uiState.searchQuery.isNotBlank() || uiState.selectedCategory != ProductCategory.ALL) {
                                viewModel.onSearchQueryChanged("")
                                viewModel.onCategorySelected(ProductCategory.ALL)
                            } else {
                                showAddDialog = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("btn_empty_action")
                    ) {
                        Text(
                            if (uiState.searchQuery.isNotBlank() || uiState.selectedCategory != ProductCategory.ALL) "Reset Filters" else "Add First Product",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("product_list_column")
                ) {
                    // Header Metric Banner
                    item {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                            border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(Slate200)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Showing Results", fontSize = 11.5.sp, color = Slate400, fontWeight = FontWeight.Bold)
                                    Text("${uiState.filteredProducts.size} Items Listed", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Slate800)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    if (uiState.lowStockCount > 0) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Amber100
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(modifier = Modifier.size(6.dp).background(Amber600, CircleShape))
                                                Spacer(modifier = Modifier.width(5.dp))
                                                Text("${uiState.lowStockCount} Low Stock", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Amber700)
                                            }
                                        }
                                    }
                                    if (uiState.outOfStockCount > 0) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Red100
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(modifier = Modifier.size(6.dp).background(Red600, CircleShape))
                                                Spacer(modifier = Modifier.width(5.dp))
                                                Text("${uiState.outOfStockCount} Out", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Red600)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Product Items
                    items(
                        items = uiState.filteredProducts,
                        key = { it.id }
                    ) { product ->
                        val cartItem = cartUiState?.cartItems?.get(product.id)
                        ProductListItemCard(
                            product = product,
                            currencyFormatter = currencyFormatter,
                            inCartQuantity = cartItem?.quantity ?: 0,
                            onItemClick = { selectedProductForDetail = product },
                            onAddToCart = {
                                cartViewModel?.addProductToCart(product, 1)
                            },
                            onQuickStockEdit = {
                                showQuickStockDialog = product
                            },
                            onDelete = {
                                viewModel.deleteProduct(product.id)
                            }
                        )
                    }
                }
            }
        }
    }

    // Shopping Cart Bottom Sheet
    if (showCartSheet && cartViewModel != null) {
        ShoppingCartSheet(
            viewModel = cartViewModel,
            onCheckoutClick = {
                showCartSheet = false
                showCheckoutDialog = true
            },
            onScanBarcodeClick = {
                showBarcodeScanner = true
            },
            onDismiss = { showCartSheet = false }
        )
    }

    // Finalize Checkout Dialog
    if (showCheckoutDialog && cartViewModel != null) {
        CheckoutFinalizeDialog(
            viewModel = cartViewModel,
            onDismiss = { showCheckoutDialog = false },
            onSaleCompleted = { receipt ->
                completedReceipt = receipt
            }
        )
    }

    // Digital Receipt Completed Dialog
    completedReceipt?.let { receipt ->
        DigitalReceiptDialog(
            receipt = receipt,
            onDismiss = { completedReceipt = null }
        )
    }

    // CameraX Barcode Scanner Dialog
    if (showBarcodeScanner) {
        BarcodeScannerDialog(
            products = uiState.products,
            onProductScanned = { scannedProduct ->
                if (cartViewModel != null) {
                    cartViewModel.addProductToCart(scannedProduct)
                } else {
                    viewModel.addProduct(
                        name = scannedProduct.name,
                        price = scannedProduct.price,
                        category = scannedProduct.category,
                        stockQuantity = scannedProduct.stockQuantity,
                        barcode = scannedProduct.barcode
                    )
                }
            },
            onDismiss = { showBarcodeScanner = false }
        )
    }

    // Add Product Dialog
    if (showAddDialog) {
        AddProductDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, price, costPrice, category, stock, threshold, barcode, unit ->
                viewModel.addProduct(
                    name = name,
                    price = price,
                    costPrice = costPrice,
                    category = category,
                    stockQuantity = stock,
                    lowStockThreshold = threshold,
                    barcode = barcode,
                    unit = unit
                )
                showAddDialog = false
            }
        )
    }

    // Quick Stock Adjustment Dialog
    showQuickStockDialog?.let { product ->
        QuickStockEditDialog(
            product = product,
            onDismiss = { showQuickStockDialog = null },
            onUpdateStock = { newQuantity ->
                viewModel.updateStockLevel(product.id, newQuantity)
                showQuickStockDialog = null
            }
        )
    }

    selectedProductForDetail?.let { product ->
        ProductDetailDialog(
            product = product,
            onDismiss = { selectedProductForDetail = null },
            onPrintBarcode = { 
                selectedProductForDetail = null
                showBluetoothPrinterForProduct = it
            }
        )
    }

    showBluetoothPrinterForProduct?.let { product ->
        com.example.presentation.products.BluetoothBarcodePrinterDialog(
            product = product,
            onDismiss = { showBluetoothPrinterForProduct = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductListItemCard(
    product: ProductItem,
    currencyFormatter: NumberFormat,
    inCartQuantity: Int = 0,
    onItemClick: () -> Unit,
    onAddToCart: () -> Unit,
    onQuickStockEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            when (dismissValue) {
                SwipeToDismissBoxValue.EndToStart -> {
                    showDeleteConfirm = true
                    false // Return false so it snaps back while showing dialog
                }
                SwipeToDismissBoxValue.StartToEnd -> {
                    onQuickStockEdit()
                    false // Return false so it snaps back
                }
                else -> false
            }
        },
        positionalThreshold = { it * 0.35f }
    )

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Product") },
            text = { Text("Are you sure you want to delete ${product.name}? This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete()
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromEndToStart = true,
        enableDismissFromStartToEnd = true,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val targetValue = dismissState.targetValue
            
            val backgroundColor by animateColorAsState(
                when (targetValue) {
                    SwipeToDismissBoxValue.StartToEnd -> Color(0xFF10B981) // Emerald for edit
                    SwipeToDismissBoxValue.EndToStart -> Color(0xFFEF4444) // Red for delete
                    SwipeToDismissBoxValue.Settled -> Color.Transparent
                }
            )
            
            val icon = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Icons.Default.Edit
                SwipeToDismissBoxValue.EndToStart -> Icons.Default.Delete
                else -> Icons.Default.Delete
            }
            
            val alignment = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                else -> Alignment.CenterEnd
            }

            if (direction != SwipeToDismissBoxValue.Settled) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(backgroundColor)
                        .padding(horizontal = 24.dp),
                    contentAlignment = alignment
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        },
        content = {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = SolidColor(
                        if (inCartQuantity > 0) TealPrimary
                        else if (product.isOutOfStock) Red200
                        else if (product.isLowStock) Amber200
                        else Slate200
                    )
                ),
                modifier = modifier
                    .fillMaxWidth()
                    .clickable { onItemClick() }
                    .testTag("product_card_${product.id}")
            ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Icon + Info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Category Icon Thumbnail
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            when {
                                product.isOutOfStock -> Red50
                                product.isLowStock -> Amber50
                                else -> TealCard
                            },
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (product.category) {
                            ProductCategory.BEVERAGES -> Icons.Default.LocalDrink
                            ProductCategory.SNACKS -> Icons.Default.Fastfood
                            ProductCategory.INSTANT_MEALS -> Icons.Default.RamenDining
                            ProductCategory.CANNED_GOODS -> Icons.Default.Kitchen
                            ProductCategory.HOUSEHOLD -> Icons.Default.CleaningServices
                            else -> Icons.Default.Inventory2
                        },
                        contentDescription = null,
                        tint = when {
                            product.isOutOfStock -> Red600
                            product.isLowStock -> Amber600
                            else -> TealPrimary
                        },
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = product.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Slate900,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = currencyFormatter.format(product.price),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.5.sp,
                            color = TealPrimary
                        )
                        Text(
                            text = "•",
                            color = Slate400,
                            fontSize = 12.sp
                        )
                        Text(
                            text = product.category.displayName,
                            color = Slate500,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Stock Level Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = when {
                                product.isOutOfStock -> Red100
                                product.isLowStock -> Amber100
                                else -> Emerald100
                            },
                            modifier = Modifier.clickable { onQuickStockEdit() }
                        ) {
                            Text(
                                text = when {
                                    product.isOutOfStock -> "Out of Stock"
                                    product.isLowStock -> "Low Stock: ${product.stockQuantity} ${product.unit}"
                                    else -> "${product.stockQuantity} ${product.unit} in stock"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    product.isOutOfStock -> Red600
                                    product.isLowStock -> Amber700
                                    else -> Emerald700
                                },
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                            )
                        }

                        if (inCartQuantity > 0) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = TealCard
                            ) {
                                Text(
                                    text = "$inCartQuantity in Cart",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TealDark,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp)
                                )
                            }
                        }

                        if (product.barcode != null) {
                            Text(
                                text = "#${product.barcode}",
                                fontSize = 10.5.sp,
                                color = Slate400,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Right Actions: Quick-Add to Cart (+Cart) and Delete/Manage
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Add to Cart Button
                Button(
                    onClick = onAddToCart,
                    enabled = !product.isOutOfStock,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (inCartQuantity > 0) TealDark else TealPrimary,
                        disabledContainerColor = Slate200
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier
                        .height(38.dp)
                        .testTag("btn_add_to_cart_${product.id}")
                ) {
                    Icon(
                        imageVector = if (inCartQuantity > 0) Icons.Default.AddShoppingCart else Icons.Default.ShoppingCart,
                        contentDescription = "Add to Cart",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (inCartQuantity > 0) "+$inCartQuantity" else "+Cart",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Delete Button
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("btn_delete_${product.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = "Delete Item",
                        tint = Slate400,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }
    }
    }
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, price: Double, costPrice: Double, category: ProductCategory, stock: Int, threshold: Int, barcode: String?, unit: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var priceStr by remember { mutableStateOf("") }
    var costPriceStr by remember { mutableStateOf("") }
    var stockStr by remember { mutableStateOf("") }
    var thresholdStr by remember { mutableStateOf("5") }
    var barcodeStr by remember { mutableStateOf("") }
    var unitStr by remember { mutableStateOf("pc") }
    var selectedCategory by remember { mutableStateOf(ProductCategory.SNACKS) }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(TealCard, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.AddShoppingCart, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text("Add New Inventory Item", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (validationError != null) {
                    Surface(
                        color = Red100,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = validationError ?: "",
                            color = Red600,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; validationError = null },
                    label = { Text("Product Name *") },
                    placeholder = { Text("e.g. San Miguel Pale Pilsen 330ml") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_product_name")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = priceStr,
                        onValueChange = { priceStr = it; validationError = null },
                        label = { Text("Selling Price (₱) *") },
                        placeholder = { Text("0.00") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_product_price")
                    )
                    OutlinedTextField(
                        value = costPriceStr,
                        onValueChange = { costPriceStr = it },
                        label = { Text("Cost Price (₱)") },
                        placeholder = { Text("0.00") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_product_cost_price")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = stockStr,
                        onValueChange = { stockStr = it },
                        label = { Text("Initial Stock") },
                        placeholder = { Text("0") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_product_stock")
                    )
                    OutlinedTextField(
                        value = unitStr,
                        onValueChange = { unitStr = it },
                        label = { Text("Unit") },
                        placeholder = { Text("pc, can, bottle") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_product_unit")
                    )
                }

                // Category Selection Dropdown
                ExposedDropdownMenuBox(
                    expanded = categoryDropdownExpanded,
                    onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedCategory.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .testTag("dropdown_product_category")
                    )
                    ExposedDropdownMenu(
                        expanded = categoryDropdownExpanded,
                        onDismissRequest = { categoryDropdownExpanded = false }
                    ) {
                        ProductCategory.values().filter { it != ProductCategory.ALL }.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.displayName) },
                                onClick = {
                                    selectedCategory = category
                                    categoryDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = barcodeStr,
                    onValueChange = { barcodeStr = it },
                    label = { Text("Barcode / SKU (Optional)") },
                    placeholder = { Text("e.g. 4800016644021") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_product_barcode")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        validationError = "Please enter a product name"
                        return@Button
                    }
                    val price = priceStr.toDoubleOrNull()
                    if (price == null || price < 0) {
                        validationError = "Please enter a valid selling price"
                        return@Button
                    }
                    val costPrice = costPriceStr.toDoubleOrNull() ?: 0.0
                    val stock = stockStr.toIntOrNull() ?: 0
                    val threshold = thresholdStr.toIntOrNull() ?: 5
                    val barcode = barcodeStr.trim().ifBlank { null }
                    val unit = unitStr.trim().ifBlank { "pc" }

                    onConfirm(name, price, costPrice, selectedCategory, stock, threshold, barcode, unit)
                },
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("btn_confirm_add_product")
            ) {
                Text("Save to Room DB", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("btn_cancel_add_product")
            ) {
                Text("Cancel", color = Slate600)
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun QuickStockEditDialog(
    product: ProductItem,
    onDismiss: () -> Unit,
    onUpdateStock: (Int) -> Unit
) {
    var stockCount by remember { mutableStateOf(product.stockQuantity.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Update Stock Level", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = product.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.5.sp,
                    color = Slate800
                )
                OutlinedTextField(
                    value = stockCount,
                    onValueChange = { stockCount = it },
                    label = { Text("Current Stock (${product.unit})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_quick_stock_edit")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val count = stockCount.toIntOrNull() ?: 0
                    onUpdateStock(count)
                },
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("btn_confirm_stock_update")
            ) {
                Text("Update", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Slate600)
            }
        },
        shape = RoundedCornerShape(18.dp)
    )
}
