package com.desert.finansim.data.repository

import androidx.room.withTransaction
import com.desert.finansim.data.local.CategoryEntity
import com.desert.finansim.data.local.DebtEntity
import com.desert.finansim.data.local.FinansimDatabase
import com.desert.finansim.data.local.InstallmentEntity
import com.desert.finansim.data.local.TransactionEntity
import com.desert.finansim.domain.DateUtils
import com.desert.finansim.domain.Money
import com.desert.finansim.domain.model.CategoryKind
import com.desert.finansim.domain.model.DebtSummary
import com.desert.finansim.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.YearMonth

class CategoryRepository(private val db: FinansimDatabase) {

    private val dao = db.categoryDao()

    val activeCategories: Flow<List<CategoryEntity>> = dao.observeActive()
    val allCategories: Flow<List<CategoryEntity>> = dao.observeAll()

    fun categoriesOfKind(kind: CategoryKind): Flow<List<CategoryEntity>> =
        dao.observeActive().map { list -> list.filter { it.kind == kind } }

    suspend fun getById(id: Long): CategoryEntity? = dao.getById(id)

    suspend fun add(category: CategoryEntity): Long = dao.insert(category)

    suspend fun update(category: CategoryEntity) = dao.update(category)

    /** Kategori silinmez, arsivlenir: gecmis islemlerin kategorisi kaybolmasin. */
    suspend fun archive(category: CategoryEntity) = dao.update(category.copy(isArchived = true))

    suspend fun restore(category: CategoryEntity) = dao.update(category.copy(isArchived = false))
}

class TransactionRepository(private val db: FinansimDatabase) {

    private val dao = db.transactionDao()

    val allTransactions: Flow<List<TransactionEntity>> = dao.observeAll()

    fun recent(limit: Int): Flow<List<TransactionEntity>> = dao.observeRecent(limit)

    fun between(start: LocalDate, end: LocalDate): Flow<List<TransactionEntity>> =
        dao.observeBetween(start.toEpochDay(), end.toEpochDay())

    fun forMonth(monthKey: Int): Flow<List<TransactionEntity>> {
        val ym = DateUtils.yearMonthOf(monthKey)
        return dao.observeBetween(DateUtils.firstDayEpoch(ym), DateUtils.lastDayEpoch(ym))
    }

    suspend fun getById(id: Long): TransactionEntity? = dao.getById(id)

    /**
     * Gelir/gider kaydeder. Tutar her zaman pozitif saklanir; yonu tur belirler.
     * Gecersiz tutar cagiran katmanda engellenir, burada son bir guvenlik kontrolu var.
     */
    suspend fun save(transaction: TransactionEntity): Long {
        require(transaction.amountMinor > 0) { "Tutar sıfırdan büyük olmalı" }
        val normalized = transaction.copy(
            amountMinor = kotlin.math.abs(transaction.amountMinor),
            createdAt = if (transaction.createdAt == 0L) System.currentTimeMillis() else transaction.createdAt,
        )
        return if (normalized.id == 0L) {
            dao.insert(normalized)
        } else {
            dao.update(normalized)
            normalized.id
        }
    }

    suspend fun delete(transaction: TransactionEntity) = dao.delete(transaction)

    suspend fun deleteById(id: Long) = dao.deleteById(id)
}

class DebtRepository(private val db: FinansimDatabase) {

    private val debtDao = db.debtDao()
    private val installmentDao = db.installmentDao()
    private val transactionDao = db.transactionDao()

    val allDebts: Flow<List<DebtEntity>> = debtDao.observeAll()

    /** Borclar + her birine odenen tutar birlikte. */
    val debtSummaries: Flow<List<DebtSummary>> = combine(
        debtDao.observeAll(),
        transactionDao.observeTotalsByDebt(TransactionType.DEBT_PAYMENT),
    ) { debts, totals ->
        val paidById = totals.associate { it.id to it.total }
        debts.map { DebtSummary(debt = it, paidMinor = paidById[it.id] ?: 0L) }
    }

    fun observeDebt(id: Long): Flow<DebtEntity?> = debtDao.observeById(id)

    fun installmentsFor(debtId: Long): Flow<List<InstallmentEntity>> =
        installmentDao.observeForDebt(debtId)

    fun pendingInstallments(): Flow<List<InstallmentEntity>> = installmentDao.observeAllPending()

    suspend fun getDebt(id: Long): DebtEntity? = debtDao.getById(id)

    suspend fun paidAmount(debtId: Long): Long =
        transactionDao.totalForDebt(debtId, TransactionType.DEBT_PAYMENT)

