package com.example.presentation.receipt

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Receipt
import com.example.domain.receipt.ReceiptGenerator
import com.example.presentation.pos.printer.BluetoothPrinterDiscoveryDialog
import com.example.ui.theme.*

/**
 * Dedicated Full Screen Composable for displaying, printing, exporting, and sharing
 * a formatted digital sales receipt generated after checkout in ShoppingCartViewModel / POS.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DigitalReceiptScreen(
    receipt: Receipt,
    onDone: () -> Unit,
    onNavigateBack: () -> Unit = onDone,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isGeneratingPdf by remember { mutableStateOf(false) }
    var showPrinterDialog by remember { mutableStateOf(false) }

    if (showPrinterDialog) {
        BluetoothPrinterDiscoveryDialog(
            receipt = receipt,
            onDismiss = { showPrinterDialog = false }
        )
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("digital_receipt_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Digital Receipt",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Slate900
                        )
                        Text(
                            text = "Receipt #${receipt.receiptNumber}",
                            fontSize = 12.sp,
                            color = Slate500,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("btn_receipt_back")
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
                        onClick = {
                            ReceiptGenerator.copyToClipboard(context, receipt)
                            Toast.makeText(context, "Receipt copied to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("btn_receipt_top_copy")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Text",
                            tint = Slate700
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Quick Action Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Bluetooth Thermal Print Button
                        Button(
                            onClick = { showPrinterDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1.1f)
                                .height(46.dp)
                                .testTag("btn_receipt_screen_print_pos")
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Print POS", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        // Share PDF Document Button
                        FilledTonalButton(
                            onClick = {
                                isGeneratingPdf = true
                                try {
                                    ReceiptGenerator.sharePdf(context, receipt)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error sharing PDF: ${e.message}", Toast.LENGTH_SHORT).show()
                                } finally {
                                    isGeneratingPdf = false
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("btn_receipt_screen_share_pdf")
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share PDF", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        // Text Summary Share Button
                        OutlinedButton(
                            onClick = {
                                ReceiptGenerator.shareTextSummary(context, receipt)
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = ButtonDefaults.outlinedButtonBorder.copy(brush = SolidColor(Slate300)),
                            modifier = Modifier
                                .weight(0.9f)
                                .height(46.dp)
                                .testTag("btn_receipt_screen_share_text")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Main Complete & Start Next Sale Button
                    Button(
                        onClick = onDone,
                        colors = ButtonDefaults.buttonColors(containerColor = Slate900),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_receipt_screen_done")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Done / Next Sale", fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Slate100)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Success Transaction Banner
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Emerald50,
                border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(Emerald200)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Emerald100),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = Emerald600,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Payment Successfully Processed",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Emerald700
                        )
                        Text(
                            text = "Total paid: ${receipt.formattedTotal} via ${receipt.paymentMethod}",
                            fontSize = 12.5.sp,
                            color = Emerald700
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Thermal Paper Styled Digital Receipt Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(Slate200)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("formatted_receipt_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Store Branding Header
                    Text(
                        text = receipt.storeName.uppercase(),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Slate900,
                        letterSpacing = 1.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = receipt.storeAddress,
                        fontSize = 11.5.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Slate500,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = receipt.storeContact,
                        fontSize = 11.5.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Slate500,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    ReceiptScreenDottedDivider()
                    Spacer(modifier = Modifier.height(10.dp))

                    // Meta Transaction Information
                    ReceiptScreenMetaRow("RECEIPT NO:", receipt.receiptNumber, isBold = true)
                    ReceiptScreenMetaRow("DATE/TIME :", receipt.formattedDate)
                    ReceiptScreenMetaRow("CASHIER   :", receipt.cashierName)
                    ReceiptScreenMetaRow("PAYMENT   :", receipt.paymentMethod, valueColor = TealDark)

                    Spacer(modifier = Modifier.height(10.dp))
                    ReceiptScreenDottedDivider()
                    Spacer(modifier = Modifier.height(8.dp))

                    // Column Table Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "ITEM",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Slate700,
                            modifier = Modifier.weight(2f)
                        )
                        Text(
                            text = "QTY",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Slate700,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(0.8f)
                        )
                        Text(
                            text = "PRICE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Slate700,
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(1.1f)
                        )
                        Text(
                            text = "TOTAL",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Slate700,
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(1.2f)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    ReceiptScreenDottedDivider()
                    Spacer(modifier = Modifier.height(6.dp))

                    // Purchased Items Breakdown
                    receipt.items.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = item.name,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Slate900,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(2f)
                            )
                            Text(
                                text = "${item.quantity}",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Slate800,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(0.8f)
                            )
                            Text(
                                text = item.formattedUnitPrice,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Slate500,
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(1.1f)
                            )
                            Text(
                                text = item.formattedSubtotal,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = Slate900,
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(1.2f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    ReceiptScreenDottedDivider()
                    Spacer(modifier = Modifier.height(10.dp))

                    // Totals, Discounts, VAT
                    ReceiptScreenMetaRow("Total Items Count:", "${receipt.totalItemCount} items")
                    ReceiptScreenMetaRow("Subtotal:", receipt.formattedSubtotal)
                    if (receipt.discount > 0) {
                        ReceiptScreenMetaRow("Discount Applied:", "-${receipt.formattedDiscount}", valueColor = Red600)
                    }
                    if (receipt.tax > 0) {
                        ReceiptScreenMetaRow("VAT / Tax (12%):", receipt.formattedTax)
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    ReceiptScreenDottedDivider()
                    Spacer(modifier = Modifier.height(8.dp))

                    // Grand Total
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TOTAL DUE",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            color = Slate900
                        )
                        Text(
                            text = receipt.formattedTotal,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            color = Slate900
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    ReceiptScreenMetaRow("Amount Tendered:", receipt.formattedTendered)
                    ReceiptScreenMetaRow(
                        "CHANGE DUE:",
                        receipt.formattedChange,
                        isBold = true,
                        valueColor = Emerald600
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    ReceiptScreenDottedDivider()
                    Spacer(modifier = Modifier.height(12.dp))

                    // Footer Appreciation Notes
                    Text(
                        text = "Maraming Salamat Po!",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Slate800
                    )
                    Text(
                        text = "Thank you for shopping with us!",
                        fontSize = 11.5.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Slate500
                    )
                    Text(
                        text = "This is a computer-generated digital receipt.",
                        fontSize = 10.5.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Slate400
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun ReceiptScreenMetaRow(
    label: String,
    value: String,
    isBold: Boolean = false,
    valueColor: Color = Slate800
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            color = Slate500
        )
        Text(
            text = value,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = valueColor
        )
    }
}

@Composable
private fun ReceiptScreenDottedDivider() {
    Text(
        text = "- - - - - - - - - - - - - - - - - - - - - - - - - - - - - -",
        fontSize = 11.sp,
        fontFamily = FontFamily.Monospace,
        color = Slate300,
        maxLines = 1,
        modifier = Modifier.fillMaxWidth()
    )
}
