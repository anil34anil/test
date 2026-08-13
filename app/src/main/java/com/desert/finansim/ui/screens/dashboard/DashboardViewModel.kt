package com.desert.finansim.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.desert.finansim.di.AppContainer
import com.desert.finansim.domain.DateUtils
import com.desert.finansim.domain.Money
import com.desert.finansim.domain.model.DashboardState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(private val container: AppContainer) : ViewModel() {

    private val _monthKey = MutableStateFlow(DateUtils.currentMonthKey())
    val monthKey: StateFlow<Int> = _monthKey.asStateFlow()

    val state: StateFlow<DashboardState> = _monthKey
        .flatMapLatest { container.analyticsRepository.dashboard(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DashboardState(),
        )

    val currencySymbol: StateFlow<String> = container.settingsRepository.currencySymbol
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Money.DEFAULT_SYMBOL)

    fun showPreviousMonth() {
        _monthKey.value = DateUtils.monthKey(
            DateUtils.yearMonthOf(_monthKey.value).minusMonths(1)
        )
    }

    fun showNextMonth() {
        _monthKey.value = DateUtils.monthKey(
            DateUtils.yearMonthOf(_monthKey.value).plusMonths(1)
        )
    }

    fun showCurrentMonth() {
        _monthKey.value = DateUtils.currentMonthKey()
    }

    /** Yaklasan odemeler listesinden taksiti dogrudan "odendi" yapar. */
    fun markInstallmentPaid(installmentId: Long) {
        viewModelScope.launch {
            val installment = container.database.installmentDao().getById(installmentId) ?: return@launch
            container.debtRepository.markInstallmentPaid(installment)
        }
    }
}
