package com.example.domain.usecase

import com.example.data.local.dao.UserDao
import com.example.data.local.entity.UserEntity
import com.example.domain.security.SecurityUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthenticateUserUseCase(private val userDao: UserDao) {

    suspend operator fun invoke(username: String, passwordRaw: String): Result<UserEntity> = withContext(Dispatchers.IO) {
        val sanitizedUsername = username.trim().lowercase()
        
        // 1. Fetch user securely by sanitized username
        val user = userDao.getUserByUsername(sanitizedUsername)
            ?: return@withContext Result.failure(IllegalArgumentException("Invalid username or password."))

        if (!user.isActive) {
            return@withContext Result.failure(IllegalStateException("Account is disabled."))
        }

        // 2. Validate password securely using PBKDF2 hash comparison
        val isValid = SecurityUtils.verifyPassword(passwordRaw, user.passwordSalt, user.passwordHash)
        
        if (isValid) {
            Result.success(user)
        } else {
            Result.failure(IllegalArgumentException("Invalid username or password.")) // Same vague error for security
        }
    }
}
