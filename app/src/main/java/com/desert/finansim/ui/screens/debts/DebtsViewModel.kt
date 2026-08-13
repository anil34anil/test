package com.desert.finansim.ui.screens.debts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.desert.finansim.di.AppContainer
import com.desert.finansim.domain.Money
import com.desert.finansim.domain.model.DebtSummary
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class DebtsUiState(
    val debts: List<DebtSummary> = emptyList(),
    val currencySymbol: String = Money.DEFAULT_SYMBOL,
) {
    val totalDebtRemaining: Long
        get() = debts.filter { !it.debt.isClosed }.sumOf { it.remainingMinor }
}

class DebtsViewModel(container: AppContainer) : ViewModel() {

    val uiState: StateFlow<DebtsUiState> = combine(
        container.debtRepository.debtSummaries,
        container.settingsRepository.currencySymbol,
    ) { debts, currency ->
        DebtsUiState(debts = debts, currencySymbol = currency)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DebtsUiState(),
    )
}
