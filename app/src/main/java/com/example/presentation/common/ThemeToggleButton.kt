package com.example.presentation.common

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

/**
 * Modern icon toggle button for switching between Light and Dark shift mode.
 */
@Composable
fun ThemeToggleIconButton(
    modifier: Modifier = Modifier
) {
    val themeState = LocalThemeState.current
    val isDark = themeState.isDarkTheme

    IconButton(
        onClick = { themeState.toggleTheme() },
        modifier = modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(
                if (isDark) Slate700 else MaterialTheme.colorScheme.surfaceVariant
            )
            .testTag("btn_toggle_theme")
    ) {
        Crossfade(targetState = isDark, animationSpec = tween(300), label = "theme_icon_crossfade") { dark ->
            if (dark) {
                Icon(
                    imageVector = Icons.Filled.LightMode,
                    contentDescription = "Switch to Light Mode",
                    tint = Amber500,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.DarkMode,
                    contentDescription = "Switch to Dark Mode",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Expanded Shift Mode Pill Switch with descriptive text label (e.g. Day Shift / Night Shift).
 */
@Composable
fun ShiftModeTogglePill(
    modifier: Modifier = Modifier
) {
    val themeState = LocalThemeState.current
    val isDark = themeState.isDarkTheme

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isDark) Slate800 else Slate100,
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(if (isDark) Slate700 else Slate200)
        ),
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable { themeState.toggleTheme() }
            .testTag("btn_shift_mode_pill")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = if (isDark) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                contentDescription = null,
                tint = if (isDark) Amber500 else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = if (isDark) "Night Shift" else "Day Shift",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color.White else Slate900
            )
        }
    }
}
