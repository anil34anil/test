package com.desert.finansim.ui.screens.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.desert.finansim.data.local.CategoryEntity
import com.desert.finansim.di.AppContainer
import com.desert.finansim.domain.DateUtils
import com.desert.finansim.domain.Money
import com.desert.finansim.domain.model.TransactionItem
import com.desert.finansim.domain.model.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Islemler ekranindaki tarih araligi secenekleri. */
enum class DateRangeFilter(val label: String) {
    THIS_MONTH("Bu ay"),
    LAST_MONTH("Geçen ay"),
    LAST_3_MONTHS("Son 3 ay"),
    THIS_YEAR("Bu yıl"),
    ALL("Tümü");

    fun range(today: LocalDate = DateUtils.today()): Pair<LocalDate, LocalDate>? = when (this) {
        THIS_MONTH -> today.withDayOfMonth(1) to today.withDayOfMonth(today.lengthOfMonth())
        LAST_MONTH -> {
            val previous = today.minusMonths(1)
            previous.withDayOfMonth(1) to previous.withDayOfMonth(previous.lengthOfMonth())
        }
        LAST_3_MONTHS -> today.minusMonths(2).withDayOfMonth(1) to
            today.withDayOfMonth(today.lengthOfMonth())
        THIS_YEAR -> LocalDate.of(today.year, 1, 1) to LocalDate.of(today.year, 12, 31)
        ALL -> null
    }
}

data class TransactionFilters(
    val type: TransactionType? = null,
    val dateRange: DateRangeFilter = DateRangeFilter.THIS_MONTH,
    val categoryId: Long? = null,
    val customRange: Pair<LocalDate, LocalDate>? = null,
)

data class TransactionsUiState(
    val items: List<TransactionItem> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val filters: TransactionFilters = TransactionFilters(),
    val currencySymbol: String = Money.DEFAULT_SYMBOL,
) {
    val incomeTotal: Long
        get() = items.filter { it.transaction.type.isCashIn }.sumOf { it.transaction.amountMinor }
    val expenseTotal: Long
        get() = items.filter { !it.transaction.type.isCashIn }.sumOf { it.transaction.amountMinor }
}

class TransactionsViewModel(private val container: AppContainer) : ViewModel() {

    private val _filters = MutableStateFlow(TransactionFilters())
    val filters: StateFlow<TransactionFilters> = _filters.asStateFlow()

    val uiState: StateFlow<TransactionsUiState> = combine(
        container.analyticsRepository.transactionItems(),
        container.categoryRepository.allCategories,
        _filters,
        container.settingsRepository.currencySymbol,
    ) { items, categories, filters, currency ->
        TransactionsUiState(
            items = applyFilters(items, filters),
            categories = categories,
            filters = filters,
            currencySymbol = currency,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TransactionsUiState(),
    )

    private fun applyFilters(
        items: List<TransactionItem>,
        filters: TransactionFilters,
    ): List<TransactionItem> {
        val range = filters.customRange ?: filters.dateRange.range()
        return items.filter { item ->
            val transaction = item.transaction
            val typeMatches = filters.type == null || transaction.type == filters.type
            val categoryMatches = filters.categoryId == null ||
                transaction.categoryId == filters.categoryId
            val dateMatches = range == null || (
                !item.date.isBefore(range.first) && !item.date.isAfter(range.second)
                )
            typeMatches && categoryMatches && dateMatches
        }
    }

    fun setType(type: TransactionType?) {
        _filters.value = _filters.value.copy(type = type)
    }

    fun setDateRange(range: DateRangeFilter) {
        _filters.value = _filters.value.copy(dateRange = range, customRange = null)
    }

    fun setCustomRange(start: LocalDate, end: LocalDate) {
        val ordered = if (start.isAfter(end)) end to start else start to end
        _filters.value = _filters.value.copy(customRange = ordered)
    }

    fun setCategory(categoryId: Long?) {
        _filters.value = _filters.value.copy(categoryId = categoryId)
    }

    fun clearFilters() {
        _filters.value = TransactionFilters()
    }

    fun delete(transactionId: Long) {
        viewModelScope.launch {
            container.transactionRepository.deleteById(transactionId)
        }
    }
}
