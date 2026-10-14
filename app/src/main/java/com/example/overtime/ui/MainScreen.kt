package com.example.overtime.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.overtime.AppState
import com.example.overtime.data.Entry

@Composable
fun MainScreen(appState: AppState) {
    var tab by remember { mutableStateOf(0) }

    // editingEntry == null и showEditor == false → ничего не открыто
    // showEditor == true и editingEntry == null → создание новой
    // showEditor == true и editingEntry != null → редактирование
    var showEditor by remember { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<Entry?>(null) }

    if (showEditor) {
        AddEntryScreen(
            appState = appState,
            existing = editingEntry,
            onDone = {
                showEditor = false
                editingEntry = null
            }
        )
        return
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    icon = { Icon(Icons.Filled.List, contentDescription = null) },
                    label = { Text(appState.t("entries")) }
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                    label = { Text(appState.t("settings")) }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            -> SettingsScreen(appState = appState)
                1 -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Здесь будут настройки")
                }
            }
        }
    }
}
