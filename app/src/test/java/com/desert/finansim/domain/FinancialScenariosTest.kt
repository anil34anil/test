package com.desert.finansim.domain

import com.desert.finansim.data.local.CreditCardEntity
import com.desert.finansim.data.local.DebtEntity
import com.desert.finansim.data.local.InstallmentEntity
import com.desert.finansim.data.local.ReceivableEntity
import com.desert.finansim.domain.model.CreditCardSummary
import com.desert.finansim.domain.model.DebtSummary
import com.desert.finansim.domain.model.DebtType
import com.desert.finansim.domain.model.InstallmentStatus
import com.desert.finansim.domain.model.MonthlyTotals
import com.desert.finansim.domain.model.ReceivableSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Sartnamedeki 1-4 numarali senaryolarin dogrulamasi.
 * Tutarlar kurus cinsindendir: 50.000 TL -> 5_000_000L
 */
class FinancialScenariosTest {

    @Test
    fun `senaryo 1 - gelir 50000 gider 10000 ise net 40000`() {
        val totals = MonthlyTotals(
            monthKey = 202608,
            incomeMinor = 5_000_000L,
            expenseMinor = 1_000_000L,
        )

        assertEquals(5_000_000L, totals.incomeMinor)
        assertEquals(1_000_000L, totals.expenseMinor)
        assertEquals(4_000_000L, totals.netMinor)
        assertEquals("40.000 ₺", Money.format(totals.netMinor))
    }

    @Test
    fun `senaryo 1b - borc odemesi net durumdan ayrica dusulur`() {
        val totals = MonthlyTotals(
            monthKey = 202608,
            incomeMinor = 5_000_000L,
            expenseMinor = 1_000_000L,
            debtPaymentMinor = 500_000L,
        )
        // Gelir - (gider + borc odemesi)
        assertEquals(3_500_000L, totals.netMinor)
        // Tahsilat gelir sayilmaz ama nakit girisidir.
        val withCollection = totals.copy(collectionMinor = 200_000L)
        assertEquals(3_500_000L, withCollection.netMinor)
        assertEquals(5_200_000L, withCollection.cashInMinor)
        assertEquals(1_500_000L, withCollection.cashOutMinor)
    }

    @Test
    fun `senaryo 2 - 12000 TL borc 12 taksit ilk taksit odenince kalan 11000`() {
        val total = 1_200_000L
        val amounts = Money.splitEqually(total, 12)

        // Her taksit 1.000 TL olmali
        amounts.forEach { assertEquals(100_000L, it) }
        assertEquals("1.000 ₺", Money.format(amounts.first()))

        val debt = DebtEntity(
            id = 1,
            name = "Test kredisi",
            type = DebtType.LOAN,
            totalAmountMinor = total,
            installmentCount = 12,
            startDate = LocalDate.of(2026, 8, 1).toEpochDay(),
        )

        // Ilk taksit odenmeden once
        val before = DebtSummary(debt = debt, paidMinor = 0L)
        assertEquals(1_200_000L, before.remainingMinor)
        assertEquals(0f, before.progress, 0.0001f)

        // Ilk taksit "odendi" -> 1.000 TL odeme islemi olusur
        val after = DebtSummary(debt = debt, paidMinor = 100_000L)
        assertEquals(1_100_000L, after.remainingMinor)
        assertEquals("11.000 ₺", Money.format(after.remainingMinor))
        assertEquals(1f / 12f, after.progress, 0.0001f)
        assertFalse(after.isSettled)
    }

    @Test
    fun `senaryo 2b - tum taksitler odenince borc kapanir`() {
        val debt = DebtEntity(
            id = 1,
            name = "Test kredisi",
            type = DebtType.LOAN,
            totalAmountMinor = 1_200_000L,
            startDate = LocalDate.of(2026, 8, 1).toEpochDay(),
        )
        val settled = DebtSummary(debt = debt, paidMinor = 1_200_000L)
        assertEquals(0L, settled.remainingMinor)
        assertTrue(settled.isSettled)
        assertEquals(1f, settled.progress, 0.0001f)
    }

