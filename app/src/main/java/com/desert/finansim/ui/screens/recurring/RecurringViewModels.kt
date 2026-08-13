package com.desert.finansim.ui.screens.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.desert.finansim.data.local.CategoryEntity
import com.desert.finansim.data.local.CreditCardEntity
import com.desert.finansim.data.local.RecurringRuleEntity
import com.desert.finansim.data.repository.RecurringSchedule
import com.desert.finansim.di.AppContainer
import com.desert.finansim.domain.DateUtils
import com.desert.finansim.domain.Money
import com.desert.finansim.domain.model.CategoryKind
import com.desert.finansim.domain.model.PaymentMethod
import com.desert.finansim.domain.model.RecurrenceFrequency
import com.desert.finansim.domain.model.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class RecurringListItem(
    val rule: RecurringRuleEntity,
    val categoryName: String?,
    val nextOccurrence: LocalDate?,
)

data class RecurringListUiState(
    val items: List<RecurringListItem> = emptyList(),
    val currencySymbol: String = Money.DEFAULT_SYMBOL,
) {
    val monthlyExpenseTotal: Long
        get() = items
            .filter {
                it.rule.isActive &&
                    it.rule.type == TransactionType.EXPENSE &&
                    it.rule.frequency == RecurrenceFrequency.MONTHLY
            }
            .sumOf { it.rule.amountMinor }

    val monthlyIncomeTotal: Long
        get() = items
            .filter {
                it.rule.isActive &&
                    it.rule.type == TransactionType.INCOME &&
                    it.rule.frequency == RecurrenceFrequency.MONTHLY
            }
            .sumOf { it.rule.amountMinor }
}

class RecurringListViewModel(private val container: AppContainer) : ViewModel() {

