package com.desert.finansim.ui.screens.debts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.desert.finansim.di.AppContainer
import com.desert.finansim.domain.Money
import com.desert.finansim.domain.model.CreditCardSummary
import com.desert.finansim.domain.model.DebtSummary
import com.desert.finansim.domain.model.ReceivableSummary
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class DebtsUiState(
    val debts: List<DebtSummary> = emptyList(),
    val receivables: List<ReceivableSummary> = emptyList(),
    val cards: List<CreditCardSummary> = emptyList(),
    val currencySymbol: String = Money.DEFAULT_SYMBOL,
) {
    val totalDebtRemaining: Long
        get() = debts.filter { !it.debt.isClosed }.sumOf { it.remainingMinor }
    val totalReceivableRemaining: Long
        get() = receivables.filter { !it.receivable.isClosed }.sumOf { it.remainingMinor }
    val totalCardUsage: Long get() = cards.sumOf { it.usedLimitMinor }
}

class DebtsViewModel(container: AppContainer) : ViewModel() {

    val uiState: StateFlow<DebtsUiState> = combine(
        container.debtRepository.debtSummaries,
        container.receivableRepository.summaries,
        container.creditCardRepository.activeCards,
        container.creditCardRepository.cardTotals,
        container.settingsRepository.currencySymbol,
    ) { debts, receivables, cards, totals, currency ->
        val (spent, paid) = totals
        DebtsUiState(
            debts = debts,
            receivables = receivables,
            cards = cards.map {
                CreditCardSummary(
                    card = it,
                    spentMinor = spent[it.id] ?: 0L,
                    paidMinor = paid[it.id] ?: 0L,
                )
            },
            currencySymbol = currency,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DebtsUiState(),
    )
}
