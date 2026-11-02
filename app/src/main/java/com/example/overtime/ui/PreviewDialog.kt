package com.example.overtime.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.overtime.AppState
import com.example.overtime.data.Entry
import com.example.overtime.data.EntryType
import java.time.YearMonth

@Composable
fun PreviewDialog(
    appState: AppState,
    month: YearMonth,
    monthEntries: List<Entry>,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onShare: () -> Unit
) {
    val hourRate = appState.settings.hourRate
    val currency = appState.currencyLabel()
    val showMoney = appState.settings.showMoneyInExport

    // Итого
    var totalHours = 0.0
    var totalMoney = 0
    var hoursMoney = 0

    // Сводка: groupId -> (productName -> (qty, money))
    val groupProducts = mutableMapOf<String?, MutableMap<String, Pair<Int, Int>>>()

    monthEntries.forEach { e ->
        if (e.type == EntryType.HOURS || e.type == EntryType.MIXED) {
            totalHours += e.durationHours()
            hoursMoney += (e.durationHours() * e.effectiveHourRate(hourRate)).toInt()
        }
        if (e.type == EntryType.PRODUCTS || e.type == EntryType.MIXED) {
            if (e.productName.isNotBlank()) {
                val map = groupProducts.getOrPut(e.productGroupId) { mutableMapOf() }
                val existing = map[e.productName] ?: (0 to 0)
                map[e.productName] = (existing.first + e.quantity) to
                        (existing.second + (e.quantity * e.productPrice))
            }
        }
        totalMoney += e.total(hourRate)
    }

    // Порядок групп
    val namedGroups = groupProducts.keys
        .filter { it != null }
        .sortedBy { id -> appState.groupName(id) }
    val hasNoGroup = groupProducts.containsKey(null) &&
            (groupProducts[null]?.isNotEmpty() == true)
    val orderedKeys: List<String?> = namedGroups + if (hasNoGroup) listOf(null) else emptyList()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "${appState.t("preview")}: $month",
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column {
                // Switch «Показать деньги»
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = appState.t("show_money"),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Switch(
                        checked = showMoney,
                        onCheckedChange = { appState.setShowMoneyInExport(it) }
                    )
                }

                Spacer(Modifier.height(8.dp))

                Column(
                    modifier = Modifier
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // --- Таблица записей ---
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(4.dp)
                            .horizontalScroll(rememberScrollState())
                    ) {
                        HeaderCell(appState.t("date"), 75.dp)
                        HeaderCell(appState.t("type"), 70.dp)
                        HeaderCell(appState.t("start_time"), 55.dp)
                        HeaderCell(appState.t("end_time"), 55.dp)
                        HeaderCell(appState.t("lunch_minutes"), 65.dp)
                        HeaderCell(appState.t("total_time"), 80.dp)
                        HeaderCell(appState.t("product"), 90.dp)
                        HeaderCell(appState.t("ready_qty"), 60.dp)
                        if (showMoney) {
                            HeaderCell(appState.t("hour_rate"), 55.dp)
                            HeaderCell(appState.t("product_price"), 50.dp)
                            HeaderCell(appState.t("total_money"), 70.dp)
                        }
                    }

                    monthEntries.forEach { e ->
                        val durH = if (e.type == EntryType.HOURS || e.type == EntryType.MIXED)
                            e.durationHours() else 0.0
                        val money = e.total(hourRate)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .horizontalScroll(rememberScrollState())
                        ) {
                            BodyCell(
                                e.date.toString() + if (e.isDayOff) " *" else "",
                                75.dp
                            )
                            BodyCell(
                                when (e.type) {
                                    EntryType.HOURS -> appState.t("type_hours")
                                    EntryType.PRODUCTS -> appState.t("type_products")
                                    EntryType.MIXED -> appState.t("type_mixed")
                                },
                                70.dp
                            )
                            BodyCell(e.startTime?.toString() ?: "—", 55.dp)
                            BodyCell(e.endTime?.toString() ?: "—", 55.dp)
                            BodyCell(
                                if (e.isDayOff && e.lunchMinutes > 0) e.lunchMinutes.toString() else "—",
                                65.dp
                            )
                            BodyCell(if (durH > 0) "%.2f".format(durH) else "—", 80.dp)
                            BodyCell(e.productName.ifBlank { "—" }, 90.dp)
                            BodyCell(if (e.quantity > 0) e.quantity.toString() else "—", 60.dp)
                            if (showMoney) {
                                BodyCell(e.effectiveHourRate(hourRate).toString(), 55.dp)
                                BodyCell(if (e.productPrice > 0) e.productPrice.toString() else "—", 50.dp)
                                BodyCell("$money $currency", 70.dp)
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // --- ИТОГО ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${appState.t("total_hours")}: %.2f".format(totalHours),
                            fontWeight = FontWeight.Bold
                        )
                        if (showMoney) {
                            Text(
                                text = "${appState.t("total_money")}: $totalMoney $currency",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // --- СВОДКА ПО ГРУППАМ ---
                    Text(
                        text = appState.t("summary_by_products").uppercase(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(4.dp))

                    if (orderedKeys.isEmpty()) {
                        Text("—", style = MaterialTheme.typography.bodySmall)
                    } else {
                        orderedKeys.forEach { groupId ->
                            val groupName = if (groupId == null) appState.t("no_group")
                            else appState.groupName(groupId).ifBlank { appState.t("no_group") }
                            val productsMap = groupProducts[groupId] ?: emptyMap()

                            // Заголовок группы
                            Text(
                                text = "▶ $groupName",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                            )

                            // Шапка мини-таблицы
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(2.dp)
                            ) {
                                HeaderCell(appState.t("product"), 140.dp)
                                HeaderCell(appState.t("quantity"), 60.dp)
                                if (showMoney) {
                                    HeaderCell(appState.t("total_money"), 90.dp)
                                }
                            }

                            var groupQty = 0
                            var groupMoney = 0

                            productsMap.keys.sorted().forEach { name ->
                                val (qty, money) = productsMap[name] ?: (0 to 0)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp, horizontal = 4.dp)
                                ) {
                                    BodyCell(name, 140.dp)
                                    BodyCell(qty.toString(), 60.dp)
                                    if (showMoney) {
                                        BodyCell("$money $currency", 90.dp)
                                    }
                                }
                                groupQty += qty
                                groupMoney += money
                            }

                            // Промежуточный итог
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp, horizontal = 4.dp)
                            ) {
                                Text(
                                    text = "${appState.t("total_for_group")}: $groupName",
                                    modifier = Modifier.width(140.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = groupQty.toString(),
                                    modifier = Modifier.width(60.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                                if (showMoney) {
                                    Text(
                                        text = "$groupMoney $currency",
                                        modifier = Modifier.width(90.dp),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Часы и деньги за часы
                    if (showMoney) {
                        Spacer(Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = appState.t("total_hours"),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "%.2f".format(totalHours),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = appState.t("hours_money"),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "$hoursMoney $currency",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onOpen) {
                Text(appState.t("open"))
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onShare) {
                    Text(appState.t("share"))
                }
                TextButton(onClick = onDismiss) {
                    Text(appState.t("close"))
                }
            }
        }
    )
}

@Composable
private fun HeaderCell(text: String, width: Dp) {
    Text(
        text = text,
        modifier = Modifier.width(width).padding(horizontal = 4.dp),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun BodyCell(text: String, width: Dp) {
    Text(
        text = text,
        modifier = Modifier.width(width).padding(horizontal = 4.dp),
        style = MaterialTheme.typography.bodySmall
    )
}
