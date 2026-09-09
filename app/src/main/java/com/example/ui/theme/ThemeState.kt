package com.example.ui.theme

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.example.data.local.prefs.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Global State holder for App Theme (Light / Dark mode).
 */
class ThemeState(
    initialIsDark: Boolean = false,
    private val context: Context? = null,
    private val coroutineScope: CoroutineScope? = null
) {
    var isDarkTheme: Boolean by mutableStateOf(initialIsDark)
        private set

    companion object {
        val KEY_IS_DARK_THEME = booleanPreferencesKey("is_dark_theme_mode")
    }

    fun toggleTheme() {
        setThemeMode(!isDarkTheme)
    }

    fun setThemeMode(isDark: Boolean) {
        isDarkTheme = isDark
        // Persist to DataStore if available
        context?.let { ctx ->
            coroutineScope?.launch {
                ctx.dataStore.edit { prefs ->
                    prefs[KEY_IS_DARK_THEME] = isDark
                }
            }
        }
    }

    suspend fun loadSavedPreference() {
        context?.let { ctx ->
            try {
                val savedValue = ctx.dataStore.data.map { prefs ->
                    prefs[KEY_IS_DARK_THEME] ?: false
                }.first()
                isDarkTheme = savedValue
            } catch (e: Exception) {
                // Default to light
            }
        }
    }
}

val LocalThemeState = compositionLocalOf {
    ThemeState()
}

@Composable
fun ProvideThemeState(
    themeState: ThemeState,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalThemeState provides themeState,
        content = content
    )
}
