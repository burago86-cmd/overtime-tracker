package com.example.overtime.util

import com.example.overtime.data.AppSettings
import com.example.overtime.data.Entry
import com.example.overtime.data.EntryType

data class ProductGroupStats(
    val groupId: String?,
    val groupName: String,
    val isKitBased: Boolean,    // Считается ли по комплектам
    val unitPrice: Int,         // Цена комплекта (если isKitBased)
    val products: Map<String, Int>, // Изделие -> Кол-во
    val totalQty: Int,          // Всего штук в группе
    val kits: Int,              // Кол-во готовых комплектов
    val money: Int              // Деньги за эту группу
)

data class ReportResult(
    val totalHours: Double,
    val hoursMoney: Int,
    val groupStats: List<ProductGroupStats>,
    val totalMoney: Int
)

object ReportCalculator {
    fun calculate(entries: List<Entry>, settings: AppSettings): ReportResult {
        var totalMinutes = 0
        var hoursMoney = 0
        val productsMap = mutableMapOf<String?, MutableMap<String, Int>>() // groupId -> (productName -> qty)

        entries.forEach { e ->
            if (e.type == EntryType.HOURS || e.type == EntryType.MIXED) {
                totalMinutes += e.durationMinutes()
                hoursMoney += (e.durationHours() * e.effectiveHourRate(settings.hourRate)).toInt()
            }
            if (e.type == EntryType.PRODUCTS || e.type == EntryType.MIXED) {
                if (e.productName.isNotBlank()) {
                    val resolvedGroupId = e.productGroupId
                        ?: settings.products.find { it.id == e.productId }?.groupId
                    val map = productsMap.getOrPut(resolvedGroupId) { mutableMapOf() }
                    map[e.productName] = (map[e.productName] ?: 0) + e.quantity
                }
            }
        }

        val groupStatsList = mutableListOf<ProductGroupStats>()
        var totalProductsMoney = 0

        // Сортировка: сначала именованные группы, потом "Без группы"
        val namedGroupIds = productsMap.keys.filter { it != null }
            .sortedBy { id -> settings.groups.find { it.id == id }?.name ?: "" }
        val hasNoGroup = productsMap.containsKey(null)
        val orderedGroupIds = namedGroupIds + if (hasNoGroup) listOf(null) else emptyList()

        orderedGroupIds.forEach { groupId ->
            val products = productsMap[groupId] ?: emptyMap()
            val groupObj = settings.groups.find { it.id == groupId }
            val groupName = groupObj?.name ?: "Без группы"
            val groupPrice = groupObj?.price

            if (groupPrice != null && groupPrice > 0) {
                // Считаем по комплектам
                val catalogProducts = settings.products.filter { it.groupId == groupId }.map { it.name }
                val allNames = (catalogProducts + products.keys).toSet()
                // Комплект = минимум по всем изделиям группы
                val kits = if (allNames.isEmpty()) 0 else allNames.minOfOrNull { products[it] ?: 0 } ?: 0
                val money = kits * groupPrice
                totalProductsMoney += money
                
                groupStatsList.add(
                    ProductGroupStats(
                        groupId = groupId,
                        groupName = groupName,
                        isKitBased = true,
                        unitPrice = groupPrice,
                        products = products,
                        totalQty = products.values.sum(),
                        kits = kits,
                        money = money
                    )
                )
            } else {
                // Обычный подсчет (штуки * цена изделия)
                var money = 0
                products.forEach { (name, qty) ->
                    val p = settings.products.find { it.name.trim().equals(name.trim(), ignoreCase = true) }
                    val price = p?.price ?: 0
                    money += qty * price
                }
                totalProductsMoney += money
                
                groupStatsList.add(
                    ProductGroupStats(
                        groupId = groupId,
                        groupName = groupName,
                        isKitBased = false,
                        unitPrice = 0,
                        products = products,
                        totalQty = products.values.sum(),
                        kits = 0,
                        money = money
                    )
                )
            }
        }

        return ReportResult(
            totalHours = totalMinutes / 60.0,
            hoursMoney = hoursMoney,
            groupStats = groupStatsList,
            totalMoney = hoursMoney + totalProductsMoney
        )
    }
}
