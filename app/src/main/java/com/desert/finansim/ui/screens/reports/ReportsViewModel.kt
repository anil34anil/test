package com.desert.finansim.ui.screens.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.desert.finansim.data.repository.DebtStatistics
import com.desert.finansim.di.AppContainer
import com.desert.finansim.domain.DateUtils
import com.desert.finansim.domain.Money
import com.desert.finansim.domain.model.CategorySpending
import com.desert.finansim.domain.model.MonthlyTotals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

data class ReportsUiState(
    val monthlyHistory: List<MonthlyTotals> = emptyList(),
    val longHistory: List<MonthlyTotals> = emptyList(),
    val categoryBreakdown: List<CategorySpending> = emptyList(),
    val debtStatistics: DebtStatistics = DebtStatistics(),
    val currencySymbol: String = Money.DEFAULT_SYMBOL,
)

@OptIn(ExperimentalCoroutinesApi::class)
class ReportsViewModel(private val container: AppContainer) : ViewModel() {

    private val _monthKey = MutableStateFlow(DateUtils.currentMonthKey())
    val monthKey: StateFlow<Int> = _monthKey.asStateFlow()

    private val breakdown = _monthKey.flatMapLatest {
        container.analyticsRepository.categoryBreakdown(it)
    }

    val uiState: StateFlow<ReportsUiState> = combine(
        container.analyticsRepository.monthlyHistory(6),
        container.analyticsRepository.monthlyHistory(12),
        breakdown,
        container.analyticsRepository.debtStatistics(),
        container.settingsRepository.currencySymbol,
    ) { history, longHistory, categories, debtStats, currency ->
        ReportsUiState(
            monthlyHistory = history,
            longHistory = longHistory,
            categoryBreakdown = categories,
            debtStatistics = debtStats,
            currencySymbol = currency,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ReportsUiState(),
    )

    fun previousMonth() {
        _monthKey.value = DateUtils.monthKey(DateUtils.yearMonthOf(_monthKey.value).minusMonths(1))
    }

    fun nextMonth() {
        _monthKey.value = DateUtils.monthKey(DateUtils.yearMonthOf(_monthKey.value).plusMonths(1))
    }
}
