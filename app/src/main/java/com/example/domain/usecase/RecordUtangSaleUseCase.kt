package com.example.domain.usecase

import com.example.data.local.dao.CustomerDao
import com.example.data.local.entity.CreditEntryType
import com.example.data.local.entity.CreditLedgerEntity
import kotlinx.coroutines.flow.first
import java.util.UUID

class RecordUtangSaleUseCase(private val customerDao: CustomerDao) {
    
    suspend operator fun invoke(
        storeId: String,
        customerId: String,
        amountCentavos: Long,
        referenceId: String // e.g. the transaction ID
    ): Result<Unit> {
        if (amountCentavos <= 0) {
            return Result.failure(IllegalArgumentException("Credit sale amount must be greater than zero."))
        }

        val customer = customerDao.getCustomerById(id = customerId, storeId = storeId)
            ?: return Result.failure(IllegalStateException("Customer not found or access denied (Tenant Isolation Breach)."))

        if (!customer.creditEnabled) {
            return Result.failure(IllegalStateException("Customer is not allowed to use Utang."))
        }

        // Validate Credit Limit
        if (customer.creditLimitCentavos != null) {
            val customersWithBalance = customerDao.getCustomersWithBalance(storeId).first()
            val currentBalance = customersWithBalance.find { it.id == customerId }?.balance ?: 0L
            
            if (currentBalance + amountCentavos > customer.creditLimitCentavos) {
                return Result.failure(IllegalStateException("Transaction exceeds allowed credit limit."))
            }
        }

        val entry = CreditLedgerEntity(
            id = UUID.randomUUID().toString(),
            customerId = customerId,
            storeId = storeId,
            entryType = CreditEntryType.CREDIT_SALE,
            referenceId = referenceId,
            debitAmount = amountCentavos, // Debt increases
            creditAmount = 0L,
            notes = "Utang Checkout"
        )

        val updatedCustomer = customer.copy(updatedAt = System.currentTimeMillis())
        customerDao.recordPaymentTransaction(updatedCustomer, entry)
        
        return Result.success(Unit)
    }
}
