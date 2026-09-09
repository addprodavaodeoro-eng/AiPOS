package com.example.presentation.cart

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.CartItem
import com.example.domain.model.Receipt
import com.example.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

/**
 * Bottom Floating Bar showing active Cart summary with buttons to view cart or finalize sale.
 */
@Composable
fun CartStickyBottomBar(
    viewModel: ShoppingCartViewModel,
    onViewCartClick: () -> Unit,
    onCheckoutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val currencyFormatter = remember {
        NumberFormat.getCurrencyInstance(Locale("en", "PH")).apply {
            maximumFractionDigits = 2
        }
    }

    AnimatedVisibility(
        visible = !uiState.isCartEmpty,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier
    ) {
        Surface(
            color = Slate900,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            shadowElevation = 12.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Cart Info Summary
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { onViewCartClick() }
                        .padding(vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(TealPrimary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Badge(
                            containerColor = Amber500,
                            contentColor = Slate900,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 4.dp, y = (-2).dp)
                        ) {
                            Text(
                                text = "${uiState.totalItemCount}",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.sp
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = "Cart",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "${uiState.totalItemCount} ${if (uiState.totalItemCount == 1) "item" else "items"}",
                            color = Slate400,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = currencyFormatter.format(uiState.grandTotal),
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Action Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onViewCartClick,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = ButtonDefaults.outlinedButtonBorder.copy(brush = SolidColor(Slate700)),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("btn_view_cart_summary")
                    ) {
                        Text("View Cart", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = onCheckoutClick,
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("btn_quick_checkout")
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Charge", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Reusable Shopping Cart UI Modal BottomSheet component for the POS & Inventory workflow.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingCartSheet(
    viewModel: ShoppingCartViewModel,
    onCheckoutClick: () -> Unit,
    onDismiss: () -> Unit,
    onScanBarcodeClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val currencyFormatter = remember {
        NumberFormat.getCurrencyInstance(Locale("en", "PH")).apply {
            maximumFractionDigits = 2
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SurfaceLight,
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = Slate300,
                width = 44.dp,
                height = 4.dp
            )
        },
        modifier = modifier.testTag("shopping_cart_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            // Cart Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(TealCard, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = null,
                            tint = TealPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Current Sale Cart",
                            fontSize = 17.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                        Text(
                            text = "${uiState.totalItemCount} items selected",
                            fontSize = 12.sp,
                            color = Slate500,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (onScanBarcodeClick != null) {
                        IconButton(
                            onClick = {
                                onDismiss()
                                onScanBarcodeClick()
                            },
                            modifier = Modifier
                                .size(34.dp)
                                .background(TealLight, CircleShape)
                                .testTag("btn_cart_scan_barcode")
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Scan Barcode",
                                tint = TealPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    if (!uiState.isCartEmpty) {
                        TextButton(
                            onClick = { viewModel.clearCart() },
                            colors = ButtonDefaults.textButtonColors(contentColor = Red600),
                            modifier = Modifier.testTag("btn_clear_cart")
                        ) {
                            Icon(Icons.Outlined.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Clear", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            HorizontalDivider(color = Slate100, modifier = Modifier.padding(vertical = 6.dp))

            // Cart Items List
            if (uiState.isCartEmpty) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp, horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(Slate100, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.RemoveShoppingCart,
                            contentDescription = null,
                            tint = Slate400,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Your cart is empty",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Slate800
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Select products from the catalog to add them to this sale session.",
                        fontSize = 13.sp,
                        color = Slate500,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .heightIn(max = 320.dp)
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = uiState.itemsList,
                        key = { it.product.id }
                    ) { cartItem ->
                        ShoppingCartItemRow(
                            cartItem = cartItem,
                            currencyFormatter = currencyFormatter,
                            onIncrement = { viewModel.incrementItem(cartItem.product.id) },
                            onDecrement = { viewModel.decrementItem(cartItem.product.id) },
                            onRemove = { viewModel.removeItem(cartItem.product.id) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Cart Summary Breakdown
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .background(Slate50, RoundedCornerShape(14.dp))
                        .border(1.dp, Slate200, RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Subtotal", color = Slate600, fontSize = 13.sp)
                        Text(currencyFormatter.format(uiState.subtotal), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Slate800)
                    }

                    if (uiState.discountPercent > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Discount (${uiState.discountPercent}%)", color = Emerald700, fontSize = 13.sp)
                            Text("- ${currencyFormatter.format(uiState.discountAmount)}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Emerald700)
                        }
                    }

                    HorizontalDivider(color = Slate200, modifier = Modifier.padding(vertical = 4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Total Amount Due", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Slate900)
                        Text(
                            text = currencyFormatter.format(uiState.grandTotal),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = TealPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Button to Finalize Sale
                Button(
                    onClick = onCheckoutClick,
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .height(52.dp)
                        .testTag("btn_proceed_checkout")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Charge / Checkout", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        Text(
                            text = currencyFormatter.format(uiState.grandTotal),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Individual Row Component for a Cart Item.
 */
@Composable
fun ShoppingCartItemRow(
    cartItem: CartItem,
    currencyFormatter: NumberFormat,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(Slate200)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("cart_item_${cartItem.product.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Title & Unit Price
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = cartItem.product.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Slate900,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${currencyFormatter.format(cartItem.product.price)} / ${cartItem.product.unit}",
                        fontSize = 12.sp,
                        color = Slate500,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "= ${currencyFormatter.format(cartItem.subtotal)}",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = TealPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Right: Quantity Stepper Controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onDecrement,
                    modifier = Modifier
                        .size(32.dp)
                        .background(Slate100, CircleShape)
                        .testTag("btn_cart_decrement_${cartItem.product.id}")
                ) {
                    Icon(
                        imageVector = if (cartItem.quantity == 1) Icons.Outlined.Delete else Icons.Default.Remove,
                        contentDescription = "Decrease",
                        tint = if (cartItem.quantity == 1) Red600 else Slate700,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .widthIn(min = 28.dp)
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${cartItem.quantity}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp,
                        color = Slate900
                    )
                }

                IconButton(
                    onClick = onIncrement,
                    modifier = Modifier
                        .size(32.dp)
                        .background(TealCard, CircleShape)
                        .testTag("btn_cart_increment_${cartItem.product.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Increase",
                        tint = TealPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Dialog for entering payment and finalizing sale with change calculation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutFinalizeDialog(
    viewModel: ShoppingCartViewModel,
    onDismiss: () -> Unit,
    onSaleCompleted: (Receipt) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var paymentMethod by remember { mutableStateOf("Cash") }
    var customerName by remember { mutableStateOf(uiState.customerName) }
    var cashReceivedInput by remember { mutableStateOf(if (uiState.paymentAmountReceived > 0) uiState.paymentAmountReceived.toString() else "") }
    var discountSelection by remember { mutableStateOf(uiState.discountPercent) }

    val currencyFormatter = remember {
        NumberFormat.getCurrencyInstance(Locale("en", "PH")).apply {
            maximumFractionDigits = 2
        }
    }

    val cashReceived = cashReceivedInput.toDoubleOrNull() ?: 0.0
    val discountAmt = uiState.subtotal * (discountSelection / 100.0)
    val totalDue = (uiState.subtotal - discountAmt).coerceAtLeast(0.0)
    val change = (cashReceived - totalDue).coerceAtLeast(0.0)
    val isValidPayment = paymentMethod != "Cash" || cashReceived >= totalDue

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(TealCard, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("Finalize Sale", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("${uiState.totalItemCount} items • Due ${currencyFormatter.format(totalDue)}", fontSize = 12.sp, color = Slate500)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Customer Name (Optional)
                OutlinedTextField(
                    value = customerName,
                    onValueChange = {
                        customerName = it
                        viewModel.setCustomerName(it)
                    },
                    label = { Text("Customer Name (Optional)") },
                    placeholder = { Text("Walk-in Customer") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_customer_name")
                )

                // Quick Discount Selector
                Column {
                    Text("Discount / Special Rate", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = Slate700)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(0.0 to "None", 5.0 to "5%", 10.0 to "10%", 20.0 to "20% (Senior)").forEach { (pct, label) ->
                            val isSelected = discountSelection == pct
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    discountSelection = pct
                                    viewModel.setDiscountPercent(pct)
                                },
                                label = { Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = TealPrimary,
                                    selectedLabelColor = Color.White
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }

                // Payment Method Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Cash", "GCash", "Maya").forEach { method ->
                        val isSelected = paymentMethod == method
                        OutlinedButton(
                            onClick = {
                                paymentMethod = method
                                if (method != "Cash") {
                                    cashReceivedInput = totalDue.toString()
                                    viewModel.setPaymentAmountReceived(totalDue)
                                }
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isSelected) TealCard else Color.Transparent,
                                contentColor = if (isSelected) TealDark else Slate600
                            ),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = SolidColor(if (isSelected) TealPrimary else Slate300)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(method, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, fontSize = 12.sp)
                        }
                    }
                }

                // Cash Tendered Input
                if (paymentMethod == "Cash") {
                    OutlinedTextField(
                        value = cashReceivedInput,
                        onValueChange = {
                            cashReceivedInput = it
                            val amt = it.toDoubleOrNull() ?: 0.0
                            viewModel.setPaymentAmountReceived(amt)
                        },
                        label = { Text("Amount Received (₱)") },
                        placeholder = { Text("e.g. 500.00") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_cash_received")
                    )

                    // Quick Cash Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(50, 100, 200, 500, 1000).forEach { bill ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Slate100,
                                modifier = Modifier
                                    .clickable {
                                        cashReceivedInput = bill.toString()
                                        viewModel.setPaymentAmountReceived(bill.toDouble())
                                    }
                            ) {
                                Text("₱$bill", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Slate700, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                            }
                        }
                    }

                    // Change Calculation Display
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (cashReceived >= totalDue) Emerald50 else Amber50, RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (cashReceived >= totalDue) "Change to Return:" else "Remaining Balance:",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = if (cashReceived >= totalDue) Emerald700 else Amber700
                        )
                        Text(
                            text = if (cashReceived >= totalDue) currencyFormatter.format(change) else currencyFormatter.format(totalDue - cashReceived),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = if (cashReceived >= totalDue) Emerald700 else Amber700
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.setCustomerName(customerName)
                    viewModel.setDiscountPercent(discountSelection)
                    viewModel.setPaymentAmountReceived(cashReceived)
                    viewModel.finalizeSale(
                        paymentMethod = paymentMethod,
                        onSaleCompleted = { receipt ->
                            onDismiss()
                            onSaleCompleted(receipt)
                        }
                    )
                },
                enabled = isValidPayment && !uiState.isFinalizingSale,
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("btn_confirm_checkout_final")
            ) {
                if (uiState.isFinalizingSale) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                } else {
                    Text("Complete Sale & Deduct DB", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Slate600)
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
