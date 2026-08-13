package com.desert.finansim.domain.model

import com.desert.finansim.data.local.CategoryEntity
import com.desert.finansim.data.local.CreditCardEntity
import com.desert.finansim.data.local.DebtEntity
import com.desert.finansim.data.local.InstallmentEntity
import com.desert.finansim.data.local.ReceivableEntity
import com.desert.finansim.data.local.TransactionEntity
import com.desert.finansim.domain.DateUtils
import com.desert.finansim.domain.Money
import java.time.LocalDate

/** Liste ekranlarinda gosterilen, iliskileri cozulmus islem satiri. */
data class TransactionItem(
    val transaction: TransactionEntity,
    val category: CategoryEntity?,
    val creditCardName: String?,
    val debtName: String?,
    val receivableName: String?,
) {
    val date: LocalDate get() = DateUtils.fromEpochDay(transaction.date)
    val displayCategory: String
        get() = category?.name
            ?: debtName
            ?: receivableName
            ?: transaction.type.label
}

/**
 * Bir borcun turetilmis ozeti.
 *
 * [paidMinor] borca yapilan DEBT_PAYMENT islemlerinin toplamidir; ayrica bir
 * "odenen" sutunu tutulmaz, boylece iki kaynak arasinda tutarsizlik olusamaz.
 */
data class DebtSummary(
    val debt: DebtEntity,
    val paidMinor: Long,
    val installments: List<InstallmentEntity> = emptyList(),
) {
    val remainingMinor: Long get() = (debt.totalAmountMinor - paidMinor).coerceAtLeast(0L)
    val progress: Float get() = Money.percent(paidMinor, debt.totalAmountMinor)
    val isSettled: Boolean get() = remainingMinor == 0L

    /** Odenmemis ilk taksit. */
    fun nextInstallment(): InstallmentEntity? =
        installments.filter { !it.isPaid }.minByOrNull { it.dueDate }

    fun statusOf(installment: InstallmentEntity, today: LocalDate = DateUtils.today()): InstallmentStatus =
        when {
            installment.isPaid -> InstallmentStatus.PAID
            DateUtils.fromEpochDay(installment.dueDate).isBefore(today) -> InstallmentStatus.OVERDUE
            else -> InstallmentStatus.PENDING
        }

    /** Sonraki odeme tarihi: taksitliyse taksit, degilse borcun son odeme tarihi. */
    fun nextDueDate(): LocalDate? =
        nextInstallment()?.let { DateUtils.fromEpochDay(it.dueDate) }
            ?: debt.dueDate?.let { DateUtils.fromEpochDay(it) }
}

data class ReceivableSummary(
    val receivable: ReceivableEntity,
    val collectedMinor: Long,
) {
    val remainingMinor: Long
        get() = (receivable.totalAmountMinor - collectedMinor).coerceAtLeast(0L)
    val progress: Float get() = Money.percent(collectedMinor, receivable.totalAmountMinor)
    val isSettled: Boolean get() = remainingMinor == 0L
}

/**
 * Kart ozeti.
 *   kullanilan limit = karta yapilan harcamalar - karta yapilan odemeler
 */
data class CreditCardSummary(
    val card: CreditCardEntity,
    val spentMinor: Long,
    val paidMinor: Long,
) {
    val usedLimitMinor: Long get() = (spentMinor - paidMinor).coerceAtLeast(0L)
    val availableLimitMinor: Long get() = (card.limitMinor - usedLimitMinor).coerceAtLeast(0L)
    val usageRatio: Float get() = Money.percent(usedLimitMinor, card.limitMinor)

    fun nextDueDate(today: LocalDate = DateUtils.today()): LocalDate {
        val thisMonth = DateUtils.safeDayOfMonth(java.time.YearMonth.from(today), card.dueDay)
        return if (thisMonth.isBefore(today)) {
            DateUtils.safeDayOfMonth(java.time.YearMonth.from(today).plusMonths(1), card.dueDay)
        } else {
            thisMonth
        }
    }
}

enum class UpcomingKind {
    INSTALLMENT, DEBT, CREDIT_CARD, RECURRING;

