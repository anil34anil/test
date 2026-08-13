package com.desert.finansim.domain

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Tum parasal degerler Long olarak "kurus" (minor unit) cinsinden tutulur.
 * Double kullanilmaz; finansal toplamlarda yuvarlama hatasi olusmasin diye.
 *
 *   1.250,75 TL  ->  125075L
 */
object Money {

    const val SCALE = 100L
    const val DEFAULT_SYMBOL = "₺"

    private const val GROUP_SEPARATOR = '.'
    private const val DECIMAL_SEPARATOR = ','

    /**
     * Turk Lirasi formati. Kurus 0 ise ondalik gosterilmez:
     *   1000000L -> "10.000 ₺"     45075L -> "450,75 ₺"
     */
    fun format(
        minor: Long,
        symbol: String = DEFAULT_SYMBOL,
        withSymbol: Boolean = true,
        forceDecimals: Boolean = false,
    ): String {
        val negative = minor < 0
        val abs = if (minor == Long.MIN_VALUE) Long.MAX_VALUE else kotlin.math.abs(minor)

        val whole = abs / SCALE
        val cents = (abs % SCALE).toInt()

        val sb = StringBuilder()
        if (negative) sb.append('-')
        sb.append(groupDigits(whole))
        if (forceDecimals || cents != 0) {
            sb.append(DECIMAL_SEPARATOR)
            sb.append(cents.toString().padStart(2, '0'))
        }
        if (withSymbol) {
            sb.append(' ')
            sb.append(symbol)
        }
        return sb.toString()
    }

    /** Isaretli gosterim: gelir "+", gider "-". Listelerde kullanilir. */
    fun formatSigned(minor: Long, cashIn: Boolean, symbol: String = DEFAULT_SYMBOL): String {
        val prefix = if (cashIn) "+" else "-"
        return prefix + format(kotlin.math.abs(minor), symbol)
    }

    /** Binlik ayraci ekler: 10000 -> "10.000" */
    fun groupDigits(value: Long): String {
        val raw = value.toString()
        if (raw.length <= 3) return raw
        val sb = StringBuilder()
        var count = 0
        for (i in raw.lastIndex downTo 0) {
            sb.append(raw[i])
            count++
            if (count % 3 == 0 && i != 0) sb.append(GROUP_SEPARATOR)
        }
        return sb.reverse().toString()
    }

    /**
     * Kullanici girdisini kurusa cevirir. Gecersiz/bos girdide null doner
     * (cagiran taraf dogrulama mesajini gosterir).
     *
     * Kabul edilen bicimler: "10000", "10.000", "10.000,50", "10000,5", "10000.50"
     */
    fun parse(input: String): Long? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null

        // Ayraclari normalize et: son ayrac ondalik, digerleri binlik kabul edilir.
        val lastComma = trimmed.lastIndexOf(',')
        val lastDot = trimmed.lastIndexOf('.')
        val decimalIndex = maxOf(lastComma, lastDot)

        val wholePart: String
        val decimalPart: String
        if (decimalIndex >= 0) {
            val tail = trimmed.substring(decimalIndex + 1)
            // "1.250" gibi girdilerde nokta binlik ayracidir, ondalik degil.
            if (tail.length == 3 && trimmed[decimalIndex] == GROUP_SEPARATOR) {
                wholePart = trimmed.filter { it.isDigit() }
                decimalPart = ""
            } else {
                wholePart = trimmed.substring(0, decimalIndex).filter { it.isDigit() }
                decimalPart = tail.filter { it.isDigit() }
            }
        } else {
            wholePart = trimmed.filter { it.isDigit() }
            decimalPart = ""
        }

        if (wholePart.isEmpty() && decimalPart.isEmpty()) return null
        if (wholePart.length > 15) return null // tasma korumasi

        val whole = if (wholePart.isEmpty()) 0L else wholePart.toLongOrNull() ?: return null
        val cents = when {
            decimalPart.isEmpty() -> 0L
            decimalPart.length == 1 -> decimalPart.toLong() * 10
            else -> decimalPart.substring(0, 2).toLong()
        }

