package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.data.local.entity.CreditLedgerEntity
import com.example.data.local.entity.CustomerEntity
import kotlinx.coroutines.flow.Flow

data class CustomerWithBalance(
    val id: String,
    val name: String,
    val mobileNumber: String?,
    val balance: Long // in centavos
)

@Dao
interface CustomerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLedgerEntry(entry: CreditLedgerEntity)

    @Query("""
        SELECT c.id, c.name, c.mobileNumber, 
               COALESCE(SUM(l.debitAmount - l.creditAmount), 0) as balance
        FROM customers c
        LEFT JOIN credit_ledger l ON c.id = l.customerId
        WHERE c.storeId = :storeId
        GROUP BY c.id
    """)
    fun getCustomersWithBalance(storeId: String): Flow<List<CustomerWithBalance>>

    @Query("SELECT * FROM credit_ledger WHERE customerId = :customerId AND storeId = :storeId ORDER BY occurredAt DESC")
    fun getCustomerLedger(customerId: String, storeId: String): Flow<List<CreditLedgerEntity>>
    
    @Query("SELECT * FROM customers WHERE id = :id AND storeId = :storeId LIMIT 1")
    suspend fun getCustomerById(id: String, storeId: String): CustomerEntity?

    @Query("SELECT * FROM customers")
    suspend fun getAllCustomers(): List<CustomerEntity>

    @Query("SELECT * FROM credit_ledger")
    suspend fun getAllCreditLedger(): List<CreditLedgerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllCustomers(customers: List<CustomerEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllLedgerEntries(entries: List<CreditLedgerEntity>)

    @Query("DELETE FROM customers")
    suspend fun deleteAllCustomers()

    @Query("DELETE FROM credit_ledger")
    suspend fun deleteAllLedgerEntries()

    @Transaction
    suspend fun recordPaymentTransaction(customer: CustomerEntity, ledgerEntry: CreditLedgerEntity) {
        // Because Room doesn't let us intercept and modify fields in the DAO wrapper for tests nicely without making it complicated,
        // we let the caller pass the mutated object, or we mutate it before calling the Dao.
        insertCustomer(customer)
        insertLedgerEntry(ledgerEntry)
    }
}
