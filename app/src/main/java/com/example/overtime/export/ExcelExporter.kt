package com.example.overtime.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.overtime.data.AppSettings
import com.example.overtime.data.Entry
import com.example.overtime.data.EntryType
import com.example.overtime.util.ReportCalculator
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

        val hasHours = monthEntries.any { it.type == EntryType.HOURS || it.type == EntryType.MIXED }
        val hasProducts = monthEntries.any { it.type == EntryType.PRODUCTS || it.type == EntryType.MIXED }
        val hasDayOff = monthEntries.any { it.isDayOff }
        val hasLunch = monthEntries.any { it.isDayOff && it.lunchMinutes > 0 }

        val colWidths = mutableMapOf<Int, Int>()

        fun writeStr(row: Int, col: Int, value: String, bold: Boolean = false, bgColor: String? = null) {
            sheet.value(row, col, value)
            if (bold) sheet.style(row, col).bold().set()
            if (bgColor != null) sheet.style(row, col).fillColor(bgColor).set()
            val len = value.length.coerceAtMost(35)
            colWidths[col] = maxOf(colWidths[col] ?: 0, len)
        }
        fun writeInt(row: Int, col: Int, value: Int, bold: Boolean = false, bgColor: String? = null) {
            writeStr(row, col, value.toString(), bold, bgColor)
        }
        fun writeDouble(row: Int, col: Int, value: Double, bold: Boolean = false, bgColor: String? = null) {
            writeStr(row, col, "%.2f".format(value), bold, bgColor)
        }

        var row = 0

        // === ЗАГОЛОВОК ===
        writeStr(row, 0, "ОТЧЁТ", bold = true)
        writeStr(row, 1, month.toString())
        row += 2

        // === ИТОГО ЗА МЕСЯЦ (Верхняя шапка) ===
        val report = ReportCalculator.calculate(monthEntries, settings)
        
        writeStr(row, 0, t("month_total").uppercase(), bold = true)
        row++

        if (hasHours) {
            writeStr(row, 0, "${t("total_hours")}:")
            writeDouble(row, 1, report.totalHours, bold = true)
            row++
        }
        if (hasProducts) {
            writeStr(row, 0, "${t("total_products")}:")
            writeInt(row, 1, report.groupStats.sumOf { it.totalQty }, bold = true)
            row++
        }
        if (showMoney) {
            writeStr(row, 0, "${t("total_money")}:")
            writeInt(row, 1, report.totalMoney, bold = true)
            row++
        }
        row++

        // === ДЕТАЛИ ===
        writeStr(row, 0, t("details").uppercase(), bold = true)
        row++

        val typesInUse = monthEntries.map { it.type }.toSet()
        if (typesInUse.size > 1) {
            writeStr(row, 0, t("legend") + ":")
            var legendCol = 1
            if (typesInUse.contains(EntryType.HOURS)) writeStr(row, legendCol++, t("type_hours"), bgColor = "E3F2FD")
            if (typesInUse.contains(EntryType.PRODUCTS)) writeStr(row, legendCol++, t("type_products"), bgColor = "E8F5E9")
            if (typesInUse.contains(EntryType.MIXED)) writeStr(row, legendCol++, t("type_mixed"), bgColor = "FFF3E0")
            row++
        }

        val colMap = mutableMapOf<String, Int>()
        var headerCol = 0

        fun addHeader(key: String) {
            colMap[key] = headerCol
            writeStr(row, headerCol, t(key), bold = true, bgColor = "DDDDDD")
            headerCol++
        }

        addHeader("date")
        if (hasHours) {
            addHeader("start_time")
            addHeader("end_time")
            if (hasDayOff) addHeader("day_off")
            if (hasLunch) addHeader("lunch_minutes")
            addHeader("total_time_h")
        }
        if (hasProducts) {
            addHeader("product")
            addHeader("ready_qty")
        }
        if (showMoney) {
            if (hasHours) addHeader("hour_rate")
            if (hasProducts) addHeader("product_price")
            addHeader("total_money")
        }

        row++

        monthEntries.forEach { e ->
            val rowColor = when (e.type) {
                EntryType.HOURS -> "E3F2FD"
                EntryType.PRODUCTS -> "E8F5E9"
                EntryType.MIXED -> "FFF3E0"
            }

            val isKit = settings.groups.find { it.id == e.productGroupId }?.price?.let { it > 0 } == true

            writeStr(row, colMap["date"]!!, e.date.toString(), bgColor = rowColor)
            if (hasHours) {
                writeStr(row, colMap["start_time"]!!, e.startTime?.toString() ?: "", bgColor = rowColor)
                writeStr(row, colMap["end_time"]!!, e.endTime?.toString() ?: "", bgColor = rowColor)
                if (hasDayOff) {
                    writeStr(row, colMap["day_off"]!!, if (e.isDayOff) t("yes") else t("no"), bgColor = rowColor)
                }
                if (hasLunch) {
                    writeInt(row, colMap["lunch_minutes"]!!, if (e.isDayOff && e.lunchMinutes > 0) e.lunchMinutes else 0, bgColor = rowColor)
                }
                writeDouble(row, colMap["total_time_h"]!!, e.durationHours(), bgColor = rowColor)
            }
            if (hasProducts) {
                writeStr(row, colMap["product"]!!, e.productName, bgColor = rowColor)
                writeInt(row, colMap["ready_qty"]!!, e.quantity, bgColor = rowColor)
            }
            if (showMoney) {
                if (hasHours) {
                    writeInt(row, colMap["hour_rate"]!!, e.effectiveHourRate(settings.hourRate), bgColor = rowColor)
                }
                if (hasProducts) {
                    writeStr(row, colMap["product_price"]!!, if (isKit) "В компл." else e.productPrice.toString(), bgColor = rowColor)
                }
                // Если это комплект, в детализации сумму не пишем (она будет в сводке)
                writeStr(row, colMap["total_money"]!!, if (isKit) "—" else e.total(settings.hourRate).toString(), bgColor = rowColor)
            }
            row++
        }
                // === СВОДКА ПО ГРУППАМ ===
        row++
        writeStr(row, 0, t("summary_by_products").uppercase(), bold = true)
        row++

        writeStr(row, 0, t("product"), bold = true)
        writeStr(row, 1, t("quantity"), bold = true)
        if (showMoney) writeStr(row, 2, t("total_money"), bold = true)
        row++

        if (report.groupStats.isEmpty()) {
            writeStr(row, 0, "—")
            row++
        } else {
            report.groupStats.forEach { group ->
                writeStr(row, 0, "▶ ${group.groupName}", bold = true)
                row++

                group.products.keys.sorted().forEach { name ->
                    val qty = group.products[name] ?: 0
                    writeStr(row, 0, "   $name")
                    writeInt(row, 1, qty)
                    if (showMoney) writeStr(row, 2, "—")
                    row++
                }

                writeStr(row, 0, "   ${t("group_total")}", bold = true)
                writeInt(row, 1, group.totalQty, bold = true)
                if (showMoney) writeInt(row, 2, group.money, bold = true)
                row++

                             if (group.isKitBased) {
                    val kitsText = if (showMoney) "   ${t("kits")}: ${group.kits} × ${group.unitPrice}" else "   ${t("kits")}: ${group.kits}"
                    writeStr(row, 0, kitsText, bold = true)
                    row++
                }

        // Итоговые часы и деньги за часы
        if (showMoney) {
            row++
            writeStr(row, 0, t("total_hours"), bold = true)
            writeDouble(row, 1, report.totalHours, bold = true)
            row++
            writeStr(row, 0, t("hours_money"), bold = true)
            writeInt(row, 2, report.hoursMoney, bold = true)
        }

        // Применяем автоширину
        colWidths.forEach { (col, len) ->
            val width = (len + 2).coerceIn(6, 35).toDouble()
            sheet.width(col, width)
        }

        workbook.finish()
        fos.close()
        return file
    }

    fun shareReport(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share report"))
    }

    fun openReport(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            shareReport(context, file)
        }
    }
}
