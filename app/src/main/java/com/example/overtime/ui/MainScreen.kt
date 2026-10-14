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
import androidx.compose.ui.Modifier
import com.example.overtime.AppState
import com.example.overtime.data.Entry

@Composable
fun MainScreen(appState: AppState) {
    var tab by remember { mutableStateOf(0) }

    // Состояние редактора:
    // showEditor == true и editingEntry == null  → создание новой записи
    // showEditor == true и editingEntry != null  → редактирование
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
            when (tab) {
                0 -> EntriesScreen(
                    appState = appState,
                    onAddClick = {
                        editingEntry = null
                        showEditor = true
                    },
                    onEditClick = { entry ->
                        editingEntry = entry
                        showEditor = true
                    }
                )
                1 -> SettingsScreen(appState = appState)
            }
        }
    }
}
