package com.example.overtime.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.overtime.data.Entry
import com.example.overtime.data.EntryType
import com.example.overtime.data.Storage
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEntryScreen(
    appState: AppState,
    existing: Entry?,
    onDone: () -> Unit
) {
    val context = LocalContext.current

    var date by remember { mutableStateOf(existing?.date ?: LocalDate.now()) }
    var type by remember { mutableStateOf(existing?.type ?: EntryType.HOURS) }
    var startTime by remember { mutableStateOf(existing?.startTime ?: LocalTime.of(18, 0)) }
    var endTime by remember { mutableStateOf(existing?.endTime ?: LocalTime.of(21, 0)) }
    var comment by remember { mutableStateOf(existing?.comment ?: "") }

    var isDayOff by remember {
        mutableStateOf(existing?.isDayOff ?: Entry.isWeekend(date))
    }
    var lunchText by remember {
        mutableStateOf(existing?.lunchMinutes?.takeIf { it > 0 }?.toString() ?: "")
    }
    var dayOffManuallyChanged by remember { mutableStateOf(existing != null) }

    LaunchedEffect(date) {
        if (!dayOffManuallyChanged) {
            isDayOff = Entry.isWeekend(date)
        }
    }

    // Индивидуальная ставка
    var customRateText by remember {
        mutableStateOf(existing?.customHourRate?.toString() ?: "")
    }

    var productId by remember { mutableStateOf(existing?.productId) }
    var quantityText by remember { mutableStateOf(existing?.quantity?.toString() ?: "") }
    var productDropdownExpanded by remember { mutableStateOf(false) }

    val products = appState.settings.products
    val currency = appState.currencyLabel()
    val defaultRate = appState.settings.hourRate

    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (existing == null) appState.t("add_entry")
                        else appState.t("edit_entry")
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = appState.t("back"))
                    }
                },
                actions = {
                    IconButton(onClick = {
                        saveEntry(
                            appState = appState,
                            existing = existing,
                            date = date,
                            type = type,
                            startTime = startTime,
                            endTime = endTime,
                            isDayOff = isDayOff,
                            lunchMinutes = lunchText.toIntOrNull() ?: 0,
                            customHourRate = customRateText.toIntOrNull(),
                            productId = productId,
                            quantity = quantityText.toIntOrNull() ?: 0,
                            comment = comment
                        )
                        onDone()
                    }) {
                        Icon(Icons.Filled.Check, contentDescription = appState.t("save"))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            OutlinedTextField(
                value = date.format(dateFormatter),
                onValueChange = { },
                readOnly = true,
                label = { Text(appState.t("date")) },
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    DatePickerDialog(
                        context,
                        { _, y, m, d -> date = LocalDate.of(y, m + 1, d) },
                        date.year, date.monthValue - 1, date.dayOfMonth
                    ).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(appState.t("select_date"))
            }

            // ---------- Галочка «Выходной» ----------
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isDayOff,
                            onCheckedChange = {
                                isDayOff = it
                                dayOffManuallyChanged = true
                            }
                        )
                        Text(
                            text = appState.t("day_off"),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = appState.t("holiday_hint"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ---------- Тип ----------
            Text(
                text = appState.t("type"),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Column {
                TypeOption(
                    label = appState.t("type_hours"),
                    selected = type == EntryType.HOURS,
                    onSelect = { type = EntryType.HOURS }
                )
                TypeOption(
                    label = appState.t("type_products"),
                    selected = type == EntryType.PRODUCTS,
                    onSelect = { type = EntryType.PRODUCTS }
                )
                TypeOption(
                    label = appState.t("type_mixed"),
                    selected = type == EntryType.MIXED,
                    onSelect = { type = EntryType.MIXED }
                )
            }

            // ---------- Время ----------
            if (type == EntryType.HOURS || type == EntryType.MIXED) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = appState.t("duration"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
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
                                Text("${appState.t("start_time")}: ${startTime.format(timeFormatter)}")
                            }
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
                                Text("${appState.t("end_time")}: ${endTime.format(timeFormatter)}")
                            }
                        }

                        if (isDayOff) {
                            Spacer(Modifier.height(12.dp))
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

                        // ---------- Индивидуальная ставка за час ----------
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "${appState.t("hour_rate")} (${appState.currencyLabel()})",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = customRateText,
                                onValueChange = { new ->
                                    if (new.all { it.isDigit() }) customRateText = new
                                },
                                placeholder = { Text(defaultRate.toString()) },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            TextButton(
                                onClick = {
                                    val value = customRateText.toIntOrNull() ?: defaultRate
                                    appState.setHourRate(value)
                                }
                            ) {
                                Text(appState.t("make_default_rate"))
                            }
                        }
                        Text(
                            text = appState.t("custom_rate_hint"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ---------- Изделие ----------
            if (type == EntryType.PRODUCTS || type == EntryType.MIXED) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = appState.t("product"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))

                        if (products.isEmpty()) {
                            Text(
                                text = "Сначала добавьте изделия в настройках",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        } else {
                            ExposedDropdownMenuBox(
                                expanded = productDropdownExpanded,
                                onExpandedChange = { productDropdownExpanded = it }
                            ) {
                                val selectedProduct = products.find { it.id == productId }
                                OutlinedTextField(
                                    value = selectedProduct?.let { "${it.name} — ${it.price} $currency" } ?: "",
                                    onValueChange = { },
                                    readOnly = true,
                                    label = { Text(appState.t("product")) },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = productDropdownExpanded) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor()
                                )
                                ExposedDropdownMenu(
                                    expanded = productDropdownExpanded,
                                    onDismissRequest = { productDropdownExpanded = false }
                                ) {
                                    products.forEach { p ->
                                        DropdownMenuItem(
                                            text = { Text("${p.name} — ${p.price} $currency") },
                                            onClick = {
                                                productId = p.id
                                                productDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(8.dp))

                            OutlinedTextField(
                                value = quantityText,
                                onValueChange = { new ->
                                    if (new.all { it.isDigit() }) quantityText = new
                                },
                                label = { Text(appState.t("quantity")) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = comment,
                onValueChange = { comment = it },
                label = { Text(appState.t("comment")) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    saveEntry(
                        appState = appState,
                        existing = existing,
                        date = date,
                        type = type,
                        startTime = startTime,
                        endTime = endTime,
                        isDayOff = isDayOff,
                        lunchMinutes = lunchText.toIntOrNull() ?: 0,
                        customHourRate = customRateText.toIntOrNull(),
                        productId = productId,
                        quantity = quantityText.toIntOrNull() ?: 0,
                        comment = comment
                    )
                    onDone()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(appState.t("save"))
            }
        }
    }
}

@Composable
private fun TypeOption(
    label: String,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onSelect)
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Text(label, modifier = Modifier.padding(start = 8.dp))
    }
}

private fun saveEntry(
    appState: AppState,
    existing: Entry?,
    date: LocalDate,
    type: EntryType,
    startTime: LocalTime,
    endTime: LocalTime,
    isDayOff: Boolean,
    lunchMinutes: Int,
    customHourRate: Int?,
    productId: String?,
    quantity: Int,
    comment: String
) {
    val product = appState.settings.products.find { it.id == productId }

    // Если customHourRate == ставка из настроек — не сохраняем индивидуальную (null)
    val effectiveCustom = customHourRate?.takeIf { it != appState.settings.hourRate }

    val newEntry = Entry(
        id = existing?.id ?: Storage.newId(),
        date = date,
        type = type,
        startTime = if (type == EntryType.HOURS || type == EntryType.MIXED) startTime else null,
        endTime = if (type == EntryType.HOURS || type == EntryType.MIXED) endTime else null,
        isDayOff = isDayOff,
        lunchMinutes = if (isDayOff) lunchMinutes else 0,
        customHourRate = if (type == EntryType.HOURS || type == EntryType.MIXED) effectiveCustom else null,
        productId = if (type == EntryType.PRODUCTS || type == EntryType.MIXED) productId else null,
        productName = if (type == EntryType.PRODUCTS || type == EntryType.MIXED) product?.name ?: "" else "",
        productPrice = if (type == EntryType.PRODUCTS || type == EntryType.MIXED) product?.price ?: 0 else 0,
        quantity = if (type == EntryType.PRODUCTS || type == EntryType.MIXED) quantity else 0,
        comment = comment
    )

    if (existing == null) appState.addEntry(newEntry)
    else appState.updateEntry(newEntry)
}
