package com.example.presentation.utang

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

data class LedgerUI(val id: String, val date: String, val type: String, val debit: Long, val credit: Long, val balanceAfter: Long)

val sampleLedger = listOf(
    LedgerUI("1", "Sep 8, 09:50 AM", "Payment", 0, 20000, 30000), // Paid ₱200, bal ₱300
    LedgerUI("2", "Sep 5, 10:15 AM", "Utang Sale", 50000, 0, 50000) // Utang ₱500, bal ₱500
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailScreen(
    customerId: String,
    onNavigateBack: () -> Unit,
    onNavigateToPayment: (String) -> Unit
) {
    // For MVP UI, we simulate fetching the customer
    val customer = sampleCustomers.find { it.id == customerId } ?: sampleCustomers.first()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(customer.name, fontWeight = FontWeight.Bold, color = Slate900) },
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
            
            // Balance Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceLight, RoundedCornerShape(24.dp))
                    .border(1.dp, Slate100, RoundedCornerShape(24.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("CURRENT BALANCE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Slate400)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "₱ ${String.format("%.2f", customer.balance / 100.0)}", 
                    fontSize = 36.sp, 
                    fontWeight = FontWeight.ExtraBold, 
                    color = if (customer.balance > 0) Red600 else Slate900
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { onNavigateToPayment(customer.id) },
                        modifier = Modifier.weight(1f).height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Record Payment", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { /* TODO: New Sale with Utang */ },
                        modifier = Modifier.weight(1f).height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Slate100, contentColor = Slate900),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("New Sale", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("TRANSACTION HISTORY", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate800, modifier = Modifier.padding(bottom = 12.dp))

            // Ledger List
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sampleLedger) { entry ->
                    LedgerRow(entry)
                }
            }
        }
    }
}

@Composable
fun LedgerRow(entry: LedgerUI) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceLight, RoundedCornerShape(16.dp))
            .border(1.dp, Slate50, RoundedCornerShape(16.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(if (entry.credit > 0) Emerald600.copy(alpha=0.1f) else Red600.copy(alpha=0.1f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (entry.credit > 0) "↓" else "↑",
                    color = if (entry.credit > 0) Emerald600 else Red600,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = entry.type, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Slate900)
                Text(text = entry.date, fontSize = 10.sp, color = Slate400)
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = if (entry.credit > 0) "- ₱${String.format("%.2f", entry.credit / 100.0)}" else "+ ₱${String.format("%.2f", entry.debit / 100.0)}", 
                fontSize = 14.sp, 
                fontWeight = FontWeight.Bold, 
                color = if (entry.credit > 0) Emerald600 else Red600
            )
            Text(text = "Bal: ₱${String.format("%.2f", entry.balanceAfter / 100.0)}", fontSize = 10.sp, color = Slate400)
        }
    }
}