    /**
     * Borcu ve (istenirse) taksit planini birlikte olusturur.
     *
     * Taksitler [Money.splitEqually] ile bolunur: kurus artigi son taksite
     * eklenir, boylece taksitlerin toplami her zaman toplam borca esittir.
     *
     * ONEMLI: Borc olusturmak bir para hareketi DEGILDIR; burada hicbir
     * [TransactionEntity] uretilmez.
     */
    suspend fun createDebt(
        debt: DebtEntity,
        installmentCount: Int?,
        firstInstallmentDate: LocalDate?,
    ): Long = db.withTransaction {
        val count = installmentCount ?: 0
        var stored = debt.copy(createdAt = System.currentTimeMillis())

        if (count > 0) {
            val firstDue = firstInstallmentDate
                ?: DateUtils.fromEpochDay(debt.startDate).plusMonths(1)
            val lastDue = shiftMonths(firstDue, count - 1)
            stored = stored.copy(
                installmentCount = count,
                endDate = lastDue.toEpochDay(),
                monthlyPaymentMinor = debt.totalAmountMinor / count,
            )
        }

        val debtId = debtDao.insert(stored)

        if (count > 0) {
            val firstDue = firstInstallmentDate
                ?: DateUtils.fromEpochDay(debt.startDate).plusMonths(1)
            val amounts = Money.splitEqually(debt.totalAmountMinor, count)
            val installments = amounts.mapIndexed { index, amount ->
                InstallmentEntity(
                    debtId = debtId,
                    number = index + 1,
                    totalCount = count,
                    amountMinor = amount,
                    dueDate = shiftMonths(firstDue, index).toEpochDay(),
                )
            }
            installmentDao.insertAll(installments)
        }
        debtId
    }

    /** Ayin gununu koruyarak ay ekler; 31'i olmayan aylarda son gune kirpar. */
    private fun shiftMonths(from: LocalDate, months: Int): LocalDate {
        val target = YearMonth.from(from).plusMonths(months.toLong())
        return DateUtils.safeDayOfMonth(target, from.dayOfMonth)
    }

    suspend fun updateDebt(debt: DebtEntity) = debtDao.update(debt)

    suspend fun deleteDebt(debt: DebtEntity) = db.withTransaction {
        // Taksitler ve bagli odeme islemleri FK CASCADE ile birlikte silinir.
        debtDao.delete(debt)
    }

    suspend fun setClosed(debtId: Long, closed: Boolean) = debtDao.setClosed(debtId, closed)

    /**
     * Taksiti "odendi" isaretler ve karsiliginda gercek bir odeme islemi olusturur.
     * Iki islem tek veritabani transaction'inda yapilir: biri basarili digeri
     * basarisiz olup tutarsizlik olusamaz.
     */
    suspend fun markInstallmentPaid(
        installment: InstallmentEntity,
        paidDate: LocalDate = DateUtils.today(),
    ) = db.withTransaction {
        if (installment.isPaid) return@withTransaction

        val debt = debtDao.getById(installment.debtId) ?: return@withTransaction

        transactionDao.insert(
            TransactionEntity(
                type = TransactionType.DEBT_PAYMENT,
                amountMinor = installment.amountMinor,
                date = paidDate.toEpochDay(),
                title = "${debt.name} • ${installment.number}/${installment.totalCount}",
                debtId = debt.id,
                installmentId = installment.id,
                createdAt = System.currentTimeMillis(),
            )
        )
        installmentDao.update(
            installment.copy(isPaid = true, paidDate = paidDate.toEpochDay())
        )
        // Butun taksitler odendiyse borcu kapat.
        val remaining = installmentDao.getForDebtOnce(debt.id).count { !it.isPaid }
        if (remaining == 0) {
            debtDao.setClosed(debt.id, true)
        }
    }

    /** Isaretlemeyi geri alir ve olusturulan odeme islemini siler. */
    suspend fun unmarkInstallmentPaid(installment: InstallmentEntity) = db.withTransaction {
        if (!installment.isPaid) return@withTransaction
        transactionDao.deleteByInstallmentId(installment.id)
        installmentDao.update(installment.copy(isPaid = false, paidDate = null))
        debtDao.setClosed(installment.debtId, false)
    }

    /** Taksit disinda serbest borc odemesi (ornegin erken kapama). */
    suspend fun addPayment(
        debtId: Long,
        amountMinor: Long,
        date: LocalDate,
        note: String = "",
    ) = db.withTransaction {
        require(amountMinor > 0) { "Ödeme tutarı sıfırdan büyük olmalı" }
        val debt = debtDao.getById(debtId) ?: return@withTransaction
        transactionDao.insert(
            TransactionEntity(
                type = TransactionType.DEBT_PAYMENT,
                amountMinor = amountMinor,
                date = date.toEpochDay(),
                title = debt.name,
                note = note,
                debtId = debtId,
                createdAt = System.currentTimeMillis(),
            )
        )
        val paid = transactionDao.totalForDebt(debtId, TransactionType.DEBT_PAYMENT)
        if (paid >= debt.totalAmountMinor) {
            debtDao.setClosed(debtId, true)
        }
    }
}
