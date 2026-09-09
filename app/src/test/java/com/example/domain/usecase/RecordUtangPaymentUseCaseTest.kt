package com.example.domain.usecase

import com.example.data.local.dao.CustomerDao
import com.example.data.local.dao.CustomerWithBalance
import com.example.data.local.entity.CreditLedgerEntity
import com.example.data.local.entity.CustomerEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// A fake DAO for fast unit testing of business logic rules
open class FakeCustomerDao : CustomerDao {
    val customers = mutableMapOf<String, CustomerEntity>()
    val ledger = mutableListOf<CreditLedgerEntity>()

    override suspend fun insertCustomer(customer: CustomerEntity) {
        customers[customer.id] = customer
    }

    override suspend fun insertLedgerEntry(entry: CreditLedgerEntity) {
        ledger.add(entry)
    }

    override fun getCustomersWithBalance(storeId: String): Flow<List<CustomerWithBalance>> {
        throw NotImplementedError()
    }

    override fun getCustomerLedger(customerId: String, storeId: String): Flow<List<CreditLedgerEntity>> {
        throw NotImplementedError()
    }

    override suspend fun getCustomerById(id: String, storeId: String): CustomerEntity? {
        val c = customers[id]
        return if (c?.storeId == storeId) c else null
    }

    override suspend fun recordPaymentTransaction(
        customer: CustomerEntity,
        ledgerEntry: CreditLedgerEntity
    ) {
        insertCustomer(customer)
        insertLedgerEntry(ledgerEntry)
    }

    override suspend fun getAllCustomers(): List<CustomerEntity> {
        return customers.values.toList()
    }

    override suspend fun getAllCreditLedger(): List<CreditLedgerEntity> {
        return ledger.toList()
    }

    override suspend fun insertAllCustomers(customers: List<CustomerEntity>) {
        customers.forEach { this.customers[it.id] = it }
    }

    override suspend fun insertAllLedgerEntries(entries: List<CreditLedgerEntity>) {
        ledger.addAll(entries)
    }

    override suspend fun deleteAllCustomers() {
        customers.clear()
    }

    override suspend fun deleteAllLedgerEntries() {
        ledger.clear()
    }
}

class RecordUtangPaymentUseCaseTest {

    private lateinit var fakeDao: FakeCustomerDao
    private lateinit var useCase: RecordUtangPaymentUseCase

    @Before
    fun setup() {
        fakeDao = FakeCustomerDao()
        useCase = RecordUtangPaymentUseCase(fakeDao)
    }

    @Test
    fun `invoke with negative amount fails validation`() = runBlocking {
        val result = useCase(
            storeId = "store1",
            customerId = "cust1",
            amountCentavos = -500 // Invalid
        )

        assertTrue("Should fail on negative amount", result.isFailure)
        assertEquals("Payment amount must be greater than zero.", result.exceptionOrNull()?.message)
        assertEquals(0, fakeDao.ledger.size)
    }

    @Test
    fun `invoke with wrong storeId fails tenant isolation check`() = runBlocking {
        fakeDao.insertCustomer(CustomerEntity(id = "cust1", storeId = "store1", name = "Juan", mobileNumber = null, address = null))

        // Attempting to pay for customer 1 using store 2's context
        val result = useCase(
            storeId = "store2", 
            customerId = "cust1",
            amountCentavos = 1000
        )

        assertTrue("Should fail tenant isolation", result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Tenant Isolation") == true)
        assertEquals(0, fakeDao.ledger.size)
    }

    @Test
    fun `invoke with valid data atomically updates customer and ledger`() = runBlocking {
        val initialCustomer = CustomerEntity(id = "cust1", storeId = "store1", name = "Juan", mobileNumber = null, address = null, updatedAt = 100L)
        fakeDao.insertCustomer(initialCustomer)

        Thread.sleep(10) // Ensure system time advances so the timestamp update is testable

        val result = useCase(
            storeId = "store1",
            customerId = "cust1",
            amountCentavos = 50000 // ₱500.00
        )

        assertTrue("Should succeed", result.isSuccess)
        assertEquals(1, fakeDao.ledger.size)
        assertEquals(50000L, fakeDao.ledger.first().creditAmount)
        assertEquals("cust1", fakeDao.ledger.first().customerId)
        
        // Ensure atomic update modified the customer timestamp
        val updatedCustomer = fakeDao.customers["cust1"]!!
        assertTrue("Expected ${updatedCustomer.updatedAt} > 100", updatedCustomer.updatedAt > 100L)
    }
}
