package com.example.overtime.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

enum class EntryType {
    HOURS,
    PRODUCTS,
    MIXED
}

data class Product(
    val id: String,
    val name: String,
    val price: Int
)

/**
 * Одна запись о переработке.
 */
data class Entry(
    val id: String,
    val date: LocalDate,
    val type: EntryType,

    val startTime: LocalTime? = null,
    val endTime: LocalTime? = null,

    // Выходной ли это день (сб/вс или вручную отмечен)
    val isDayOff: Boolean = false,

    // Обед в минутах (только для выходных)
    val lunchMinutes: Int = 0,

    val productId: String? = null,
    val productName: String = "",
    val productPrice: Int = 0,
    val quantity: Int = 0,

    val comment: String = ""
) {
    /**
     * Полная длительность смены в минутах (без вычета обеда).
     */
    fun rawDurationMinutes(): Int {
        val s = startTime ?: return 0
        val e = endTime ?: return 0
        val startMin = s.hour * 60 + s.minute
        val endMin = e.hour * 60 + e.minute
        return if (endMin >= startMin) endMin - startMin
        else (24 * 60 - startMin) + endMin
    }

    /**
     * Итоговая длительность с вычетом обеда (если выходной).
     */
    fun durationMinutes(): Int {
        val raw = rawDurationMinutes()
        if (raw == 0) return 0
        val lunch = if (isDayOff) lunchMinutes.coerceAtLeast(0) else 0
        return (raw - lunch).coerceAtLeast(0)
    }

    fun durationHours(): Double = durationMinutes() / 60.0

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

    fun total(hourRate: Int): Int {
        val hoursMoney = if (type == EntryType.HOURS || type == EntryType.MIXED)
            (durationHours() * hourRate).toInt() else 0
        val productsMoney = if (type == EntryType.PRODUCTS || type == EntryType.MIXED)
            quantity * productPrice else 0
        return hoursMoney + productsMoney
    }

    companion object {
        /**
         * Автоматически определяем выходной по календарю.
         */
        fun isWeekend(date: LocalDate): Boolean {
            val dow = date.dayOfWeek
            return dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY
        }
    }
}

data class AppSettings(
    val hourRate: Int = 10,
    val language: String = "ru",
    val currency: String = "BYN",
    val darkTheme: Boolean = true,
    val products: List<Product> = emptyList()
)

data class MonthSummary(
    val totalMinutes: Int = 0,
    val totalMoney: Int = 0,
    val productsCount: Map<String, Int> = emptyMap()
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
