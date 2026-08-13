package com.desert.finansim.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.desert.finansim.data.local.RecurringRuleEntity
import com.desert.finansim.di.AppContainer
import com.desert.finansim.domain.DateUtils
import com.desert.finansim.domain.Money
import com.desert.finansim.domain.model.CategoryKind
import com.desert.finansim.domain.model.RecurrenceFrequency
import com.desert.finansim.domain.model.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class OnboardingState(
    val openingBalanceText: String = "",
    val monthlyIncomeText: String = "",
    val fixedExpenseText: String = "",
    val currencySymbol: String = Money.DEFAULT_SYMBOL,
    val isSaving: Boolean = false,
)

class OnboardingViewModel(private val container: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    fun setOpeningBalance(value: String) {
        _state.value = _state.value.copy(openingBalanceText = value)
    }

    fun setMonthlyIncome(value: String) {
        _state.value = _state.value.copy(monthlyIncomeText = value)
    }

    fun setFixedExpense(value: String) {
        _state.value = _state.value.copy(fixedExpenseText = value)
    }

    fun skip() {
        viewModelScope.launch {
            container.settingsRepository.setOnboardingCompleted(true)
        }
    }

    /**
     * Girilen alanlari kaydeder. Gelir ve sabit gider, tek seferlik islem
     * yerine "sabit kayit" olarak tanimlanir; boylece her ay otomatik islenir.
     */
    fun finish() {
        if (_state.value.isSaving) return
        _state.value = _state.value.copy(isSaving = true)

        viewModelScope.launch {
            try {
                val current = _state.value
                val today = DateUtils.today()

                Money.parse(current.openingBalanceText)?.takeIf { it > 0 }?.let { balance ->
                    container.settingsRepository.setOpeningBalance(balance)
                }

                val categories = container.categoryRepository.activeCategories.first()

                Money.parse(current.monthlyIncomeText)?.takeIf { it > 0 }?.let { income ->
                    val salaryCategory = categories
                        .firstOrNull { it.kind == CategoryKind.INCOME && it.name == "Maaş" }
                        ?: categories.firstOrNull { it.kind == CategoryKind.INCOME }
                    container.recurringRepository.save(
                        RecurringRuleEntity(
                            title = "Maaş",
                            type = TransactionType.INCOME,
                            amountMinor = income,
                            categoryId = salaryCategory?.id,
                            frequency = RecurrenceFrequency.MONTHLY,
                            dayOfMonth = today.dayOfMonth,
                            startDate = today.toEpochDay(),
                        )
                    )
                }

                Money.parse(current.fixedExpenseText)?.takeIf { it > 0 }?.let { expense ->
                    val rentCategory = categories
                        .firstOrNull { it.kind == CategoryKind.EXPENSE && it.name == "Kira" }
                        ?: categories.firstOrNull { it.kind == CategoryKind.EXPENSE }
                    container.recurringRepository.save(
                        RecurringRuleEntity(
                            title = "Kira",
                            type = TransactionType.EXPENSE,
                            amountMinor = expense,
                            categoryId = rentCategory?.id,
                            frequency = RecurrenceFrequency.MONTHLY,
                            dayOfMonth = 1,
                            startDate = today.withDayOfMonth(1).toEpochDay(),
                        )
                    )
                }

                container.recurringGenerator.generateDue()
            } catch (_: Exception) {
                // Onboarding hicbir sekilde kullaniciyi kilitlemesin.
            } finally {
                container.settingsRepository.setOnboardingCompleted(true)
                _state.value = _state.value.copy(isSaving = false)
            }
        }
    }
}
