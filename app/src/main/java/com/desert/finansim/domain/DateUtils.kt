package com.desert.finansim.domain

import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Tarihler veritabaninda epochDay (Long) olarak tutulur; saat dilimi tasimaz,
 * boylece cihaz saati/zaman dilimi degisse de "15 Agustos" 15 Agustos kalir.
 */
object DateUtils {

    val MONTH_NAMES = listOf(
        "Ocak", "Şubat", "Mart", "Nisan", "Mayıs", "Haziran",
        "Temmuz", "Ağustos", "Eylül", "Ekim", "Kasım", "Aralık",
    )

    val DAY_NAMES = listOf(
        "Pazartesi", "Salı", "Çarşamba", "Perşembe", "Cuma", "Cumartesi", "Pazar",
    )

    fun today(): LocalDate = LocalDate.now(ZoneId.systemDefault())

    fun LocalDate.toEpochDayLong(): Long = this.toEpochDay()

    fun fromEpochDay(epochDay: Long): LocalDate = LocalDate.ofEpochDay(epochDay)

    /** Ay anahtari: 2026-08 -> 202608. Sorgu ve butce eslesmesinde kullanilir. */
    fun monthKey(date: LocalDate): Int = date.year * 100 + date.monthValue

    fun monthKey(yearMonth: YearMonth): Int = yearMonth.year * 100 + yearMonth.monthValue

    fun yearMonthOf(monthKey: Int): YearMonth =
        YearMonth.of(monthKey / 100, monthKey % 100)

    fun currentMonthKey(): Int = monthKey(today())

    fun firstDayEpoch(yearMonth: YearMonth): Long = yearMonth.atDay(1).toEpochDay()

    fun lastDayEpoch(yearMonth: YearMonth): Long = yearMonth.atEndOfMonth().toEpochDay()

    /** "15 Ağustos 2026" */
    fun formatFull(date: LocalDate): String =
        "${date.dayOfMonth} ${MONTH_NAMES[date.monthValue - 1]} ${date.year}"

    /** "15 Ağustos" */
    fun formatDayMonth(date: LocalDate): String =
        "${date.dayOfMonth} ${MONTH_NAMES[date.monthValue - 1]}"

    /** "Ağustos 2026" */
    fun formatMonthYear(yearMonth: YearMonth): String =
        "${MONTH_NAMES[yearMonth.monthValue - 1]} ${yearMonth.year}"

    fun formatMonthYear(monthKey: Int): String = formatMonthYear(yearMonthOf(monthKey))

    /** "Ağu" — grafik eksenlerinde kullanilir. */
    fun formatMonthShort(yearMonth: YearMonth): String =
        MONTH_NAMES[yearMonth.monthValue - 1].take(3)

    /** "Bugün" / "Yarın" / "3 gün sonra" / "2 gün gecikti" */
    fun relativeLabel(date: LocalDate, reference: LocalDate = today()): String {
        val days = ChronoUnit.DAYS.between(reference, date)
        return when {
            days == 0L -> "Bugün"
            days == 1L -> "Yarın"
            days == -1L -> "Dün"
            days > 1L -> "$days gün sonra"
            else -> "${-days} gün gecikti"
        }
    }

    fun daysUntil(date: LocalDate, reference: LocalDate = today()): Long =
        ChronoUnit.DAYS.between(reference, date)

    /**
     * Ayin belirli gununu guvenli sekilde uretir: 31'i olmayan aylarda
     * ayin son gunune kirpar (ornegin her ayin 31'i -> Subat'ta 28/29).
     */
    fun safeDayOfMonth(yearMonth: YearMonth, dayOfMonth: Int): LocalDate {
        val day = dayOfMonth.coerceIn(1, yearMonth.lengthOfMonth())
        return yearMonth.atDay(day)
    }

    /** Son [count] ayin anahtarlarini eskiden yeniye dondurur. */
    fun lastMonthKeys(count: Int, reference: LocalDate = today()): List<Int> {
        val current = YearMonth.from(reference)
        return (count - 1 downTo 0).map { monthKey(current.minusMonths(it.toLong())) }
    }
}
