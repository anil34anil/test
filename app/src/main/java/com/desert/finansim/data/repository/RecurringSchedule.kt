package com.desert.finansim.data.repository

import com.desert.finansim.data.local.RecurringRuleEntity
import com.desert.finansim.domain.DateUtils
import com.desert.finansim.domain.model.RecurrenceFrequency
import java.time.LocalDate
import java.time.YearMonth

/**
 * Sabit gelir/gider kurallarinin hangi tarihlerde tekrarlandigini hesaplar.
 *
 * Saf fonksiyonlar; veritabanina dokunmaz, boylece birim testi kolaydir.
 */
object RecurringSchedule {

    /** Sonsuz donguye karsi ust sinir (yaklasik 10 yil haftalik tekrar). */
    private const val MAX_ITERATIONS = 600

    /**
     * [from] ve [to] (ikisi de dahil) arasindaki tekrar tarihleri.
     * Kuralin baslangic/bitis tarihleri disina cikmaz.
     */
    fun occurrencesIn(rule: RecurringRuleEntity, from: LocalDate, to: LocalDate): List<LocalDate> {
        if (from.isAfter(to)) return emptyList()

        val ruleStart = DateUtils.fromEpochDay(rule.startDate)
        val ruleEnd = rule.endDate?.let { DateUtils.fromEpochDay(it) }

        val windowStart = maxOf(from, ruleStart)
        val windowEnd = if (ruleEnd != null) minOf(to, ruleEnd) else to
        if (windowStart.isAfter(windowEnd)) return emptyList()

        val result = mutableListOf<LocalDate>()
        when (rule.frequency) {
            RecurrenceFrequency.MONTHLY -> {
                val day = rule.dayOfMonth ?: ruleStart.dayOfMonth
                var month = YearMonth.from(windowStart)
                val lastMonth = YearMonth.from(windowEnd)
                var guard = 0
                while (!month.isAfter(lastMonth) && guard++ < MAX_ITERATIONS) {
                    val date = DateUtils.safeDayOfMonth(month, day)
                    if (!date.isBefore(windowStart) && !date.isAfter(windowEnd)) {
                        result += date
                    }
                    month = month.plusMonths(1)
                }
            }

            RecurrenceFrequency.WEEKLY -> {
                // Kuralin baslangicindan itibaren 7 gunluk adimlar.
                val targetDow = rule.dayOfWeek ?: ruleStart.dayOfWeek.value
                var date = ruleStart
                // Ilk hedef gune hizala.
                val shift = ((targetDow - date.dayOfWeek.value) + 7) % 7
                date = date.plusDays(shift.toLong())
                var guard = 0
                while (!date.isAfter(windowEnd) && guard++ < MAX_ITERATIONS) {
                    if (!date.isBefore(windowStart)) result += date
                    date = date.plusWeeks(1)
                }
            }

            RecurrenceFrequency.YEARLY -> {
                val month = rule.monthOfYear ?: ruleStart.monthValue
                val day = rule.dayOfMonth ?: ruleStart.dayOfMonth
                var year = windowStart.year
                var guard = 0
                while (year <= windowEnd.year && guard++ < MAX_ITERATIONS) {
                    val date = DateUtils.safeDayOfMonth(YearMonth.of(year, month), day)
                    if (!date.isBefore(windowStart) && !date.isAfter(windowEnd)) {
                        result += date
                    }
                    year++
                }
            }
        }
        return result
    }

    /** [reference] dahil, bundan sonraki ilk tekrar tarihi. */
    fun nextOccurrence(rule: RecurringRuleEntity, reference: LocalDate = DateUtils.today()): LocalDate? {
        // Bir yillik pencere her frekans icin en az bir tekrar yakalar.
        return occurrencesIn(rule, reference, reference.plusYears(1)).firstOrNull()
    }

    /**
     * Heniz uretilmemis, vadesi gecmis/gelmis tekrarlar.
     * [RecurringRuleEntity.lastGeneratedDate] tarihine kadar olanlar atlanir,
     * boylece ayni donem icin ikinci kez islem olusturulmaz.
     */
    fun pendingOccurrences(rule: RecurringRuleEntity, today: LocalDate): List<LocalDate> {
        if (!rule.isActive) return emptyList()
        val ruleStart = DateUtils.fromEpochDay(rule.startDate)
        val lastGenerated = rule.lastGeneratedDate?.let { DateUtils.fromEpochDay(it) }
        val from = if (lastGenerated != null) lastGenerated.plusDays(1) else ruleStart
        if (from.isAfter(today)) return emptyList()
        return occurrencesIn(rule, from, today)
    }
}
