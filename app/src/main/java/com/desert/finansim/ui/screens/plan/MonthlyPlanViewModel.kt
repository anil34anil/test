package com.desert.finansim.ui.screens.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.desert.finansim.di.AppContainer
import com.desert.finansim.domain.DateUtils
import com.desert.finansim.domain.Money
import com.desert.finansim.domain.model.MonthlyPlan
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class MonthlyPlanViewModel(container: AppContainer) : ViewModel() {

    private val _monthKey = MutableStateFlow(DateUtils.currentMonthKey())
    val monthKey: StateFlow<Int> = _monthKey.asStateFlow()

    val plan: StateFlow<MonthlyPlan> = _monthKey
        .flatMapLatest { container.analyticsRepository.monthlyPlan(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MonthlyPlan(
                monthKey = DateUtils.currentMonthKey(),
                actualIncomeMinor = 0,
                actualExpenseMinor = 0,
                actualDebtPaymentMinor = 0,
                expectedIncomeMinor = 0,
                expectedExpenseMinor = 0,
                expectedDebtPaymentMinor = 0,
            ),
        )

    val currencySymbol: StateFlow<String> = container.settingsRepository.currencySymbol
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Money.DEFAULT_SYMBOL)

    fun previousMonth() {
        _monthKey.value = DateUtils.monthKey(DateUtils.yearMonthOf(_monthKey.value).minusMonths(1))
    }

    fun nextMonth() {
        _monthKey.value = DateUtils.monthKey(DateUtils.yearMonthOf(_monthKey.value).plusMonths(1))
    }
}
