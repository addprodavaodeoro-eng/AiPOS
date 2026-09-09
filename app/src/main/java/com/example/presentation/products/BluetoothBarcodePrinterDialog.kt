package com.example.presentation.products

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.domain.model.ProductItem
import com.example.domain.printer.BluetoothPrinter
import com.example.domain.printer.BluetoothReceiptPrinterManager
import com.example.domain.printer.PrintJobStatus
import com.example.ui.theme.*
import kotlinx.coroutines.launch

/**
 * Modern Bottom Sheet / Dialog for Discovering Bluetooth POS Printers,
 * Selecting Paper Width (58mm / 80mm), and executing ESC/POS Receipt Printing.
 */
@Composable
fun BluetoothBarcodePrinterDialog(
    product: ProductItem,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val printerManager = remember { BluetoothReceiptPrinterManager(context) }

    val discoveredPrinters by printerManager.discoveredPrinters.collectAsState()
    val printStatus by printerManager.printStatus.collectAsState()

    var selectedPrinter by remember { mutableStateOf<BluetoothPrinter?>(null) }
    var selectedPaperWidth by remember { mutableStateOf(58) } // 58mm or 80mm

    val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
    } else {
        arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.all { it }) {
            printerManager.loadPairedPrinters()
            printerManager.startDiscovery()
        } else {
            printerManager.loadPairedPrinters()
        }
    }

    LaunchedEffect(Unit) {
        if (printerManager.hasBluetoothPermission()) {
            printerManager.loadPairedPrinters()
            printerManager.startDiscovery()
        } else {
            permissionLauncher.launch(requiredPermissions)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            printerManager.cleanup()
        }
    }

    // Auto-select first printer if none selected
    LaunchedEffect(discoveredPrinters) {
        if (selectedPrinter == null && discoveredPrinters.isNotEmpty()) {
            selectedPrinter = discoveredPrinters.first()
        }
    }

    Dialog(
        onDismissRequest = {
            if (printStatus !is PrintJobStatus.Printing && printStatus !is PrintJobStatus.Connecting) {
                onDismiss()
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(24.dp))
                .testTag("bluetooth_printer_dialog"),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Blue100,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Print,
                                    contentDescription = null,
                                    tint = Blue600,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Bluetooth POS Printer",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )
                            Text(
                                text = "Discover & print thermal receipt",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate400
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_printer_dialog_button")
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Slate400)
                    }
                }

                // Status Banner (Scanning, Connecting, Printing, Error, Success)
                AnimatedVisibility(
                    visible = printStatus !is PrintJobStatus.Idle,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = when (printStatus) {
                            is PrintJobStatus.Scanning -> Blue100
                            is PrintJobStatus.Connecting, is PrintJobStatus.Printing -> TealLight
                            is PrintJobStatus.Success -> Emerald100
                            is PrintJobStatus.Error -> Red100
                            PrintJobStatus.Idle -> Slate100
                        },
                        border = BorderStroke(
                            1.dp,
                            when (printStatus) {
                                is PrintJobStatus.Scanning -> Blue600.copy(alpha = 0.3f)
                                is PrintJobStatus.Connecting, is PrintJobStatus.Printing -> TealPrimary.copy(alpha = 0.3f)
                                is PrintJobStatus.Success -> Emerald600.copy(alpha = 0.3f)
                                is PrintJobStatus.Error -> Red600.copy(alpha = 0.3f)
                                PrintJobStatus.Idle -> Slate200
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            when (val status = printStatus) {
                                is PrintJobStatus.Scanning -> {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Blue600)
                                    Text("Scanning for nearby Bluetooth printers…", fontSize = 12.sp, color = Blue600, fontWeight = FontWeight.Medium)
                                }
                                is PrintJobStatus.Connecting -> {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = TealPrimary)
                                    Text("Connecting to ${status.printerName}…", fontSize = 12.sp, color = TealPrimary, fontWeight = FontWeight.Medium)
                                }
                                is PrintJobStatus.Printing -> {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = TealPrimary)
                                    Text("Printing receipt (${status.progressPercentage}%)…", fontSize = 12.sp, color = TealPrimary, fontWeight = FontWeight.Bold)
                                }
                                is PrintJobStatus.Success -> {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Emerald600, modifier = Modifier.size(20.dp))
                                    Column {
                                        Text("Receipt Printed Successfully!", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Emerald700)
                                        Text("Sent to ${status.printerName}", fontSize = 11.sp, color = Emerald600)
                                    }
                                }
                                is PrintJobStatus.Error -> {
                                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Red600, modifier = Modifier.size(20.dp))
                                    Text(status.message, fontSize = 11.5.sp, color = Red600, fontWeight = FontWeight.Medium)
                                }
                                PrintJobStatus.Idle -> Unit
                            }
                        }
                    }
                }

                // Paper Width Selector Segment (58mm vs 80mm standard POS widths)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Thermal Paper Roll Size:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Slate700
                    )
                    Row(
                        modifier = Modifier
                            .background(Slate100, RoundedCornerShape(10.dp))
                            .padding(2.dp)
                    ) {
                        listOf(58, 80).forEach { width ->
                            val isSelected = selectedPaperWidth == width
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
                                shadowElevation = if (isSelected) 2.dp else 0.dp,
                                modifier = Modifier
                                    .clickable { selectedPaperWidth = width }
                                    .testTag("paper_width_${width}mm")
                            ) {
                                Text(
                                    text = "${width}mm",
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) TealPrimary else Slate600,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                // Discovered Printers Section Header & Refresh Scan Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "AVAILABLE PRINTERS (${discoveredPrinters.size})",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp,
                        color = Slate400
                    )
                    TextButton(
                        onClick = {
                            printerManager.startDiscovery()
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.testTag("refresh_discovery_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Rescan", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Printers List
                if (discoveredPrinters.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(Slate50, RoundedCornerShape(16.dp))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.BluetoothSearching, contentDescription = null, tint = Slate300, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("No Bluetooth Printers Found", fontWeight = FontWeight.Bold, color = Slate700, fontSize = 14.sp)
                            Text("Turn on printer Bluetooth and ensure it is discoverable.", color = Slate400, fontSize = 11.5.sp, textAlign = TextAlign.Center)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(discoveredPrinters, key = { it.address }) { printer ->
                            val isSelected = selectedPrinter?.address == printer.address
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        selectedPrinter = printer
                                        selectedPaperWidth = printer.paperWidthMm
                                        printerManager.resetStatus()
                                    }
                                    .testTag("printer_item_${printer.address}"),
                                color = if (isSelected) TealLight.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(
                                    if (isSelected) 1.5.dp else 1.dp,
                                    if (isSelected) TealPrimary else Slate200
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .background(if (isSelected) TealPrimary else Slate100, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Print,
                                                contentDescription = null,
                                                tint = if (isSelected) Color.White else Slate500,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        Column {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = printer.displayName,
                                                    fontSize = 13.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Slate900,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                if (printer.isBonded) {
                                                    Surface(
                                                        shape = CircleShape,
                                                        color = Emerald100
                                                    ) {
                                                        Text(
                                                            text = "Paired",
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Emerald700,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            Text(
                                                text = "${printer.address} • Default ${printer.paperWidthMm}mm",
                                                fontSize = 11.sp,
                                                color = Slate400
                                            )
                                        }
                                    }

                                    RadioButton(
                                        selected = isSelected,
                                        onClick = {
                                            selectedPrinter = printer
                                            selectedPaperWidth = printer.paperWidthMm
                                            printerManager.resetStatus()
                                        },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = TealPrimary,
                                            unselectedColor = Slate300
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // Summary of barcode to be printed
                Surface(
                    color = Slate50,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Slate200),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Barcode: ${product.barcode ?: product.id.takeLast(12)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Slate600
                        )
                        Text(
                            text = product.name,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TealPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 150.dp)
                        )
                    }
                }

                // Print Action Button
                val isPrinting = printStatus is PrintJobStatus.Printing || printStatus is PrintJobStatus.Connecting
                Button(
                    onClick = {
                        val targetPrinter = selectedPrinter
                        if (targetPrinter != null) {
                            coroutineScope.launch {
                                printerManager.printBarcode(
                                    printer = targetPrinter,
                                    barcode = product.barcode ?: product.id.takeLast(12), productName = product.name
                                    
                                )
                            }
                        } else {
                            Toast.makeText(context, "Please select a printer first", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = selectedPrinter != null && !isPrinting,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("execute_print_button")
                ) {
                    if (isPrinting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Transmitting ESC/POS…", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (selectedPrinter != null) "Print to ${selectedPrinter?.name?.take(16)}" else "Select a Printer",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
