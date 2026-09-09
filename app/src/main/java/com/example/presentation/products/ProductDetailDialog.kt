package com.example.presentation.products

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.barcode.BarcodeGenerator
import com.example.domain.model.ProductItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailDialog(
    product: ProductItem,
    onDismiss: () -> Unit,
    onPrintBarcode: (ProductItem) -> Unit
) {
    var barcodeBitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    LaunchedEffect(product.barcode, product.id) {
        val codeToUse = if (!product.barcode.isNullOrBlank()) product.barcode!! else product.id.takeLast(12)
        barcodeBitmap = BarcodeGenerator.generateBarcodeBitmap(codeToUse)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Product Details", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = product.name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Text(
                    text = "Stock: ${product.stockQuantity} ${product.unit}",
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (barcodeBitmap != null) {
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Image(
                            bitmap = barcodeBitmap!!.asImageBitmap(),
                            contentDescription = "Barcode",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .padding(8.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = product.barcode ?: product.id.takeLast(12),
                        fontSize = 14.sp,
                        letterSpacing = 2.sp,
                        color = Color.DarkGray
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onPrintBarcode(product) }) {
                Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Print Barcode")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
