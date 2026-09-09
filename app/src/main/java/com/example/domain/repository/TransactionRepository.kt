package com.example.domain.repository

import com.example.data.local.dao.TransactionLogDao
import com.example.data.local.entity.TransactionLogEntity
import com.example.domain.model.DailySalesSummary
import com.example.domain.model.HourlySalesBucket
import com.example.domain.model.PaymentBreakdown
import com.example.domain.model.Receipt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

interface ITransactionRepository {
    val allTransactions: Flow<List<TransactionLogEntity>>
    fun getTransactionsByDate(dateString: String): Flow<List<TransactionLogEntity>>
    fun getDailySalesSummary(dateString: String): Flow<DailySalesSummary>
    suspend fun recordTransaction(receipt: Receipt): TransactionLogEntity
    suspend fun recordManualTransaction(entity: TransactionLogEntity)
    suspend fun getTransactionById(id: String): TransactionLogEntity?
    suspend fun seedSampleTransactionsIfEmpty()
    suspend fun deleteTransaction(id: String)
    suspend fun deleteAllTransactions()
}

class TransactionRepository(
    private val transactionLogDao: TransactionLogDao
) : ITransactionRepository {

    override val allTransactions: Flow<List<TransactionLogEntity>> =
        transactionLogDao.getAllTransactions().flowOn(Dispatchers.IO)

    override fun getTransactionsByDate(dateString: String): Flow<List<TransactionLogEntity>> =
        transactionLogDao.getTransactionsByDate(dateString).flowOn(Dispatchers.IO)

    override fun getDailySalesSummary(dateString: String): Flow<DailySalesSummary> {
        return transactionLogDao.getTransactionsByDate(dateString).map { list ->
            val totalGross = list.sumOf { it.totalAmount }
            val totalDiscount = list.sumOf { it.discountAmount }
            val totalNet = totalGross // net amount collected
            val totalTransactions = list.size
            val totalItems = list.sumOf { it.totalItemCount }
            val avgTicket = if (totalTransactions > 0) totalGross / totalTransactions else 0.0

            // Payment Breakdown
            val byPayment = list.groupBy { it.paymentMethod.ifBlank { "Cash" } }
            val paymentBreakdowns = byPayment.map { (method, txs) ->
                val methodTotal = txs.sumOf { it.totalAmount }
                val pct = if (totalGross > 0) ((methodTotal / totalGross) * 100).toFloat() else 0f
                PaymentBreakdown(
                    paymentMethod = method,
                    totalAmount = methodTotal,
                    count = txs.size,
                    percentage = pct
                )
            }.sortedByDescending { it.totalAmount }

            // Hourly Distribution (e.g. 6AM-9AM, 9AM-12PM, 12PM-3PM, 3PM-6PM, 6PM-9PM, 9PM-12AM)
            val hourlyBuckets = mutableMapOf<String, Pair<Double, Int>>()
            val bucketNames = listOf(
                "6 AM - 9 AM" to (6..8),
                "9 AM - 12 PM" to (9..11),
                "12 PM - 3 PM" to (12..14),
                "3 PM - 6 PM" to (15..17),
                "6 PM - 9 PM" to (18..20),
                "9 PM - 12 AM" to (21..23)
            )

            bucketNames.forEach { (label, _) ->
                hourlyBuckets[label] = 0.0 to 0
            }

            val cal = Calendar.getInstance()
            list.forEach { tx ->
                cal.timeInMillis = tx.timestamp
                val hour = cal.get(Calendar.HOUR_OF_DAY)
                val matchedBucket = bucketNames.find { (_, range) -> hour in range }?.first ?: "9 AM - 12 PM"
                val current = hourlyBuckets[matchedBucket] ?: (0.0 to 0)
                hourlyBuckets[matchedBucket] = (current.first + tx.totalAmount) to (current.second + 1)
            }

            val hourlyList = bucketNames.map { (label, _) ->
                val data = hourlyBuckets[label] ?: (0.0 to 0)
                HourlySalesBucket(
                    hourLabel = label,
                    totalAmount = data.first,
                    count = data.second
                )
            }

            val displayDate = try {
                val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateString)
                if (parsed != null) SimpleDateFormat("MMMM dd, yyyy", Locale.US).format(parsed) else dateString
            } catch (e: Exception) {
                dateString
            }

            DailySalesSummary(
                dateString = dateString,
                displayDate = displayDate,
                totalGrossSales = totalGross,
                totalNetSales = totalNet,
                totalDiscount = totalDiscount,
                totalTransactions = totalTransactions,
                totalItemsSold = totalItems,
                averageTicketSize = avgTicket,
                paymentBreakdowns = paymentBreakdowns,
                hourlyBreakdowns = hourlyList,
                transactions = list
            )
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun recordTransaction(receipt: Receipt): TransactionLogEntity = withContext(Dispatchers.IO) {
        val summaryStr = receipt.items.joinToString(", ") { "${it.quantity}x ${it.name}" }
        val itemsJson = buildString {
            append("[")
            receipt.items.forEachIndexed { index, item ->
                append("{\"name\":\"${item.name}\",\"qty\":${item.quantity},\"price\":${item.unitPrice},\"subtotal\":${item.subtotal}}")
                if (index < receipt.items.size - 1) append(",")
            }
            append("]")
        }

        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(receipt.timestamp))

        val entity = TransactionLogEntity(
            receiptNumber = receipt.receiptNumber,
            timestamp = receipt.timestamp,
            dateString = dateStr,
            totalAmount = receipt.totalAmount,
            subtotal = receipt.subtotal,
            discountAmount = receipt.discount,
            taxAmount = receipt.tax,
            paymentMethod = receipt.paymentMethod,
            amountTendered = receipt.amountTendered,
            changeAmount = receipt.change,
            totalItemCount = receipt.totalItemCount,
            itemsSummary = summaryStr,
            itemsJson = itemsJson,
            cashierName = receipt.cashierName
        )
        transactionLogDao.insertTransaction(entity)
        entity
    }

    override suspend fun recordManualTransaction(entity: TransactionLogEntity) = withContext(Dispatchers.IO) {
        transactionLogDao.insertTransaction(entity)
    }

    override suspend fun getTransactionById(id: String): TransactionLogEntity? = withContext(Dispatchers.IO) {
        transactionLogDao.getTransactionById(id)
    }

    override suspend fun deleteTransaction(id: String) = withContext(Dispatchers.IO) {
        transactionLogDao.deleteTransactionById(id)
    }

    override suspend fun deleteAllTransactions() = withContext(Dispatchers.IO) {
        transactionLogDao.deleteAllTransactions()
    }

    override suspend fun seedSampleTransactionsIfEmpty() = withContext(Dispatchers.IO) {
        if (transactionLogDao.getTotalTransactionCount() == 0) {
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val now = System.currentTimeMillis()

            val sampleLogs = listOf(
                TransactionLogEntity(
                    receiptNumber = "RCP-20260907-081522",
                    timestamp = now - 14400000L, // 4 hrs ago
                    dateString = todayStr,
                    totalAmount = 145.00,
                    subtotal = 145.00,
                    discountAmount = 0.0,
                    paymentMethod = "Cash",
                    amountTendered = 200.00,
                    changeAmount = 55.00,
                    totalItemCount = 4,
                    itemsSummary = "2x Lucky Me Pancit Canton, 2x Kopiko 3-in-1 Blanca",
                    cashierName = "Aling Nena"
                ),
                TransactionLogEntity(
                    receiptNumber = "RCP-20260907-094510",
                    timestamp = now - 10800000L, // 3 hrs ago
                    dateString = todayStr,
                    totalAmount = 380.00,
                    subtotal = 380.00,
                    discountAmount = 0.0,
                    paymentMethod = "GCash",
                    amountTendered = 380.00,
                    changeAmount = 0.0,
                    totalItemCount = 5,
                    itemsSummary = "1x Coca-Cola 1.5L, 2x Datu Puti Soy Sauce, 2x Silver Swan Vinegar",
                    cashierName = "Aling Nena"
                ),
                TransactionLogEntity(
                    receiptNumber = "RCP-20260907-111045",
                    timestamp = now - 7200000L, // 2 hrs ago
                    dateString = todayStr,
                    totalAmount = 520.00,
                    subtotal = 540.00,
                    discountAmount = 20.00,
                    paymentMethod = "Cash",
                    amountTendered = 1000.00,
                    changeAmount = 480.00,
                    totalItemCount = 6,
                    itemsSummary = "2x Purefoods Corned Beef, 2x Century Tuna Flakes in Oil, 2x Mega Sardines",
                    cashierName = "Aling Nena"
                ),
                TransactionLogEntity(
                    receiptNumber = "RCP-20260907-123015",
                    timestamp = now - 3600000L, // 1 hr ago
                    dateString = todayStr,
                    totalAmount = 265.00,
                    subtotal = 265.00,
                    discountAmount = 0.0,
                    paymentMethod = "Maya",
                    amountTendered = 265.00,
                    changeAmount = 0.0,
                    totalItemCount = 3,
                    itemsSummary = "1x Bear Brand Adult Plus, 2x Great Taste White Coffee",
                    cashierName = "Aling Nena"
                ),
                TransactionLogEntity(
                    receiptNumber = "RCP-20260907-130540",
                    timestamp = now - 1800000L, // 30 mins ago
                    dateString = todayStr,
                    totalAmount = 190.00,
                    subtotal = 190.00,
                    discountAmount = 0.0,
                    paymentMethod = "Cash",
                    amountTendered = 200.00,
                    changeAmount = 10.00,
                    totalItemCount = 3,
                    itemsSummary = "1x Surf Powder Blossom Fresh, 2x Downy Sunrise Fresh Sachet",
                    cashierName = "Aling Nena"
                )
            )

            transactionLogDao.insertAll(sampleLogs)
        }
    }
}
