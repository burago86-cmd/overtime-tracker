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

        val monthEntries = entries
            .filter { YearMonth.from(it.date) == month }
            .sortedBy { it.date }

        val file = File(context.cacheDir, "overtime_${month}.xlsx")
        val fos = FileOutputStream(file)

        val workbook = Workbook(fos, "OvertimeTracker", "1.0")
        val sheet: Worksheet = workbook.newWorksheet("Report")

        // Заголовки на языке приложения
        val headers = listOf(
            t("date"),
            t("type"),
            t("start_time"),
            t("end_time"),
            t("day_off"),
            t("lunch_minutes"),
            t("total_time_h"),
            t("product"),
            t("quantity"),
            t("hour_rate"),
            t("product_price"),
            t("total_money")
        )
        headers.forEachIndexed { col, title ->
            sheet.value(0, col, title)
            sheet.style(0, col).bold().set()
        }

        var row = 1
        var totalMinutes = 0
        var totalMoney = 0

        monthEntries.forEach { e ->
            sheet.value(row, 0, e.date.toString())
            sheet.value(row, 1, when (e.type) {
                EntryType.HOURS -> t("type_hours")
                EntryType.PRODUCTS -> t("type_products")
                EntryType.MIXED -> t("type_mixed")
            })
            sheet.value(row, 2, e.startTime?.toString() ?: "")
            sheet.value(row, 3, e.endTime?.toString() ?: "")
            sheet.value(row, 4, if (e.isDayOff) t("yes") else t("no"))
            sheet.value(row, 5, if (e.isDayOff && e.lunchMinutes > 0) e.lunchMinutes else 0)
            sheet.value(row, 6, e.durationHours())
            sheet.value(row, 7, e.productName)
            sheet.value(row, 8, e.quantity)
            sheet.value(row, 9, settings.hourRate)
            sheet.value(row, 10, e.productPrice)
            sheet.value(row, 11, e.total(settings.hourRate))

            if (e.type == EntryType.HOURS || e.type == EntryType.MIXED) {
                totalMinutes += e.durationMinutes()
            }
            totalMoney += e.total(settings.hourRate)
            row++
        }

        // Итого
        sheet.value(row + 1, 0, t("month_total").uppercase())
        sheet.style(row + 1, 0).bold().set()
        sheet.value(row + 1, 6, totalMinutes / 60.0)
        sheet.value(row + 1, 11, totalMoney)
        sheet.style(row + 1, 6).bold().set()
        sheet.style(row + 1, 11).bold().set()

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
