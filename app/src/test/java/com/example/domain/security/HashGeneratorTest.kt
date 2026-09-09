package com.example.domain.security

import org.junit.Test

class HashGeneratorTest {
    @Test
    fun generateAdminHash() {
        val salt = SecurityUtils.generateSalt()
        val hash = SecurityUtils.hashPassword("admin2026@", salt)
        println("SALT_VAL: $salt")
        println("HASH_VAL: $hash")
    }
}
