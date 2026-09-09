package com.example.domain.auth

import com.example.data.local.dao.UserDao
import com.example.data.local.entity.UserEntity
import com.example.data.local.entity.UserRole
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class FakeStaffUserDao : UserDao {
    val users = mutableMapOf<String, UserEntity>()

    override suspend fun insertUser(user: UserEntity) {
        users[user.id] = user
    }

    override suspend fun updateUser(user: UserEntity) {
        users[user.id] = user
    }

    override suspend fun getUserByUsername(username: String): UserEntity? {
        return users.values.firstOrNull { it.username.equals(username, ignoreCase = true) }
    }

    override suspend fun getUserById(userId: String): UserEntity? {
        return users[userId]
    }

    override suspend fun getAllUsers(): List<UserEntity> {
        return users.values.toList()
    }

    override fun getAllUsersFlow(): Flow<List<UserEntity>> {
        return flowOf(users.values.toList())
    }

    override suspend fun insertAllUsers(users: List<UserEntity>) {
        users.forEach { this.users[it.id] = it }
    }

    override suspend fun deleteAllUsers() {
        users.clear()
    }

    override suspend fun deleteUserById(userId: String) {
        users.remove(userId)
    }

    override suspend fun getUserByPin(pin: String): UserEntity? {
        return users.values.firstOrNull { it.pin == pin }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class StaffPinAndLockoutTest {

    private lateinit var fakeDao: FakeStaffUserDao
    private lateinit var sessionManager: StaffSessionManager
    private val testScope = TestScope()

    private val adminUser = UserEntity(
        id = "user_admin",
        storeId = "store_main",
        username = "admin",
        passwordHash = "hash1",
        passwordSalt = "salt1",
        role = UserRole.OWNER,
        pin = "1234",
        isActive = true
    )

    private val mariaCashier = UserEntity(
        id = "user_maria",
        storeId = "store_main",
        username = "maria",
        passwordHash = "hash2",
        passwordSalt = "salt2",
        role = UserRole.CASHIER,
        pin = "5678",
        isActive = true
    )

    private val deactivatedCashier = UserEntity(
        id = "user_juan",
        storeId = "store_main",
        username = "juan",
        passwordHash = "hash3",
        passwordSalt = "salt3",
        role = UserRole.CASHIER,
        pin = "9999",
        isActive = false
    )

    @Before
    fun setup() = runTest {
        fakeDao = FakeStaffUserDao()
        fakeDao.insertUser(adminUser)
        fakeDao.insertUser(mariaCashier)
        fakeDao.insertUser(deactivatedCashier)

        sessionManager = StaffSessionManager(
            userDao = fakeDao,
            sessionManager = null,
            scope = testScope
        )
    }

    @Test
    fun testUnlockWithValid4DigitPin_setsActiveStaffAndUnlocks() = runTest {
        // Initial state
        sessionManager.lock()
        assertTrue(sessionManager.isLocked.value)

        // Enter Maria's 4-digit PIN
        val result = sessionManager.unlockWithPin("5678")
        assertTrue(result.isSuccess)
        assertFalse(sessionManager.isLocked.value)
        assertEquals("maria", sessionManager.activeStaff.value?.username)
        assertEquals(UserRole.CASHIER, sessionManager.activeStaff.value?.role)
    }

    @Test
    fun testUnlockWithInvalidPin_failsAndRemainsLocked() = runTest {
        sessionManager.lock()
        assertTrue(sessionManager.isLocked.value)

        val result = sessionManager.unlockWithPin("0000")
        assertTrue(result.isFailure)
        assertTrue(sessionManager.isLocked.value)
    }

    @Test
    fun testUnlockWithNon4DigitPin_failsValidation() = runTest {
        sessionManager.lock()
        val result = sessionManager.unlockWithPin("12")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("4 digits") == true)
    }

    @Test
    fun testDeactivatedStaffPin_rejected() = runTest {
        sessionManager.lock()
        val result = sessionManager.unlockWithPin("9999")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("deactivated") == true)
        assertTrue(sessionManager.isLocked.value)
    }

    @Test
    fun testSwitchStaffWithPin_tracksIndividualResponsibility() = runTest {
        // First, admin logs in
        sessionManager.unlockWithPin("1234")
        assertEquals("admin", sessionManager.activeStaff.value?.username)

        // Maria switches in using her PIN
        val switchResult = sessionManager.switchStaff("5678")
        assertTrue(switchResult.isSuccess)
        assertEquals("maria", sessionManager.activeStaff.value?.username)
        assertFalse(sessionManager.isLocked.value)
    }

    @Test
    fun testCreateStaffWithUniquePin() = runTest {
        // Create new cashier
        val createResult = sessionManager.createStaff(
            username = "pedro",
            role = UserRole.CASHIER,
            pin = "4321"
        )
        assertTrue(createResult.isSuccess)

        // Try creating with duplicate PIN 1234 (already owned by admin)
        val duplicateResult = sessionManager.createStaff(
            username = "clara",
            role = UserRole.CASHIER,
            pin = "1234"
        )
        assertTrue(duplicateResult.isFailure)
        assertTrue(duplicateResult.exceptionOrNull()?.message?.contains("already in use") == true)
    }

    @Test
    fun testManualLockout() = runTest {
        sessionManager.unlockWithPin("1234")
        assertFalse(sessionManager.isLocked.value)

        sessionManager.lock()
        assertTrue(sessionManager.isLocked.value)
    }
}
