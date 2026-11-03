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
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.List
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
import com.example.overtime.export.ExcelExporter
import com.example.overtime.util.ReportCalculator
import com.example.overtime.util.ReportResult
import java.time.LocalDate
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
    var showCalculator by remember { mutableStateOf(false) }
    var isCalendarView by remember { mutableStateOf(false) }
    var selectedCalendarDate by remember { mutableStateOf<LocalDate?>(null) }

    val monthEntries = entries.filter {
        YearMonth.from(it.date) == selectedMonth
    }

    // Используем калькулятор для подсчета денег и сводки
    val report = ReportCalculator.calculate(monthEntries, appState.settings)

    val calendarDayEntries = selectedCalendarDate?.let { date ->
        monthEntries.filter { it.date == date }
    } ?: emptyList()

    if (showPreview) {
        PreviewDialog(
            appState = appState,
            month = selectedMonth,
            monthEntries = monthEntries,
            onDismiss = { showPreview = false },
            onOpen = {
                try {
                    val file = ExcelExporter.buildReport(
                        context = context,
                        entries = entries,
                        settings = appState.settings,
                        month = selectedMonth
                    )
                    ExcelExporter.openReport(context, file)
                } catch (e: Exception) {
                    Toast.makeText(context, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
                }
            },
            onShare = {
                try {
                    val file = ExcelExporter.buildReport(
                        context = context,
                        entries = entries,
                        settings = appState.settings,
                        month = selectedMonth
                    )
                    ExcelExporter.shareReport(context, file)
                } catch (e: Exception) {
                    Toast.makeText(context, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    if (showCalculator) {
        TimeCalculatorDialog(
            appState = appState,
            onDismiss = { showCalculator = false },
            onCreateEntry = { start, end, dayOff, lunch ->
                showCalculator = false
                Toast.makeText(
                    context,
                    "$start–$end (${if (dayOff) "выходной" else "будний"}, обед $lunch мин)",
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(appState.t("app_name")) },
                actions = {
                    IconButton(onClick = { 
                        isCalendarView = !isCalendarView 
                        selectedCalendarDate = null 
                    }) {
                        Icon(
                            imageVector = if (isCalendarView) Icons.Filled.List else Icons.Filled.CalendarToday,
                            contentDescription = if (isCalendarView) "Список" else "Календарь"
                        )
                    }
                    IconButton(onClick = { showCalculator = true }) {
                        Icon(Icons.Filled.Calculate, contentDescription = appState.t("calculator"))
                    }
                    IconButton(onClick = {
                        if (monthEntries.isEmpty()) {
                            Toast.makeText(context, appState.t("export_empty"), Toast.LENGTH_SHORT).show()
                        } else {
                            showPreview = true
                        }
                    }) {
                        Icon(Icons.Filled.Share, contentDescription = appState.t("export_excel"))
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
                report = report,
                appState = appState,
                onPrevMonth = { 
                    selectedMonth = selectedMonth.minusMonths(1) 
                    selectedCalendarDate = null 
                },
                onNextMonth = { 
                    selectedMonth = selectedMonth.plusMonths(1) 
                    selectedCalendarDate = null
                }
            )

            Spacer(Modifier.height(8.dp))

            if (isCalendarView) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        MonthCalendar(
                            month = selectedMonth,
                            entries = monthEntries,
                            selectedDate = selectedCalendarDate,
                            lang = appState.settings.language,
                            onDayClick = { date ->
                                if (selectedCalendarDate == date) {
                                    selectedCalendarDate = null
                                } else {
                                    selectedCalendarDate = date
                                }
                            }
                        )
                    }
                    
                    if (selectedCalendarDate != null) {
                        item {
                            Text(
                                text = "Записи за ${selectedCalendarDate?.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }

                        if (calendarDayEntries.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(32.dp), 
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(appState.t("no_entries"), style = MaterialTheme.typography.bodyLarge)
                                }
                            }
                        } else {
                            items(calendarDayEntries, key = { it.id }) { entry ->
                                Box(modifier = Modifier.padding(horizontal = 12.dp)) {
                                    EntryCard(
                                        entry = entry,
                                        hourRate = hourRate,
                                        appState = appState,
                                        onClick = { onEditClick(entry) },
                                        onDelete = { appState.deleteEntry(entry.id) }
                                    )
                                }
                            }
                        }
                    } else {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(32.dp), 
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Выберите день в календаре",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            } else {
                if (monthEntries.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(appState.t("no_entries"), style = MaterialTheme.typography.bodyLarge)
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
}

@Composable
private fun SummaryCard(
    month: YearMonth,
    report: ReportResult,
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
                    value = "%.2f".format(report.totalHours)
                )
                SummaryItem(
                    label = appState.t("total_products"),
                    value = "${report.groupStats.sumOf { it.totalQty }} ${appState.t("pieces_short")}"
                )
                SummaryItem(
                    label = appState.t("total_money"),
                    value = "${report.totalMoney} ${appState.currencyLabel()}"
                )
            }

            if (report.groupStats.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                report.groupStats.forEach { group ->
                    Text(
                        text = "• ${group.groupName}: ${group.totalQty} ${appState.t("pieces_short")}" +
                               if (group.isKitBased) " (${group.kits} компл.)" else "",
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
                // Шапка с датой и временем
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val dateStr = entry.date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                    val timeStr = entry.createdAt?.format(DateTimeFormatter.ofPattern("HH:mm"))
                    val headerStr = if (timeStr != null) "$dateStr, $timeStr" else dateStr

                    Text(
                        text = headerStr,
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

                if ((entry.type == EntryType.HOURS || entry.type == EntryType.MIXED) && entry.startTime != null && entry.endTime != null) {
                    Text(
                        text = "${entry.startTime}–${entry.endTime} • ${entry.durationText()}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (entry.type == EntryType.PRODUCTS || entry.type == EntryType.MIXED) {
                    Text(
                        text = "${entry.productName} × ${entry.quantity}",
                        style = MaterialTheme.typography.bodySmall
                    )
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

                if (entry.inProgress > 0) {
                    Text(
                        text = "🔧 ${appState.t("in_progress_short")}: ${entry.inProgress} ${appState.t("pieces_short")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (entry.progressPercent != null) {
                    Text(
                        text = "${appState.t("progress_percent")}: ${entry.progressPercent}${appState.t("percent_short")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary
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
    // Ищем группу через productGroupId, а если его нет — через справочник изделий
    val resolvedGroupId = entry.productGroupId
        ?: appState.settings.products.find { it.id == entry.productId }?.groupId
    
    // Проверяем, есть ли у найденной группы цена
    val isKit = appState.settings.groups.find { it.id == resolvedGroupId }?.price?.let { it > 0 } == true
    
    Text(
        text = if (isKit && entry.quantity > 0) "В компл." else "${entry.total(hourRate)} ${appState.currencyLabel()}",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = if (isKit) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface
    )
    IconButton(onClick = onDelete) {
        Icon(
            Icons.Filled.Delete,
            contentDescription = appState.t("delete"),
            tint = MaterialTheme.colorScheme.error
        )
    }
            }
            

private fun localeFor(lang: String): Locale =
    if (lang == "en") Locale.ENGLISH else Locale("ru")
