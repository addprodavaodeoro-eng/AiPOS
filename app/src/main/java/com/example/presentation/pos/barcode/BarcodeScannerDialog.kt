package com.example.presentation.pos.barcode

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.domain.model.ProductItem
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

/**
 * Result state when a barcode is scanned or entered.
 */
sealed interface BarcodeScanResult {
    data class Found(val product: ProductItem, val isAdded: Boolean) : BarcodeScanResult
    data class NotFound(val barcode: String) : BarcodeScanResult
    data class OutOfStock(val product: ProductItem) : BarcodeScanResult
}

/**
 * High-performance full-screen / dialog barcode scanner sheet using CameraX.
 * Includes live camera preview with target reticle, flashlight toggle, quick manual barcode input,
 * recent catalog quick-scan buttons for fast testing, vibration haptic feedback,
 * and automatic addition to the active cart.
 */
@Composable
fun BarcodeScannerDialog(
    products: List<ProductItem>,
    onProductScanned: (ProductItem) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var isTorchEnabled by remember { mutableStateOf(false) }
    var manualBarcodeInput by remember { mutableStateOf("") }
    var scanFeedback by remember { mutableStateOf<BarcodeScanResult?>(null) }
    var lastScannedBarcode by remember { mutableStateOf<String?>(null) }
    var isProcessingScan by remember { mutableStateOf(false) }

    fun triggerVibration() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(80)
            }
        } catch (_: Exception) {
            // Safe fallback if vibration permission or hardware unavailable
        }
    }

    fun processBarcode(rawCode: String) {
        val cleanBarcode = rawCode.trim()
        if (cleanBarcode.isEmpty() || isProcessingScan) return

        isProcessingScan = true
        lastScannedBarcode = cleanBarcode
        triggerVibration()

        val matchedProduct = products.firstOrNull {
            it.barcode != null && it.barcode.equals(cleanBarcode, ignoreCase = true)
        }

        if (matchedProduct != null) {
            if (matchedProduct.stockQuantity <= 0) {
                scanFeedback = BarcodeScanResult.OutOfStock(matchedProduct)
            } else {
                onProductScanned(matchedProduct)
                scanFeedback = BarcodeScanResult.Found(matchedProduct, isAdded = true)
            }
        } else {
            scanFeedback = BarcodeScanResult.NotFound(cleanBarcode)
        }

        coroutineScope.launch {
            delay(1500)
            isProcessingScan = false
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .testTag("barcode_scanner_dialog")
        ) {
            // Camera Preview or Fallback View
            if (hasCameraPermission) {
                CameraPreview(
                    isTorchEnabled = isTorchEnabled,
                    onBarcodeDetected = { detectedCode ->
                        if (!isProcessingScan && detectedCode != lastScannedBarcode) {
                            processBarcode(detectedCode)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Slate900)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Slate800,
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.VideocamOff,
                                    contentDescription = null,
                                    tint = Amber500,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                        Text(
                            text = "Camera Permission Required",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Allow camera access to scan barcodes on product packaging directly into the cart.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Slate400,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.testTag("request_camera_permission_button")
                        ) {
                            Text("Grant Permission")
                        }
                    }
                }
            }

            // Scanning Reticle & Overlay HUD
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp, vertical = 80.dp),
                contentAlignment = Alignment.Center
            ) {
                // Aiming Reticle Frame
                Box(
                    modifier = Modifier
                        .size(width = 280.dp, height = 180.dp)
                        .border(
                            width = 2.dp,
                            color = when (val fb = scanFeedback) {
                                is BarcodeScanResult.Found -> Emerald600
                                is BarcodeScanResult.NotFound -> Red500
                                is BarcodeScanResult.OutOfStock -> Amber500
                                null -> Color.White.copy(alpha = 0.85f)
                            },
                            shape = RoundedCornerShape(18.dp)
                        )
                        .background(
                            Color.White.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(18.dp)
                        )
                        .testTag("barcode_reticle")
                ) {
                    // Central Red Scan Aiming Line
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxWidth(0.9f)
                            .height(2.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color.Transparent,
                                        if (scanFeedback != null) Emerald600 else Red500,
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    // Corner indicators
                    Text(
                        text = "ALIGN BARCODE WITHIN FRAME",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 8.dp)
                    )
                }
            }

            // Top Bar Controls: Back, Title, Flashlight
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(42.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        .testTag("close_scanner_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Scanner",
                        tint = Color.White
                    )
                }

                Surface(
                    color = Color.Black.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = TealLight,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "CameraX Scanner",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                IconButton(
                    onClick = { isTorchEnabled = !isTorchEnabled },
                    enabled = hasCameraPermission,
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            if (isTorchEnabled) Amber500 else Color.Black.copy(alpha = 0.5f),
                            CircleShape
                        )
                        .testTag("toggle_torch_button")
                ) {
                    Icon(
                        imageVector = if (isTorchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "Toggle Torch",
                        tint = if (isTorchEnabled) Color.Black else Color.White
                    )
                }
            }

            // Bottom Floating Controls: Scan feedback banner + Quick Catalog bar + Manual Entry
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Scan Feedback Banner (Success / Not Found / Out of Stock)
                AnimatedVisibility(
                    visible = scanFeedback != null,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                    exit = fadeOut()
                ) {
                    scanFeedback?.let { result ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("scan_feedback_banner"),
                            shape = RoundedCornerShape(14.dp),
                            color = when (result) {
                                is BarcodeScanResult.Found -> Emerald700
                                is BarcodeScanResult.NotFound -> Red600
                                is BarcodeScanResult.OutOfStock -> Amber600
                            },
                            shadowElevation = 6.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = when (result) {
                                        is BarcodeScanResult.Found -> Icons.Default.CheckCircle
                                        is BarcodeScanResult.NotFound -> Icons.Default.ErrorOutline
                                        is BarcodeScanResult.OutOfStock -> Icons.Default.Warning
                                    },
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = when (result) {
                                            is BarcodeScanResult.Found -> "Added to Cart (+1)"
                                            is BarcodeScanResult.NotFound -> "Barcode Not Found"
                                            is BarcodeScanResult.OutOfStock -> "Product Out of Stock"
                                        },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = when (result) {
                                            is BarcodeScanResult.Found -> "${result.product.name} • ₱${String.format("%,.2f", result.product.price)}"
                                            is BarcodeScanResult.NotFound -> "Code: ${result.barcode} • Please register in inventory"
                                            is BarcodeScanResult.OutOfStock -> "${result.product.name} has 0 units remaining"
                                        },
                                        fontSize = 11.5.sp,
                                        color = Color.White.copy(alpha = 0.9f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                // Quick Barcode Chips (Ideal for emulator testing & fast lookup of in-store products)
                val itemsWithBarcode = remember(products) {
                    products.filter { !it.barcode.isNullOrBlank() }.take(6)
                }
                if (itemsWithBarcode.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.Black.copy(alpha = 0.65f))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "QUICK TEST BARCODES (TAP TO SIMULATE SCAN):",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            fontWeight = FontWeight.Bold,
                            color = Slate400
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(itemsWithBarcode) { prod ->
                                Surface(
                                    color = Slate800,
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(0.5.dp, Slate700),
                                    modifier = Modifier
                                        .clickable {
                                            prod.barcode?.let { processBarcode(it) }
                                        }
                                        .testTag("quick_barcode_${prod.id}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.QrCode,
                                            contentDescription = null,
                                            tint = TealLight,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = prod.name.take(14) + "…",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Manual Barcode Input Row
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = manualBarcodeInput,
                            onValueChange = { manualBarcodeInput = it },
                            placeholder = {
                                Text(
                                    "Enter or scan barcode digits…",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                    color = Slate400
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    if (manualBarcodeInput.isNotBlank()) {
                                        processBarcode(manualBarcodeInput)
                                        manualBarcodeInput = ""
                                    }
                                }
                            ),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Keyboard,
                                    contentDescription = null,
                                    tint = Slate400,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("manual_barcode_input"),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Button(
                            onClick = {
                                if (manualBarcodeInput.isNotBlank()) {
                                    processBarcode(manualBarcodeInput)
                                    manualBarcodeInput = ""
                                }
                            },
                            enabled = manualBarcodeInput.isNotBlank(),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("submit_manual_barcode_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddShoppingCart,
                                contentDescription = "Add Item",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add")
                        }
                    }
                }
            }
        }
    }
}

/**
 * CameraX Preview Composable with ImageAnalysis for real-time barcode scanning.
 */
@Composable
private fun CameraPreview(
    isTorchEnabled: Boolean,
    onBarcodeDetected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    var cameraControl by remember { mutableStateOf<androidx.camera.core.CameraControl?>(null) }

    LaunchedEffect(isTorchEnabled, cameraControl) {
        cameraControl?.enableTorch(isTorchEnabled)
    }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }

            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    imageAnalysis.setAnalyzer(
                        cameraExecutor,
                        BarcodeAnalyzer { detectedCode ->
                            onBarcodeDetected(detectedCode)
                        }
                    )

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    cameraProvider.unbindAll()
                    val camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                    cameraControl = camera.cameraControl
                    camera.cameraControl.enableTorch(isTorchEnabled)
                } catch (exc: Exception) {
                    exc.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = modifier
    )
}
