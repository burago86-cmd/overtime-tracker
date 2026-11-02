package com.example.overtime

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import com.example.overtime.data.AppSettings
import com.example.overtime.data.Entry
import com.example.overtime.data.Product
import com.example.overtime.data.ProductGroup
import com.example.overtime.data.Storage
import com.example.overtime.data.StorageDto
import com.example.overtime.util.NotificationScheduler
import com.example.overtime.util.Strings
import kotlinx.serialization.json.Json
import java.io.File

class AppState(context: Context) {

    private val storage = Storage(context)
    private val appContext = context.applicationContext
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    var entries by mutableStateOf<List<Entry>>(emptyList())
        private set

    var settings by mutableStateOf(AppSettings())
        private set

    var isUnlocked by mutableStateOf(false)
        private set

    var lastPauseTime by mutableStateOf(0L)
    var failedPinAttempts by mutableStateOf(0)
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
    fun lock() { isUnlocked = false }
    fun onAppPaused() { lastPauseTime = System.currentTimeMillis() }
    fun onAppResumed() {
        if (!settings.pinEnabled) { isUnlocked = true; return }
        val elapsed = System.currentTimeMillis() - lastPauseTime
        val thresholdMs = settings.lockAfterMinutes * 60L * 1000L
        if (lastPauseTime == 0L || elapsed >= thresholdMs) isUnlocked = false
    }
    fun tryPin(input: String): Boolean {
        if (System.currentTimeMillis() < pinLockedUntil) return false
        if (input == settings.pin) { unlock(); return true }
        failedPinAttempts++
        if (failedPinAttempts >= 5) {
            pinLockedUntil = System.currentTimeMillis() + 30_000L
            failedPinAttempts = 0
        }
        return false
    }
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
        if (enabled) NotificationScheduler.schedule(appContext, settings.reminderHour, settings.reminderMinute)
        else NotificationScheduler.cancel(appContext)
    }
    fun setReminderTime(hour: Int, minute: Int) {
        settings = settings.copy(reminderHour = hour, reminderMinute = minute)
        persist()
        if (settings.reminderEnabled) NotificationScheduler.schedule(appContext, hour, minute)
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

    // --- Изделия ---
    fun addProduct(name: String, price: Int, groupId: String? = null) {
        val newProduct = Product(
            id = Storage.newId(),
            name = name,
            price = price,
            groupId = groupId
        )
        settings = settings.copy(products = settings.products + newProduct)
        persist()
    }
    fun updateProduct(id: String, name: String, price: Int, groupId: String? = null) {
        settings = settings.copy(
            products = settings.products.map {
                if (it.id == id) it.copy(name = name, price = price, groupId = groupId) else it
            }
        )
        persist()
    }
    fun deleteProduct(id: String) {
        settings = settings.copy(products = settings.products.filterNot { it.id == id })
        persist()
    }

    // --- Группы ---
    fun addGroup(name: String) {
        val newGroup = ProductGroup(id = Storage.newId(), name = name)
        settings = settings.copy(groups = settings.groups + newGroup)
        persist()
    }
    fun updateGroup(id: String, name: String) {
        settings = settings.copy(
            groups = settings.groups.map { if (it.id == id) it.copy(name = name) else it }
        )
        persist()
    }
    fun deleteGroup(id: String) {
        val updatedProducts = settings.products.map {
            if (it.groupId == id) it.copy(groupId = null) else it
        }
        settings = settings.copy(
            groups = settings.groups.filterNot { it.id == id },
            products = updatedProducts
        )
        persist()
    }
    fun groupName(groupId: String?): String {
        if (groupId == null) return ""
        return settings.groups.find { it.id == groupId }?.name ?: ""
    }

    // --- Резервная копия ---

    /**
     * Экспортирует текущую базу в JSON-файл и открывает «Поделиться».
     */
    fun exportBackup(context: Context) {
        try {
            storage.save(entries, settings)

            val dataFile = File(context.filesDir, "overtime_data.json")
            if (!dataFile.exists()) {
                Toast.makeText(context, "Файл данных не найден", Toast.LENGTH_SHORT).show()
                return
            }

            val exportFile = File(context.cacheDir, "overtime_backup.json")
            dataFile.copyTo(exportFile, overwrite = true)

            val uri = FileProvider.getUriForFile(
                context,
                context.packageName + ".fileprovider",
                exportFile
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Export backup"))
        } catch (e: Exception) {
            Toast.makeText(context, "Ошибка экспорта: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Открывает системный выбор файла для импорта.
     */
    fun importBackup(context: Context, launcher: ActivityResultLauncher<Intent>) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
        launcher.launch(intent)
    }

    /**
     * Обработка выбранного файла (вызывается после выбора).
     */
    fun handleImportUri(context: Context, uri: Uri) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream == null) {
                Toast.makeText(context, "Не удалось открыть файл", Toast.LENGTH_LONG).show()
                return
            }
            val text = inputStream.bufferedReader().use { it.readText() }
            inputStream.close()

            // Проверяем валидность JSON
            json.decodeFromString<StorageDto>(text)

            // Сохраняем во внутренний файл
            val dataFile = File(context.filesDir, "overtime_data.json")
            dataFile.writeText(text)

            // Перезагружаем состояние
            val (loadedEntries, loadedSettings) = storage.load()
            entries = loadedEntries
            settings = loadedSettings

            Toast.makeText(context, "Импорт завершён", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Ошибка импорта: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun persist() {
        storage.save(entries, settings)
    }
}
