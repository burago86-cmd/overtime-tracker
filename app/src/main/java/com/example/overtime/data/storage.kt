package com.example.overtime.data

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

@Serializable
data class EntryDto(
    val id: String,
    val date: String,
    val type: String,
    val startTime: String? = null,
    val endTime: String? = null,
    val isDayOff: Boolean = false,
    val lunchMinutes: Int = 0,
    val customHourRate: Int? = null,
    val productId: String? = null,
    val productName: String = "",
    val productPrice: Int = 0,
    val quantity: Int = 0,
    val inProgress: Int = 0,
    val progressPercent: Int? = null,
    val comment: String = ""
)

@Serializable
data class ProductDto(
    val id: String,
    val name: String,
    val price: Int
)

@Serializable
data class SettingsDto(
    val hourRate: Int = 10,
    val language: String = "ru",
    val currency: String = "BYN",
    val darkTheme: Boolean = true,
    val products: List<ProductDto> = emptyList(),
    val showMoneyInExport: Boolean = true,
    val showProgressPercent: Boolean = false,
    val pinEnabled: Boolean = false,
    val pin: String = "",
    val lockAfterMinutes: Int = 5,
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = 21,
    val reminderMinute: Int = 0
)

@Serializable
data class StorageDto(
    val entries: List<EntryDto> = emptyList(),
    val settings: SettingsDto = SettingsDto()
)

class Storage(private val context: Context) {

    private val file = File(context.filesDir, "overtime_data.json")
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    fun load(): Pair<List<Entry>, AppSettings> {
        if (!file.exists()) return emptyList<Entry>() to AppSettings()
        return try {
            val dto: StorageDto = json.decodeFromString(file.readText())
            val entries = dto.entries.mapNotNull { it.toEntry() }
            val settings = dto.settings.toSettings()
            entries to settings
        } catch (e: Exception) {
            emptyList<Entry>() to AppSettings()
        }
    }

    fun save(entries: List<Entry>, settings: AppSettings) {
        val dto = StorageDto(
            entries = entries.map { it.toDto() },
            settings = settings.toDto()
        )
        file.writeText(json.encodeToString(dto))
    }

    fun wipe() {
        file.delete()
    }

    private fun Entry.toDto() = EntryDto(
        id = id,
        date = date.toString(),
        type = type.name,
        startTime = startTime?.toString(),
        endTime = endTime?.toString(),
        isDayOff = isDayOff,
        lunchMinutes = lunchMinutes,
        customHourRate = customHourRate,
        productId = productId,
        productName = productName,
        productPrice = productPrice,
        quantity = quantity,
        inProgress = inProgress,
        progressPercent = progressPercent,
        comment = comment
    )

    private fun EntryDto.toEntry(): Entry? = try {
        Entry(
            id = id,
            date = LocalDate.parse(date),
            type = EntryType.valueOf(type),
            startTime = startTime?.let { LocalTime.parse(it) },
            endTime = endTime?.let { LocalTime.parse(it) },
            isDayOff = isDayOff,
            lunchMinutes = lunchMinutes,
            customHourRate = customHourRate,
            productId = productId,
            productName = productName,
            productPrice = productPrice,
            quantity = quantity,
            inProgress = inProgress,
            progressPercent = progressPercent,
            comment = comment
        )
    } catch (e: Exception) { null }

    private fun AppSettings.toDto() = SettingsDto(
        hourRate = hourRate,
        language = language,
        currency = currency,
        darkTheme = darkTheme,
        products = products.map { ProductDto(it.id, it.name, it.price) },
        showMoneyInExport = showMoneyInExport,
        showProgressPercent = showProgressPercent,
        pinEnabled = pinEnabled,
        pin = pin,
        lockAfterMinutes = lockAfterMinutes,
        reminderEnabled = reminderEnabled,
        reminderHour = reminderHour,
        reminderMinute = reminderMinute
    )

    private fun SettingsDto.toSettings() = AppSettings(
        hourRate = hourRate,
        language = language,
        currency = currency,
        darkTheme = darkTheme,
        products = products.map { Product(it.id, it.name, it.price) },
        showMoneyInExport = showMoneyInExport,
        showProgressPercent = showProgressPercent,
        pinEnabled = pinEnabled,
        pin = pin,
        lockAfterMinutes = lockAfterMinutes,
        reminderEnabled = reminderEnabled,
        reminderHour = reminderHour,
        reminderMinute = reminderMinute
    )

    companion object {
        fun newId(): String = UUID.randomUUID().toString()
    }
}
