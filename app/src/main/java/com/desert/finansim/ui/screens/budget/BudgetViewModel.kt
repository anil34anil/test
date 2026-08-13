package com.desert.finansim.ui.screens.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.desert.finansim.data.local.CategoryEntity
import com.desert.finansim.di.AppContainer
import com.desert.finansim.domain.DateUtils
import com.desert.finansim.domain.Money
import com.desert.finansim.domain.model.CategoryKind
import com.desert.finansim.domain.model.TransactionType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BudgetRow(
    val category: CategoryEntity,
    val budgetMinor: Long,
    val spentMinor: Long,
)

data class BudgetUiState(
    val rows: List<BudgetRow> = emptyList(),
    val currencySymbol: String = Money.DEFAULT_SYMBOL,
) {
    val totalBudget: Long get() = rows.sumOf { it.budgetMinor }
    val totalSpent: Long get() = rows.sumOf { it.spentMinor }
}

@OptIn(ExperimentalCoroutinesApi::class)
class BudgetViewModel(private val container: AppContainer) : ViewModel() {

    private val _monthKey = MutableStateFlow(DateUtils.currentMonthKey())
    val monthKey: StateFlow<Int> = _monthKey.asStateFlow()

    val uiState: StateFlow<BudgetUiState> = _monthKey.flatMapLatest { month ->
        val yearMonth = DateUtils.yearMonthOf(month)
        combine(
            container.categoryRepository.activeCategories,
            container.budgetRepository.forMonth(month),
            container.database.transactionDao().observeBetween(
                DateUtils.firstDayEpoch(yearMonth),
                DateUtils.lastDayEpoch(yearMonth),
            ),
            container.settingsRepository.currencySymbol,
        ) { categories, budgets, transactions, currency ->
            val budgetByCategory = budgets.associate { it.categoryId to it.amountMinor }
            val spentByCategory = transactions
                .filter { it.type == TransactionType.EXPENSE && it.categoryId != null }
                .groupBy { it.categoryId!! }
                .mapValues { entry -> entry.value.sumOf { it.amountMinor } }

            BudgetUiState(
                rows = categories
                    .filter { it.kind == CategoryKind.EXPENSE }
                    .map { category ->
                        BudgetRow(
                            category = category,
                            budgetMinor = budgetByCategory[category.id] ?: 0L,
                            spentMinor = spentByCategory[category.id] ?: 0L,
                        )
                    }
                    // Butcesi olanlar ve harcama yapilanlar once gorunsun.
                    .sortedWith(
                        compareByDescending<BudgetRow> { it.budgetMinor > 0 }
                            .thenByDescending { it.spentMinor }
                    ),
                currencySymbol = currency,
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BudgetUiState(),
    )

    fun previousMonth() {
        _monthKey.value = DateUtils.monthKey(DateUtils.yearMonthOf(_monthKey.value).minusMonths(1))
    }

    fun nextMonth() {
        _monthKey.value = DateUtils.monthKey(DateUtils.yearMonthOf(_monthKey.value).plusMonths(1))
    }

    fun setBudget(categoryId: Long, amountText: String) {
        val amount = Money.parse(amountText) ?: return
        if (amount <= 0L) return
        viewModelScope.launch {
            container.budgetRepository.setBudget(categoryId, _monthKey.value, amount)
        }
    }

    fun removeBudget(categoryId: Long) {
        viewModelScope.launch {
            container.budgetRepository.remove(categoryId, _monthKey.value)
        }
    }

    fun copyFromPreviousMonth() {
        viewModelScope.launch {
            container.budgetRepository.copyFromPreviousMonth(_monthKey.value)
        }
    }
}
