package com.example.domain.receipt

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.domain.model.Receipt
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

object ReceiptGenerator {

    /**
     * Generates a clean, monospace-formatted text summary suitable for sharing via
     * SMS, messaging apps (WhatsApp, Viber, Messenger), email, or notes.
     */
    fun generateTextSummary(receipt: Receipt): String {
        val line = "------------------------------------------"
        val doubleLine = "=========================================="

        return buildString {
            appendLine(doubleLine)
            appendLine(centerText(receipt.storeName.uppercase(Locale.US), 42))
            appendLine(centerText(receipt.storeAddress, 42))
            appendLine(centerText(receipt.storeContact, 42))
            appendLine(doubleLine)
            appendLine("Receipt #: ${receipt.receiptNumber}")
            appendLine("Date     : ${receipt.formattedDate}")
            appendLine("Cashier  : ${receipt.cashierName}")
            appendLine("Payment  : ${receipt.paymentMethod}")
            appendLine(line)
            appendLine(String.format(Locale.US, "%-20s %4s %7s %8s", "ITEM", "QTY", "PRICE", "TOTAL"))
            appendLine(line)

            for (item in receipt.items) {
                val itemName = if (item.name.length > 20) item.name.take(19) + "…" else item.name
                appendLine(
                    String.format(
                        Locale.US,
                        "%-20s %4d %7s %8s",
                        itemName,
                        item.quantity,
                        item.formattedUnitPrice,
                        item.formattedSubtotal
                    )
                )
            }

            appendLine(line)
            appendLine(String.format(Locale.US, "%-26s %15s", "Items Count (${receipt.totalItemCount}):", ""))
            appendLine(String.format(Locale.US, "%-26s %15s", "Subtotal:", receipt.formattedSubtotal))
            if (receipt.discount > 0) {
                appendLine(String.format(Locale.US, "%-26s %15s", "Discount:", "-${receipt.formattedDiscount}"))
            }
            if (receipt.tax > 0) {
                appendLine(String.format(Locale.US, "%-26s %15s", "VAT / Tax:", receipt.formattedTax))
            }
            appendLine(line)
            appendLine(String.format(Locale.US, "%-24s %17s", "TOTAL AMOUNT:", receipt.formattedTotal))
            appendLine(String.format(Locale.US, "%-24s %17s", "Amount Tendered:", receipt.formattedTendered))
            appendLine(String.format(Locale.US, "%-24s %17s", "CHANGE DUE:", receipt.formattedChange))
            appendLine(doubleLine)
            appendLine(centerText("Maraming Salamat Po!", 42))
            appendLine(centerText("Thank you for your purchase!", 42))
            appendLine(centerText("Please keep this digital receipt.", 42))
            appendLine(doubleLine)
        }
    }

    private fun centerText(text: String, width: Int): String {
        if (text.length >= width) return text
        val padding = (width - text.length) / 2
        return " ".repeat(padding) + text
    }

