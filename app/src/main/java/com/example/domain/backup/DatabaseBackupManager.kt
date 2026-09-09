package com.example.domain.backup

import android.content.Context
import android.net.Uri
import com.example.data.local.AppDatabase
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Handles generating and restoring JSON-based complete Room database snapshots
 * for data recovery and offline backups.
 */
class DatabaseBackupManager(
    private val context: Context,
    private val database: AppDatabase
) {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val snapshotAdapter = moshi.adapter(DatabaseSnapshot::class.java).indent("  ")

    /**
     * Creates a snapshot of all active tables and exports it to local storage or an external URI.
     */
    suspend fun createSnapshot(): DatabaseSnapshot = withContext(Dispatchers.IO) {
        val products = database.productDao().getAllProductsList()
        val customers = database.customerDao().getAllCustomers()
        val ledger = database.customerDao().getAllCreditLedger()
        val expenses = database.expenseDao().getAllExpenses()
        val users = database.userDao().getAllUsers()
        val transactions = database.transactionLogDao().getAllTransactionsList()

        DatabaseSnapshot(
            version = 1,
            exportedAt = System.currentTimeMillis(),
            appVersion = "1.0.0",
            storeName = "Aling Nena's Sari-Sari Store",
            products = products,
            customers = customers,
            creditLedger = ledger,
            expenses = expenses,
            users = users,
            transactions = transactions
        )
    }

    /**
     * Exports snapshot to app's backup folder in internal/external files directory.
     */
    suspend fun exportSnapshotToLocalStorage(passwordRaw: String): Result<Pair<File, BackupStats>> = withContext(Dispatchers.IO) {
        try {
            val snapshot = createSnapshot()
            val json = snapshotAdapter.toJson(snapshot)
            val encryptedData = com.example.domain.security.SecurityUtils.encryptData(json, passwordRaw)

            val backupDir = File(context.filesDir, "backups").apply { mkdirs() }
            val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
            val fileName = "aipos_backup_${dateFormat.format(Date(snapshot.exportedAt))}.json"
            val file = File(backupDir, fileName)

            FileOutputStream(file).use { out ->
                out.write(encryptedData.toByteArray(Charsets.UTF_8))
                out.flush()
            }

            val stats = BackupStats(
                productCount = snapshot.products.size,
                customerCount = snapshot.customers.size,
                ledgerCount = snapshot.creditLedger.size,
                expenseCount = snapshot.expenses.size,
                userCount = snapshot.users.size,
                transactionCount = snapshot.transactions.size,
                timestamp = snapshot.exportedAt
            )
            Result.success(Pair(file, stats))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Exports snapshot directly to a user-selected SAF (Storage Access Framework) Uri.
     */
    suspend fun exportSnapshotToUri(uri: Uri, passwordRaw: String): Result<BackupStats> = withContext(Dispatchers.IO) {
        try {
            val snapshot = createSnapshot()
            val json = snapshotAdapter.toJson(snapshot)
            val encryptedData = com.example.domain.security.SecurityUtils.encryptData(json, passwordRaw)

            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(encryptedData.toByteArray(Charsets.UTF_8))
                out.flush()
            } ?: return@withContext Result.failure(Exception("Could not open destination storage file"))

            val stats = BackupStats(
                productCount = snapshot.products.size,
                customerCount = snapshot.customers.size,
                ledgerCount = snapshot.creditLedger.size,
                expenseCount = snapshot.expenses.size,
                userCount = snapshot.users.size,
                transactionCount = snapshot.transactions.size,
                timestamp = snapshot.exportedAt
            )
            Result.success(stats)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Restores database records from a JSON string snapshot.
     */
    suspend fun restoreFromSnapshot(snapshot: DatabaseSnapshot, replaceExisting: Boolean = true): Result<BackupStats> = withContext(Dispatchers.IO) {
        try {
            if (replaceExisting) {
                // Clear existing records before importing
                if (snapshot.products.isNotEmpty()) database.productDao().deleteAllProducts()
                if (snapshot.customers.isNotEmpty()) database.customerDao().deleteAllCustomers()
                if (snapshot.creditLedger.isNotEmpty()) database.customerDao().deleteAllLedgerEntries()
                if (snapshot.expenses.isNotEmpty()) database.expenseDao().deleteAllExpenses()
                if (snapshot.users.isNotEmpty()) database.userDao().deleteAllUsers()
                if (snapshot.transactions.isNotEmpty()) database.transactionLogDao().deleteAllTransactions()
            }

            if (snapshot.products.isNotEmpty()) {
                database.productDao().insertAll(snapshot.products)
            }
            if (snapshot.customers.isNotEmpty()) {
                database.customerDao().insertAllCustomers(snapshot.customers)
            }
            if (snapshot.creditLedger.isNotEmpty()) {
                database.customerDao().insertAllLedgerEntries(snapshot.creditLedger)
            }
            if (snapshot.expenses.isNotEmpty()) {
                database.expenseDao().insertAllExpenses(snapshot.expenses)
            }
            if (snapshot.users.isNotEmpty()) {
                database.userDao().insertAllUsers(snapshot.users)
            }
            if (snapshot.transactions.isNotEmpty()) {
                database.transactionLogDao().insertAll(snapshot.transactions)
            }

            val stats = BackupStats(
                productCount = snapshot.products.size,
                customerCount = snapshot.customers.size,
                ledgerCount = snapshot.creditLedger.size,
                expenseCount = snapshot.expenses.size,
                userCount = snapshot.users.size,
                transactionCount = snapshot.transactions.size,
                timestamp = snapshot.exportedAt
            )
            Result.success(stats)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Parses and restores from a selected file URI via Storage Access Framework.
     */
    suspend fun restoreFromUri(uri: Uri, passwordRaw: String, replaceExisting: Boolean = true): Result<BackupStats> = withContext(Dispatchers.IO) {
        try {
            val encryptedString = context.contentResolver.openInputStream(uri)?.use { stream: InputStream ->
                stream.bufferedReader(Charsets.UTF_8).readText()
            } ?: return@withContext Result.failure(Exception("Could not read backup file from selected storage"))

            val jsonString = com.example.domain.security.SecurityUtils.decryptData(encryptedString, passwordRaw)

            val snapshot = snapshotAdapter.fromJson(jsonString)
                ?: return@withContext Result.failure(Exception("Invalid backup format or corrupt JSON file"))

            restoreFromSnapshot(snapshot, replaceExisting)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Lists all backup files stored in app internal backup directory.
     */
    fun listLocalBackups(): List<File> {
        val backupDir = File(context.filesDir, "backups")
        if (!backupDir.exists()) return emptyList()
        return backupDir.listFiles { file -> file.extension.lowercase() == "json" }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }
}
