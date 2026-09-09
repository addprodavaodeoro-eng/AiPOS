package com.example.domain.usecase

import com.example.data.local.dao.UserDao
import com.example.data.local.entity.UserEntity
import com.example.data.local.entity.UserRole
import com.example.domain.security.SecurityUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SeedDefaultAdminUseCase(private val userDao: UserDao) {

    suspend operator fun invoke(storeId: String) = withContext(Dispatchers.IO) {
        val defaultSalt = "EmiZE0ZxTBdaDYChPgeKIA=="
        val defaultHash = "QS7RTLEsrO2qnfqsBiFjK3h0JF+dLM8couA9VMSXJ60="

        val existingAdmin = userDao.getUserByUsername("admin")
        if (existingAdmin == null) {
            val adminUser = UserEntity(
                storeId = storeId,
                username = "admin",
                passwordHash = defaultHash,
                passwordSalt = defaultSalt,
                role = UserRole.OWNER,
                pin = "1234",
                requirePasswordChange = false,
                isActive = true
            )
            userDao.insertUser(adminUser)
        } else if (existingAdmin.pin == null) {
            userDao.updateUser(existingAdmin.copy(pin = "1234"))
        }

        // Seed default Cashier (PIN: 0000)
        val existingCashier = userDao.getUserByUsername("cashier")
        if (existingCashier == null) {
            userDao.insertUser(
                UserEntity(
                    storeId = storeId,
                    username = "cashier",
                    passwordHash = defaultHash,
                    passwordSalt = defaultSalt,
                    role = UserRole.CASHIER,
                    pin = "0000",
                    requirePasswordChange = false,
                    isActive = true
                )
            )
        } else if (existingCashier.pin == null) {
            userDao.updateUser(existingCashier.copy(pin = "0000"))
        }

        // Seed Maria Santos (PIN: 2468)
        val existingMaria = userDao.getUserByUsername("maria")
        if (existingMaria == null) {
            userDao.insertUser(
                UserEntity(
                    storeId = storeId,
                    username = "maria",
                    passwordHash = defaultHash,
                    passwordSalt = defaultSalt,
                    role = UserRole.CASHIER,
                    pin = "2468",
                    requirePasswordChange = false,
                    isActive = true
                )
            )
        }
    }
}
