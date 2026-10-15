package com.example.overtime

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.overtime.data.AppSettings
import com.example.overtime.data.Entry
import com.example.overtime.data.Product
import com.example.overtime.data.Storage
import com.example.overtime.util.NotificationScheduler
import com.example.overtime.util.Strings

class AppState(context: Context) {

    private val storage = Storage(context)
    private val appContext = context.applicationContext

    var entries by mutableStateOf<List<Entry>>(emptyList())
        private set

    var settings by mutableStateOf(AppSettings())
        private set

    // --- PIN ---
    // Разблокировано ли приложение сейчас
    var isUnlocked by mutableStateOf(false)
        private set

    // Время последнего сворачивания (0 = не сворачивали)
    var lastPauseTime by mutableStateOf(0L)

    // Счётчик неудачных попыток
    var failedPinAttempts by mutableStateOf(0)
    // Время, до которого заблокирован ввод (0 = не заблокирован)
    var pinLockedUntil by mutableStateOf(0L)

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

    // --- PIN ---

    fun setPin(pin: String) {
        settings = settings.copy(pin = pin, pinEnabled = true)
        persist()
    }

    fun disablePin() {
        settings = settings.copy(pin = "", pinEnabled = false)
        persist()
    }

    fun setPinEnabled(enabled: Boolean) {
        settings = settings.copy(pinEnabled = enabled)
        persist()
    }

    fun setLockAfterMinutes(minutes: Int) {
        settings = settings.copy(lockAfterMinutes = minutes)
        persist()
    }

    fun unlock() {
        isUnlocked = true
        failedPinAttempts = 0
        pinLockedUntil = 0L
    }

    fun lock() {
        isUnlocked = false
    }

    fun onAppPaused() {
        lastPauseTime = System.currentTimeMillis()
    }

    fun onAppResumed() {
        if (!settings.pinEnabled) {
            isUnlocked = true
            return
        }
        // Если PIN включён — проверяем, прошло ли lockAfterMinutes
        val elapsed = System.currentTimeMillis() - lastPauseTime
        val thresholdMs = settings.lockAfterMinutes * 60L * 1000L
        if (lastPauseTime == 0L || elapsed >= thresholdMs) {
            isUnlocked = false
        }
    }

    /**
     * Попытка ввода PIN. Возвращает true если верный.
     */
    fun tryPin(input: String): Boolean {
        if (System.currentTimeMillis() < pinLockedUntil) return false

        if (input == settings.pin) {
            unlock()
            return true
        }

        failedPinAttempts++
        if (failedPinAttempts >= 5) {
            // Блокировка на 30 секунд
            pinLockedUntil = System.currentTimeMillis() + 30_000L
            failedPinAttempts = 0
        }
        return false
    }

    /**
     * Сброс всех данных (при «Забыл PIN»).
     */
    fun wipeAllData() {
        storage.wipe()
        entries = emptyList()
        settings = AppSettings()
        isUnlocked = false
    }

    // --- Напоминание ---

    fun setReminderEnabled(enabled: Boolean) {
        settings = settings.copy(reminderEnabled = enabled)
        persist()
        if (enabled) {
            NotificationScheduler.schedule(
                appContext,
                settings.reminderHour,
                settings.reminderMinute
            )
        } else {
            NotificationScheduler.cancel(appContext)
        }
    }

    fun setReminderTime(hour: Int, minute: Int) {
        settings = settings.copy(reminderHour = hour, reminderMinute = minute)
        persist()
        if (settings.reminderEnabled) {
            NotificationScheduler.schedule(appContext, hour, minute)
        }
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
