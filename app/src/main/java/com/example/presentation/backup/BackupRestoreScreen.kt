package com.example.presentation.backup

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.backup.BackupRestoreState
import com.example.domain.backup.BackupStats
import com.example.presentation.common.AdminResetConfirmationDialog
import com.example.presentation.common.ResetScope
import com.example.presentation.common.ThemeToggleIconButton
import com.example.ui.theme.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreScreen(
    viewModel: BackupViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val localBackups by viewModel.localBackups.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showConfirmImportDialog by remember { mutableStateOf<File?>(null) }
    var showConfirmUriImportDialog by remember { mutableStateOf<android.net.Uri?>(null) }
    var showResetDialog by remember { mutableStateOf(false) }

    // Dialog state for passwords
    var showExportPasswordDialog by remember { mutableStateOf(false) }
    var pendingExportUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var showImportPasswordDialog by remember { mutableStateOf(false) }
    var pendingImportUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var pendingImportFile by remember { mutableStateOf<File?>(null) }

    // SAF File Creator for Export
    val exportFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            pendingExportUri = it
            showExportPasswordDialog = true
        }
    }

    // SAF File Picker for Import
    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { showConfirmUriImportDialog = it }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Database Backup & Recovery",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 17.sp
                        )
                        Text(
                            "Local Snapshots & Data Migration",
                            fontSize = 11.sp,
                            color = Slate400
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("backup_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                actions = {
                    ThemeToggleIconButton(modifier = Modifier.padding(end = 4.dp))
                    IconButton(
                        onClick = { viewModel.refreshLocalBackups() },
                        modifier = Modifier.testTag("backup_refresh_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Info Banner
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = TealCard),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(TealCardBorder)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(TealPrimary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text("Offline-Safe Database Protection", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Slate900)
                                Text(
                                    "Save complete JSON snapshots of inventory, customers, utang ledger, and expenses to your device storage for offline recovery.",
                                    fontSize = 11.5.sp,
                                    color = Slate600,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }

                // Status Banner / Alert
                item {
                    when (val state = uiState) {
                        is BackupRestoreState.Processing -> {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFBFDBFE))),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color(0xFF2563EB))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(state.message, fontSize = 12.5.sp, color = Color(0xFF1E40AF), fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                        is BackupRestoreState.ExportSuccess -> {
                            SuccessBanner(
                                title = "Snapshot Exported Successfully!",
                                message = "Saved to local storage with ${state.stats.productCount} products, ${state.stats.customerCount} customers, ${state.stats.ledgerCount} ledger entries, and ${state.stats.transactionCount} transactions.",
                                stats = state.stats,
                                onDismiss = { viewModel.resetState() }
                            )
                        }
                        is BackupRestoreState.ImportSuccess -> {
                            SuccessBanner(
                                title = "Database Recovered & Restored!",
                                message = "Restored ${state.stats.productCount} products, ${state.stats.customerCount} customers, ${state.stats.ledgerCount} ledger records, and ${state.stats.transactionCount} transactions into database.",
                                stats = state.stats,
                                onDismiss = { viewModel.resetState() }
                            )
                        }
                        is BackupRestoreState.Error -> {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFFECACA))),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Error, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(22.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(state.message, fontSize = 12.5.sp, color = Color(0xFF991B1B), modifier = Modifier.weight(1f))
                                    IconButton(onClick = { viewModel.resetState() }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF991B1B), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                        BackupRestoreState.Idle -> {}
                    }
                }

                // Export Actions Card
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Slate100)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color(0xFFDCFCE7), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.FileUpload, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Export Database Snapshot", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Slate900)
                                    Text("Create full backup files of all tables", fontSize = 11.5.sp, color = Slate500)
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { 
                                        pendingExportUri = null
                                        showExportPasswordDialog = true
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("export_internal_backup_button")
                                ) {
                                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Quick Backup", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
                                        val defaultFileName = "aipos_backup_${dateFormat.format(Date())}.json"
                                        exportFileLauncher.launch(defaultFileName)
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("export_saf_backup_button")
                                ) {
                                    Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Export As...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Import Actions Card
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Slate100)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color(0xFFFEF3C7), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.FileDownload, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Import & Data Recovery", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Slate900)
                                    Text("Restore database from JSON snapshot file", fontSize = 11.5.sp, color = Slate500)
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            FilledTonalButton(
                                onClick = {
                                    importFileLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("import_file_picker_button")
                            ) {
                                Icon(Icons.Default.DriveFolderUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Select Backup File from Storage", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Local Backups History Section
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "LOCAL BACKUP SNAPSHOTS (${localBackups.size})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate400,
                            letterSpacing = 1.sp
                        )
                    }
                }

                if (localBackups.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Slate100)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.BackupTable, contentDescription = null, tint = Slate300, modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("No local backups found", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Slate600)
                                Text("Tap 'Quick Backup' to create your first database snapshot.", fontSize = 11.5.sp, color = Slate400)
                            }
                        }
                    }
                } else {
                    items(localBackups) { file ->
                        LocalBackupItem(
                            file = file,
                            onRestore = { showConfirmImportDialog = file },
                            onShare = {
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, file.readText(Charsets.UTF_8))
                                    type = "application/json"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share Backup Snapshot"))
                            }
                        )
                    }
                }

                // Danger Zone Section
                item {
                    Text(
                        "DANGER ZONE & FACTORY PURGE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFDC2626),
                        letterSpacing = 1.sp
                    )
                }

                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFFCA5A5))
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(Color(0xFFFEE2E2), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.DeleteForever,
                                        contentDescription = null,
                                        tint = Color(0xFFDC2626),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        "Delete & Reset Products / Sales",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        "Requires Admin password authorization to purge local database records.",
                                        fontSize = 11.5.sp,
                                        color = Slate400,
                                        lineHeight = 15.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Button(
                                onClick = { showResetDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("btn_open_admin_reset_dialog")
                            ) {
                                Icon(
                                    Icons.Default.LockReset,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Authorize Reset with Admin Password",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }

    if (showResetDialog) {
        AdminResetConfirmationDialog(
            initialScope = ResetScope.ALL_PRODUCTS_AND_SALES,
            onDismiss = { showResetDialog = false },
            onPerformReset = { scope, reseedSampleCatalog, password, onResult ->
                viewModel.executeReset(scope, reseedSampleCatalog, password) { success, error ->
                    if (success) {
                        showResetDialog = false
                    }
                    onResult(success, error)
                }
            }
        )
    }

    if (showExportPasswordDialog) {
        BackupPasswordDialog(
            title = "Encrypt Backup File",
            message = "Enter a password to encrypt and protect this backup file. You will need this password to restore from it later.",
            onConfirm = { password ->
                showExportPasswordDialog = false
                pendingExportUri?.let {
                    viewModel.exportToUri(it, password)
                    pendingExportUri = null
                } ?: run {
                    viewModel.exportToLocalStorage(password)
                }
            },
            onDismiss = {
                showExportPasswordDialog = false
                pendingExportUri = null
            }
        )
    }

    if (showImportPasswordDialog) {
        BackupPasswordDialog(
            title = "Decrypt Backup File",
            message = "Enter the password used to encrypt this backup file.",
            onConfirm = { password ->
                showImportPasswordDialog = false
                pendingImportUri?.let {
                    viewModel.restoreFromUri(it, password)
                    pendingImportUri = null
                }
                pendingImportFile?.let {
                    viewModel.restoreFromFile(it, password)
                    pendingImportFile = null
                }
            },
            onDismiss = {
                showImportPasswordDialog = false
                pendingImportUri = null
                pendingImportFile = null
            }
        )
    }

    // Confirm Restore Dialog for Local File
    showConfirmImportDialog?.let { file ->
        AlertDialog(
            onDismissRequest = { showConfirmImportDialog = null },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFDC2626)) },
            title = { Text("Confirm Database Recovery", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Text(
                    "Restoring snapshot '${file.name}' will sync and replace current database tables with the backup contents. Are you sure you want to proceed?",
                    fontSize = 13.sp,
                    color = Slate700
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = showConfirmImportDialog
                        showConfirmImportDialog = null
                        target?.let {
                            pendingImportFile = it
                            pendingImportUri = null
                            showImportPasswordDialog = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    modifier = Modifier.testTag("confirm_restore_dialog_button")
                ) {
                    Text("Restore Database")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmImportDialog = null }) {
                    Text("Cancel", color = Slate600)
                }
            }
        )
    }

    // Confirm Restore Dialog for SAF Chosen Uri
    showConfirmUriImportDialog?.let { uri ->
        AlertDialog(
            onDismissRequest = { showConfirmUriImportDialog = null },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFDC2626)) },
            title = { Text("Restore from Storage File", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Text(
                    "This will restore database records from the selected backup file. Existing records will be updated/replaced with snapshot data.",
                    fontSize = 13.sp,
                    color = Slate700
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val targetUri = showConfirmUriImportDialog
                        showConfirmUriImportDialog = null
                        targetUri?.let {
                            pendingImportUri = it
                            pendingImportFile = null
                            showImportPasswordDialog = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    modifier = Modifier.testTag("confirm_uri_restore_dialog_button")
                ) {
                    Text("Confirm Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmUriImportDialog = null }) {
                    Text("Cancel", color = Slate600)
                }
            }
        )
    }
}

@Composable
fun SuccessBanner(
    title: String,
    message: String,
    stats: BackupStats,
    onDismiss: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFBBF7D0))),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(title, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF15803D), modifier = Modifier.size(16.dp))
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(message, fontSize = 12.sp, color = Color(0xFF166534), lineHeight = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StatBadge("Products", stats.productCount.toString())
                StatBadge("Customers", stats.customerCount.toString())
                StatBadge("Ledger", stats.ledgerCount.toString())
                StatBadge("Transactions", stats.transactionCount.toString())
            }
        }
    }
}

