package com.desert.finansim.data.repository

import com.desert.finansim.data.local.CategoryEntity
import com.desert.finansim.data.local.FinansimDatabase
import com.desert.finansim.data.local.InstallmentEntity
import com.desert.finansim.data.local.TransactionEntity
import com.desert.finansim.domain.DateUtils
import com.desert.finansim.domain.model.DashboardState
import com.desert.finansim.domain.model.DebtSummary
import com.desert.finansim.domain.model.MonthlyTotals
import com.desert.finansim.domain.model.PaymentMethod
import com.desert.finansim.domain.model.TransactionItem
import com.desert.finansim.domain.model.TransactionType
import com.desert.finansim.domain.model.UpcomingKind
import com.desert.finansim.domain.model.UpcomingPayment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate

/**
 * Ana ekran ve islemler listesi icin turetilmis veriyi hesaplar.
 *
 * Toplamlar SQL yerine bellekte hesaplanir: kisisel finans veri hacminde
 * (birkac bin satir) fark edilmez, karsiliginda muhasebe kurallari tek yerde
 * ve okunabilir sekilde durur.
 */
class AnalyticsRepository(
    private val db: FinansimDatabase,
    private val debtRepository: DebtRepository,
    private val settingsRepository: SettingsRepository,
) {

    private val transactionDao = db.transactionDao()
    private val categoryDao = db.categoryDao()
    private val installmentDao = db.installmentDao()

    /** Yaklasan odemelerde kac gun ileriye bakilir. */
    private val upcomingWindowDays = 30L

    // ---------------------------------------------------------------- toplamlar

    fun totalsOf(transactions: List<TransactionEntity>, monthKey: Int): MonthlyTotals {
        var income = 0L
        var expense = 0L
        var debtPayment = 0L
        for (tx in transactions) {
            when (tx.type) {
                TransactionType.INCOME -> income += tx.amountMinor
                TransactionType.EXPENSE -> expense += tx.amountMinor
                TransactionType.DEBT_PAYMENT -> debtPayment += tx.amountMinor
            }
        }
        return MonthlyTotals(monthKey, income, expense, debtPayment)
    }

    /**
     * Eldeki para.
     *
     * Kredi karti ile yapilan harcamalar nakitten DUSULMEZ; kredi kartinin
     * kendisi ayrica takip edilmedigi icin, kart ekstresi normal bir borc
     * odemesi (DEBT_PAYMENT) olarak eklendiginde nakitten cikar.
     */
    val cashOnHand: Flow<Long> = combine(
        transactionDao.observeAll(),
        settingsRepository.openingBalanceMinor,
    ) { transactions, opening ->
        var balance = opening
        for (tx in transactions) {
            when (tx.type) {
                TransactionType.INCOME -> balance += tx.amountMinor
                TransactionType.EXPENSE ->
                    if (tx.paymentMethod != PaymentMethod.CREDIT_CARD) balance -= tx.amountMinor
                TransactionType.DEBT_PAYMENT -> balance -= tx.amountMinor
            }
        }
        balance
    }

    // ---------------------------------------------------------------- dashboard

    private data class Obligations(
        val debts: List<DebtSummary>,
        val pendingInstallments: List<InstallmentEntity>,
    )

    private val obligations: Flow<Obligations> = combine(
        debtRepository.debtSummaries,
        installmentDao.observeAllPending(),
    ) { debts, installments ->
        Obligations(debts, installments)
    }

    fun dashboard(monthKey: Int): Flow<DashboardState> {
        val ym = DateUtils.yearMonthOf(monthKey)
        val monthTransactions = transactionDao.observeBetween(
            DateUtils.firstDayEpoch(ym), DateUtils.lastDayEpoch(ym)
        )

        val ledger = combine(
            monthTransactions,
            transactionDao.observeRecent(6),
            categoryDao.observeAll(),
        ) { monthTx, recent, categories -> Triple(monthTx, recent, categories) }

        return combine(ledger, obligations, cashOnHand) { (monthTx, recent, categories), obl, cash ->
            val today = DateUtils.today()
            val totals = totalsOf(monthTx, monthKey)

            val debtRemaining = obl.debts
                .filter { !it.debt.isClosed }
                .sumOf { it.remainingMinor }

            // Bu ay vadesi gelen (henuz odenmemis) taksitler
            val dueThisMonth = obl.pendingInstallments
                .filter { DateUtils.monthKey(DateUtils.fromEpochDay(it.dueDate)) == monthKey }
                .sumOf { it.amountMinor }

            val upcoming = buildUpcoming(obl, today)

            DashboardState(
                monthKey = monthKey,
                totals = totals,
                cashOnHandMinor = cash,
                totalDebtRemainingMinor = debtRemaining,
                debtDueThisMonthMinor = dueThisMonth,
                upcoming = upcoming,
                recentTransactions = resolveItems(recent, categories, obl),
                isLoading = false,
            )
        }
    }

    private fun buildUpcoming(obl: Obligations, today: LocalDate): List<UpcomingPayment> {
        val horizon = today.plusDays(upcomingWindowDays)
        val result = mutableListOf<UpcomingPayment>()
        val debtsById = obl.debts.associateBy { it.debt.id }

        // 1) Odenmemis taksitler (gecikmisler dahil)
        obl.pendingInstallments.forEach { installment ->
            val due = DateUtils.fromEpochDay(installment.dueDate)
            if (due.isAfter(horizon)) return@forEach
            val debt = debtsById[installment.debtId] ?: return@forEach
            result += UpcomingPayment(
                key = "inst-${installment.id}",
                title = debt.debt.name,
                subtitle = "${installment.number}/${installment.totalCount} taksit",
                amountMinor = installment.amountMinor,
                date = due,
                kind = UpcomingKind.INSTALLMENT,
                installmentId = installment.id,
                debtId = debt.debt.id,
            )
        }

        // 2) Taksitsiz, son odeme tarihi olan acik borclar
        obl.debts.forEach { summary ->
            if (summary.debt.isClosed || summary.isSettled) return@forEach
            // Taksitli borclar zaten yukarida taksit taksit listelendi.
            if (obl.pendingInstallments.any { it.debtId == summary.debt.id }) return@forEach
            val due = summary.debt.dueDate?.let { DateUtils.fromEpochDay(it) } ?: return@forEach
            if (due.isAfter(horizon)) return@forEach
            result += UpcomingPayment(
                key = "debt-${summary.debt.id}",
                title = summary.debt.name,
                subtitle = summary.debt.type.label,
                amountMinor = summary.remainingMinor,
                date = due,
                kind = UpcomingKind.DEBT,
                debtId = summary.debt.id,
            )
        }

        return result.sortedWith(compareBy({ it.date }, { -it.amountMinor }))
    }

    private fun resolveItems(
        transactions: List<TransactionEntity>,
        categories: List<CategoryEntity>,
        obl: Obligations,
    ): List<TransactionItem> {
        val categoriesById = categories.associateBy { it.id }
        val debtsById = obl.debts.associate { it.debt.id to it.debt.name }
        return transactions.map { tx ->
            TransactionItem(
                transaction = tx,
                category = tx.categoryId?.let { categoriesById[it] },
                debtName = tx.debtId?.let { debtsById[it] },
            )
        }
    }

    /** Islemler ekrani icin iliskileri cozulmus tam liste. */
    fun transactionItems(): Flow<List<TransactionItem>> =
        combine(transactionDao.observeAll(), categoryDao.observeAll(), obligations) { txs, cats, obl ->
            resolveItems(txs, cats, obl)
        }
}
