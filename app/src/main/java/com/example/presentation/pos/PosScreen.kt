package com.example.presentation.pos

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.model.CartItem
import com.example.domain.model.ProductItem
import com.example.domain.model.Receipt
import com.example.domain.model.ReceiptItem
import com.example.domain.model.SampleProductCatalog
import com.example.presentation.common.ThemeToggleIconButton
import com.example.presentation.components.NetworkStatusIndicator
import com.example.presentation.pos.barcode.BarcodeScannerDialog
import com.example.presentation.pos.components.ProductGrid
import com.example.ui.theme.*
import androidx.compose.ui.input.pointer.pointerInput
import com.example.domain.auth.StaffSessionManager
import com.example.presentation.auth.SwitchStaffDialog
import com.example.data.local.entity.UserRole
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(
    onNavigateBack: () -> Unit = {},
    products: List<ProductItem> = SampleProductCatalog.items,
    staffSessionManager: StaffSessionManager? = null,
    onCheckoutSuccess: (List<CartItem>, Receipt) -> Unit = { _, _ -> }
) {
    val activeStaff by staffSessionManager?.activeStaff?.collectAsState() ?: remember { mutableStateOf(null) }
    var showSwitchStaffDialog by remember { mutableStateOf(false) }

    // In-memory cart state for active sale
    var cartItems by remember { mutableStateOf<Map<String, CartItem>>(emptyMap()) }
    var showCheckoutDialog by remember { mutableStateOf(false) }
    var showCartBottomSheet by remember { mutableStateOf(false) }
    var showBarcodeScanner by remember { mutableStateOf(false) }
    var completedReceipt by remember { mutableStateOf<Receipt?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val totalItemsCount = remember(cartItems) {
        cartItems.values.sumOf { it.quantity }
    }

    val totalAmount = remember(cartItems) {
        cartItems.values.sumOf { it.subtotal }
    }

    val formattedTotal = remember(totalAmount) {
        "₱${String.format("%,.2f", totalAmount)}"
    }

    fun handleAddToCart(product: ProductItem) {
        val current = cartItems[product.id]
        val updated = if (current != null) {
            current.copy(quantity = current.quantity + 1)
        } else {
            CartItem(product = product, quantity = 1)
        }
        cartItems = cartItems + (product.id to updated)
    }

    fun handleRemoveFromCart(product: ProductItem) {
        val current = cartItems[product.id] ?: return
        if (current.quantity > 1) {
            cartItems = cartItems + (product.id to current.copy(quantity = current.quantity - 1))
        } else {
            cartItems = cartItems - product.id
        }
    }

    fun handleClearCart() {
        cartItems = emptyMap()
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent()
                        staffSessionManager?.recordActivity()
                    }
                }
            }
    ) {
        val isWideScreen = maxWidth > 720.dp

        Scaffold(
            topBar = {
                val staffName = activeStaff?.let {
                    it.username.replaceFirstChar { c -> c.uppercase() } + if (it.role == UserRole.OWNER) " (Owner)" else " (Cashier)"
                } ?: "Admin / Cashier"

                PosTopBar(
                    onNavigateBack = onNavigateBack,
                    activeStaffName = staffName,
                    onSwitchStaff = { showSwitchStaffDialog = true },
                    onLockPos = { staffSessionManager?.lock() },
                    onScanBarcode = { showBarcodeScanner = true }
                )
            },
            bottomBar = {
                // Floating Bottom Checkout Bar on Compact Screens (Phones)
                if (!isWideScreen && totalItemsCount > 0) {
                    CompactCartBottomBar(
                        itemCount = totalItemsCount,
                        totalFormatted = formattedTotal,
                        onViewCart = { showCartBottomSheet = true },
                        onCharge = { showCheckoutDialog = true }
                    )
                }
            }
        ) { innerPadding ->
            if (isWideScreen) {
                // Canonical Two-Pane Layout for Tablets / Foldables
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    // Left Pane: Responsive Product Grid
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        ProductGrid(
                            products = products,
                            cartItemCounts = cartItems.mapValues { it.value.quantity },
                            onAddToCart = ::handleAddToCart,
                            onRemoveFromCart = ::handleRemoveFromCart,
                            onScanBarcode = { showBarcodeScanner = true },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    VerticalDivider(color = Slate200, thickness = 1.dp)

                    // Right Pane: Persistent Order Summary
                    TabletOrderSummaryPane(
                        cartItems = cartItems.values.toList(),
                        totalFormatted = formattedTotal,
                        totalItems = totalItemsCount,
                        onAddToCart = ::handleAddToCart,
                        onRemoveFromCart = ::handleRemoveFromCart,
                        onClearCart = ::handleClearCart,
                        onProceedToCharge = { showCheckoutDialog = true },
                        modifier = Modifier
                            .width(360.dp)
                            .fillMaxHeight()
                    )
                }
            } else {
                // Single Pane Layout for Phones
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    ProductGrid(
                        products = products,
                        cartItemCounts = cartItems.mapValues { it.value.quantity },
                        onAddToCart = ::handleAddToCart,
                        onRemoveFromCart = ::handleRemoveFromCart,
                        onScanBarcode = { showBarcodeScanner = true },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    // Modal Bottom Sheet for Cart on Phones
    if (showCartBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCartBottomSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            CartDetailContent(
                cartItems = cartItems.values.toList(),
                totalFormatted = formattedTotal,
                totalItems = totalItemsCount,
                onAddToCart = ::handleAddToCart,
                onRemoveFromCart = ::handleRemoveFromCart,
                onClearCart = {
                    handleClearCart()
                    showCartBottomSheet = false
                },
                onCharge = {
                    showCartBottomSheet = false
                    showCheckoutDialog = true
                }
            )
        }
    }

    // Checkout / Payment Method Dialog
    if (showCheckoutDialog) {
        CheckoutConfirmationDialog(
            totalAmount = totalAmount,
            totalFormatted = formattedTotal,
            totalItems = totalItemsCount,
            onDismiss = { showCheckoutDialog = false },
            onConfirmPayment = { method, amountTendered, change ->
                val soldItems = cartItems.values.toList()
                val receiptNumber = "RCP-" + SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
                val staffResponsibility = activeStaff?.let {
                    it.username.replaceFirstChar { c -> c.uppercase() } + if (it.role == UserRole.OWNER) " (Owner)" else " (Cashier)"
                } ?: "Admin / Cashier"

                val receipt = Receipt(
                    receiptNumber = receiptNumber,
                    timestamp = System.currentTimeMillis(),
                    items = soldItems.map {
                        ReceiptItem(
                            id = it.product.id,
                            name = it.product.name,
                            unitPrice = it.product.price,
                            quantity = it.quantity,
                            subtotal = it.subtotal,
                            unit = it.product.unit
                        )
                    },
                    subtotal = totalAmount,
                    discount = 0.0,
                    tax = 0.0,
                    totalAmount = totalAmount,
                    paymentMethod = method,
                    amountTendered = amountTendered,
                    change = change,
                    cashierName = staffResponsibility
                )

                onCheckoutSuccess(soldItems, receipt)

                showCheckoutDialog = false
                completedReceipt = receipt
            }
        )
    }

    // Switch Staff Dialog
    if (showSwitchStaffDialog && staffSessionManager != null) {
        SwitchStaffDialog(
            staffSessionManager = staffSessionManager,
            onDismiss = { showSwitchStaffDialog = false }
        )
    }

    // Digital Receipt Dialog (PDF / Text Summary)
    completedReceipt?.let { receipt ->
        DigitalReceiptDialog(
            receipt = receipt,
            onDismiss = {
                handleClearCart()
                completedReceipt = null
            }
        )
    }

    // CameraX Barcode Scanner Dialog
    if (showBarcodeScanner) {
        BarcodeScannerDialog(
            products = products,
            onProductScanned = { scannedProduct ->
                handleAddToCart(scannedProduct)
            },
            onDismiss = {
                showBarcodeScanner = false
            }
        )
    }
}

@Composable
private fun PosTopBar(
    onNavigateBack: () -> Unit,
    activeStaffName: String = "",
    onSwitchStaff: () -> Unit = {},
    onLockPos: () -> Unit = {},
    onScanBarcode: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .border(width = 1.dp, color = Slate100)
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .size(38.dp)
                    .testTag("pos_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            Column {
                Text(
                    text = stringResource(id = R.string.pos_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "REGISTER 01 • ",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        fontWeight = FontWeight.Bold,
                        color = Slate400
                    )
                    Text(
                        text = activeStaffName,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Quick Staff Switch Button
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .clickable { onSwitchStaff() }
                    .testTag("pos_switch_staff_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Badge,
                        contentDescription = "Switch Staff",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Switch",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Quick Lock POS Button
            IconButton(
                onClick = onLockPos,
                modifier = Modifier
                    .size(38.dp)
                    .background(Red50, RoundedCornerShape(8.dp))
                    .testTag("pos_quick_lock_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Lock POS",
                    tint = Red600,
                    modifier = Modifier.size(18.dp)
                )
            }

            NetworkStatusIndicator()
            ThemeToggleIconButton()
            IconButton(
                onClick = onScanBarcode,
                modifier = Modifier
                    .size(38.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                    .testTag("barcode_scanner_button")
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = "Scan Barcode",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun CompactCartBottomBar(
    itemCount: Int,
    totalFormatted: String,
    onViewCart: () -> Unit,
    onCharge: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        color = MaterialTheme.colorScheme.primary,
        shape = RoundedCornerShape(18.dp),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onViewCart() }
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color.White.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$itemCount",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }

                Column {
                    Text(
                        text = "Current Order",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Text(
                        text = totalFormatted,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
            }

            Button(
                onClick = onCharge,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("charge_cart_button")
            ) {
                Text(
                    text = "Charge",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun TabletOrderSummaryPane(
    cartItems: List<CartItem>,
    totalFormatted: String,
    totalItems: Int,
    onAddToCart: (ProductItem) -> Unit,
    onRemoveFromCart: (ProductItem) -> Unit,
    onClearCart: () -> Unit,
    onProceedToCharge: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Current Order",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Slate800
                )
                Text(
                    text = "$totalItems items",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate400
                )
            }

            if (cartItems.isNotEmpty()) {
                TextButton(onClick = onClearCart) {
                    Text("Clear", color = Red600, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (cartItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = null,
                        tint = Slate300,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "Cart is empty",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Slate400
                    )
                    Text(
                        text = "Tap on items in the grid to add them",
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate400
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(cartItems, key = { it.product.id }) { item ->
                    CartItemRow(
                        item = item,
                        onAdd = { onAddToCart(item.product) },
                        onRemove = { onRemoveFromCart(item.product) }
                    )
                }
            }
        }

        HorizontalDivider(color = Slate200, modifier = Modifier.padding(vertical = 12.dp))

        // Total Summary Breakdown
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Subtotal", style = MaterialTheme.typography.bodyMedium, color = Slate400)
                Text(totalFormatted, style = MaterialTheme.typography.bodyMedium, color = Slate800)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Total", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = Slate900)
                Text(totalFormatted, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onProceedToCharge,
            enabled = cartItems.isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text(
                text = "Charge $totalFormatted",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
private fun CartDetailContent(
    cartItems: List<CartItem>,
    totalFormatted: String,
    totalItems: Int,
    onAddToCart: (ProductItem) -> Unit,
    onRemoveFromCart: (ProductItem) -> Unit,
    onClearCart: () -> Unit,
    onCharge: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Current Order",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Slate800
                )
                Text(
                    text = "$totalItems items in cart",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate400
                )
            }

            if (cartItems.isNotEmpty()) {
                TextButton(onClick = onClearCart) {
                    Text("Clear All", color = Red600)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 300.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(cartItems, key = { it.product.id }) { item ->
                CartItemRow(
                    item = item,
                    onAdd = { onAddToCart(item.product) },
                    onRemove = { onRemoveFromCart(item.product) }
                )
            }
        }

        HorizontalDivider(color = Slate200, modifier = Modifier.padding(vertical = 12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Total Amount",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Slate800
            )
            Text(
                text = totalFormatted,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onCharge,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                text = "Proceed to Charge ($totalFormatted)",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun CartItemRow(
    item: CartItem,
    onAdd: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Slate50, RoundedCornerShape(12.dp))
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.product.name,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Slate800,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${item.product.formattedPrice} × ${item.quantity} = ${item.formattedSubtotal}",
                style = MaterialTheme.typography.bodySmall,
                color = Slate400
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Decrease",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }

            Text(
                text = "${item.quantity}",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = Slate800
            )

            IconButton(
                onClick = onAdd,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Increase",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun CheckoutConfirmationDialog(
    totalAmount: Double,
    totalFormatted: String,
    totalItems: Int,
    onDismiss: () -> Unit,
    onConfirmPayment: (method: String, amountTendered: Double, change: Double) -> Unit
) {
    var selectedPaymentMethod by remember { mutableStateOf("Cash") }
    var cashTenderedText by remember { mutableStateOf("") }

    val tendered = remember(selectedPaymentMethod, cashTenderedText, totalAmount) {
        if (selectedPaymentMethod == "Cash") {
            cashTenderedText.toDoubleOrNull() ?: totalAmount
        } else {
            totalAmount
        }
    }

    val change = remember(tendered, totalAmount) {
        maxOf(0.0, tendered - totalAmount)
    }

    val isInsufficient = selectedPaymentMethod == "Cash" &&
            cashTenderedText.isNotBlank() &&
            (cashTenderedText.toDoubleOrNull() ?: 0.0) < totalAmount

    // Smart cash presets based on total amount
    val cashPresets = remember(totalAmount) {
        val standardNotes = listOf(50.0, 100.0, 200.0, 500.0, 1000.0)
        standardNotes.filter { it >= totalAmount }.take(4)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Charge $totalFormatted",
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )
                Text(
                    text = "Total $totalItems item(s) in active cart",
                    fontSize = 12.sp,
                    color = Slate400
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Select Payment Method:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Slate700
                )

                listOf("Cash", "GCash QR", "Maya QR", "Utang (Store Credit)").forEach { method ->
                    val isSelected = selectedPaymentMethod == method
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Slate200,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { selectedPaymentMethod = method }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = method,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Slate800,
                            fontSize = 14.sp
                        )
                        RadioButton(
                            selected = isSelected,
                            onClick = { selectedPaymentMethod = method },
                            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                        )
                    }
                }

                // Cash specific: Tendered & Change calculator
                if (selectedPaymentMethod == "Cash") {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Cash Tendered:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Slate700
                    )

                    OutlinedTextField(
                        value = cashTenderedText,
                        onValueChange = { input ->
                            if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}\$"))) {
                                cashTenderedText = input
                            }
                        },
                        label = { Text("Amount Received") },
                        placeholder = { Text(String.format(Locale.US, "%.2f", totalAmount)) },
                        prefix = { Text("₱ ", fontWeight = FontWeight.Bold) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        isError = isInsufficient,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("cash_tendered_input")
                    )

                    // Quick Presets
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SuggestionChip(
                            onClick = { cashTenderedText = String.format(Locale.US, "%.2f", totalAmount) },
                            label = { Text("Exact", fontSize = 11.sp) },
                            modifier = Modifier.testTag("preset_exact")
                        )
                        cashPresets.forEach { note ->
                            SuggestionChip(
                                onClick = { cashTenderedText = note.toInt().toString() },
                                label = { Text("₱${note.toInt()}", fontSize = 11.sp) },
                                modifier = Modifier.testTag("preset_${note.toInt()}")
                            )
                        }
                    }

                    // Change Summary Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isInsufficient) Red50 else Emerald50)
                            .border(1.dp, if (isInsufficient) Red200 else Emerald200, RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        if (isInsufficient) {
                            Text(
                                text = "⚠️ Cash is less than total by ₱${String.format(Locale.US, "%,.2f", totalAmount - (cashTenderedText.toDoubleOrNull() ?: 0.0))}",
                                color = Red600,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Change Due:",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Emerald700
                                )
                                Text(
                                    text = "₱${String.format(Locale.US, "%,.2f", change)}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Emerald700
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalTendered = if (selectedPaymentMethod == "Cash") {
                        cashTenderedText.toDoubleOrNull() ?: totalAmount
                    } else {
                        totalAmount
                    }
                    val finalChange = maxOf(0.0, finalTendered - totalAmount)
                    onConfirmPayment(selectedPaymentMethod, finalTendered, finalChange)
                },
                enabled = !isInsufficient,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.testTag("confirm_sale_button")
            ) {
                Text("Confirm Sale & Receipt")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Slate400)
            }
        },
        shape = RoundedCornerShape(18.dp),
        containerColor = MaterialTheme.colorScheme.surface
    )
}