@Composable
fun StatBadge(label: String, count: String) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = Color(0xFFDCFCE7)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "$label: ", fontSize = 10.sp, color = Color(0xFF166534))
            Text(text = count, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
        }
    }
}

@Composable
fun LocalBackupItem(
    file: File,
    onRestore: () -> Unit,
    onShare: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.US)
    val formattedDate = dateFormat.format(Date(file.lastModified()))
    val fileSizeKb = String.format(Locale.US, "%.1f KB", file.length() / 1024f)

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Slate100)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(Color(0xFFE2E8F0), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.InsertDriveFile, contentDescription = null, tint = Slate600, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(file.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Slate900, maxLines = 1)
                    Text("$formattedDate • $fileSizeKb", fontSize = 11.sp, color = Slate500)
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onShare,
                    modifier = Modifier.size(32.dp).testTag("share_backup_${file.name}")
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share", tint = Slate600, modifier = Modifier.size(16.dp))
                }
                FilledTonalButton(
                    onClick = onRestore,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("restore_backup_${file.name}")
                ) {
                    Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Restore", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun BackupPasswordDialog(
    title: String,
    message: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.VpnKey, contentDescription = null, tint = TealPrimary) },
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(message, fontSize = 13.sp, color = Slate700)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Password,
                        imeAction = androidx.compose.ui.text.input.ImeAction.Done
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TealPrimary,
                        focusedLabelColor = TealPrimary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (password.isNotEmpty()) {
                        onConfirm(password)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                enabled = password.isNotEmpty()
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Slate600)
            }
        }
    )
}
