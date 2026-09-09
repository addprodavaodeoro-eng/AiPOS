package com.example.domain.usecase

import com.example.data.local.dao.UserDao
import com.example.data.local.entity.UserEntity
import com.example.data.local.entity.UserRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeUserDao : UserDao {
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

    override suspend fun insertAllUsers(users: List<UserEntity>) {
        users.forEach { this.users[it.id] = it }
    }

    override suspend fun deleteAllUsers() {
        users.clear()
    }

    override fun getAllUsersFlow(): Flow<List<UserEntity>> {
        return flowOf(users.values.toList())
    }

    override suspend fun deleteUserById(userId: String) {
        users.remove(userId)
    }

    override suspend fun getUserByPin(pin: String): UserEntity? {
        return users.values.firstOrNull { it.pin == pin }
    }
}

class AuthenticationTests {

    private lateinit var fakeDao: FakeUserDao
    private lateinit var authUseCase: AuthenticateUserUseCase

    @Before
    fun setup() {
        fakeDao = FakeUserDao()
        authUseCase = AuthenticateUserUseCase(fakeDao)
    }

    @Test
    fun `Correct credentials successfully log in`() = kotlinx.coroutines.runBlocking {
        // Pre-seed the default admin (using the generated salt/hash)
        val defaultSalt = "EmiZE0ZxTBdaDYChPgeKIA=="
        val defaultHash = "QS7RTLEsrO2qnfqsBiFjK3h0JF+dLM8couA9VMSXJ60="
        
        fakeDao.insertUser(UserEntity(
            storeId = "store1",
            username = "admin",
            passwordHash = defaultHash,
            passwordSalt = defaultSalt,
            role = UserRole.OWNER
        ))

        // ACT
        val result = authUseCase("admin", "admin2026@")
        
        // ASSERT
        assertTrue("Expected successful login", result.isSuccess)
        assertEquals(UserRole.OWNER, result.getOrNull()?.role)
    }

    @Test
    fun `Incorrect password is rejected`() = kotlinx.coroutines.runBlocking {
        val defaultSalt = "EmiZE0ZxTBdaDYChPgeKIA=="
        val defaultHash = "QS7RTLEsrO2qnfqsBiFjK3h0JF+dLM8couA9VMSXJ60="
        
        fakeDao.insertUser(UserEntity(
            storeId = "store1",
            username = "admin",
            passwordHash = defaultHash,
            passwordSalt = defaultSalt,
            role = UserRole.OWNER
        ))

        val result = authUseCase("admin", "wrong_password")
        assertTrue("Expected login failure", result.isFailure)
        assertEquals("Invalid username or password.", result.exceptionOrNull()?.message)
    }

    @Test
    fun `Incorrect username is rejected`() = kotlinx.coroutines.runBlocking {
        val result = authUseCase("nonexistent_user", "admin2026@")
        assertTrue("Expected login failure", result.isFailure)
        assertEquals("Invalid username or password.", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun `Username is case-insensitive`() = kotlinx.coroutines.runBlocking {
        val defaultSalt = "EmiZE0ZxTBdaDYChPgeKIA=="
        val defaultHash = "QS7RTLEsrO2qnfqsBiFjK3h0JF+dLM8couA9VMSXJ60="
        
        fakeDao.insertUser(UserEntity(
            storeId = "store1",
            username = "admin",
            passwordHash = defaultHash,
            passwordSalt = defaultSalt,
            role = UserRole.OWNER
        ))

        val result = authUseCase("AdMiN", "admin2026@")
        assertTrue("Expected successful login", result.isSuccess)
    }
}
