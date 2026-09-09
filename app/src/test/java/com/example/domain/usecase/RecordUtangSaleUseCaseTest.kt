package com.example.domain.usecase

import com.example.data.local.dao.CustomerWithBalance
import com.example.data.local.entity.CustomerEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RecordUtangSaleUseCaseTest {
    
    private lateinit var fakeDao: FakeCustomerDao
    private lateinit var useCase: RecordUtangSaleUseCase

    @Before
    fun setup() {
        fakeDao = object : FakeCustomerDao() {
            override fun getCustomersWithBalance(storeId: String): Flow<List<CustomerWithBalance>> {
                // Mock balance computation for limit checking
                val balance = ledger.sumOf { it.debitAmount - it.creditAmount }
                return flowOf(listOf(CustomerWithBalance("cust1", "Juan", null, balance)))
            }
        }
        useCase = RecordUtangSaleUseCase(fakeDao)
    }

    @Test
    fun `invoke fails if credit limit exceeded`() = runBlocking {
        val initialCustomer = CustomerEntity(
            id = "cust1", storeId = "store1", name = "Juan", mobileNumber = null, address = null,
            creditEnabled = true, creditLimitCentavos = 100000 // ₱1,000.00
        )
        fakeDao.insertCustomer(initialCustomer)

        // Give them an initial utang
        useCase(storeId = "store1", customerId = "cust1", amountCentavos = 80000, referenceId = "tx1")

        // Try to add more, crossing the ₱1k limit
        val result = useCase(storeId = "store1", customerId = "cust1", amountCentavos = 30000, referenceId = "tx2")

        assertTrue(result.isFailure)
        assertEquals("Transaction exceeds allowed credit limit.", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun `invoke fails if credit disabled`() = runBlocking {
        val initialCustomer = CustomerEntity(
            id = "cust1", storeId = "store1", name = "Juan", mobileNumber = null, address = null,
            creditEnabled = false // DISABLED
        )
        fakeDao.insertCustomer(initialCustomer)

        val result = useCase(storeId = "store1", customerId = "cust1", amountCentavos = 10000, referenceId = "tx1")

        assertTrue(result.isFailure)
        assertEquals("Customer is not allowed to use Utang.", result.exceptionOrNull()?.message)
    }
}
