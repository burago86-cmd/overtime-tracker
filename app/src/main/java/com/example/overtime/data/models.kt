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

data class Entry(
    val id: String,
    val date: LocalDate,
    val type: EntryType,

    val startTime: LocalTime? = null,
    val endTime: LocalTime? = null,

    val isDayOff: Boolean = false,
    val lunchMinutes: Int = 0,

    // Индивидуальная ставка за час. null = использовать ставку из настроек.
    val customHourRate: Int? = null,

    val productId: String? = null,
    val productName: String = "",
    val productPrice: Int = 0,
    val quantity: Int = 0,

    val comment: String = ""
) {
    /**
     * Ставка, которая реально применяется к этой записи.
     */
    fun effectiveHourRate(defaultRate: Int): Int =
        customHourRate ?: defaultRate

    fun rawDurationMinutes(): Int {
        val s = startTime ?: return 0
        val e = endTime ?: return 0
        val startMin = s.hour * 60 + s.minute
        val endMin = e.hour * 60 + e.minute
        return if (endMin >= startMin) endMin - startMin
        else (24 * 60 - startMin) + endMin
    }

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

    fun total(defaultHourRate: Int): Int {
        val rate = effectiveHourRate(defaultHourRate)
        val hoursMoney = if (type == EntryType.HOURS || type == EntryType.MIXED)
            (durationHours() * rate).toInt() else 0
        val productsMoney = if (type == EntryType.PRODUCTS || type == EntryType.MIXED)
            quantity * productPrice else 0
        return hoursMoney + productsMoney
    }

    companion object {
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
    val products: List<Product> = emptyList(),
    // Показывать ли деньги (ставка, цена, сумма) в предпросмотре и Excel
    val showMoneyInExport: Boolean = true
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
