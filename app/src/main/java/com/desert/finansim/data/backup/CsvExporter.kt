package com.desert.finansim.data.backup

import com.desert.finansim.data.local.FinansimDatabase
import com.desert.finansim.domain.DateUtils
import com.desert.finansim.domain.Money
import com.desert.finansim.domain.model.TransactionType
import kotlinx.coroutines.flow.first

/**
 * Islemleri CSV olarak disari aktarir.
 *
 * Turkiye'de Excel varsayilan olarak noktali virgul ayracini ve ondalik
 * virgulu bekledigi icin ayrac ";" secildi. Dosyanin basina UTF-8 BOM
 * eklenir, aksi halde Excel Turkce karakterleri bozuk gosterir.
 */
class CsvExporter(private val db: FinansimDatabase) {

    private companion object {
        const val SEPARATOR = ';'
        const val BOM = "\uFEFF"
    }

    suspend fun exportTransactions(): String {
        val transactions = db.transactionDao().getAllOnce().sortedByDescending { it.date }
        val categories = db.categoryDao().observeAll().first().associateBy { it.id }
        val cards = db.creditCardDao().getAllOnce().associateBy { it.id }
        val debts = db.debtDao().getAllOnce().associateBy { it.id }
        val receivables = db.receivableDao().getAllOnce().associateBy { it.id }
        val installments = db.installmentDao().getAllOnce().associateBy { it.id }

        val builder = StringBuilder(BOM)
        builder.appendRow(
            listOf(
                "Tarih", "İşlem Tipi", "Açıklama", "Kategori",
                "Tutar", "Ödeme Yöntemi", "Durum",
            )
        )

        transactions.forEach { tx ->
            val date = DateUtils.fromEpochDay(tx.date)
            val category = tx.categoryId?.let { categories[it]?.name }
                ?: tx.debtId?.let { debts[it]?.name }
                ?: tx.receivableId?.let { receivables[it]?.personName }
                ?: "-"

            val paymentMethod = when {
                tx.creditCardId != null -> cards[tx.creditCardId]?.name ?: tx.paymentMethod.label
                else -> tx.paymentMethod.label
            }

            val status = when (tx.type) {
                TransactionType.DEBT_PAYMENT -> {
                    val installment = tx.installmentId?.let { installments[it] }
                    if (installment != null) {
                        "Ödendi (${installment.number}/${installment.totalCount})"
                    } else {
                        "Ödendi"
                    }
                }
                TransactionType.RECEIVABLE_COLLECTION -> "Tahsil edildi"
                else -> "Gerçekleşti"
            }

            // Tutarin isareti nakit yonunu gosterir; ondalik ayraci virgul.
            val signedAmount = (if (tx.type.isCashIn) "" else "-") +
                Money.format(tx.amountMinor, withSymbol = false, forceDecimals = true)

            builder.appendRow(
                listOf(
                    DateUtils.formatFull(date),
                    tx.type.label,
                    tx.title.ifBlank { "-" } + if (tx.note.isBlank()) "" else " — ${tx.note}",
                    category,
                    signedAmount,
                    paymentMethod,
                    status,
                )
            )
        }
        return builder.toString()
    }

    fun suggestedFileName(): String {
        val today = DateUtils.today()
        return "finansim-islemler-%04d%02d%02d.csv".format(today.year, today.monthValue, today.dayOfMonth)
    }

    private fun StringBuilder.appendRow(values: List<String>) {
        values.forEachIndexed { index, value ->
            if (index > 0) append(SEPARATOR)
            append(escape(value))
        }
        append("\r\n")
    }

    /** Ayrac, tirnak veya satir sonu iceren degerleri tirnak icine alir. */
    private fun escape(value: String): String {
        val needsQuotes = value.any { it == SEPARATOR || it == '"' || it == '\n' || it == '\r' }
        val escaped = value.replace("\"", "\"\"")
        return if (needsQuotes) "\"$escaped\"" else escaped
    }
}
