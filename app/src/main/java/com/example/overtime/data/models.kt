package com.example.overtime.data

import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit

/**
 * Тип записи: чем именно занимался в переработке.
 */
enum class EntryType {
    HOURS,      // только часы
    PRODUCTS,   // только изделия
    MIXED       // и часы, и изделия
}

/**
 * Изделие в справочнике.
 */
data class Product(
    val id: String,
    val name: String,
    val price: Int           // цена в BYN (целое)
)

/**
 * Одна запись о переработке.
 * Может содержать часы (смена startTime..endTime), изделия или и то, и другое.
 */
data class Entry(
    val id: String,
    val date: LocalDate,
    val type: EntryType,

    // Поля для типа HOURS и MIXED
    val startTime: LocalTime? = null,   // например, 20:00
    val endTime: LocalTime? = null,     // например, 23:00

    // Поля для типа PRODUCTS и MIXED
    val productId: String? = null,
    val productName: String = "",
    val productPrice: Int = 0,
    val quantity: Int = 0,

    val comment: String = ""
) {
    /**
     * Длительность смены в минутах.
     * Если смена переходит через полночь (22:00 → 01:30) — тоже считает корректно.
     * Возвращает 0, если времён нет.
     */
    fun durationMinutes(): Int {
        val s = startTime ?: return 0
        val e = endTime ?: return 0
        val startMin = s.hour * 60 + s.minute
        val endMin = e.hour * 60 + e.minute
        return if (endMin >= startMin) endMin - startMin
        else (24 * 60 - startMin) + endMin
    }

    /**
     * Длительность в часах (для отображения).
     */
    fun durationHours(): Double = durationMinutes() / 60.0

    /**
     * Красивое отображение длительности: "2 ч 30 мин".
     */
    fun durationText(): String {
        val m = durationMinutes()
        val h = m / 60
        val min = m % 60
        return when {
            h == 0 -> "$min мин"
            min == 0 -> "$h ч"
            else -> "$h ч $min мин"
        }
    }

    /**
     * Сколько денег принесла эта запись.
     */
    fun total(hourRate: Int): Int {
        val hoursMoney = if (type == EntryType.HOURS || type == EntryType.MIXED)
            (durationHours() * hourRate).toInt() else 0
        val productsMoney = if (type == EntryType.PRODUCTS || type == EntryType.MIXED)
            quantity * productPrice else 0
        return hoursMoney + productsMoney
    }
}

/**
 * Настройки приложения.
 */
data class AppSettings(
    val hourRate: Int = 10,
    val language: String = "ru",              // "ru" или "en"
    val darkTheme: Boolean = true,
    val products: List<Product> = emptyList()
)

/**
 * Итоги за месяц.
 */
data class MonthSummary(
    val totalMinutes: Int = 0,
    val totalMoney: Int = 0,
    val productsCount: Map<String, Int> = emptyMap()  // имя изделия -> количество
) {
    fun hoursText(): String {
        val h = totalMinutes / 60
        val m = totalMinutes % 60
        return when {
            h == 0 -> "$m мин"
            m == 0 -> "$h ч"
            else -> "$h ч $m мин"
        }
    }
}
