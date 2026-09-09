package com.example.presentation.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.domain.backup.BackupRestoreState
import com.example.domain.backup.DatabaseBackupManager
import com.example.domain.usecase.ResetDataUseCase
import com.example.presentation.common.ResetScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class BackupViewModel(
    private val backupManager: DatabaseBackupManager,
    private val resetDataUseCase: ResetDataUseCase? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow<BackupRestoreState>(BackupRestoreState.Idle)
    val uiState: StateFlow<BackupRestoreState> = _uiState.asStateFlow()

    private val _localBackups = MutableStateFlow<List<File>>(emptyList())
    val localBackups: StateFlow<List<File>> = _localBackups.asStateFlow()

    init {
        refreshLocalBackups()
    }

    fun refreshLocalBackups() {
        _localBackups.value = backupManager.listLocalBackups()
    }

    fun exportToLocalStorage(passwordRaw: String) {
        viewModelScope.launch {
            _uiState.value = BackupRestoreState.Processing("Exporting snapshot to local storage...")
            val result = backupManager.exportSnapshotToLocalStorage(passwordRaw)
            result.fold(
                onSuccess = { (file, stats) ->
                    _uiState.value = BackupRestoreState.ExportSuccess(file.absolutePath, stats)
                    refreshLocalBackups()
                },
                onFailure = { error ->
                    _uiState.value = BackupRestoreState.Error("Export failed: ${error.localizedMessage ?: "Unknown error"}")
                }
            )
        }
    }

    fun exportToUri(uri: android.net.Uri, passwordRaw: String) {
        viewModelScope.launch {
            _uiState.value = BackupRestoreState.Processing("Writing snapshot to selected storage file...")
            val result = backupManager.exportSnapshotToUri(uri, passwordRaw)
            result.fold(
                onSuccess = { stats ->
                    _uiState.value = BackupRestoreState.ExportSuccess(uri.toString(), stats)
                },
                onFailure = { error ->
                    _uiState.value = BackupRestoreState.Error("Export to selected storage failed: ${error.localizedMessage ?: "Unknown error"}")
                }
            )
        }
    }

    fun restoreFromUri(uri: android.net.Uri, passwordRaw: String) {
        viewModelScope.launch {
            _uiState.value = BackupRestoreState.Processing("Reading snapshot and restoring database tables...")
            val result = backupManager.restoreFromUri(uri, passwordRaw, replaceExisting = true)
            result.fold(
                onSuccess = { stats ->
                    _uiState.value = BackupRestoreState.ImportSuccess(stats)
                    refreshLocalBackups()
                },
                onFailure = { error ->
                    _uiState.value = BackupRestoreState.Error("Database recovery failed: ${error.localizedMessage ?: "Corrupt or invalid backup file"}")
                }
            )
        }
    }

    fun restoreFromFile(file: File, passwordRaw: String) {
        viewModelScope.launch {
            _uiState.value = BackupRestoreState.Processing("Restoring from local snapshot file...")
            try {
                val encryptedString = file.readText(Charsets.UTF_8)
                val jsonString = com.example.domain.security.SecurityUtils.decryptData(encryptedString, passwordRaw)
                val moshi = com.squareup.moshi.Moshi.Builder()
                    .addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
                    .build()
                val snapshot = moshi.adapter(com.example.domain.backup.DatabaseSnapshot::class.java).fromJson(jsonString)
                if (snapshot == null) {
                    _uiState.value = BackupRestoreState.Error("Invalid or corrupted backup JSON file")
                    return@launch
                }
                val result = backupManager.restoreFromSnapshot(snapshot, replaceExisting = true)
                result.fold(
                    onSuccess = { stats ->
                        _uiState.value = BackupRestoreState.ImportSuccess(stats)
                    },
                    onFailure = { error ->
                        _uiState.value = BackupRestoreState.Error("Recovery failed: ${error.localizedMessage}")
                    }
                )
            } catch (e: Exception) {
                _uiState.value = BackupRestoreState.Error("Recovery failed: ${e.localizedMessage}")
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
                onResult(false, "Reset service is not initialized.")
                return@launch
            }

            val isValid = resetDataUseCase.verifyAdminPassword(passwordRaw)
            if (!isValid) {
                onResult(false, "Incorrect admin password. Action aborted.")
                return@launch
            }

            _uiState.value = BackupRestoreState.Processing("Purging data as requested...")
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
                _uiState.value = BackupRestoreState.Idle
                onResult(true, null)
            } catch (e: Exception) {
                _uiState.value = BackupRestoreState.Error("Reset failed: ${e.localizedMessage}")
                onResult(false, e.localizedMessage ?: "Unknown error occurred during reset")
            }
        }
    }

    fun resetState() {
        _uiState.value = BackupRestoreState.Idle
    }
}

class BackupViewModelFactory(
    private val backupManager: DatabaseBackupManager,
    private val resetDataUseCase: ResetDataUseCase? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BackupViewModel::class.java)) {
            return BackupViewModel(backupManager, resetDataUseCase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
