package com.example.domain.barcode

import kotlin.random.Random

object BarcodeUtils {
    /**
     * Generates a random 12-digit UPC-A string, or a 13-digit EAN-13 string.
     * We'll default to a 12-digit numerical string.
     */
    fun generateRandomBarcode(): String {
        val sb = java.lang.StringBuilder()
        // Standard EAN-13 or UPC-A barcode can be generated. Let's make a generic 12 digit one.
        for (i in 0 until 12) {
            sb.append(Random.nextInt(10))
        }
        return sb.toString()
    }
}
