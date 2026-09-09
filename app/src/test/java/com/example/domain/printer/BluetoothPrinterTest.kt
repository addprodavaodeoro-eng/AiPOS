package com.example.domain.printer

import com.example.domain.model.Receipt
import com.example.domain.model.ReceiptItem
import org.junit.Assert.*
import org.junit.Test

class BluetoothPrinterTest {

    private val sampleReceipt = Receipt(
        receiptNumber = "REC-20260906-0042",
        timestamp = 1788771600000L,
        items = listOf(
            ReceiptItem("1", "Datu Puti Vinegar 1L", 45.0, 2, 90.0),
            ReceiptItem("2", "Lucky Me Pancit Canton", 18.50, 4, 74.0)
        ),
        subtotal = 164.0,
        discount = 0.0,
        tax = 0.0,
        totalAmount = 164.0,
        paymentMethod = "Cash",
        amountTendered = 200.0,
        change = 36.0,
        cashierName = "Nena",
        storeName = "Aling Nena Sari-Sari Store"
    )

    @Test
    fun escPosFormatter_buildsValidByteArray() {
        val bytes58 = EscPosReceiptFormatter.buildEscPosBytes(sampleReceipt, paperWidthMm = 58)
        assertNotNull(bytes58)
        assertTrue(bytes58.isNotEmpty())
        // Verify init ESC @
        assertEquals(EscPosReceiptFormatter.ESC, bytes58[0])
        assertEquals('@'.code.toByte(), bytes58[1])

        val bytes80 = EscPosReceiptFormatter.buildEscPosBytes(sampleReceipt, paperWidthMm = 80)
        assertNotNull(bytes80)
        assertTrue(bytes80.isNotEmpty())
    }

    @Test
    fun bluetoothPrinter_displayNameHandlesEmpty() {
        val printer = BluetoothPrinter(name = "", address = "00:11:22:33:44:55")
        assertEquals("Unknown Bluetooth Printer", printer.displayName)

        val namedPrinter = BluetoothPrinter(name = "RP-58", address = "00:11:22:33:44:55")
        assertEquals("RP-58", namedPrinter.displayName)
    }
}