    val uiState: StateFlow<RecurringListUiState> = combine(
        container.recurringRepository.allRules,
        container.categoryRepository.allCategories,
        container.settingsRepository.currencySymbol,
    ) { rules, categories, currency ->
        val categoriesById = categories.associateBy { it.id }
        RecurringListUiState(
            items = rules.map { rule ->
                RecurringListItem(
                    rule = rule,
                    categoryName = rule.categoryId?.let { categoriesById[it]?.name },
                    nextOccurrence = if (rule.isActive) {
                        RecurringSchedule.nextOccurrence(rule)
                    } else {
                        null
                    },
                )
            },
            currencySymbol = currency,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RecurringListUiState(),
    )

    fun setActive(id: Long, active: Boolean) {
        viewModelScope.launch { container.recurringRepository.setActive(id, active) }
    }

    fun delete(rule: RecurringRuleEntity) {
        viewModelScope.launch { container.recurringRepository.delete(rule) }
    }

    /** Vadesi gelmis tekrarlari hemen isler (kullanici "şimdi uygula" derse). */
    fun generateNow() {
        viewModelScope.launch { container.recurringGenerator.generateDue() }
    }
}

data class RecurringFormState(
    val title: String = "",
    val type: TransactionType = TransactionType.EXPENSE,
    val amountText: String = "",
    val categoryId: Long? = null,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val creditCardId: Long? = null,
    val frequency: RecurrenceFrequency = RecurrenceFrequency.MONTHLY,
    val dayOfMonthText: String = "1",
    val dayOfWeek: Int = 1,
    val monthOfYear: Int = 1,
    val startDate: LocalDate = DateUtils.today(),
    val hasEndDate: Boolean = false,
    val endDate: LocalDate = DateUtils.today().plusYears(1),
    val isActive: Boolean = true,
    val isEditing: Boolean = false,
    val titleError: String? = null,
    val amountError: String? = null,
    val dayError: String? = null,
    val saved: Boolean = false,
    val isSaving: Boolean = false,
)

data class RecurringFormOptions(
    val categories: List<CategoryEntity> = emptyList(),
    val cards: List<CreditCardEntity> = emptyList(),
    val currencySymbol: String = Money.DEFAULT_SYMBOL,
)

class RecurringFormViewModel(
    private val container: AppContainer,
    private val ruleId: Long,
) : ViewModel() {

    private val _state = MutableStateFlow(RecurringFormState())
    val state: StateFlow<RecurringFormState> = _state.asStateFlow()

    val options: StateFlow<RecurringFormOptions> = combine(
        container.categoryRepository.activeCategories,
        container.creditCardRepository.activeCards,
        container.settingsRepository.currencySymbol,
    ) { categories, cards, currency ->
        RecurringFormOptions(categories, cards, currency)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecurringFormOptions())

    init {
        if (ruleId != 0L) load()
    }

    private fun load() {
        viewModelScope.launch {
            val rule = container.recurringRepository.getById(ruleId) ?: return@launch
            _state.value = RecurringFormState(
                title = rule.title,
                type = rule.type,
                amountText = Money.format(rule.amountMinor, withSymbol = false),
                categoryId = rule.categoryId,
                paymentMethod = rule.paymentMethod,
                creditCardId = rule.creditCardId,
                frequency = rule.frequency,
                dayOfMonthText = (rule.dayOfMonth ?: 1).toString(),
                dayOfWeek = rule.dayOfWeek ?: 1,
                monthOfYear = rule.monthOfYear ?: 1,
                startDate = DateUtils.fromEpochDay(rule.startDate),
                hasEndDate = rule.endDate != null,
                endDate = rule.endDate?.let { DateUtils.fromEpochDay(it) }
                    ?: DateUtils.today().plusYears(1),
                isActive = rule.isActive,
                isEditing = true,
            )
        }
    }

    fun setTitle(value: String) { _state.value = _state.value.copy(title = value, titleError = null) }
    fun setAmount(value: String) { _state.value = _state.value.copy(amountText = value, amountError = null) }
    fun setCategory(id: Long?) { _state.value = _state.value.copy(categoryId = id) }
    fun setCreditCard(id: Long?) { _state.value = _state.value.copy(creditCardId = id) }
    fun setStartDate(value: LocalDate) { _state.value = _state.value.copy(startDate = value) }
    fun setEndDate(value: LocalDate) { _state.value = _state.value.copy(endDate = value) }
    fun setHasEndDate(value: Boolean) { _state.value = _state.value.copy(hasEndDate = value) }
    fun setActive(value: Boolean) { _state.value = _state.value.copy(isActive = value) }
    fun setDayOfWeek(value: Int) { _state.value = _state.value.copy(dayOfWeek = value) }
    fun setMonthOfYear(value: Int) { _state.value = _state.value.copy(monthOfYear = value) }

    fun setType(value: TransactionType) {
        _state.value = _state.value.copy(type = value, categoryId = null)
    }

    fun setPaymentMethod(value: PaymentMethod) {
        _state.value = _state.value.copy(
            paymentMethod = value,
            creditCardId = if (value == PaymentMethod.CREDIT_CARD) _state.value.creditCardId else null,
        )
    }

    fun setFrequency(value: RecurrenceFrequency) {
        _state.value = _state.value.copy(frequency = value, dayError = null)
    }

    fun setDayOfMonth(value: String) {
        _state.value = _state.value.copy(dayOfMonthText = value, dayError = null)
    }

    fun categoriesForType(all: List<CategoryEntity>): List<CategoryEntity> {
        val kind = if (_state.value.type == TransactionType.INCOME) {
            CategoryKind.INCOME
        } else {
            CategoryKind.EXPENSE
        }
        return all.filter { it.kind == kind }
    }

    fun save() {
        val current = _state.value
        if (current.isSaving) return

        if (current.title.isBlank()) {
            _state.value = current.copy(titleError = "Ad boş olamaz")
            return
        }
        val amount = Money.parse(current.amountText)
        if (amount == null || amount <= 0L) {
            _state.value = current.copy(amountError = "Geçerli bir tutar girin")
            return
        }

        val dayOfMonth = current.dayOfMonthText.toIntOrNull()
        if (current.frequency != RecurrenceFrequency.WEEKLY &&
            (dayOfMonth == null || dayOfMonth !in 1..31)
        ) {
            _state.value = current.copy(dayError = "Gün 1 ile 31 arasında olmalı")
            return
        }
        if (current.hasEndDate && current.endDate.isBefore(current.startDate)) {
            _state.value = current.copy(dayError = "Bitiş tarihi başlangıçtan önce olamaz")
            return
        }

        _state.value = current.copy(isSaving = true)
        viewModelScope.launch {
            try {
                val existing = if (ruleId != 0L) {
                    container.recurringRepository.getById(ruleId)
                } else {
                    null
                }
                container.recurringRepository.save(
                    RecurringRuleEntity(
                        id = ruleId,
                        title = current.title.trim(),
                        type = current.type,
                        amountMinor = amount,
                        categoryId = current.categoryId,
                        paymentMethod = current.paymentMethod,
                        creditCardId = current.creditCardId,
                        frequency = current.frequency,
                        dayOfMonth = dayOfMonth,
                        dayOfWeek = current.dayOfWeek,
                        monthOfYear = current.monthOfYear,
                        startDate = current.startDate.toEpochDay(),
                        endDate = if (current.hasEndDate) current.endDate.toEpochDay() else null,
                        isActive = current.isActive,
                        // Uretim gecmisi korunur ki gecmis donemler tekrar islenmesin.
                        lastGeneratedDate = existing?.lastGeneratedDate,
                    )
                )
                // Yeni kural bugune kadar birikmis tekrarlari hemen olustursun.
                container.recurringGenerator.generateDue()
                _state.value = _state.value.copy(saved = true, isSaving = false)
            } catch (error: Exception) {
                _state.value = _state.value.copy(
                    isSaving = false,
                    amountError = "Kaydedilemedi: ${error.message ?: "bilinmeyen hata"}",
                )
            }
        }
    }
}