        val negative = trimmed.startsWith("-")
        val result = whole * SCALE + cents
        return if (negative) -result else result
    }

    /**
     * Tutar alanina yazilirken canli bicimlendirme.
     * Sadece rakam ve tek bir ondalik ayraci birakir, tam kismi gruplar.
     */
    fun formatWhileTyping(raw: String): String {
        val cleaned = StringBuilder()
        var decimalSeen = false
        for (ch in raw) {
            when {
                ch.isDigit() -> cleaned.append(ch)
                (ch == ',' || ch == '.') && !decimalSeen && cleaned.isNotEmpty() -> {
                    decimalSeen = true
                    cleaned.append(DECIMAL_SEPARATOR)
                }
            }
        }
        val text = cleaned.toString()
        if (text.isEmpty()) return ""

        val sepIndex = text.indexOf(DECIMAL_SEPARATOR)
        return if (sepIndex < 0) {
            groupDigits(text.trimStart('0').ifEmpty { "0" }.toLongOrNull() ?: return text)
        } else {
            val whole = text.substring(0, sepIndex).trimStart('0').ifEmpty { "0" }
            val dec = text.substring(sepIndex + 1).take(2)
            val groupedWhole = whole.toLongOrNull()?.let { groupDigits(it) } ?: whole
            "$groupedWhole$DECIMAL_SEPARATOR$dec"
        }
    }

    /**
     * Duzenlenebilir tutar alaninin "ham" (gruplama noktasi ICERMEYEN) metni.
     * Mevcut bir kaydi duzenlerken alanin baslangic degerini doldurmak icin
     * kullanilir; boylece alanin gercek metni her zaman [sanitizeAmountInput]
     * ile ayni bicimde kalir (nokta asla iceremez).
     *
     *   40000000L -> "400000"     40050L -> "400,50"
     */
    fun formatRaw(minor: Long): String {
        val negative = minor < 0
        val abs = if (minor == Long.MIN_VALUE) Long.MAX_VALUE else kotlin.math.abs(minor)
        val whole = abs / SCALE
        val cents = (abs % SCALE).toInt()

        val sb = StringBuilder()
        if (negative) sb.append('-')
        sb.append(whole)
        if (cents != 0) {
            sb.append(DECIMAL_SEPARATOR)
            sb.append(cents.toString().padStart(2, '0'))
        }
        return sb.toString()
    }

    /**
     * Tutar alaninin DUZENLENEBILIR metnini temizler: sadece rakam ve en fazla
     * bir ondalik ayraci birakir, BASKA HICBIR KARAKTER EKLEMEZ (nokta gibi
     * gruplama karakterleri dahil). Klavyenin (IME) gordugu asil metin boylece
     * her zaman kullanicinin yazdigi karakterlerin bir alt kumesi olur; ekrandaki
     * gruplama (bin ayraci) sadece [AmountVisualTransformation] ile GORSEL olarak
     * eklenir, duzenlenebilir metne asla yazilmaz.
     *
     * Bu ayrim onemli: eski tasarimda her tus vurusunda bicimlendirilmis metin
     * dogrudan TextField'e geri yaziliyordu, bu da bazi Android klavyelerinde
     * (composing-region tutan IME'lerde) senkron yeniden yazmanin klavyenin kendi
     * arabellegiyle cakismasina ve yanlis/eksik karakter eklenmesine yol aciyordu.
     */
    fun sanitizeAmountInput(raw: String): String {
        val wholeBuilder = StringBuilder()
        val decimalBuilder = StringBuilder()
        var decimalSeen = false
        for (ch in raw) {
            when {
                ch.isDigit() -> {
                    if (decimalSeen) {
                        if (decimalBuilder.length < 2) decimalBuilder.append(ch)
                    } else {
                        if (wholeBuilder.length < 15) wholeBuilder.append(ch)
                    }
                }
                (ch == ',' || ch == '.') && !decimalSeen && wholeBuilder.isNotEmpty() -> {
                    decimalSeen = true
                }
            }
        }
        return if (decimalSeen) "$wholeBuilder$DECIMAL_SEPARATOR$decimalBuilder" else wholeBuilder.toString()
    }

    /** Yuzde hesabi; taksit/butce oranlarinda kullanilir. Payda 0 ise 0 doner. */
    fun percent(part: Long, total: Long): Float {
        if (total <= 0L) return 0f
        return (part.toDouble() / total.toDouble()).toFloat().coerceIn(0f, 1f)
    }

    /**
     * Bir tutari n esit taksite boler; kurus artigi SON taksite eklenir.
     * Boylece taksitlerin toplami her zaman ana tutara esittir.
     */
    fun splitEqually(total: Long, count: Int): List<Long> {
        require(count > 0) { "Taksit sayısı 0 olamaz" }
        val base = total / count
        val remainder = total - base * count
        return List(count) { index ->
            if (index == count - 1) base + remainder else base
        }
    }

    /** Yillik faiz orani ile basit aylik faiz tutari (bilgilendirme amacli). */
    fun monthlyInterest(principal: Long, annualRatePercent: Double): Long {
        if (annualRatePercent <= 0.0 || principal <= 0L) return 0L
        return BigDecimal(principal)
            .multiply(BigDecimal(annualRatePercent))
            .divide(BigDecimal(1200), 0, RoundingMode.HALF_UP)
            .toLong()
    }
}
