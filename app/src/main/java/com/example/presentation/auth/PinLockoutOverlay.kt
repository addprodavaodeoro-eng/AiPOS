package com.example.presentation.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.domain.auth.StaffSessionManager
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PinLockoutOverlay(
    staffSessionManager: StaffSessionManager,
    modifier: Modifier = Modifier
) {
    val isLocked by staffSessionManager.isLocked.collectAsState()
    val activeStaff by staffSessionManager.activeStaff.collectAsState()
    val allStaff by staffSessionManager.allStaffFlow().collectAsState(initial = emptyList())

    if (isLocked) {
        Dialog(
            onDismissRequest = { /* Cannot dismiss without entering valid PIN */ },
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false
            )
        ) {
            PinKeypadScreen(
                title = "POS Security Lockout",
                subtitle = "Register locked due to user inactivity.\nEnter your 4-digit PIN to resume.",
                activeStaffName = activeStaff?.username?.replaceFirstChar { it.uppercase() } ?: "Staff",
                staffList = allStaff.map { "${it.username.replaceFirstChar { c -> c.uppercase() }} (${it.pin ?: "••••"})" },
                onPinEntered = { pin ->
                    staffSessionManager.unlockWithPin(pin)
                }
            )
        }
    }
}

@Composable
fun SwitchStaffDialog(
    staffSessionManager: StaffSessionManager,
    onDismiss: () -> Unit
) {
    val allStaff by staffSessionManager.allStaffFlow().collectAsState(initial = emptyList())
    val activeStaff by staffSessionManager.activeStaff.collectAsState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        PinKeypadScreen(
            title = "Switch Active Cashier",
            subtitle = "Enter your 4-digit PIN to take over register shift.",
            activeStaffName = activeStaff?.username?.replaceFirstChar { it.uppercase() } ?: "None",
            staffList = allStaff.map { "${it.username.replaceFirstChar { c -> c.uppercase() }} (${it.pin ?: "••••"})" },
            onCancel = onDismiss,
            onPinEntered = { pin ->
                val res = staffSessionManager.switchStaff(pin)
                if (res.isSuccess) {
                    onDismiss()
                }
                res
            }
        )
    }
}

@Composable
fun PinKeypadScreen(
    title: String,
    subtitle: String,
    activeStaffName: String,
    staffList: List<String> = emptyList(),
    onCancel: (() -> Unit)? = null,
    onPinEntered: suspend (String) -> Result<*>,
    modifier: Modifier = Modifier
) {
    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var isChecking by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    fun handleDigit(digit: String) {
        if (enteredPin.length < 4 && !isChecking) {
            val newPin = enteredPin + digit
            enteredPin = newPin
            errorMessage = null

            if (newPin.length == 4) {
                isChecking = true
                coroutineScope.launch {
                    val result = onPinEntered(newPin)
                    if (result.isSuccess) {
                        successMessage = "Authorized! Welcome."
                        delay(250)
                    } else {
                        errorMessage = result.exceptionOrNull()?.message ?: "Invalid 4-digit PIN"
                        enteredPin = ""
                        isChecking = false
                    }
                }
            }
        }
    }

    fun handleBackspace() {
        if (enteredPin.isNotEmpty() && !isChecking) {
            enteredPin = enteredPin.dropLast(1)
            errorMessage = null
        }
    }

    fun handleClear() {
        if (!isChecking) {
            enteredPin = ""
            errorMessage = null
        }
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("pin_lockout_surface"),
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.98f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Lock Icon Badge
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Lock",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = title,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 4 PIN Dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until 4) {
                    val isFilled = i < enteredPin.length
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    successMessage != null -> Emerald600
                                    errorMessage != null -> Red600
                                    isFilled -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.outlineVariant
                                }
                            )
                            .border(
                                width = 1.dp,
                                color = if (isFilled) MaterialTheme.colorScheme.primary else Slate300,
                                shape = CircleShape
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Status feedback
            Box(modifier = Modifier.height(24.dp), contentAlignment = Alignment.Center) {
                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                } else if (successMessage != null) {
                    Text(
                        text = successMessage ?: "",
                        color = Emerald600,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else if (activeStaffName.isNotBlank()) {
                    Text(
                        text = "Current Shift: $activeStaffName",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Numeric Keypad 3x4
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val rows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("CLEAR", "0", "BACK")
                )

                for (row in rows) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        for (key in row) {
                            KeypadButton(
                                text = key,
                                onClick = {
                                    when (key) {
                                        "CLEAR" -> handleClear()
                                        "BACK" -> handleBackspace()
                                        else -> handleDigit(key)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Optional staff PIN tips for testing convenience
            if (staffList.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Staff PINs: " + staffList.joinToString(" • "),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (onCancel != null) {
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(onClick = onCancel) {
                    Text("Cancel", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun KeypadButton(
    text: String,
    onClick: () -> Unit
) {
    val isAction = text == "CLEAR" || text == "BACK"
    val containerColor = if (isAction) {
        MaterialTheme.colorScheme.surfaceVariant
    } else {
        MaterialTheme.colorScheme.surface
    }

    Box(
        modifier = Modifier
            .size(width = 76.dp, height = 56.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(containerColor)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
            .testTag("keypad_btn_$text"),
        contentAlignment = Alignment.Center
    ) {
        if (text == "BACK") {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Backspace,
                contentDescription = "Backspace",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        } else {
            Text(
                text = text,
                fontSize = if (isAction) 11.sp else 22.sp,
                fontWeight = if (isAction) FontWeight.Bold else FontWeight.SemiBold,
                color = if (isAction) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