    val label: String
        get() = when (this) {
            INSTALLMENT -> "Taksit"
            DEBT -> "Borç"
            CREDIT_CARD -> "Kredi Kartı"
            RECURRING -> "Sabit Gider"
        }
}

data class UpcomingPayment(
    val key: String,
    val title: String,
    val subtitle: String,
    val amountMinor: Long,
    val date: LocalDate,
    val kind: UpcomingKind,
    val installmentId: Long? = null,
    val debtId: Long? = null,
    val creditCardId: Long? = null,
) {
    val isOverdue: Boolean get() = date.isBefore(DateUtils.today())
    val daysUntil: Long get() = DateUtils.daysUntil(date)

    /** 3 gun ve altinda kalanlar dashboard'da vurgulanir. */
    val isUrgent: Boolean get() = isOverdue || daysUntil <= 3
}

/** Bir ayin gerceklesmis para hareketleri. */
data class MonthlyTotals(
    val monthKey: Int,
    val incomeMinor: Long = 0,
    val expenseMinor: Long = 0,
    val debtPaymentMinor: Long = 0,
    val collectionMinor: Long = 0,
) {
    /** Gelir - (gider + borc odemesi). Tahsilat gelir sayilmaz, ayri gosterilir. */
    val netMinor: Long get() = incomeMinor - expenseMinor - debtPaymentMinor
    val cashInMinor: Long get() = incomeMinor + collectionMinor
    val cashOutMinor: Long get() = expenseMinor + debtPaymentMinor
}

data class CategorySpending(
    val categoryId: Long?,
    val name: String,
    val colorArgb: Long,
    val amountMinor: Long,
    val ratio: Float,
)

data class BudgetStatus(
    val categoryId: Long,
    val categoryName: String,
    val colorArgb: Long,
    val budgetMinor: Long,
    val spentMinor: Long,
) {
    val remainingMinor: Long get() = budgetMinor - spentMinor
    val ratio: Float get() = Money.percent(spentMinor, budgetMinor)
    val isExceeded: Boolean get() = spentMinor > budgetMinor
    /** %90 ve uzeri kullanimda uyari gosterilir. */
    val isNearLimit: Boolean get() = !isExceeded && budgetMinor > 0 && ratio >= 0.9f
}

/** Ana ekranin tek seferde ihtiyac duydugu tum veriler. */
data class DashboardState(
    val monthKey: Int = DateUtils.currentMonthKey(),
    val totals: MonthlyTotals = MonthlyTotals(DateUtils.currentMonthKey()),
    val cashOnHandMinor: Long = 0,
    val totalDebtRemainingMinor: Long = 0,
    val totalReceivableRemainingMinor: Long = 0,
    val debtDueThisMonthMinor: Long = 0,
    val upcoming: List<UpcomingPayment> = emptyList(),
    val recentTransactions: List<TransactionItem> = emptyList(),
    val budgetWarnings: List<BudgetStatus> = emptyList(),
    val isLoading: Boolean = true,
)

/**
 * Aylik plan: gerceklesen ile beklenen ayri ayri tutulur (sartname 15).
 */
data class MonthlyPlan(
    val monthKey: Int,
    val actualIncomeMinor: Long,
    val actualExpenseMinor: Long,
    val actualDebtPaymentMinor: Long,
    val expectedIncomeMinor: Long,
    val expectedExpenseMinor: Long,
    val expectedDebtPaymentMinor: Long,
) {
    val projectedIncomeMinor: Long get() = actualIncomeMinor + expectedIncomeMinor
    val projectedExpenseMinor: Long get() = actualExpenseMinor + expectedExpenseMinor
    val projectedDebtPaymentMinor: Long get() = actualDebtPaymentMinor + expectedDebtPaymentMinor
    val projectedNetMinor: Long
        get() = projectedIncomeMinor - projectedExpenseMinor - projectedDebtPaymentMinor
    val actualNetMinor: Long
        get() = actualIncomeMinor - actualExpenseMinor - actualDebtPaymentMinor
}
