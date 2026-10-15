package com.example.overtime

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.overtime.ui.MainScreen
import com.example.overtime.ui.PinScreen

class MainActivity : ComponentActivity() {

    private lateinit var appState: AppState

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appState = AppState(applicationContext)

        setContent {
            OvertimeApp(appState)
        }
    }

    override fun onResume() {
        super.onResume()
        if (::appState.isInitialized) {
            appState.onAppResumed()
        }
    }

    override fun onPause() {
        super.onPause()
        if (::appState.isInitialized) {
            appState.onAppPaused()
        }
    }
}

@Composable
fun OvertimeApp(appState: AppState) {
    val colorScheme = if (appState.settings.darkTheme) darkColorScheme() else lightColorScheme()

    MaterialTheme(colorScheme = colorScheme) {
        Surface(modifier = Modifier.fillMaxSize()) {
            if (appState.settings.pinEnabled && !appState.isUnlocked) {
                PinScreen(appState)
            } else {
                MainScreen(appState)
            }
        }
    }
}
