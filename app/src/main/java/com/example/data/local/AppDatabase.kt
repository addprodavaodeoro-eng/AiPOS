package com.example.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.data.local.dao.CustomerDao
import com.example.data.local.dao.ExpenseDao
import com.example.data.local.dao.ProductDao
import com.example.data.local.dao.TransactionLogDao
import com.example.data.local.dao.UserDao
import com.example.data.local.entity.CreditLedgerEntity
import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.ExpenseEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.TransactionLogEntity
import com.example.data.local.entity.UserEntity

@Database(
    entities = [
        CustomerEntity::class, 
        CreditLedgerEntity::class,
        ExpenseEntity::class,
        UserEntity::class,
        ProductEntity::class,
        TransactionLogEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun customerDao(): CustomerDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun userDao(): UserDao
    abstract fun productDao(): ProductDao
    abstract fun transactionLogDao(): TransactionLogDao
}
