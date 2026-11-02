package com.example.overtime.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.overtime.data.AppSettings
import com.example.overtime.data.Entry
import com.example.overtime.data.EntryType
import com.example.overtime.util.Strings
import org.dhatim.fastexcel.Workbook
import org.dhatim.fastexcel.Worksheet
import java.io.File
import java.io.FileOutputStream
import java.time.YearMonth

object ExcelExporter {

    fun buildReport(
        context: Context,
        entries: List<Entry>,
        settings: AppSettings,
        month: YearMonth
    ): File {
        val lang = settings.language
        fun t(key: String) = Strings.t(key, lang)

        val showMoney = settings.showMoneyInExport

        val monthEntries = entries
            .filter { YearMonth.from(it.date) == month }
            .sortedBy { it.date }

        val file = File(context.cacheDir, "overtime_${month}.xlsx")
        val fos = FileOutputStream(file)

        val workbook = Workbook(fos, "OvertimeTracker", "1.0")
        val sheet: Worksheet = workbook.newWorksheet("Report")

        // --- Заголовки ---
        val headers = mutableListOf(
            t("date"),
            t("type"),
            t("start_time"),
            t("end_time"),
            t("day_off"),
            t("lunch_minutes"),
            t("total_time_h"),
            t("product"),
            t("ready_qty")
        )
        if (showMoney) {
            headers.add(t("hour_rate"))
            headers.add(t("product_price"))
            headers.add(t("total_money"))
        }

        headers.forEachIndexed { col, title ->
            sheet.value(0, col, title)
            sheet.style(0, col).bold().set()
        }

        var row = 1
        var totalMinutes = 0
        var totalReady = 0
        var totalMoney = 0

        // groupId -> (productName -> (qty, money))
        val groupProducts = mutableMapOf<String?, MutableMap<String, Pair<Int, Int>>>()
        var hoursMoney = 0

        val idxTotalTime = 6
        val idxReady = 8
        val idxTotalMoney = if (showMoney) 11 else -1

        monthEntries.forEach { e ->
            var col = 0
            sheet.value(row, col++, e.date.toString())
            sheet.value(row, col++, when (e.type) {
                EntryType.HOURS -> t("type_hours")
                EntryType.PRODUCTS -> t("type_products")
                EntryType.MIXED -> t("type_mixed")
            })
            sheet.value(row, col++, e.startTime?.toString() ?: "")
            sheet.value(row, col++, e.endTime?.toString() ?: "")
            sheet.value(row, col++, if (e.isDayOff) t("yes") else t("no"))
            sheet.value(row, col++, if (e.isDayOff && e.lunchMinutes > 0) e.lunchMinutes else 0)
            sheet.value(row, col++, e.durationHours())
            sheet.value(row, col++, e.productName)
            sheet.value(row, col++, e.quantity)

            if (showMoney) {
                sheet.value(row, col++, e.effectiveHourRate(settings.hourRate))
                sheet.value(row, col++, e.productPrice)
                sheet.value(row, col++, e.total(settings.hourRate))
            }

            if (e.type == EntryType.HOURS || e.type == EntryType.MIXED) {
                totalMinutes += e.durationMinutes()
                if (showMoney) {
                    hoursMoney += (e.durationHours() * e.effectiveHourRate(settings.hourRate)).toInt()
                }
            }
            if (e.type == EntryType.PRODUCTS || e.type == EntryType.MIXED) {
                totalReady += e.quantity
                if (e.productName.isNotBlank()) {
                    val resolvedGroupId = e.productGroupId
                        ?: settings.products.find { it.id == e.productId }?.groupId
                    val map = groupProducts.getOrPut(resolvedGroupId) { mutableMapOf() }
                    val existing = map[e.productName] ?: (0 to 0)
                    map[e.productName] = (existing.first + e.quantity) to
                            (existing.second + (e.quantity * e.productPrice))
                }
            }
            totalMoney += e.total(settings.hourRate)
            row++
        }

        // --- ИТОГО ЗА МЕСЯЦ ---
        sheet.value(row + 1, 0, t("month_total").uppercase())
        sheet.style(row + 1, 0).bold().set()
        sheet.value(row + 1, idxTotalTime, totalMinutes / 60.0)
        sheet.style(row + 1, idxTotalTime).bold().set()
        sheet.value(row + 1, idxReady, totalReady)
        sheet.style(row + 1, idxReady).bold().set()

        if (showMoney) {
            sheet.value(row + 1, idxTotalMoney, totalMoney)
            sheet.style(row + 1, idxTotalMoney).bold().set()
        }

        // --- СВОДКА ПО ГРУППАМ ---
        var summaryRow = row + 3

        sheet.value(summaryRow, 0, t("summary_by_products").uppercase())
        sheet.style(summaryRow, 0).bold().set()
        summaryRow++

        sheet.value(summaryRow, 0, t("product"))
        sheet.style(summaryRow, 0).bold().set()
        sheet.value(summaryRow, 1, t("quantity"))
        sheet.style(summaryRow, 1).bold().set()
        if (showMoney) {
            sheet.value(summaryRow, 2, t("total_money"))
            sheet.style(summaryRow, 2).bold().set()
        }
        summaryRow++

        val namedGroups = groupProducts.keys
            .filter { it != null }
            .sortedBy { id -> settings.groups.find { it.id == id }?.name ?: "" }
        val hasNoGroup = groupProducts.containsKey(null) &&
                (groupProducts[null]?.isNotEmpty() == true)

        val orderedKeys: List<String?> = namedGroups + if (hasNoGroup) listOf(null) else emptyList()

        if (orderedKeys.isEmpty()) {
            sheet.value(summaryRow, 0, "—")
            summaryRow++
        } else {
            orderedKeys.forEach { groupId ->
                val groupName = if (groupId == null) t("no_group")
                else settings.groups.find { it.id == groupId }?.name ?: t("no_group")
                val productsMap = groupProducts[groupId] ?: emptyMap<String, Pair<Int, Int>>()

                sheet.value(summaryRow, 0, "▶ $groupName")
                sheet.style(summaryRow, 0).bold().set()
                summaryRow++

                var groupQty = 0
                var groupMoney = 0
                var kits: Int? = null

                productsMap.keys.sorted().forEach { name ->
                    val (qty, money) = productsMap[name] ?: (0 to 0)
                    sheet.value(summaryRow, 0, "   $name")
                    sheet.value(summaryRow, 1, qty)
                    if (showMoney) {
                        sheet.value(summaryRow, 2, money)
                    }
                    groupQty += qty
                    groupMoney += money
                    kits = if (kits == null) qty else minOf(kits!!, qty)
                    summaryRow++
                }

                sheet.value(summaryRow, 0, "   ${t("total_for_group")}: $groupName")
                sheet.style(summaryRow, 0).bold().set()
                sheet.value(summaryRow, 1, groupQty)
                sheet.style(summaryRow, 1).bold().set()
                if (showMoney) {
                    sheet.value(summaryRow, 2, groupMoney)
                    sheet.style(summaryRow, 2).bold().set()
                }
                summaryRow++

                // Комплекты
                val kitsCount = kits ?: 0
                sheet.value(summaryRow, 0, "   ${t("kits")}: $kitsCount")
                sheet.style(summaryRow, 0).bold().set()
                summaryRow++
            }
        }

        if (showMoney) {
            summaryRow++
            sheet.value(summaryRow, 0, t("total_hours"))
            sheet.style(summaryRow, 0).bold().set()
            sheet.value(summaryRow, 1, "%.2f".format(totalMinutes / 60.0))
            sheet.style(summaryRow, 1).bold().set()

            summaryRow++
            sheet.value(summaryRow, 0, t("hours_money"))
            sheet.style(summaryRow, 0).bold().set()
            sheet.value(summaryRow, 2, hoursMoney)
            sheet.style(summaryRow, 2).bold().set()
        }

        workbook.finish()
        fos.close()
        return file
    }

    fun shareReport(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            context.packageName + ".fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share report"))
    }

    fun openReport(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            context.packageName + ".fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(
                uri,
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            shareReport(context, file)
        }
    }
}
