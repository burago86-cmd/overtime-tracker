package com.example.overtime.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.overtime.data.AppSettings
import com.example.overtime.data.Entry
import com.example.overtime.data.EntryType
import org.dhatim.fastexcel.Workbook
import org.dhatim.fastexcel.Worksheet
import java.io.File
import java.io.FileOutputStream
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

object ExcelExporter {

    /**
     * Генерирует .xlsx за указанный месяц и возвращает File.
     */
    fun buildReport(
        context: Context,
        entries: List<Entry>,
        settings: AppSettings,
        month: YearMonth
    ): File {
        val monthEntries = entries
            .filter { YearMonth.from(it.date) == month }
            .sortedBy { it.date }

        val file = File(context.cacheDir, "overtime_${month}.xlsx")
        val fos = FileOutputStream(file)

        val workbook = Workbook(fos, "OvertimeTracker", "1.0")
        val sheet: Worksheet = workbook.newWorksheet("Report")

        // Заголовки
        val headers = listOf(
            "Date", "Type", "Start", "End", "Duration", "Product", "Qty", "Rate", "Price", "Total"
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
            sheet.value(row, 1, e.type.name)
            sheet.value(row, 2, e.startTime?.toString() ?: "")
            sheet.value(row, 3, e.endTime?.toString() ?: "")
            sheet.value(row, 4, e.durationHours())
            sheet.value(row, 5, e.productName)
            sheet.value(row, 6, e.quantity)
            sheet.value(row, 7, settings.hourRate)
            sheet.value(row, 8, e.productPrice)
            sheet.value(row, 9, e.total(settings.hourRate))

            if (e.type == EntryType.HOURS || e.type == EntryType.MIXED) {
                totalMinutes += e.durationMinutes()
            }
            totalMoney += e.total(settings.hourRate)
            row++
        }

        // Итоги
          sheet.value(row + 1, 0, "TOTAL")
        sheet.style(row + 1, 0).bold().set()
        sheet.value(row + 1, 4, totalMinutes / 60.0)
        sheet.value(row + 1, 9, totalMoney)
        sheet.style(row + 1, 4).bold().set()
        sheet.style(row + 1, 9).bold().set()

        workbook.finish()
        fos.close()
        return file
    }

    /**
     * Открывает системный диалог «Поделиться» с готовым файлом.
     */
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
}
