package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity)

    @Update
    suspend fun updateExpense(expense: ExpenseEntity)

    @Query("SELECT * FROM expenses WHERE storeId = :storeId AND isVoided = 0 ORDER BY occurredAt DESC")
    fun getActiveExpenses(storeId: String): Flow<List<ExpenseEntity>>
    
    @Query("SELECT * FROM expenses WHERE id = :id AND storeId = :storeId LIMIT 1")
    suspend fun getExpenseById(id: String, storeId: String): ExpenseEntity?

    @Query("SELECT * FROM expenses")
    suspend fun getAllExpenses(): List<ExpenseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllExpenses(expenses: List<ExpenseEntity>)

    @Query("DELETE FROM expenses")
    suspend fun deleteAllExpenses()

    @Query("SELECT SUM(amountCentavos) FROM expenses WHERE storeId = :storeId AND isVoided = 0")
    fun getTotalActiveExpenses(storeId: String): Flow<Long?>
}
