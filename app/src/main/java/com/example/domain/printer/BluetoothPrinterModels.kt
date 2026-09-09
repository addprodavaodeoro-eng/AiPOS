package com.example.domain.printer

import android.bluetooth.BluetoothDevice
import java.util.Locale

data class BluetoothPrinter(
    val name: String,
    val address: String,
    val isBonded: Boolean = false,
    val isConnected: Boolean = false,
    val paperWidthMm: Int = 58 // 58mm standard thermal receipt printer, or 80mm
) {
    val displayName: String
        get() = name.ifBlank { "Unknown Bluetooth Printer" }
}

sealed interface PrintJobStatus {
    object Idle : PrintJobStatus
    object Scanning : PrintJobStatus
    data class Connecting(val printerName: String) : PrintJobStatus
    data class Printing(val printerName: String, val progressPercentage: Int) : PrintJobStatus
    data class Success(val printerName: String, val timestamp: Long = System.currentTimeMillis()) : PrintJobStatus
    data class Error(val message: String) : PrintJobStatus
}

object EscPosReceiptFormatter {

    // Standard ESC/POS Control Bytes
    val ESC: Byte = 0x1B
    val GS: Byte = 0x1D
    val LF: Byte = 0x0A

    val CMD_INIT: ByteArray = byteArrayOf(ESC, '@'.code.toByte())
    val CMD_ALIGN_LEFT: ByteArray = byteArrayOf(ESC, 'a'.code.toByte(), 0x00)
    val CMD_ALIGN_CENTER: ByteArray = byteArrayOf(ESC, 'a'.code.toByte(), 0x01)
    val CMD_ALIGN_RIGHT: ByteArray = byteArrayOf(ESC, 'a'.code.toByte(), 0x02)
    val CMD_BOLD_ON: ByteArray = byteArrayOf(ESC, 'E'.code.toByte(), 0x01)
    val CMD_BOLD_OFF: ByteArray = byteArrayOf(ESC, 'E'.code.toByte(), 0x00)
    val CMD_DOUBLE_HEIGHT: ByteArray = byteArrayOf(GS, '!'.code.toByte(), 0x10)
    val CMD_NORMAL_SIZE: ByteArray = byteArrayOf(GS, '!'.code.toByte(), 0x00)
    val CMD_FEED_CUT: ByteArray = byteArrayOf(ESC, 'd'.code.toByte(), 0x04, GS, 'V'.code.toByte(), 0x42, 0x00)

    /**
     * Builds ESC/POS thermal printer byte stream from a digital receipt model.
     * Supports 32-column (58mm) and 48-column (80mm) widths.
     */
    fun buildEscPosBytes(receipt: com.example.domain.model.Receipt, paperWidthMm: Int = 58): ByteArray {
        val maxCols = if (paperWidthMm == 80) 48 else 32
        val stream = java.io.ByteArrayOutputStream()

        fun write(bytes: ByteArray) = stream.write(bytes)
        fun writeText(text: String) = stream.write(text.toByteArray(Charsets.ISO_8859_1))
        fun line(text: String = "") {
            writeText(text)
            stream.write(LF.toInt())
        }

        fun divider() {
            line("-".repeat(maxCols))
        }

        fun padRow(left: String, right: String): String {
            val totalLen = left.length + right.length
            return if (totalLen >= maxCols) {
                left.take(maxCols - right.length - 1) + " " + right
            } else {
                left + " ".repeat(maxCols - totalLen) + right
            }
        }

        // 1. Initialize
        write(CMD_INIT)

        // 2. Header (Center Aligned)
        write(CMD_ALIGN_CENTER)
        write(CMD_DOUBLE_HEIGHT)
        write(CMD_BOLD_ON)
        line(receipt.storeName)
        write(CMD_NORMAL_SIZE)
        write(CMD_BOLD_OFF)
        line(receipt.storeAddress)
        line(receipt.storeContact)
        line()

        // 3. Receipt Details (Left Aligned)
        write(CMD_ALIGN_LEFT)
        divider()
        line(padRow("Receipt #:", receipt.receiptNumber))
        line(padRow("Date:", receipt.formattedDate))
        line(padRow("Cashier:", receipt.cashierName))
        line(padRow("Payment:", receipt.paymentMethod))
        divider()

        // 4. Items Table
        write(CMD_BOLD_ON)
        if (maxCols == 32) {
            line(padRow("ITEM", "QTY   AMOUNT"))
        } else {
            line(padRow("ITEM (QTY x PRICE)", "TOTAL"))
        }
        write(CMD_BOLD_OFF)
        divider()

        for (item in receipt.items) {
            val itemLineLeft = "${item.name} (${item.quantity}x)"
            val itemLineRight = item.formattedSubtotal
            line(padRow(itemLineLeft, itemLineRight))
        }
        divider()

        // 5. Financial Totals
        line(padRow("Subtotal:", receipt.formattedSubtotal))
        if (receipt.discount > 0) {
            line(padRow("Discount:", "-${receipt.formattedDiscount}"))
        }
        if (receipt.tax > 0) {
            line(padRow("VAT / Tax:", receipt.formattedTax))
        }
        divider()

        write(CMD_BOLD_ON)
        write(CMD_DOUBLE_HEIGHT)
        line(padRow("TOTAL:", receipt.formattedTotal))
        write(CMD_NORMAL_SIZE)
        write(CMD_BOLD_OFF)
        divider()

        line(padRow("Amount Tendered:", receipt.formattedTendered))
        line(padRow("Change Due:", receipt.formattedChange))
        divider()

        // 6. Footer message
        write(CMD_ALIGN_CENTER)
        line()
        line("THANK YOU FOR YOUR PATRONAGE!")
        line("Please come again.")
        line()

        // 7. Paper Feed and Cut
        write(CMD_FEED_CUT)

        return stream.toByteArray()
    }
}
