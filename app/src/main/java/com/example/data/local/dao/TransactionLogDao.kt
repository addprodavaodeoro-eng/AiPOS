package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.TransactionLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionLogDao {

    @Query("SELECT * FROM transaction_logs ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionLogEntity>>

    @Query("SELECT * FROM transaction_logs ORDER BY timestamp DESC")
    suspend fun getAllTransactionsList(): List<TransactionLogEntity>

    @Query("SELECT * FROM transaction_logs WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: String): TransactionLogEntity?

    @Query("SELECT * FROM transaction_logs WHERE receiptNumber = :receiptNumber LIMIT 1")
    suspend fun getTransactionByReceiptNumber(receiptNumber: String): TransactionLogEntity?

    @Query("SELECT * FROM transaction_logs WHERE dateString = :dateString ORDER BY timestamp DESC")
    fun getTransactionsByDate(dateString: String): Flow<List<TransactionLogEntity>>

    @Query("SELECT * FROM transaction_logs WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    fun getTransactionsBetween(startTime: Long, endTime: Long): Flow<List<TransactionLogEntity>>

    @Query("SELECT COUNT(*) FROM transaction_logs WHERE dateString = :dateString")
    fun getCountByDate(dateString: String): Flow<Int>

    @Query("SELECT COALESCE(SUM(totalAmount), 0.0) FROM transaction_logs WHERE dateString = :dateString")
    fun getTotalSalesByDate(dateString: String): Flow<Double>

    @Query("SELECT COALESCE(SUM(totalItemCount), 0) FROM transaction_logs WHERE dateString = :dateString")
    fun getTotalItemsSoldByDate(dateString: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM transaction_logs")
    suspend fun getTotalTransactionCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<TransactionLogEntity>)

    @Update
    suspend fun updateTransaction(transaction: TransactionLogEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionLogEntity)

    @Query("DELETE FROM transaction_logs WHERE id = :id")
    suspend fun deleteTransactionById(id: String)

    @Query("DELETE FROM transaction_logs")
    suspend fun deleteAllTransactions()
}
