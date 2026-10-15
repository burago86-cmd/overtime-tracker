package com.example.overtime.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.overtime.AppState
import kotlinx.coroutines.delay

@Composable
fun PinScreen(appState: AppState) {
    var input by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var showForgotDialog by remember { mutableStateOf(false) }
    var lockedRemaining by remember { mutableStateOf(0) }

    // Тикаем каждую секунду, если заблокировано
    LaunchedEffect(appState.pinLockedUntil) {
        while (true) {
            val now = System.currentTimeMillis()
            val remain = ((appState.pinLockedUntil - now) / 1000).toInt()
            lockedRemaining = if (remain > 0) remain else 0
            if (lockedRemaining == 0) break
            delay(1000)
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "🔐",
                    fontSize = 56.sp
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = appState.t("enter_pin"),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))

                if (lockedRemaining > 0) {
                    Text(
                        text = "${appState.t("locked_for")} $lockedRemaining ${appState.t("seconds_short")}",
                        color = MaterialTheme.colorScheme.error
                    )
                } else if (errorMessage.isNotEmpty()) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(Modifier.height(24.dp))

                // Точки
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    repeat(input.length) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.fillMaxSize()
                            ) {}
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))

                // Цифровая клавиатура 1-9, 0, ⌫
                val keys = listOf(
                    listOf("1","2","3"),
                    listOf("4","5","6"),
                    listOf("7","8","9"),
                    listOf("","0","⌫")
                )
                keys.forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(vertical = 6.dp)
                    ) {
                        row.forEach { key ->
                            if (key.isEmpty()) {
                                Spacer(Modifier.size(72.dp))
                            } else {
                                Button(
                                    onClick = {
                                        if (lockedRemaining > 0) return@Button
                                        errorMessage = ""
                                        when (key) {
                                            "⌫" -> if (input.isNotEmpty()) input = input.dropLast(1)
                                            else -> {
                                                if (input.length < 8) {
                                                    input += key
                                                    // Автопроверка при достижении длины сохранённого PIN
                                                    if (input.length == appState.settings.pin.length) {
                                                        val ok = appState.tryPin(input)
                                                        if (!ok) {
                                                            errorMessage = appState.t("wrong_pin")
                                                            input = ""
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier.size(72.dp)
                                ) {
                                    Text(key, fontSize = 22.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                TextButton(onClick = { showForgotDialog = true }) {
                    Text(appState.t("forgot_pin"))
                }
            }
        }
    }

    if (showForgotDialog) {
        AlertDialog(
            onDismissRequest = { showForgotDialog = false },
            title = { Text(appState.t("forgot_pin")) },
            text = { Text(appState.t("forgot_pin_warning")) },
            confirmButton = {
                TextButton(onClick = {
                    appState.wipeAllData()
                    showForgotDialog = false
                }) {
                    Text(
                        text = appState.t("reset_data"),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgotDialog = false }) {
                    Text(appState.t("cancel"))
                }
            }
        )
    }
}
