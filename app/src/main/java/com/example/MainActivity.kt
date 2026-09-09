package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.example.presentation.navigation.AiPOSNavGraph
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ProvideThemeState
import com.example.ui.theme.ThemeState

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val coroutineScope = rememberCoroutineScope()
      val themeState = remember {
        ThemeState(
          initialIsDark = false,
          context = applicationContext,
          coroutineScope = coroutineScope
        )
      }

      LaunchedEffect(Unit) {
        themeState.loadSavedPreference()
      }

      ProvideThemeState(themeState = themeState) {
        MyApplicationTheme {
          AiPOSNavGraph()
        }
      }
    }
  }
}
