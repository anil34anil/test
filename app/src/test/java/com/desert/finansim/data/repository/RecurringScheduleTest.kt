package com.desert.finansim.data.repository

import com.desert.finansim.data.local.RecurringRuleEntity
import com.desert.finansim.domain.Money
import com.desert.finansim.domain.model.RecurrenceFrequency
import com.desert.finansim.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Sartnamedeki 5. senaryo: aylik 20.000 TL kira gideri tanimlanir,
 * bir sonraki aya gecildiginde kira otomatik olusmalidir.
 */
class RecurringScheduleTest {

    private fun rentRule(
        startDate: LocalDate = LocalDate.of(2026, 8, 1),
        dayOfMonth: Int = 1,
        lastGenerated: LocalDate? = null,
        active: Boolean = true,
    ) = RecurringRuleEntity(
        id = 1,
        title = "Kira",
        type = TransactionType.EXPENSE,
        amountMinor = 2_000_000L, // 20.000 TL
        frequency = RecurrenceFrequency.MONTHLY,
        dayOfMonth = dayOfMonth,
        startDate = startDate.toEpochDay(),
        lastGeneratedDate = lastGenerated?.toEpochDay(),
        isActive = active,
    )

    @Test
    fun `senaryo 5 - sonraki aya gecince kira otomatik olusur`() {
        val rule = rentRule(lastGenerated = LocalDate.of(2026, 8, 1))

        // Agustos ayindayken yeni bir kayit uretilmemeli
        assertTrue(RecurringSchedule.pendingOccurrences(rule, LocalDate.of(2026, 8, 20)).isEmpty())

        // Eylul'e gecince 1 Eylul kaydi uretilmeli
        val pending = RecurringSchedule.pendingOccurrences(rule, LocalDate.of(2026, 9, 3))
        assertEquals(listOf(LocalDate.of(2026, 9, 1)), pending)
        assertEquals("20.000 ₺", Money.format(rule.amountMinor))
    }

    @Test
    fun `atlanan aylar toplu olarak yakalanir`() {
        // Uygulama uc ay acilmamis olsun
        val rule = rentRule(lastGenerated = LocalDate.of(2026, 8, 1))
        val pending = RecurringSchedule.pendingOccurrences(rule, LocalDate.of(2026, 11, 5))
        assertEquals(
            listOf(
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 11, 1),
            ),
            pending,
        )
    }

    @Test
    fun `pasif kural islem uretmez`() {
        val rule = rentRule(lastGenerated = LocalDate.of(2026, 8, 1), active = false)
        assertTrue(RecurringSchedule.pendingOccurrences(rule, LocalDate.of(2026, 12, 1)).isEmpty())
    }

    @Test
    fun `baslangic tarihinden onceki donemler uretilmez`() {
        val rule = rentRule(startDate = LocalDate.of(2026, 8, 1))
        val pending = RecurringSchedule.pendingOccurrences(rule, LocalDate.of(2026, 8, 15))
        assertEquals(listOf(LocalDate.of(2026, 8, 1)), pending)
    }

    @Test
    fun `bitis tarihinden sonra uretim durur`() {
        val rule = rentRule(lastGenerated = LocalDate.of(2026, 8, 1))
            .copy(endDate = LocalDate.of(2026, 9, 30).toEpochDay())
        val pending = RecurringSchedule.pendingOccurrences(rule, LocalDate.of(2026, 12, 1))
        assertEquals(listOf(LocalDate.of(2026, 9, 1)), pending)
    }

    @Test
    fun `31 ini olmayan ayda ayin son gunune kirpilir`() {
        val rule = rentRule(startDate = LocalDate.of(2026, 1, 31), dayOfMonth = 31)
        val occurrences = RecurringSchedule.occurrencesIn(
            rule,
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 4, 30),
        )
        assertEquals(
            listOf(
                LocalDate.of(2026, 1, 31),
                LocalDate.of(2026, 2, 28),
                LocalDate.of(2026, 3, 31),
                LocalDate.of(2026, 4, 30),
            ),
            occurrences,
        )
    }

    @Test
    fun `haftalik tekrar dogru gune hizalanir`() {
        // 3 Agustos 2026 Pazartesi
        val rule = RecurringRuleEntity(
            id = 2,
            title = "Haftalık market",
            type = TransactionType.EXPENSE,
            amountMinor = 50_000L,
            frequency = RecurrenceFrequency.WEEKLY,
            dayOfWeek = 1, // Pazartesi
            startDate = LocalDate.of(2026, 8, 1).toEpochDay(), // Cumartesi
        )
        val occurrences = RecurringSchedule.occurrencesIn(
            rule,
            LocalDate.of(2026, 8, 1),
            LocalDate.of(2026, 8, 31),
        )
        assertEquals(
            listOf(
                LocalDate.of(2026, 8, 3),
                LocalDate.of(2026, 8, 10),
                LocalDate.of(2026, 8, 17),
                LocalDate.of(2026, 8, 24),
                LocalDate.of(2026, 8, 31),
            ),
            occurrences,
        )
        occurrences.forEach { assertEquals(java.time.DayOfWeek.MONDAY, it.dayOfWeek) }
    }

    @Test
    fun `yillik tekrar yilda bir kez uretir`() {
        val rule = RecurringRuleEntity(
            id = 3,
            title = "Sigorta",
            type = TransactionType.EXPENSE,
            amountMinor = 300_000L,
            frequency = RecurrenceFrequency.YEARLY,
            monthOfYear = 3,
            dayOfMonth = 15,
            startDate = LocalDate.of(2025, 1, 1).toEpochDay(),
        )
        val occurrences = RecurringSchedule.occurrencesIn(
            rule,
            LocalDate.of(2025, 1, 1),
            LocalDate.of(2027, 12, 31),
        )
        assertEquals(
            listOf(
                LocalDate.of(2025, 3, 15),
                LocalDate.of(2026, 3, 15),
                LocalDate.of(2027, 3, 15),
            ),
            occurrences,
        )
    }

    @Test
    fun `sonraki tekrar tarihi bulunur`() {
        val rule = rentRule()
        assertEquals(
            LocalDate.of(2026, 9, 1),
            RecurringSchedule.nextOccurrence(rule, LocalDate.of(2026, 8, 15)),
        )
    }
}
