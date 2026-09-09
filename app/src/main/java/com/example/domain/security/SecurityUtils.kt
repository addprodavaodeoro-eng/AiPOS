package com.example.domain.security

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object SecurityUtils {

    private const val ITERATIONS = 100000 // Recommended minimum for PBKDF2
    private const val KEY_LENGTH = 256
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val ENCRYPTION_ALGORITHM = "AES/GCM/NoPadding"
    private const val AES = "AES"

    fun generateSalt(): String {
        val random = SecureRandom()
        val salt = ByteArray(16)
        random.nextBytes(salt)
        return Base64.getEncoder().encodeToString(salt)
    }

    fun hashPassword(password: String, saltBase64: String): String {
        val salt = Base64.getDecoder().decode(saltBase64)
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance(ALGORITHM)
        val hash = factory.generateSecret(spec).encoded
        return Base64.getEncoder().encodeToString(hash)
    }

    fun verifyPassword(password: String, saltBase64: String, expectedHashBase64: String): Boolean {
        val hash = hashPassword(password, saltBase64)
        return hash == expectedHashBase64
    }

    /**
     * Encrypts data using AES-GCM with a key derived from the password.
     * Output format: Base64(salt):Base64(iv):Base64(encryptedData)
     */
    fun encryptData(data: String, passwordRaw: String): String {
        val salt = generateSalt()
        val key = generateSecretKey(passwordRaw, salt)
        
        val cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM)
        val iv = ByteArray(12)
        SecureRandom().nextBytes(iv)
        val gcmSpec = GCMParameterSpec(128, iv)
        
        cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec)
        val encryptedBytes = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
        
        val ivBase64 = Base64.getEncoder().encodeToString(iv)
        val encryptedBase64 = Base64.getEncoder().encodeToString(encryptedBytes)
        
        return "$salt:$ivBase64:$encryptedBase64"
    }

    /**
     * Decrypts an AES-GCM encrypted string (format: Base64(salt):Base64(iv):Base64(encryptedData)).
     */
    fun decryptData(encryptedString: String, passwordRaw: String): String {
        val parts = encryptedString.split(":")
        if (parts.size != 3) {
            throw IllegalArgumentException("Invalid encrypted file format or corrupted data.")
        }
        
        val salt = parts[0]
        val iv = Base64.getDecoder().decode(parts[1])
        val encryptedBytes = Base64.getDecoder().decode(parts[2])
        
        val key = generateSecretKey(passwordRaw, salt)
        
        val cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM)
        val gcmSpec = GCMParameterSpec(128, iv)
        
        cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec)
        val decryptedBytes = cipher.doFinal(encryptedBytes)
        
        return String(decryptedBytes, Charsets.UTF_8)
    }

    private fun generateSecretKey(password: String, saltBase64: String): SecretKey {
        val salt = Base64.getDecoder().decode(saltBase64)
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance(ALGORITHM)
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, AES)
    }
}

