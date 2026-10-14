package com.example.overtime.data

import java.time.LocalDate

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
 * Пример: "Деталь А", цена 5 BYN за штуку.
 */
data class Product(
    val id: String,          // уникальный id (генерим случайно)
    val name: String,        // название
    val price: Int           // цена в BYN (целое)
)

/**
 * Одна запись о переработке.
 * Может содержать часы, изделия или и то, и другое.
 */
data class Entry(
    val id: String,
    val date: LocalDate,           // дата переработки
    val type: EntryType,

    // Поля для типа HOURS и MIXED
    val hours: Double = 0.0,       // часы (например, 2.5)

    // Поля для типа PRODUCTS и MIXED
    val productId: String? = null, // ссылка на Product.id
    val productName: String = "",  // дублируем имя — на случай если изделие удалят
    val productPrice: Int = 0,     // дублируем цену — фиксируем на момент записи
    val quantity: Int = 0,         // количество изделий

    val comment: String = ""       // произвольный комментарий
) {
    /**
     * Сколько денег принесла эта запись.
     * Считаем по часовой ставке + по изделиям.
     */
    fun total(hourRate: Int): Int {
        val hoursMoney = if (type == EntryType.HOURS || type == EntryType.MIXED)
            (hours * hourRate).toInt() else 0
        val productsMoney = if (type == EntryType.PRODUCTS || type == EntryType.MIXED)
            quantity * productPrice else 0
        return hoursMoney + productsMoney
    }
}

/**
 * Настройки приложения.
 */
data class AppSettings(
    val hourRate: Int = 10,                   // ставка за час в BYN
    val language: String = "ru",              // "ru" или "en"
    val darkTheme: Boolean = true,            // тёмная тема
    val products: List<Product> = emptyList() // справочник изделий
)
