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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "${appState.t("preview")}: $month",
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(4.dp)
                        .horizontalScroll(rememberScrollState())
                ) {
                    HeaderCell(appState.t("date"), 75.dp)
                    HeaderCell(appState.t("type"), 60.dp)
                    HeaderCell(appState.t("start_time"), 55.dp)
                    HeaderCell(appState.t("end_time"), 55.dp)
                    HeaderCell(appState.t("total_time"), 80.dp)
                    HeaderCell(appState.t("product"), 90.dp)
                    HeaderCell(appState.t("quantity"), 50.dp)
                    HeaderCell(appState.t("product_price"), 50.dp)
                    HeaderCell(appState.t("total_money"), 70.dp)
                }

                var totalHours = 0.0
                var totalMoney = 0

                monthEntries.forEach { e ->
                    val durH = if (e.type == EntryType.HOURS || e.type == EntryType.MIXED)
                        e.durationHours() else 0.0
                    val money = e.total(hourRate)
                    totalHours += durH
                    totalMoney += money

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .horizontalScroll(rememberScrollState())
                    ) {
                        BodyCell(e.date.toString(), 75.dp)
                        BodyCell(
                            when (e.type) {
                                EntryType.HOURS -> appState.t("type_hours")
                                EntryType.PRODUCTS -> appState.t("type_products")
                                EntryType.MIXED -> appState.t("type_mixed")
                            },
                            60.dp
                        )
                        BodyCell(e.startTime?.toString() ?: "—", 55.dp)
                        BodyCell(e.endTime?.toString() ?: "—", 55.dp)
                        BodyCell(if (durH > 0) "%.2f".format(durH) else "—", 80.dp)
                        BodyCell(e.productName.ifBlank { "—" }, 90.dp)
                        BodyCell(if (e.quantity > 0) e.quantity.toString() else "—", 50.dp)
                        BodyCell(if (e.productPrice > 0) e.productPrice.toString() else "—", 50.dp)
                        BodyCell("$money $currency", 70.dp)
                    }
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${appState.t("total_hours")}: %.2f".format(totalHours),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${appState.t("total_money")}: $totalMoney $currency",
                        fontWeight = FontWeight.Bold
                    )
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
