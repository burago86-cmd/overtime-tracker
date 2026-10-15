package com.example.overtime.ui

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.overtime.AppState
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Диалог-калькулятор времени.
 * Считает разницу между временами, вычитает обед (если выходной),
 * умножает на ставку и может передать данные в форму добавления.
 *
 * @param initialStart начальное время начала (для переноса из формы)
 * @param initialEnd начальное время конца
 * @param initialIsDayOff галочка «выходной»
 * @param initialLunch обед в минутах
 * @param onCreateEntry вызывается по кнопке «Создать запись»
 */
@Composable
fun TimeCalculatorDialog(
    appState: AppState,
    initialStart: LocalTime = LocalTime.of(18, 0),
    initialEnd: LocalTime = LocalTime.of(21, 0),
    initialIsDayOff: Boolean = false,
    initialLunch: Int = 0,
    onDismiss: () -> Unit,
    onCreateEntry: (start: LocalTime, end: LocalTime, isDayOff: Boolean, lunch: Int) -> Unit
) {
    val context = LocalContext.current

    var startTime by remember { mutableStateOf(initialStart) }
    var endTime by remember { mutableStateOf(initialEnd) }
    var isDayOff by remember { mutableStateOf(initialIsDayOff) }
    var lunchText by remember { mutableStateOf(initialLunch.takeIf { it > 0 }?.toString() ?: "") }
    var rateText by remember { mutableStateOf(appState.settings.hourRate.toString()) }

    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val currency = appState.currencyLabel()

    // Расчёты
    val rawMinutes = run {
        val s = startTime.hour * 60 + startTime.minute
        val e = endTime.hour * 60 + endTime.minute
        if (e >= s) e - s else (24 * 60 - s) + e
    }
    val lunchMinutes = if (isDayOff) lunchText.toIntOrNull() ?: 0 else 0
    val totalMinutes = (rawMinutes - lunchMinutes).coerceAtLeast(0)
    val totalHours = totalMinutes / 60.0
    val rate = rateText.toIntOrNull() ?: appState.settings.hourRate
    val totalMoney = (totalHours * rate).toInt()

    val durationText = run {
        val h = totalMinutes / 60
        val m = totalMinutes % 60
        when {
            h == 0 -> "$m ${appState.t("minutes_short")}"
            m == 0 -> "$h ${appState.t("hours_short")}"
            else -> "$h ${appState.t("hours_short")} $m ${appState.t("minutes_short")}"
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "🧮 ${appState.t("calculator")}",
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

                // Время начала
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = appState.t("start_time"),
                        modifier = Modifier.width(80.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(
                        onClick = {
                            TimePickerDialog(
                                context,
                                { _, h, m -> startTime = LocalTime.of(h, m) },
                                startTime.hour, startTime.minute, true
                            ).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(startTime.format(timeFormatter))
                    }
                }

                // Время конца
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = appState.t("end_time"),
                        modifier = Modifier.width(80.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(
                        onClick = {
                            TimePickerDialog(
                                context,
                                { _, h, m -> endTime = LocalTime.of(h, m) },
                                endTime.hour, endTime.minute, true
                            ).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(endTime.format(timeFormatter))
                    }
                }

                // Галочка «Выходной»
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isDayOff,
                        onCheckedChange = { isDayOff = it }
                    )
                    Text(
                        text = appState.t("day_off"),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                // Обед (только для выходных)
                if (isDayOff) {
                    OutlinedTextField(
                        value = lunchText,
                        onValueChange = { new ->
                            if (new.all { it.isDigit() }) lunchText = new
                        },
                        label = { Text(appState.t("lunch_minutes")) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                // Ставка
                OutlinedTextField(
                    value = rateText,
                    onValueChange = { new ->
                        if (new.all { it.isDigit() }) rateText = new
                    },
                    label = { Text("${appState.t("hour_rate")} ($currency)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(Modifier.height(4.dp))

                // Итоги
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(appState.t("duration"))
                            Text(durationText, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(appState.t("total_money"))
                            Text(
                                "$totalMoney $currency",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onCreateEntry(startTime, endTime, isDayOff, lunchMinutes)
            }) {
                Text(appState.t("create_entry"))
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDismiss) {
                    Text(appState.t("close"))
                }
            }
        }
    )
}