    /**
     * Renders and saves a PDF document representing a thermal sales receipt.
     */
    fun generatePdf(context: Context, receipt: Receipt): File {
        val receiptsDir = File(context.cacheDir, "receipts").apply {
            if (!exists()) mkdirs()
        }
        val file = File(receiptsDir, "Receipt_${receipt.receiptNumber}.pdf")

        val pageWidth = 400
        val baseHeight = 380
        val itemsHeight = receipt.items.size * 32
        val pageHeight = maxOf(600, baseHeight + itemsHeight)

        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        // Background
        canvas.drawColor(Color.WHITE)

        val textPaint = Paint().apply {
            color = Color.rgb(30, 41, 59) // Slate 800
            textSize = 12f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        }

        val boldPaint = Paint().apply {
            color = Color.rgb(15, 23, 42) // Slate 900
            textSize = 14f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }

        val headerPaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 18f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        val subHeaderPaint = Paint().apply {
            color = Color.rgb(100, 116, 139) // Slate 500
            textSize = 11f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }

        val linePaint = Paint().apply {
            color = Color.rgb(203, 213, 225) // Slate 300
            strokeWidth = 1.2f
            style = Paint.Style.STROKE
        }

        val boldLinePaint = Paint().apply {
            color = Color.rgb(71, 85, 105) // Slate 600
            strokeWidth = 2f
            style = Paint.Style.STROKE
        }

        var y = 40f
        val margin = 24f
        val contentWidth = pageWidth - (margin * 2)

        // Store Header
        canvas.drawText(receipt.storeName.uppercase(Locale.US), pageWidth / 2f, y, headerPaint)
        y += 18f
        canvas.drawText(receipt.storeAddress, pageWidth / 2f, y, subHeaderPaint)
        y += 16f
        canvas.drawText(receipt.storeContact, pageWidth / 2f, y, subHeaderPaint)
        y += 20f

        // Top Double Line
        canvas.drawLine(margin, y, pageWidth - margin, y, boldLinePaint)
        y += 18f

        // Metadata
        textPaint.textSize = 11f
        canvas.drawText("Receipt #: ${receipt.receiptNumber}", margin, y, boldPaint)
        y += 16f
        canvas.drawText("Date     : ${receipt.formattedDate}", margin, y, textPaint)
        y += 16f
        canvas.drawText("Cashier  : ${receipt.cashierName}", margin, y, textPaint)
        y += 16f
        canvas.drawText("Payment  : ${receipt.paymentMethod}", margin, y, textPaint)
        y += 18f

        // Table Header
        canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
        y += 16f
        boldPaint.textSize = 11f
        canvas.drawText("ITEM", margin, y, boldPaint)
        canvas.drawText("QTY", margin + 180f, y, boldPaint)
        canvas.drawText("PRICE", margin + 230f, y, boldPaint)
        canvas.drawText("TOTAL", margin + 300f, y, boldPaint)
        y += 10f
        canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
        y += 20f

        // Items List
        textPaint.textSize = 11f
        for (item in receipt.items) {
            val name = if (item.name.length > 22) item.name.take(21) + "…" else item.name
            canvas.drawText(name, margin, y, textPaint)
            canvas.drawText("${item.quantity}", margin + 185f, y, textPaint)
            canvas.drawText(item.formattedUnitPrice, margin + 230f, y, textPaint)
            canvas.drawText(item.formattedSubtotal, margin + 300f, y, boldPaint)
            y += 22f
        }

        y += 4f
        canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
        y += 20f

        // Financials
        canvas.drawText("Items Count: ${receipt.totalItemCount}", margin, y, textPaint)
        canvas.drawText("Subtotal:", margin + 180f, y, textPaint)
        canvas.drawText(receipt.formattedSubtotal, margin + 300f, y, textPaint)
        y += 18f

        if (receipt.discount > 0) {
            canvas.drawText("Discount:", margin + 180f, y, textPaint)
            canvas.drawText("-${receipt.formattedDiscount}", margin + 300f, y, textPaint)
            y += 18f
        }

        if (receipt.tax > 0) {
            canvas.drawText("VAT / Tax:", margin + 180f, y, textPaint)
            canvas.drawText(receipt.formattedTax, margin + 300f, y, textPaint)
            y += 18f
        }

        canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
        y += 22f

        // Grand Total in Large Bold
        val totalPaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 15f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        canvas.drawText("TOTAL AMOUNT:", margin, y, totalPaint)
        canvas.drawText(receipt.formattedTotal, margin + 280f, y, totalPaint)
        y += 20f

        canvas.drawText("Amount Tendered:", margin, y, textPaint)
        canvas.drawText(receipt.formattedTendered, margin + 300f, y, textPaint)
        y += 18f

        val changePaint = Paint().apply {
            color = Color.rgb(5, 150, 105) // Emerald 600
            textSize = 13f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        canvas.drawText("CHANGE DUE:", margin, y, changePaint)
        canvas.drawText(receipt.formattedChange, margin + 300f, y, changePaint)
        y += 24f

        // Footer Divider
        canvas.drawLine(margin, y, pageWidth - margin, y, boldLinePaint)
        y += 22f

        canvas.drawText("Maraming Salamat Po!", pageWidth / 2f, y, subHeaderPaint)
        y += 16f
        canvas.drawText("Please keep this digital copy for your records.", pageWidth / 2f, y, subHeaderPaint)

        document.finishPage(page)

        FileOutputStream(file).use { out ->
            document.writeTo(out)
        }
        document.close()

        return file
    }

    /**
     * Shares the plain text summary using Android's native Share Sheet.
     */
    fun shareTextSummary(context: Context, receipt: Receipt) {
        val summary = generateTextSummary(receipt)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Receipt #${receipt.receiptNumber} - ${receipt.storeName}")
            putExtra(Intent.EXTRA_TEXT, summary)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(intent, "Share Digital Receipt via...").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    /**
     * Generates the PDF and launches the native Android Share Sheet with the PDF attachment.
     */
    fun sharePdf(context: Context, receipt: Receipt): File {
        val file = generatePdf(context, receipt)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_SUBJECT, "Digital Receipt - ${receipt.receiptNumber}")
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(intent, "Share PDF Receipt...").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
        return file
    }

    /**
     * Opens the generated PDF in an external PDF viewer.
     */
    fun viewPdf(context: Context, receipt: Receipt): File {
        val file = generatePdf(context, receipt)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(intent, "Open PDF Receipt with...").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
        return file
    }

    /**
     * Copies the formatted receipt text to the device clipboard.
     */
    fun copyToClipboard(context: Context, receipt: Receipt) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Receipt #${receipt.receiptNumber}", generateTextSummary(receipt))
        clipboard.setPrimaryClip(clip)
    }
}
