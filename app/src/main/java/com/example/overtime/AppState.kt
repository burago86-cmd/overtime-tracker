package com.example.overtime

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.overtime.data.AppSettings
import com.example.overtime.data.Entry
import com.example.overtime.data.Storage
import com.example.overtime.util.Strings

/**
 * Глобальное состояние приложения.
 * Держит записи и настройки, сохраняет в JSON при каждом изменении.
 */
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

    // --- Язык и тема ---

    fun t(key: String): String = Strings.t(key, settings.language)

    fun setLanguage(lang: String) {
        settings = settings.copy(language = lang)
        persist()
    }

    fun setDarkTheme(dark: Boolean) {
        settings = settings.copy(darkTheme = dark)
        persist()
    }

    // --- Записи ---

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

    // --- Настройки ---

    fun setHourRate(rate: Int) {
        settings = settings.copy(hourRate = rate)
        persist()
    }

    fun addProduct(name: String, price: Int) {
        val newProduct = com.example.overtime.data.Product(
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

    // --- Сохранение ---

    private fun persist() {
        storage.save(entries, settings)
    }
}
