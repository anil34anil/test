package com.desert.finansim.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyTest {

    @Test
    fun `tam sayilar binlik ayracla bicimlenir`() {
        assertEquals("10.000 ₺", Money.format(1_000_000L))
        assertEquals("45.000 ₺", Money.format(4_500_000L))
        assertEquals("1.250 ₺", Money.format(125_000L))
        assertEquals("800 ₺", Money.format(80_000L))
        assertEquals("0 ₺", Money.format(0L))
    }

    @Test
    fun `kurus varsa ondalik gosterilir`() {
        assertEquals("450,75 ₺", Money.format(45_075L))
        assertEquals("1.250,05 ₺", Money.format(125_005L))
    }

    @Test
    fun `negatif tutarlar isaretini korur`() {
        assertEquals("-1.250 ₺", Money.format(-125_000L))
    }

    @Test
    fun `sembolsuz bicimleme calisir`() {
        assertEquals("10.000", Money.format(1_000_000L, withSymbol = false))
        assertEquals("10.000,00", Money.format(1_000_000L, withSymbol = false, forceDecimals = true))
    }

    @Test
    fun `kullanici girdisi kurusa cevrilir`() {
        assertEquals(1_000_000L, Money.parse("10000"))
        assertEquals(1_000_000L, Money.parse("10.000"))
        assertEquals(1_000_050L, Money.parse("10.000,50"))
        assertEquals(1_000_050L, Money.parse("10000,50"))
        // "10000,5" -> 10.000,50 TL: tek haneli ondalik "50 kurus" demektir.
        assertEquals(1_000_050L, Money.parse("10000,5"))
        assertEquals(125_000L, Money.parse("1.250"))
    }

    @Test
    fun `gecersiz girdi null doner`() {
        assertNull(Money.parse(""))
        assertNull(Money.parse("   "))
        assertNull(Money.parse("abc"))
    }

    @Test
    fun `yazarken bicimleme binlik ayraci ekler`() {
        assertEquals("10.000", Money.formatWhileTyping("10000"))
        assertEquals("1.250,75", Money.formatWhileTyping("1250,75"))
        // Ondalik ayraci en fazla bir kez, kurus en fazla iki hane.
        assertEquals("1.250,75", Money.formatWhileTyping("1250,75,9"))
        assertEquals("", Money.formatWhileTyping("abc"))
    }

    @Test
    fun `taksit bolmede kurus artigi son taksite eklenir`() {
        // 1.000,00 TL / 3 taksit -> 333,33 + 333,33 + 333,34
        val parts = Money.splitEqually(100_000L, 3)
        assertEquals(3, parts.size)
        assertEquals(33_333L, parts[0])
        assertEquals(33_333L, parts[1])
        assertEquals(33_334L, parts[2])
        // En onemlisi: toplam her zaman ana tutara esit olmali.
        assertEquals(100_000L, parts.sum())
    }

    @Test
    fun `esit bolunen taksitlerde artik olusmaz`() {
        val parts = Money.splitEqually(1_200_000L, 12)
        assertEquals(12, parts.size)
        parts.forEach { assertEquals(100_000L, it) }
        assertEquals(1_200_000L, parts.sum())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `taksit sayisi sifir olamaz`() {
        Money.splitEqually(100_000L, 0)
    }

    @Test
    fun `sanitizeAmountInput hicbir karakter eklemez sadece filtreler`() {
        // Kullanicinin yazdigi ham rakamlar hicbir gruplama karakteri almadan geri doner.
        assertEquals("400000", Money.sanitizeAmountInput("400000"))
        assertEquals("4", Money.sanitizeAmountInput("4"))
        assertEquals("40", Money.sanitizeAmountInput("40"))
        assertEquals("400", Money.sanitizeAmountInput("400"))
        assertEquals("4000", Money.sanitizeAmountInput("4000"))
        assertEquals("40000", Money.sanitizeAmountInput("40000"))
    }

    @Test
    fun `sanitizeAmountInput ondalik ayracini virgule cevirir ve tek kez birakir`() {
        assertEquals("1250,75", Money.sanitizeAmountInput("1250.75"))
        assertEquals("1250,75", Money.sanitizeAmountInput("1250,75"))
        // Ikinci ondalik ayraci yok sayilir.
        assertEquals("1250,75", Money.sanitizeAmountInput("1250,75,9"))
        // Kurus en fazla iki hane.
        assertEquals("1250,75", Money.sanitizeAmountInput("1250,759"))
    }

    @Test
    fun `sanitizeAmountInput bos ve gecersiz girdide bos doner`() {
        assertEquals("", Money.sanitizeAmountInput(""))
        assertEquals("", Money.sanitizeAmountInput("abc"))
        // Tam kisim yokken ondalik ayraci kabul edilmez.
        assertEquals("", Money.sanitizeAmountInput(","))
    }

    @Test
    fun `formatRaw gruplama noktasi icermez`() {
        assertEquals("400000", Money.formatRaw(40_000_000L))
        assertEquals("400,50", Money.formatRaw(40_050L))
        assertEquals("0", Money.formatRaw(0L))
        assertEquals("-1250", Money.formatRaw(-125_000L))
    }

    @Test
    fun `yuzde hesabi sinirlarda guvenli`() {
        assertEquals(0f, Money.percent(50L, 0L), 0.0001f)
        assertEquals(0.5f, Money.percent(50L, 100L), 0.0001f)
        // Butce asilsa bile oran 1'i gecmez (ilerleme cubugu tasmasin).
        assertEquals(1f, Money.percent(150L, 100L), 0.0001f)
    }
}
