package com.example.presentation.sales

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.TransactionLogEntity
import com.example.domain.model.DailySalesSummary
import com.example.domain.repository.ITransactionRepository
import com.example.domain.usecase.ResetDataUseCase
import com.example.presentation.common.ResetScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class DailySalesUiState(
    val selectedDateString: String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()),
    val summary: DailySalesSummary = DailySalesSummary(),
    val selectedPaymentFilter: String = "ALL", // ALL, Cash, GCash, Maya, Utang
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val selectedTransactionForDetail: TransactionLogEntity? = null
) {
    val filteredTransactions: List<TransactionLogEntity>
        get() = summary.transactions.filter { tx ->
            val matchesPayment = selectedPaymentFilter == "ALL" ||
                    tx.paymentMethod.equals(selectedPaymentFilter, ignoreCase = true)
            val matchesQuery = searchQuery.isBlank() ||
                    tx.receiptNumber.contains(searchQuery, ignoreCase = true) ||
                    tx.itemsSummary.contains(searchQuery, ignoreCase = true) ||
                    (tx.customerName != null && tx.customerName.contains(searchQuery, ignoreCase = true))
            matchesPayment && matchesQuery
        }

    val isToday: Boolean
        get() {
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            return selectedDateString == todayStr
        }
}

class DailySalesViewModel(
    private val transactionRepository: ITransactionRepository,
    private val resetDataUseCase: ResetDataUseCase? = null
) : ViewModel() {

    private val _selectedDateString = MutableStateFlow(
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    )
    private val _selectedPaymentFilter = MutableStateFlow("ALL")
    private val _searchQuery = MutableStateFlow("")
    private val _selectedTransactionForDetail = MutableStateFlow<TransactionLogEntity?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<DailySalesUiState> = _selectedDateString
        .flatMapLatest { dateStr ->
            transactionRepository.getDailySalesSummary(dateStr).map { summary ->
                dateStr to summary
            }
        }
        .combine(_selectedPaymentFilter) { (dateStr, summary), paymentFilter ->
            Triple(dateStr, summary, paymentFilter)
        }
        .combine(_searchQuery) { (dateStr, summary, paymentFilter), query ->
            DailySalesUiState(
                selectedDateString = dateStr,
                summary = summary,
                selectedPaymentFilter = paymentFilter,
                searchQuery = query,
                selectedTransactionForDetail = _selectedTransactionForDetail.value
            )
        }
        .combine(_selectedTransactionForDetail) { state, selectedTx ->
            state.copy(selectedTransactionForDetail = selectedTx)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DailySalesUiState()
        )

    init {
        viewModelScope.launch {
            transactionRepository.seedSampleTransactionsIfEmpty()
        }
    }

    fun selectDate(dateString: String) {
        _selectedDateString.value = dateString
    }

    fun goToPreviousDay() {
        val current = _selectedDateString.value
        val cal = Calendar.getInstance()
        try {
            val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(current)
            if (parsed != null) {
                cal.time = parsed
                cal.add(Calendar.DAY_OF_YEAR, -1)
                _selectedDateString.value = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
            }
        } catch (e: Exception) {
            // keep current
        }
    }

    fun goToNextDay() {
        val current = _selectedDateString.value
        val cal = Calendar.getInstance()
        try {
            val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(current)
            if (parsed != null) {
                cal.time = parsed
                cal.add(Calendar.DAY_OF_YEAR, 1)
                _selectedDateString.value = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
            }
        } catch (e: Exception) {
            // keep current
        }
    }

    fun goToToday() {
        _selectedDateString.value = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    fun setPaymentFilter(filter: String) {
        _selectedPaymentFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun showTransactionDetail(transaction: TransactionLogEntity) {
        _selectedTransactionForDetail.value = transaction
    }

    fun dismissTransactionDetail() {
        _selectedTransactionForDetail.value = null
    }

    fun deleteTransaction(id: String) {
        viewModelScope.launch {
            transactionRepository.deleteTransaction(id)
            if (_selectedTransactionForDetail.value?.id == id) {
                _selectedTransactionForDetail.value = null
            }
        }
    }

    fun executeReset(
        scope: ResetScope,
        reseedSampleCatalog: Boolean,
        passwordRaw: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            if (resetDataUseCase == null) {
                onResult(false, "Reset service is not available.")
                return@launch
            }

            val isValid = resetDataUseCase.verifyAdminPassword(passwordRaw)
            if (!isValid) {
                onResult(false, "Incorrect admin password. Action aborted.")
                return@launch
            }

            try {
                when (scope) {
                    ResetScope.ALL_PRODUCTS_AND_SALES -> {
                        resetDataUseCase.resetAll(reseedSampleCatalog)
                    }
                    ResetScope.PRODUCTS_ONLY -> {
                        resetDataUseCase.resetProducts(reseedSampleCatalog)
                    }
                    ResetScope.SALES_AND_TRANSACTIONS_ONLY -> {
                        resetDataUseCase.resetSalesAndTransactions()
                    }
                }
                _selectedTransactionForDetail.value = null
                onResult(true, null)
            } catch (e: Exception) {
                onResult(false, e.localizedMessage ?: "Failed to reset records.")
            }
        }
    }
}

class DailySalesViewModelFactory(
    private val transactionRepository: ITransactionRepository,
    private val resetDataUseCase: ResetDataUseCase? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DailySalesViewModel::class.java)) {
            return DailySalesViewModel(transactionRepository, resetDataUseCase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
