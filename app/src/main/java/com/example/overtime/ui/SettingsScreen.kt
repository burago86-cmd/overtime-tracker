package com.example.overtime.ui

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.overtime.AppState
import com.example.overtime.data.Product

@Composable
fun SettingsScreen(appState: AppState) {

    var hourRateText by remember {
        mutableStateOf(appState.settings.hourRate.toString())
    }

    var showProductDialog by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<Product?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = appState.t("settings"),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {

            // Ставка за час
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "${appState.t("hour_rate")} (${appState.currencyLabel()})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = hourRateText,
                                onValueChange = { new ->
                                    if (new.all { it.isDigit() }) hourRateText = new
                                },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            Button(onClick = {
                                val rate = hourRateText.toIntOrNull() ?: return@Button
                                appState.setHourRate(rate)
                            }) {
                                Text(appState.t("save"))
                            }
                        }
                    }
                }
            }

            // Валюта
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = appState.t("currency"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { appState.setCurrency("BYN") },
                                enabled = appState.settings.currency != "BYN"
                            ) { Text("BYN") }
                            Button(
                                onClick = { appState.setCurrency("USD") },
                                enabled = appState.settings.currency != "USD"
                            ) { Text("USD") }
                        }
                    }
                }
            }

            // Язык
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = appState.t("language"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { appState.setLanguage("ru") },
                                enabled = appState.settings.language != "ru"
                            ) { Text(appState.t("russian")) }
                            Button(
                                onClick = { appState.setLanguage("en") },
                                enabled = appState.settings.language != "en"
                            ) { Text(appState.t("english")) }
                        }
                    }
                }
            }

            // Тёмная тема
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = appState.t("dark_theme"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Switch(
                            checked = appState.settings.darkTheme,
                            onCheckedChange = { appState.setDarkTheme(it) }
                        )
                    }
                }
            }

            // Тумблер «Показывать проценты»
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = appState.t("show_progress_percent"),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Switch(
                                checked = appState.settings.showProgressPercent,
                                onCheckedChange = { appState.setShowProgressPercent(it) }
                            )
                        }
                        Text(
                            text = appState.t("show_progress_percent_hint"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Справочник изделий
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = appState.t("products_catalog"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Button(onClick = {
                        editingProduct = null
                        showProductDialog = true
                    }) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.padding(start = 4.dp))
                        Text(appState.t("add_product"))
                    }
                }
            }

            if (appState.settings.products.isEmpty()) {
                item {
                    Text(
                        text = "—",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(appState.settings.products, key = { it.id }) { product ->
                    Card {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = product.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${product.price} ${appState.currencyLabel()}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            IconButton(onClick = {
                                editingProduct = product
                                showProductDialog = true
                            }) {
                                Icon(Icons.Filled.Edit, contentDescription = appState.t("edit_entry"))
                            }
                            IconButton(onClick = { appState.deleteProduct(product.id) }) {
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

            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    if (showProductDialog) {
        ProductDialog(
            appState = appState,
            existing = editingProduct,
            onDismiss = { showProductDialog = false },
            onSave = { name, price ->
                val editing = editingProduct
                if (editing == null) {
                    appState.addProduct(name, price)
                } else {
                    appState.updateProduct(editing.id, name, price)
                }
                showProductDialog = false
            }
        )
    }
}

@Composable
private fun ProductDialog(
    appState: AppState,
    existing: Product?,
    onDismiss: () -> Unit,
    onSave: (name: String, price: Int) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var priceText by remember { mutableStateOf(existing?.price?.toString() ?: "") }

    val valid = name.isNotBlank() && (priceText.toIntOrNull() ?: 0) > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (existing == null) appState.t("add_product")
                else appState.t("edit_entry")
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(appState.t("product_name")) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { new ->
                        if (new.all { it.isDigit() }) priceText = new
                    },
                    label = { Text("${appState.t("product_price")} (${appState.currencyLabel()})") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name.trim(), priceText.toIntOrNull() ?: 0) },
                enabled = valid
            ) {
                Text(appState.t("save"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(appState.t("cancel"))
            }
        }
    )
}
