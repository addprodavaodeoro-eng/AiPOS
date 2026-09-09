package com.example.domain.usecase

import com.example.data.local.dao.UserDao
import com.example.data.local.entity.UserRole
import com.example.domain.model.SampleProductCatalog
import com.example.domain.model.toProductEntity
import com.example.domain.repository.IProductRepository
import com.example.domain.repository.ITransactionRepository
import com.example.domain.security.SecurityUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Clean use case for securely resetting products, sales logs, and transactions,
 * guarded by administrator/owner password verification.
 */
class ResetDataUseCase(
    private val userDao: UserDao,
    private val productRepository: IProductRepository,
    private val transactionRepository: ITransactionRepository
) {

    /**
     * Verifies whether the provided raw password matches any active Admin/Owner account.
     */
    suspend fun verifyAdminPassword(passwordRaw: String): Boolean = withContext(Dispatchers.IO) {
        if (passwordRaw.isBlank()) return@withContext false

        val users = userDao.getAllUsers()
        if (users.isEmpty()) {
            // If database has no users yet, allow default admin credentials
            return@withContext passwordRaw == "admin" || passwordRaw == "admin123"
        }

        // Check if matching any OWNER or MANAGER or 'admin' user
        for (user in users) {
            if (user.isActive && (user.role == UserRole.OWNER || user.role == UserRole.MANAGER || user.username.equals("admin", ignoreCase = true))) {
                val matches = SecurityUtils.verifyPassword(passwordRaw, user.passwordSalt, user.passwordHash)
                if (matches) return@withContext true
            }
        }

        // Also check if matches known default seed admin
        val defaultSalt = "EmiZE0ZxTBdaDYChPgeKIA=="
        val defaultHash = "QS7RTLEsrO2qnfqsBiFjK3h0JF+dLM8couA9VMSXJ60="
        if (SecurityUtils.verifyPassword(passwordRaw, defaultSalt, defaultHash)) {
            return@withContext true
        }

        false
    }

    /**
     * Deletes all products from inventory, with option to re-populate default sari-sari catalog.
     */
    suspend fun resetProducts(reseedSampleCatalog: Boolean = false) = withContext(Dispatchers.IO) {
        productRepository.deleteAllProducts()
        if (reseedSampleCatalog) {
            productRepository.addProducts(SampleProductCatalog.items)
        }
    }

    /**
     * Deletes all sales history, receipts, and transaction logs.
     */
    suspend fun resetSalesAndTransactions() = withContext(Dispatchers.IO) {
        transactionRepository.deleteAllTransactions()
    }

    /**
     * Complete wipe of both inventory and sales transactions.
     */
    suspend fun resetAll(reseedSampleCatalog: Boolean = false) = withContext(Dispatchers.IO) {
        productRepository.deleteAllProducts()
        transactionRepository.deleteAllTransactions()
        if (reseedSampleCatalog) {
            productRepository.addProducts(SampleProductCatalog.items)
        }
    }
}
