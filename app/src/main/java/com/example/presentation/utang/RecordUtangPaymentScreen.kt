package com.example.presentation.utang

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordUtangPaymentScreen(
    customerId: String,
    onNavigateBack: () -> Unit,
    onPaymentComplete: () -> Unit
) {
    val customer = sampleCustomers.find { it.id == customerId } ?: sampleCustomers.first()
    var paymentAmount by remember { mutableStateOf("") }
    var selectedMethod by remember { mutableStateOf("CASH") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Record Payment", fontWeight = FontWeight.Bold, color = Slate900) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceLight)
            )
        },
        containerColor = BackgroundLight
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            Text("CUSTOMER", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Slate400)
            Text(customer.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Slate900)
            Text("Balance: ₱ ${String.format("%.2f", customer.balance / 100.0)}", fontSize = 14.sp, color = Red600, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = paymentAmount,
                onValueChange = { paymentAmount = it },
                label = { Text("Payment Amount (₱)", color = Slate400) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = SurfaceLight,
                    focusedContainerColor = SurfaceLight,
                    unfocusedBorderColor = Slate200,
                    focusedBorderColor = TealPrimary
                ),
                textStyle = LocalTextStyle.current.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold)
            )

            Spacer(modifier = Modifier.height(24.dp))
            Text("PAYMENT METHOD", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Slate400)
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PaymentMethodChip("CASH", selectedMethod) { selectedMethod = "CASH" }
                PaymentMethodChip("GCASH", selectedMethod) { selectedMethod = "GCASH" }
                PaymentMethodChip("MAYA", selectedMethod) { selectedMethod = "MAYA" }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onPaymentComplete,
                modifier = Modifier.fillMaxWidth().height(64.dp).padding(bottom = 20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Record Payment", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun RowScope.PaymentMethodChip(method: String, selected: String, onClick: () -> Unit) {
    val isSelected = method == selected
    Box(
        modifier = Modifier
            .weight(1f)
            .height(56.dp)
            .background(if (isSelected) TealLight else SurfaceLight, RoundedCornerShape(12.dp))
            .border(2.dp, if (isSelected) TealPrimary else Slate100, RoundedCornerShape(12.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = method,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) TealDark else Slate400
        )
    }
}
