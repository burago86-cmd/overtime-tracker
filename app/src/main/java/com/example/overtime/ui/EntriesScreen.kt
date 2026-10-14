package com.example.overtime.ui

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import com.example.overtime.data.Entry
import com.example.overtime.data.EntryType
import com.example.overtime.data.MonthSummary
import com.example.overtime.export.ExcelExporter
import java.io.File
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntriesScreen(
    appState: AppState,
    onAddClick: () -> Unit,
    onEditClick: (Entry) -> Unit
) {
    val context = LocalContext.current
    val entries = appState.entries
    val hourRate = appState.settings.hourRate

    var selectedMonth by remember { mutableStateOf(YearMonth.now()) }
    var showPreview by remember { mutableStateOf(false) }
    var previewFile by remember { mutableStateOf<File?>(null) }

    val monthEntries = entries.filter {
        YearMonth.from(it.date) == selectedMonth
    }

    val summary = calculateSummary(monthEntries, hourRate)

    if (showPreview && previewFile != null) {
        PreviewDialog(
            appState = appState,
            month = selectedMonth,
            monthEntries = monthEntries,
            onDismiss = {
                showPreview = false
                previewFile = null
            },
            onOpen = {
                val f = previewFile
                if (f != null) ExcelExporter.openReport(context, f)
            },
            onShare = {
                val f = previewFile
                if (f != null) ExcelExporter.shareReport(context, f)
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(appState.t("app_name")) },
                actions = {
                    IconButton(onClick = {
                        if (monthEntries.isEmpty()) {
                            Toast.makeText(
                                context,
                                appState.t("export_empty"),
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            try {
                                val file = ExcelExporter.buildReport(
                                    context = context,
                                    entries = entries,
                                    settings = appState.settings,
                                    month = selectedMonth
                                )
                                previewFile = file
                                showPreview = true
                            } catch (e: Exception) {
                                Toast.makeText(
                                    context,
                                    "Ошибка: ${e.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }) {
                        Icon(
                            Icons.Filled.Share,
                            contentDescription = appState.t("export_excel")
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(Icons.Filled.Add, contentDescription = appState.t("add_entry"))
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            SummaryCard(
                month = selectedMonth,
                summary = summary,
                appState = appState,
                onPrevMonth = { selectedMonth = selectedMonth.minusMonths(1) },
                onNextMonth = { selectedMonth = selectedMonth.plusMonths(1) }
            )

            Spacer(Modifier.height(8.dp))

            if (monthEntries.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = appState.t("no_entries"),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(monthEntries, key = { it.id }) { entry ->
                        EntryCard(
                            entry = entry,
                            hourRate = hourRate,
                            appState = appState,
                            onClick = { onEditClick(entry) },
                            onDelete = { appState.deleteEntry(entry.id) }
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(
    month: YearMonth,
    summary: MonthSummary,
    appState: AppState,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "◀",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier
                        .clickable { onPrevMonth() }
                        .padding(8.dp)
                )
                Text(
                    text = month.format(
                        DateTimeFormatter.ofPattern(
                            "LLLL yyyy",
                            localeFor(appState.settings.language)
                        )
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "▶",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier
                        .clickable { onNextMonth() }
                        .padding(8.dp)
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryItem(
                    label = appState.t("total_hours"),
                    value = summary.hoursText()
                )
                SummaryItem(
                    label = appState.t("total_products"),
                    value = "${summary.productsCount.values.sum()} ${appState.t("pieces_short")}"
                )
                SummaryItem(
                    label = appState.t("total_money"),
                    value = "${summary.totalMoney} ${appState.currencyLabel()}"
                )
            }

            if (summary.productsCount.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                summary.productsCount.forEach { (name, qty) ->
                    Text(
                        text = "• $name: $qty ${appState.t("pieces_short")}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun EntryCard(
    entry: Entry,
    hourRate: Int,
    appState: AppState,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = entry.date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (entry.isDayOff) {
                        Text(
                            text = " • ${appState.t("day_off")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                val typeLabel = when (entry.type) {
                    EntryType.HOURS -> appState.t("type_hours")
                    EntryType.PRODUCTS -> appState.t("type_products")
                    EntryType.MIXED -> appState.t("type_mixed")
                }
                Text(
                    text = typeLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )

                when (entry.type) {
                    EntryType.HOURS -> {
                        Text(
                            text = "${entry.startTime}–${entry.endTime} • ${entry.durationText()}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    EntryType.PRODUCTS -> {
                        Text(
                            text = "${entry.productName} × ${entry.quantity}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    EntryType.MIXED -> {
                        Text(
                            text = "${entry.startTime}–${entry.endTime} • ${entry.durationText()}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "${entry.productName} × ${entry.quantity}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                if (entry.isDayOff && entry.lunchMinutes > 0) {
                    Text(
                        text = "${appState.t("lunch")}: ${entry.lunchMinutes} ${appState.t("minutes_short")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (entry.customHourRate != null) {
                    Text(
                        text = "★ ${appState.t("hour_rate")}: ${entry.customHourRate} ${appState.currencyLabel()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (entry.comment.isNotBlank()) {
                    Text(
                        text = entry.comment,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${entry.total(hourRate)} ${appState.currencyLabel()}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = appState.t("delete"),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

private fun calculateSummary(entries: List<Entry>, hourRate: Int): MonthSummary {
    var totalMinutes = 0
    var totalMoney = 0
    val productsCount = mutableMapOf<String, Int>()

    entries.forEach { entry ->
        if (entry.type == EntryType.HOURS || entry.type == EntryType.MIXED) {
            totalMinutes += entry.durationMinutes()
        }
        if (entry.type == EntryType.PRODUCTS || entry.type == EntryType.MIXED) {
            productsCount[entry.productName] =
                (productsCount[entry.productName] ?: 0) + entry.quantity
        }
        totalMoney += entry.total(hourRate)
    }

    return MonthSummary(totalMinutes, totalMoney, productsCount)
}

private fun localeFor(lang: String): Locale =
    if (lang == "en") Locale.ENGLISH else Locale("ru")
