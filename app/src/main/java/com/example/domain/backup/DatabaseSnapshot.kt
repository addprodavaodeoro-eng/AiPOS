package com.example.domain.backup

import com.example.data.local.entity.CreditLedgerEntity
import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.ExpenseEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.UserEntity
import com.example.data.local.entity.TransactionLogEntity
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DatabaseSnapshot(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val appVersion: String = "1.0.0",
    val storeName: String = "Aling Nena's Sari-Sari Store",
    val products: List<ProductEntity> = emptyList(),
    val customers: List<CustomerEntity> = emptyList(),
    val creditLedger: List<CreditLedgerEntity> = emptyList(),
    val expenses: List<ExpenseEntity> = emptyList(),
    val users: List<UserEntity> = emptyList(),
    val transactions: List<TransactionLogEntity> = emptyList()
) {
    val totalRecordsCount: Int
        get() = products.size + customers.size + creditLedger.size + expenses.size + users.size + transactions.size
}

data class BackupStats(
    val productCount: Int,
    val customerCount: Int,
    val ledgerCount: Int,
    val expenseCount: Int,
    val userCount: Int,
    val transactionCount: Int,
    val timestamp: Long
)


sealed interface BackupRestoreState {
    object Idle : BackupRestoreState
    data class Processing(val message: String) : BackupRestoreState
    data class ExportSuccess(val filePath: String, val stats: BackupStats) : BackupRestoreState
    data class ImportSuccess(val stats: BackupStats) : BackupRestoreState
    data class Error(val message: String) : BackupRestoreState
}
