package com.example.domain.auth

import com.example.data.local.dao.UserDao
import com.example.data.local.entity.UserEntity
import com.example.data.local.entity.UserRole
import com.example.data.local.prefs.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class StaffSessionManager(
    private val userDao: UserDao,
    private val sessionManager: SessionManager? = null,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {
    private val _activeStaff = MutableStateFlow<UserEntity?>(null)
    val activeStaff: StateFlow<UserEntity?> = _activeStaff.asStateFlow()

    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    // Default lockout after 60 seconds of user inactivity. (0 = disabled)
    private val _lockoutTimeoutSeconds = MutableStateFlow(60)
    val lockoutTimeoutSeconds: StateFlow<Int> = _lockoutTimeoutSeconds.asStateFlow()

    private val _remainingSeconds = MutableStateFlow(60)
    val remainingSeconds: StateFlow<Int> = _remainingSeconds.asStateFlow()

    @Volatile
    private var lastActivityTime: Long = System.currentTimeMillis()

    init {
        // Initialize default active staff from existing session or default admin
        scope.launch {
            val users = userDao.getAllUsers()
            val initialUser = users.firstOrNull { it.role == UserRole.OWNER }
                ?: users.firstOrNull { it.role == UserRole.CASHIER }
                ?: users.firstOrNull()

            _activeStaff.value = initialUser
            recordActivity()

            // Inactivity monitor loop
            while (isActive) {
                delay(1000L)
                val timeout = _lockoutTimeoutSeconds.value
                if (timeout > 0 && !_isLocked.value && _activeStaff.value != null) {
                    val elapsedSeconds = ((System.currentTimeMillis() - lastActivityTime) / 1000).toInt()
                    val remaining = (timeout - elapsedSeconds).coerceAtLeast(0)
                    _remainingSeconds.value = remaining

                    if (elapsedSeconds >= timeout) {
                        _isLocked.value = true
                    }
                }
            }
        }
    }

    fun recordActivity() {
        lastActivityTime = System.currentTimeMillis()
        _remainingSeconds.value = _lockoutTimeoutSeconds.value
    }

    fun lock() {
        _isLocked.value = true
    }

    suspend fun unlockWithPin(pin: String): Result<UserEntity> = withContext(Dispatchers.IO) {
        val trimmedPin = pin.trim()
        if (trimmedPin.length != 4 || !trimmedPin.all { it.isDigit() }) {
            return@withContext Result.failure(IllegalArgumentException("PIN must be exactly 4 digits."))
        }

        val user = userDao.getUserByPin(trimmedPin)
            ?: return@withContext Result.failure(IllegalArgumentException("Invalid PIN. Access denied."))

        if (!user.isActive) {
            return@withContext Result.failure(IllegalStateException("This staff account is deactivated."))
        }

        _activeStaff.value = user
        _isLocked.value = false
        recordActivity()
        sessionManager?.createSession(user.id, user.storeId)

        Result.success(user)
    }

    suspend fun switchStaff(pin: String): Result<UserEntity> {
        return unlockWithPin(pin)
    }

    fun setLockoutTimeout(seconds: Int) {
        _lockoutTimeoutSeconds.value = seconds.coerceAtLeast(0)
        recordActivity()
    }

    fun allStaffFlow(): Flow<List<UserEntity>> {
        return userDao.getAllUsersFlow()
    }

    suspend fun createStaff(
        username: String,
        role: UserRole,
        pin: String,
        storeId: String = "store1"
    ): Result<UserEntity> = withContext(Dispatchers.IO) {
        val cleanName = username.trim()
        val cleanPin = pin.trim()

        if (cleanName.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Staff name cannot be empty."))
        }

        if (cleanPin.length != 4 || !cleanPin.all { it.isDigit() }) {
            return@withContext Result.failure(IllegalArgumentException("PIN must be exactly 4 digits."))
        }

        val existingWithPin = userDao.getUserByPin(cleanPin)
        if (existingWithPin != null) {
            return@withContext Result.failure(IllegalArgumentException("PIN '$cleanPin' is already in use by ${existingWithPin.username}."))
        }

        val existingUser = userDao.getUserByUsername(cleanName.lowercase())
        if (existingUser != null) {
            return@withContext Result.failure(IllegalArgumentException("A staff member with name '$cleanName' already exists."))
        }

        val defaultSalt = "EmiZE0ZxTBdaDYChPgeKIA=="
        val defaultHash = "QS7RTLEsrO2qnfqsBiFjK3h0JF+dLM8couA9VMSXJ60="

        val newStaff = UserEntity(
            id = UUID.randomUUID().toString(),
            storeId = storeId,
            username = cleanName,
            passwordHash = defaultHash,
            passwordSalt = defaultSalt,
            role = role,
            pin = cleanPin,
            requirePasswordChange = false,
            isActive = true
        )

        userDao.insertUser(newStaff)
        Result.success(newStaff)
    }

    suspend fun updateStaffPin(userId: String, newPin: String): Result<Unit> = withContext(Dispatchers.IO) {
        val cleanPin = newPin.trim()
        if (cleanPin.length != 4 || !cleanPin.all { it.isDigit() }) {
            return@withContext Result.failure(IllegalArgumentException("PIN must be exactly 4 digits."))
        }

        val existingWithPin = userDao.getUserByPin(cleanPin)
        if (existingWithPin != null && existingWithPin.id != userId) {
            return@withContext Result.failure(IllegalArgumentException("PIN '$cleanPin' is already in use by ${existingWithPin.username}."))
        }

        val user = userDao.getUserById(userId)
            ?: return@withContext Result.failure(IllegalArgumentException("User not found."))

        userDao.updateUser(user.copy(pin = cleanPin, updatedAt = System.currentTimeMillis()))
        if (_activeStaff.value?.id == userId) {
            _activeStaff.value = user.copy(pin = cleanPin)
        }
        Result.success(Unit)
    }

    suspend fun deleteStaff(userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val user = userDao.getUserById(userId)
            ?: return@withContext Result.failure(IllegalArgumentException("User not found."))

        if (user.role == UserRole.OWNER) {
            val allUsers = userDao.getAllUsers()
            val ownerCount = allUsers.count { it.role == UserRole.OWNER }
            if (ownerCount <= 1) {
                return@withContext Result.failure(IllegalStateException("Cannot delete the primary Store Owner account."))
            }
        }

        userDao.deleteUserById(userId)
        if (_activeStaff.value?.id == userId) {
            val fallback = userDao.getAllUsers().firstOrNull()
            _activeStaff.value = fallback
        }
        Result.success(Unit)
    }
}
