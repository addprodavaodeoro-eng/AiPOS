package com.example.domain.usecase

import com.example.data.local.dao.ExpenseDao
import com.example.data.local.entity.ExpenseCategory
import com.example.data.local.entity.ExpenseEntity
import java.util.UUID

class RecordExpenseUseCase(private val expenseDao: ExpenseDao) {
    
    suspend operator fun invoke(
        storeId: String,
        amountCentavos: Long,
        category: ExpenseCategory,
        description: String?,
        userId: String,
        receiptUri: String? = null
    ): Result<Unit> {
        // Validation: Positive amount
        if (amountCentavos <= 0) {
            return Result.failure(IllegalArgumentException("Expense amount must be greater than zero."))
        }

        val expense = ExpenseEntity(
            id = UUID.randomUUID().toString(), // Should migrate to UUIDv7
            storeId = storeId,
            amountCentavos = amountCentavos,
            category = category,
            description = description,
            recordedByUserId = userId,
            receiptImageUri = receiptUri,
            isVoided = false
        )

        expenseDao.insertExpense(expense)
        
        return Result.success(Unit)
    }
}
