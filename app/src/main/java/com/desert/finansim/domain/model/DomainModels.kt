package com.desert.finansim.domain.model

import com.desert.finansim.data.local.CategoryEntity
import com.desert.finansim.data.local.DebtEntity
import com.desert.finansim.data.local.InstallmentEntity
import com.desert.finansim.data.local.TransactionEntity
import com.desert.finansim.domain.DateUtils
import com.desert.finansim.domain.Money
import java.time.LocalDate

/** Liste ekranlarinda gosterilen, iliskileri cozulmus islem satiri. */
data class TransactionItem(
    val transaction: TransactionEntity,
    val category: CategoryEntity?,
    val debtName: String?,
) {
    val date: LocalDate get() = DateUtils.fromEpochDay(transaction.date)
    val displayCategory: String
        get() = category?.name ?: debtName ?: transaction.type.label
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

enum class UpcomingKind {
    INSTALLMENT, DEBT;

    val label: String
        get() = when (this) {
            INSTALLMENT -> "Taksit"
            DEBT -> "Borç"
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
) {
    /** Gelir - (gider + borc odemesi). */
    val netMinor: Long get() = incomeMinor - expenseMinor - debtPaymentMinor
}

/** Ana ekranin tek seferde ihtiyac duydugu tum veriler. */
data class DashboardState(
    val monthKey: Int = DateUtils.currentMonthKey(),
    val totals: MonthlyTotals = MonthlyTotals(DateUtils.currentMonthKey()),
    val cashOnHandMinor: Long = 0,
    val totalDebtRemainingMinor: Long = 0,
    val debtDueThisMonthMinor: Long = 0,
    val upcoming: List<UpcomingPayment> = emptyList(),
    val recentTransactions: List<TransactionItem> = emptyList(),
    val isLoading: Boolean = true,
)
