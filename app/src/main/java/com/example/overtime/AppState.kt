package com.example.overtime

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.overtime.data.AppSettings
import com.example.overtime.data.Entry
import com.example.overtime.data.Product
import com.example.overtime.data.Storage
import com.example.overtime.util.Strings

class AppState(context: Context) {

    private val storage = Storage(context)

    var entries by mutableStateOf<List<Entry>>(emptyList())
        private set

    var settings by mutableStateOf(AppSettings())
        private set

    init {
        val (loadedEntries, loadedSettings) = storage.load()
        entries = loadedEntries
        settings = loadedSettings
    }

    fun t(key: String): String = Strings.t(key, settings.language)

    fun currencyLabel(): String = if (settings.currency == "USD") "USD" else "BYN"

    fun setLanguage(lang: String) {
        settings = settings.copy(language = lang)
        persist()
    }

    fun setCurrency(curr: String) {
        settings = settings.copy(currency = curr)
        persist()
    }

    fun setDarkTheme(dark: Boolean) {
        settings = settings.copy(darkTheme = dark)
        persist()
    }

    fun setShowMoneyInExport(show: Boolean) {
        settings = settings.copy(showMoneyInExport = show)
        persist()
    }

    fun setShowProgressPercent(show: Boolean) {
        settings = settings.copy(showProgressPercent = show)
        persist()
    }

    fun addEntry(entry: Entry) {
        entries = (entries + entry).sortedByDescending { it.date }
        persist()
    }

    fun updateEntry(entry: Entry) {
        entries = entries.map { if (it.id == entry.id) entry else it }
            .sortedByDescending { it.date }
        persist()
    }

    fun deleteEntry(id: String) {
        entries = entries.filterNot { it.id == id }
        persist()
    }

    fun setHourRate(rate: Int) {
        settings = settings.copy(hourRate = rate)
        persist()
    }

    fun addProduct(name: String, price: Int) {
        val newProduct = Product(
            id = Storage.newId(),
            name = name,
            price = price
        )
        settings = settings.copy(products = settings.products + newProduct)
        persist()
    }

    fun updateProduct(id: String, name: String, price: Int) {
        settings = settings.copy(
            products = settings.products.map {
                if (it.id == id) it.copy(name = name, price = price) else it
            }
        )
        persist()
    }

    fun deleteProduct(id: String) {
        settings = settings.copy(products = settings.products.filterNot { it.id == id })
        persist()
    }

    private fun persist() {
        storage.save(entries, settings)
    }
}
