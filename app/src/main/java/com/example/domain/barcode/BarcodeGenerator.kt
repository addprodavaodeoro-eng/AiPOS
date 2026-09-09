package com.example.domain.barcode

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException

object BarcodeGenerator {
    /**
     * Generates a Bitmap for a given barcode string.
     */
    fun generateBarcodeBitmap(content: String, width: Int = 400, height: Int = 150): Bitmap? {
        if (content.isBlank()) return null
        return try {
            val bitMatrix = MultiFormatWriter().encode(
                content,
                BarcodeFormat.CODE_128,
                width,
                height
            )
            val bmWidth = bitMatrix.width
            val bmHeight = bitMatrix.height
            val bitmap = Bitmap.createBitmap(bmWidth, bmHeight, Bitmap.Config.ARGB_8888)

            for (x in 0 until bmWidth) {
                for (y in 0 until bmHeight) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            bitmap
        } catch (e: WriterException) {
            e.printStackTrace()
            null
        }
    }
}