    @Test
    fun `taksit durumu bugune gore turetilir`() {
        val today = LocalDate.of(2026, 8, 13)
        val debt = DebtEntity(
            id = 1,
            name = "Kredi",
            type = DebtType.LOAN,
            totalAmountMinor = 300_000L,
            startDate = today.toEpochDay(),
        )
        val overdue = InstallmentEntity(
            id = 1, debtId = 1, number = 1, totalCount = 3,
            amountMinor = 100_000L,
            dueDate = today.minusDays(5).toEpochDay(),
        )
        val pending = overdue.copy(id = 2, number = 2, dueDate = today.plusDays(20).toEpochDay())
        val paid = overdue.copy(id = 3, number = 3, isPaid = true)

        val summary = DebtSummary(debt, 0L, listOf(overdue, pending, paid))
        assertEquals(InstallmentStatus.OVERDUE, summary.statusOf(overdue, today))
        assertEquals(InstallmentStatus.PENDING, summary.statusOf(pending, today))
        assertEquals(InstallmentStatus.PAID, summary.statusOf(paid, today))

        // Sonraki odenecek taksit, odenmemisler arasinda en erken vadeli olan.
        assertEquals(overdue.id, summary.nextInstallment()?.id)
    }

    @Test
    fun `senaryo 3 - 5000 TL alacaktan 2000 tahsil edilince kalan 3000`() {
        val receivable = ReceivableEntity(
            id = 1,
            personName = "Ahmet",
            totalAmountMinor = 500_000L,
            startDate = LocalDate.of(2026, 8, 1).toEpochDay(),
        )

        val summary = ReceivableSummary(receivable = receivable, collectedMinor = 200_000L)
        assertEquals(300_000L, summary.remainingMinor)
        assertEquals("3.000 ₺", Money.format(summary.remainingMinor))
        assertFalse(summary.isSettled)

        val fully = ReceivableSummary(receivable = receivable, collectedMinor = 500_000L)
        assertEquals(0L, fully.remainingMinor)
        assertTrue(fully.isSettled)
    }

    @Test
    fun `senaryo 4 - 50000 limitli kartta 5000 harcama sonrasi kalan limit 45000`() {
        val card = CreditCardEntity(
            id = 1,
            name = "Test Kart",
            limitMinor = 5_000_000L,
            statementDay = 1,
            dueDay = 10,
        )

        val summary = CreditCardSummary(card = card, spentMinor = 500_000L, paidMinor = 0L)
        assertEquals(500_000L, summary.usedLimitMinor)
        assertEquals(4_500_000L, summary.availableLimitMinor)
        assertEquals("5.000 ₺", Money.format(summary.usedLimitMinor))
        assertEquals("45.000 ₺", Money.format(summary.availableLimitMinor))
        assertEquals(0.1f, summary.usageRatio, 0.0001f)
    }

    @Test
    fun `senaryo 4b - ekstre odemesi kullanilan limiti azaltir`() {
        val card = CreditCardEntity(
            id = 1, name = "Test Kart", limitMinor = 5_000_000L,
        )
        val afterPayment = CreditCardSummary(card, spentMinor = 500_000L, paidMinor = 200_000L)
        assertEquals(300_000L, afterPayment.usedLimitMinor)
        assertEquals(4_700_000L, afterPayment.availableLimitMinor)
    }

    @Test
    fun `kart son odeme tarihi ay gecince bir sonraki aya kayar`() {
        val card = CreditCardEntity(id = 1, name = "Kart", limitMinor = 100_000L, dueDay = 10)

        // Ayin 5'i -> bu ayin 10'u
        assertEquals(
            LocalDate.of(2026, 8, 10),
            CreditCardSummary(card, 0, 0).nextDueDate(LocalDate.of(2026, 8, 5)),
        )
        // Ayin 15'i -> gelecek ayin 10'u
        assertEquals(
            LocalDate.of(2026, 9, 10),
            CreditCardSummary(card, 0, 0).nextDueDate(LocalDate.of(2026, 8, 15)),
        )
    }

    @Test
    fun `31 gunu olmayan ayda son odeme gunu ay sonuna kirpilir`() {
        val card = CreditCardEntity(id = 1, name = "Kart", limitMinor = 100_000L, dueDay = 31)
        // Subat 2026'da 28 gun var
        assertEquals(
            LocalDate.of(2026, 2, 28),
            CreditCardSummary(card, 0, 0).nextDueDate(LocalDate.of(2026, 2, 1)),
        )
    }
}
