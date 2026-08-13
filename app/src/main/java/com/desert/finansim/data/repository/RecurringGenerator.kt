package com.desert.finansim.data.repository

import androidx.room.withTransaction
import com.desert.finansim.data.local.FinansimDatabase
import com.desert.finansim.data.local.TransactionEntity
import com.desert.finansim.domain.DateUtils
import java.time.LocalDate

/**
 * Sabit gelir/giderleri gercek islemlere donusturur.
 *
 * Uygulama her acildiginda ve gunluk hatirlatma isinde calisir. Ayni donem
 * icin ikinci kez islem uretilmemesi iki katmanla garanti edilir:
 *  1. Kuralin [lastGeneratedDate] alani ilerletilir.
 *  2. Ayni kural + ayni tarih icin kayit varsa atlanir.
 */
class RecurringGenerator(private val db: FinansimDatabase) {

    suspend fun generateDue(today: LocalDate = DateUtils.today()): Int = db.withTransaction {
        val ruleDao = db.recurringRuleDao()
        val transactionDao = db.transactionDao()
        val rules = ruleDao.getActiveOnce()
        if (rules.isEmpty()) return@withTransaction 0

        val existing = transactionDao.getAllOnce()
            .filter { it.recurringRuleId != null }
            .map { it.recurringRuleId to it.date }
            .toHashSet()

        var created = 0
        rules.forEach { rule ->
            val occurrences = RecurringSchedule.pendingOccurrences(rule, today)
            if (occurrences.isEmpty()) return@forEach

            occurrences.forEach { date ->
                val key: Pair<Long?, Long> = rule.id to date.toEpochDay()
                if (existing.contains(key)) return@forEach

                transactionDao.insert(
                    TransactionEntity(
                        type = rule.type,
                        amountMinor = rule.amountMinor,
                        date = date.toEpochDay(),
                        title = rule.title,
                        note = rule.note,
                        categoryId = rule.categoryId,
                        paymentMethod = rule.paymentMethod,
                        creditCardId = rule.creditCardId,
                        recurringRuleId = rule.id,
                        createdAt = System.currentTimeMillis(),
                    )
                )
                created++
            }

            ruleDao.update(
                rule.copy(lastGeneratedDate = occurrences.max().toEpochDay())
            )
        }
        created
    }
}
