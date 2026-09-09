package com.example.presentation.pos

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.domain.model.Receipt
import com.example.domain.receipt.ReceiptGenerator
import com.example.presentation.pos.printer.BluetoothPrinterDiscoveryDialog
import com.example.ui.theme.*

@Composable
fun DigitalReceiptDialog(
    receipt: Receipt,
    onDismiss: () -> Unit,
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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(24.dp))
                .testTag("digital_receipt_dialog"),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Success Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Emerald100),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Success",
                                tint = Emerald600,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Transaction Complete",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )
                            Text(
                                text = "Digital Receipt Generated",
                                fontSize = 12.sp,
                                color = Slate400
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("receipt_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Slate400
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable Thermal Paper Container
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Slate50)
                        .border(1.dp, Slate200, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Store Info
                        Text(
                            text = receipt.storeName.uppercase(),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Slate900,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = receipt.storeAddress,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Slate500,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = receipt.storeContact,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Slate500
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        ReceiptDottedDivider()
                        Spacer(modifier = Modifier.height(10.dp))

                        // Transaction Meta
                        ReceiptMetaRow("Receipt #:", receipt.receiptNumber, isBold = true)
                        ReceiptMetaRow("Date/Time:", receipt.formattedDate)
                        ReceiptMetaRow("Cashier  :", receipt.cashierName)
                        ReceiptMetaRow("Payment  :", receipt.paymentMethod)

                        Spacer(modifier = Modifier.height(10.dp))
                        ReceiptDottedDivider()
                        Spacer(modifier = Modifier.height(8.dp))

                        // Items Table Header
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
                        ReceiptDottedDivider()
                        Spacer(modifier = Modifier.height(6.dp))

                        // Items List
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
                        ReceiptDottedDivider()
                        Spacer(modifier = Modifier.height(10.dp))

                        // Totals & Financials
                        ReceiptMetaRow("Total Items:", "${receipt.totalItemCount} items")
                        ReceiptMetaRow("Subtotal:", receipt.formattedSubtotal)
                        if (receipt.discount > 0) {
                            ReceiptMetaRow("Discount:", "-${receipt.formattedDiscount}", valueColor = Red600)
                        }
                        if (receipt.tax > 0) {
                            ReceiptMetaRow("VAT / Tax:", receipt.formattedTax)
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        ReceiptDottedDivider()
                        Spacer(modifier = Modifier.height(8.dp))

                        // Grand Total in Prominent Display
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TOTAL AMOUNT",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace,
                                color = Slate900
                            )
                            Text(
                                text = receipt.formattedTotal,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace,
                                color = Slate900
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        ReceiptMetaRow("Amount Tendered:", receipt.formattedTendered)
                        ReceiptMetaRow(
                            "CHANGE DUE:",
                            receipt.formattedChange,
                            isBold = true,
                            valueColor = Emerald600
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        ReceiptDottedDivider()
                        Spacer(modifier = Modifier.height(12.dp))

                        // Footer Note
                        Text(
                            text = "Maraming Salamat Po!",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Slate800
                        )
                        Text(
                            text = "Please keep this digital copy.",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Slate400
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons: Print POS, Share PDF, Share Text, Copy Text
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Print Thermal POS Button
                    Button(
                        onClick = { showPrinterDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                        modifier = Modifier
                            .weight(1.1f)
                            .testTag("receipt_print_pos_button"),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Print POS", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }

                    // Share PDF Button
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
                            .weight(0.9f)
                            .testTag("receipt_share_pdf_button"),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("PDF", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }

                    // Share Text Summary Button
                    FilledTonalButton(
                        onClick = {
                            ReceiptGenerator.shareTextSummary(context, receipt)
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(0.9f)
                            .testTag("receipt_share_text_button"),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }

                    // Copy Text Button
                    OutlinedButton(
                        onClick = {
                            ReceiptGenerator.copyToClipboard(context, receipt)
                            Toast.makeText(context, "Receipt copied to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("receipt_copy_button"),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Primary Done Button
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("receipt_done_button")
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Done / New Transaction", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ReceiptMetaRow(
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
private fun ReceiptDottedDivider() {
    Text(
        text = "- - - - - - - - - - - - - - - - - - - - - - - - - - - - - -",
        fontSize = 11.sp,
        fontFamily = FontFamily.Monospace,
        color = Slate300,
        maxLines = 1,
        modifier = Modifier.fillMaxWidth()
    )
}
