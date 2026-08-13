package com.desert.finansim.data.repository

import com.desert.finansim.data.local.BudgetEntity
import com.desert.finansim.data.local.CategoryEntity
import com.desert.finansim.data.local.CreditCardEntity
import com.desert.finansim.data.local.FinansimDatabase
import com.desert.finansim.data.local.InstallmentEntity
import com.desert.finansim.data.local.RecurringRuleEntity
import com.desert.finansim.data.local.TransactionEntity
import com.desert.finansim.domain.DateUtils
import com.desert.finansim.domain.Money
import com.desert.finansim.domain.model.BudgetStatus
import com.desert.finansim.domain.model.CategorySpending
import com.desert.finansim.domain.model.CreditCardSummary
import com.desert.finansim.domain.model.DashboardState
import com.desert.finansim.domain.model.DebtSummary
import com.desert.finansim.domain.model.MonthlyPlan
import com.desert.finansim.domain.model.MonthlyTotals
import com.desert.finansim.domain.model.PaymentMethod
import com.desert.finansim.domain.model.ReceivableSummary
import com.desert.finansim.domain.model.TransactionItem
import com.desert.finansim.domain.model.TransactionType
import com.desert.finansim.domain.model.UpcomingKind
import com.desert.finansim.domain.model.UpcomingPayment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/**
 * Ana ekran, raporlar ve aylik plan icin turetilmis veriyi hesaplar.
 *
 * Toplamlar SQL yerine bellekte hesaplanir: kisisel finans veri hacminde
 * (birkac bin satir) fark edilmez, karsiliginda muhasebe kurallari tek yerde
 * ve okunabilir sekilde durur.
 */
