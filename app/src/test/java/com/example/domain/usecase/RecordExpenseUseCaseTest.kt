package com.example.domain.usecase

import com.example.data.local.dao.ExpenseDao
import com.example.data.local.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import com.example.data.local.entity.ExpenseCategory

class FakeExpenseDao : ExpenseDao {
    val expenses = mutableMapOf<String, ExpenseEntity>()

    override suspend fun insertExpense(expense: ExpenseEntity) {
        expenses[expense.id] = expense
    }

    override suspend fun updateExpense(expense: ExpenseEntity) {
        expenses[expense.id] = expense
    }

    override fun getActiveExpenses(storeId: String): Flow<List<ExpenseEntity>> {
        throw NotImplementedError()
    }

    override suspend fun getExpenseById(id: String, storeId: String): ExpenseEntity? {
        val e = expenses[id]
        return if (e?.storeId == storeId) e else null
    }

    override fun getTotalActiveExpenses(storeId: String): Flow<Long?> {
        throw NotImplementedError()
    }

    override suspend fun getAllExpenses(): List<ExpenseEntity> {
        return expenses.values.toList()
    }

    override suspend fun insertAllExpenses(expenses: List<ExpenseEntity>) {
        expenses.forEach { this.expenses[it.id] = it }
    }

    override suspend fun deleteAllExpenses() {
        expenses.clear()
    }
}

class RecordExpenseUseCaseTest {
    private lateinit var fakeDao: FakeExpenseDao
    private lateinit var useCase: RecordExpenseUseCase

    @Before
    fun setup() {
        fakeDao = FakeExpenseDao()
        useCase = RecordExpenseUseCase(fakeDao)
    }

    @Test
    fun `invoke with valid data saves expense`() = runBlocking {
        val result = useCase(
            storeId = "store1",
            amountCentavos = 150000, // ₱1,500.00
            category = ExpenseCategory.ELECTRICITY,
            description = "Electric bill",
            userId = "user1"
        )
        assertTrue(result.isSuccess)
        assertEquals(1, fakeDao.expenses.size)
        val saved = fakeDao.expenses.values.first()
        assertEquals(150000L, saved.amountCentavos)
        assertEquals(ExpenseCategory.ELECTRICITY, saved.category)
    }

    @Test
    fun `invoke with zero amount fails`() = runBlocking {
        val result = useCase(
            storeId = "store1",
            amountCentavos = 0,
            category = ExpenseCategory.SUPPLIES,
            description = null,
            userId = "user1"
        )
        assertTrue(result.isFailure)
        assertEquals(0, fakeDao.expenses.size)
    }
}
