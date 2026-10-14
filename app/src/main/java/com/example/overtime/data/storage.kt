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

/**
 * DTO для сериализации — приводим LocalDate/LocalTime к строкам,
 * потому что kotlinx.serialization не знает про java.time по умолчанию.
 */
@Serializable
data class EntryDto(
    val id: String,
    val date: String,           // ISO: "2026-10-14"
    val type: String,           // "HOURS", "PRODUCTS", "MIXED"
    val startTime: String? = null,  // "20:00"
    val endTime: String? = null,    // "23:00"
    val productId: String? = null,
    val productName: String = "",
    val productPrice: Int = 0,
    val quantity: Int = 0,
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
    val darkTheme: Boolean = true,
    val products: List<ProductDto> = emptyList()
)

@Serializable
data class StorageDto(
    val entries: List<EntryDto> = emptyList(),
    val settings: SettingsDto = SettingsDto()
)

/**
 * Хранилище: сохраняет в JSON-файл в приватной папке приложения.
 */
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

    // --- Конвертеры ---

    private fun Entry.toDto() = EntryDto(
        id = id,
        date = date.toString(),
        type = type.name,
        startTime = startTime?.toString(),
        endTime = endTime?.toString(),
        productId = productId,
        productName = productName,
        productPrice = productPrice,
        quantity = quantity,
        comment = comment
    )

    private fun EntryDto.toEntry(): Entry? = try {
        Entry(
            id = id,
            date = LocalDate.parse(date),
            type = EntryType.valueOf(type),
            startTime = startTime?.let { LocalTime.parse(it) },
            endTime = endTime?.let { LocalTime.parse(it) },
            productId = productId,
            productName = productName,
            productPrice = productPrice,
            quantity = quantity,
            comment = comment
        )
    } catch (e: Exception) { null }

    private fun AppSettings.toDto() = SettingsDto(
        hourRate = hourRate,
        language = language,
        darkTheme = darkTheme,
        products = products.map { ProductDto(it.id, it.name, it.price) }
    )

    private fun SettingsDto.toSettings() = AppSettings(
        hourRate = hourRate,
        language = language,
        darkTheme = darkTheme,
        products = products.map { Product(it.id, it.name, it.price) }
    )

    companion object {
        fun newId(): String = UUID.randomUUID().toString()
    }
}