class AnalyticsRepository(
    private val db: FinansimDatabase,
    private val debtRepository: DebtRepository,
    private val receivableRepository: ReceivableRepository,
    private val creditCardRepository: CreditCardRepository,
    private val settingsRepository: SettingsRepository,
) {

    private val transactionDao = db.transactionDao()
    private val categoryDao = db.categoryDao()
    private val installmentDao = db.installmentDao()
    private val budgetDao = db.budgetDao()
    private val recurringDao = db.recurringRuleDao()

    /** Yaklasan odemelerde kac gun ileriye bakilir. */
    private val upcomingWindowDays = 30L

    // ---------------------------------------------------------------- toplamlar

    fun totalsOf(transactions: List<TransactionEntity>, monthKey: Int): MonthlyTotals {
        var income = 0L
        var expense = 0L
        var debtPayment = 0L
        var collection = 0L
        for (tx in transactions) {
            when (tx.type) {
                TransactionType.INCOME -> income += tx.amountMinor
                TransactionType.EXPENSE -> expense += tx.amountMinor
                TransactionType.DEBT_PAYMENT -> debtPayment += tx.amountMinor
                TransactionType.RECEIVABLE_COLLECTION -> collection += tx.amountMinor
            }
        }
        return MonthlyTotals(monthKey, income, expense, debtPayment, collection)
    }

    /**
     * Eldeki para.
     *
     * Kredi karti ile yapilan harcamalar nakitten DUSULMEZ; onlar kartin
     * kullanilan limitini artirir ve ancak ekstre odendiginde nakitten cikar.
     * Boylece "eldeki para" gercekten elde olan parayi gosterir.
     */
    val cashOnHand: Flow<Long> = combine(
        transactionDao.observeAll(),
        settingsRepository.openingBalanceMinor,
    ) { transactions, opening ->
        var balance = opening
        for (tx in transactions) {
            when (tx.type) {
                TransactionType.INCOME -> balance += tx.amountMinor
                TransactionType.RECEIVABLE_COLLECTION -> balance += tx.amountMinor
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
        val receivables: List<ReceivableSummary>,
        val cards: List<CreditCardSummary>,
        val pendingInstallments: List<InstallmentEntity>,
        val recurringRules: List<RecurringRuleEntity>,
    )

    private val cardSummaries: Flow<List<CreditCardSummary>> = combine(
        creditCardRepository.activeCards,
        creditCardRepository.cardTotals,
    ) { cards, (spent, paid) ->
        cards.map {
            CreditCardSummary(
                card = it,
                spentMinor = spent[it.id] ?: 0L,
                paidMinor = paid[it.id] ?: 0L,
            )
        }
    }

    private val obligations: Flow<Obligations> = combine(
        debtRepository.debtSummaries,
        receivableRepository.summaries,
        cardSummaries,
        installmentDao.observeAllPending(),
        recurringDao.observeAll(),
    ) { debts, receivables, cards, installments, rules ->
        Obligations(debts, receivables, cards, installments, rules)
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

        val planning = combine(
            budgetDao.observeForMonth(monthKey),
            cashOnHand,
        ) { budgets, cash -> budgets to cash }

        return combine(ledger, obligations, planning) { (monthTx, recent, categories), obl, (budgets, cash) ->
            val today = DateUtils.today()
            val totals = totalsOf(monthTx, monthKey)

            val debtRemaining = obl.debts
                .filter { !it.debt.isClosed }
                .sumOf { it.remainingMinor }
            val receivableRemaining = obl.receivables
                .filter { !it.receivable.isClosed }
                .sumOf { it.remainingMinor }

            // Bu ay vadesi gelen (henuz odenmemis) taksitler
            val dueThisMonth = obl.pendingInstallments
                .filter { DateUtils.monthKey(DateUtils.fromEpochDay(it.dueDate)) == monthKey }
                .sumOf { it.amountMinor }

            val upcoming = buildUpcoming(obl, today)

            val budgetStatuses = buildBudgetStatuses(budgets, categories, monthTx)

            DashboardState(
                monthKey = monthKey,
                totals = totals,
                cashOnHandMinor = cash,
                totalDebtRemainingMinor = debtRemaining,
                totalReceivableRemainingMinor = receivableRemaining,
                debtDueThisMonthMinor = dueThisMonth,
                upcoming = upcoming,
                recentTransactions = resolveItems(recent, categories, obl),
                budgetWarnings = budgetStatuses.filter { it.isExceeded || it.isNearLimit },
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

        // 3) Kredi kartlari: borcu olanlarin son odeme tarihi
        obl.cards.forEach { card ->
            if (card.usedLimitMinor <= 0L) return@forEach
            val due = card.nextDueDate(today)
            if (due.isAfter(horizon)) return@forEach
            result += UpcomingPayment(
                key = "card-${card.card.id}",
                title = card.card.name,
                subtitle = "Son ödeme",
                amountMinor = card.usedLimitMinor,
                date = due,
                kind = UpcomingKind.CREDIT_CARD,
                creditCardId = card.card.id,
            )
        }

        // 4) Yaklasan sabit giderler
        obl.recurringRules.forEach { rule ->
            if (!rule.isActive || rule.type != TransactionType.EXPENSE) return@forEach
            val next = RecurringSchedule.nextOccurrence(rule, today) ?: return@forEach
            if (next.isAfter(horizon)) return@forEach
            result += UpcomingPayment(
                key = "rec-${rule.id}-${next.toEpochDay()}",
                title = rule.title,
                subtitle = rule.frequency.label,
                amountMinor = rule.amountMinor,
                date = next,
                kind = UpcomingKind.RECURRING,
            )
        }

        return result.sortedWith(compareBy({ it.date }, { -it.amountMinor }))
    }

    private fun buildBudgetStatuses(
        budgets: List<BudgetEntity>,
        categories: List<CategoryEntity>,
        monthTransactions: List<TransactionEntity>,
    ): List<BudgetStatus> {
        if (budgets.isEmpty()) return emptyList()
        val categoriesById = categories.associateBy { it.id }
        val spentByCategory = monthTransactions
            .filter { it.type == TransactionType.EXPENSE && it.categoryId != null }
            .groupBy { it.categoryId!! }
            .mapValues { entry -> entry.value.sumOf { it.amountMinor } }

        return budgets.mapNotNull { budget ->
            val category = categoriesById[budget.categoryId] ?: return@mapNotNull null
            BudgetStatus(
                categoryId = budget.categoryId,
                categoryName = category.name,
                colorArgb = category.colorArgb,
                budgetMinor = budget.amountMinor,
                spentMinor = spentByCategory[budget.categoryId] ?: 0L,
            )
        }.sortedByDescending { it.ratio }
    }

    private fun resolveItems(
        transactions: List<TransactionEntity>,
        categories: List<CategoryEntity>,
        obl: Obligations,
    ): List<TransactionItem> {
        val categoriesById = categories.associateBy { it.id }
        val cardsById = obl.cards.associate { it.card.id to it.card.name }
        val debtsById = obl.debts.associate { it.debt.id to it.debt.name }
        val receivablesById = obl.receivables.associate { it.receivable.id to it.receivable.personName }
        return transactions.map { tx ->
            TransactionItem(
                transaction = tx,
                category = tx.categoryId?.let { categoriesById[it] },
                creditCardName = tx.creditCardId?.let { cardsById[it] },
                debtName = tx.debtId?.let { debtsById[it] },
                receivableName = tx.receivableId?.let { receivablesById[it] },
            )
        }
    }

    /** Islemler ekrani icin iliskileri cozulmus tam liste. */
    fun transactionItems(): Flow<List<TransactionItem>> =
        combine(transactionDao.observeAll(), categoryDao.observeAll(), obligations) { txs, cats, obl ->
            resolveItems(txs, cats, obl)
        }

    // ---------------------------------------------------------------- raporlar

    /** Son [monthCount] ayin gelir/gider/net toplamlari (eskiden yeniye). */
    fun monthlyHistory(monthCount: Int): Flow<List<MonthlyTotals>> {
        val keys = DateUtils.lastMonthKeys(monthCount)
        val firstMonth = DateUtils.yearMonthOf(keys.first())
        val lastMonth = DateUtils.yearMonthOf(keys.last())
        return transactionDao.observeBetween(
            DateUtils.firstDayEpoch(firstMonth),
            DateUtils.lastDayEpoch(lastMonth),
        ).map { transactions ->
            val byMonth = transactions.groupBy {
                DateUtils.monthKey(DateUtils.fromEpochDay(it.date))
            }
            keys.map { key -> totalsOf(byMonth[key].orEmpty(), key) }
        }
    }

    /** Secilen ayin gider dagilimi (kategori bazli, buyukten kucuge). */
    fun categoryBreakdown(monthKey: Int): Flow<List<CategorySpending>> {
        val ym = DateUtils.yearMonthOf(monthKey)
        return combine(
            transactionDao.observeBetween(DateUtils.firstDayEpoch(ym), DateUtils.lastDayEpoch(ym)),
            categoryDao.observeAll(),
        ) { transactions, categories ->
            val categoriesById = categories.associateBy { it.id }
            val expenses = transactions.filter { it.type == TransactionType.EXPENSE }
            val total = expenses.sumOf { it.amountMinor }
            if (total == 0L) return@combine emptyList<CategorySpending>()

            expenses.groupBy { it.categoryId }
                .map { (categoryId, list) ->
                    val amount = list.sumOf { it.amountMinor }
                    val category = categoryId?.let { categoriesById[it] }
                    CategorySpending(
                        categoryId = categoryId,
                        name = category?.name ?: "Diğer",
                        colorArgb = category?.colorArgb ?: 0xFF7E8A97,
                        amountMinor = amount,
                        ratio = Money.percent(amount, total),
                    )
                }
                .sortedByDescending { it.amountMinor }
        }
    }

    fun budgetStatuses(monthKey: Int): Flow<List<BudgetStatus>> {
        val ym = DateUtils.yearMonthOf(monthKey)
        return combine(
            budgetDao.observeForMonth(monthKey),
            categoryDao.observeAll(),
            transactionDao.observeBetween(DateUtils.firstDayEpoch(ym), DateUtils.lastDayEpoch(ym)),
        ) { budgets, categories, transactions ->
            buildBudgetStatuses(budgets, categories, transactions)
        }
    }

    // ------------------------------------------------------------ aylik plan

    /**
     * Aylik plan: gerceklesen tutarlar islemlerden, beklenen tutarlar ise
     * heniz gerceklesmemis sabit gider/gelir ve odenmemis taksitlerden gelir.
     * Ikisi bilerek ayri tutulur (sartname 15).
     */
    fun monthlyPlan(monthKey: Int): Flow<MonthlyPlan> {
        val ym = DateUtils.yearMonthOf(monthKey)
        val start = ym.atDay(1)
        val end = ym.atEndOfMonth()

        return combine(
            transactionDao.observeBetween(start.toEpochDay(), end.toEpochDay()),
            recurringDao.observeAll(),
            installmentDao.observeAllPending(),
        ) { transactions, rules, installments ->
            val actual = totalsOf(transactions, monthKey)
            val today = DateUtils.today()

            // Bu ay icinde henuz gerceklesmemis sabit gelir/giderler
            var expectedIncome = 0L
            var expectedExpense = 0L
            rules.forEach { rule ->
                if (!rule.isActive) return@forEach
                val alreadyGenerated = transactions.any { it.recurringRuleId == rule.id }
                if (alreadyGenerated) return@forEach
                val occurrences = RecurringSchedule.occurrencesIn(rule, start, end)
                    .filter { !it.isBefore(today) }
                val amount = rule.amountMinor * occurrences.size
                when (rule.type) {
                    TransactionType.INCOME -> expectedIncome += amount
                    TransactionType.EXPENSE -> expectedExpense += amount
                    else -> Unit
                }
            }

            // Bu ay vadesi gelen, henuz odenmemis taksitler
            val expectedDebt = installments
                .filter {
                    val due = DateUtils.fromEpochDay(it.dueDate)
                    !due.isBefore(start) && !due.isAfter(end)
                }
                .sumOf { it.amountMinor }

            MonthlyPlan(
                monthKey = monthKey,
                actualIncomeMinor = actual.incomeMinor,
                actualExpenseMinor = actual.expenseMinor,
                actualDebtPaymentMinor = actual.debtPaymentMinor,
                expectedIncomeMinor = expectedIncome,
                expectedExpenseMinor = expectedExpense,
                expectedDebtPaymentMinor = expectedDebt,
            )
        }
    }

    /** Borc istatistikleri: toplam borc / odenen / kalan / aylik yuk. */
    fun debtStatistics(): Flow<DebtStatistics> = combine(
        debtRepository.debtSummaries,
        installmentDao.observeAllPending(),
    ) { debts, pending ->
        val open = debts.filter { !it.debt.isClosed }
        val monthKey = DateUtils.currentMonthKey()
        DebtStatistics(
            totalMinor = open.sumOf { it.debt.totalAmountMinor },
            paidMinor = debts.sumOf { it.paidMinor },
            remainingMinor = open.sumOf { it.remainingMinor },
            monthlyLoadMinor = pending
                .filter { DateUtils.monthKey(DateUtils.fromEpochDay(it.dueDate)) == monthKey }
                .sumOf { it.amountMinor },
            openCount = open.size,
        )
    }
}

data class DebtStatistics(
    val totalMinor: Long = 0,
    val paidMinor: Long = 0,
    val remainingMinor: Long = 0,
    val monthlyLoadMinor: Long = 0,
    val openCount: Int = 0,
)
