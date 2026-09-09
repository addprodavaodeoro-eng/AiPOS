package com.example.domain.usecase

import com.example.data.local.dao.CustomerDao
import com.example.data.local.entity.CreditEntryType
import com.example.data.local.entity.CreditLedgerEntity
import java.util.UUID

class RecordUtangPaymentUseCase(private val customerDao: CustomerDao) {
    
    suspend operator fun invoke(
        storeId: String,
        customerId: String,
        amountCentavos: Long,
        referenceId: String? = null,
        notes: String? = null
    ): Result<Unit> {
        // CRITICAL FIX: Validate amount is positive.
        // A negative payment mathematically becomes a credit sale, corrupting the ledger intent.
        if (amountCentavos <= 0) {
            return Result.failure(IllegalArgumentException("Payment amount must be greater than zero."))
        }

        // CRITICAL FIX: Tenant Isolation.
        // Guarantee the customer exists AND belongs to the requesting storeId.
        val customer = customerDao.getCustomerById(id = customerId, storeId = storeId)
            ?: return Result.failure(IllegalStateException("Customer not found or access denied (Tenant Isolation Breach)."))

        val entry = CreditLedgerEntity(
            id = UUID.randomUUID().toString(), // To be upgraded to UUIDv7
            customerId = customerId,
            storeId = storeId,
            entryType = CreditEntryType.PAYMENT,
            referenceId = referenceId,
            debitAmount = 0L,
            creditAmount = amountCentavos,
            notes = notes
        )

        // CRITICAL FIX: Atomic Transaction. Update customer timestamp and insert ledger together.
        val updatedCustomer = customer.copy(updatedAt = System.currentTimeMillis())
        customerDao.recordPaymentTransaction(updatedCustomer, entry)
        
        return Result.success(Unit)
    }
}
