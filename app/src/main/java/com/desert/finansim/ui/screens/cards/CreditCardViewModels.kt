package com.desert.finansim.ui.screens.cards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.desert.finansim.data.local.CategoryEntity
import com.desert.finansim.data.local.CreditCardEntity
import com.desert.finansim.data.local.TransactionEntity
import com.desert.finansim.di.AppContainer
import com.desert.finansim.domain.Money
import com.desert.finansim.domain.model.CreditCardSummary
import com.desert.finansim.domain.model.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Kart rengi secenekleri — kartlari listede birbirinden ayirmak icin. */
val CARD_COLORS = listOf(
    0xFF2F6F62, 0xFF3A6EA5, 0xFF9B5DE5, 0xFFE2703A,
    0xFF00A9A5, 0xFFC63B3B, 0xFF56626D,
)

data class CreditCardFormState(
    val name: String = "",
    val bank: String = "",
    val limitText: String = "",
    val statementDayText: String = "1",
    val dueDayText: String = "10",
    val colorArgb: Long = CARD_COLORS.first(),
    val isEditing: Boolean = false,
    val nameError: String? = null,
    val limitError: String? = null,
    val dayError: String? = null,
    val saved: Boolean = false,
    val isSaving: Boolean = false,
)

class CreditCardFormViewModel(
    private val container: AppContainer,
    private val cardId: Long,
) : ViewModel() {

    private val _state = MutableStateFlow(CreditCardFormState())
    val state: StateFlow<CreditCardFormState> = _state.asStateFlow()

    val currencySymbol: StateFlow<String> = container.settingsRepository.currencySymbol
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Money.DEFAULT_SYMBOL)

    init {
        if (cardId != 0L) load()
    }

    private fun load() {
        viewModelScope.launch {
            val card = container.creditCardRepository.getById(cardId) ?: return@launch
            _state.value = CreditCardFormState(
                name = card.name,
                bank = card.bank,
                limitText = Money.format(card.limitMinor, withSymbol = false),
                statementDayText = card.statementDay.toString(),
                dueDayText = card.dueDay.toString(),
                colorArgb = card.colorArgb,
                isEditing = true,
            )
        }
    }

    fun setName(value: String) { _state.value = _state.value.copy(name = value, nameError = null) }
    fun setBank(value: String) { _state.value = _state.value.copy(bank = value) }
    fun setLimit(value: String) { _state.value = _state.value.copy(limitText = value, limitError = null) }
    fun setColor(value: Long) { _state.value = _state.value.copy(colorArgb = value) }

    fun setStatementDay(value: String) {
        _state.value = _state.value.copy(statementDayText = value, dayError = null)
    }

    fun setDueDay(value: String) {
        _state.value = _state.value.copy(dueDayText = value, dayError = null)
    }

    fun save() {
        val current = _state.value
        if (current.isSaving) return

        if (current.name.isBlank()) {
            _state.value = current.copy(nameError = "Kart adı boş olamaz")
            return
        }
        val limit = Money.parse(current.limitText)
        if (limit == null || limit <= 0L) {
            _state.value = current.copy(limitError = "Geçerli bir limit girin")
            return
        }
        val statementDay = current.statementDayText.toIntOrNull()
        val dueDay = current.dueDayText.toIntOrNull()
        if (statementDay == null || statementDay !in 1..31 || dueDay == null || dueDay !in 1..31) {
            _state.value = current.copy(dayError = "Gün 1 ile 31 arasında olmalı")
            return
        }

        _state.value = current.copy(isSaving = true)
        viewModelScope.launch {
            try {
                container.creditCardRepository.save(
                    CreditCardEntity(
                        id = cardId,
                        name = current.name.trim(),
                        bank = current.bank.trim(),
                        limitMinor = limit,
                        statementDay = statementDay,
                        dueDay = dueDay,
                        colorArgb = current.colorArgb,
                    )
                )
                _state.value = _state.value.copy(saved = true, isSaving = false)
            } catch (error: Exception) {
                _state.value = _state.value.copy(
                    isSaving = false,
                    limitError = "Kaydedilemedi: ${error.message ?: "bilinmeyen hata"}",
                )
            }
        }
    }
}

data class CreditCardDetailUiState(
    val summary: CreditCardSummary? = null,
    val transactions: List<TransactionEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val currencySymbol: String = Money.DEFAULT_SYMBOL,
) {
    /** Ekstre odemeleri harcamalardan ayri gosterilir. */
    val expenses: List<TransactionEntity>
        get() = transactions.filter { it.type == TransactionType.EXPENSE }
    val payments: List<TransactionEntity>
        get() = transactions.filter { it.type == TransactionType.DEBT_PAYMENT }
}

class CreditCardDetailViewModel(
    private val container: AppContainer,
    private val cardId: Long,
) : ViewModel() {

    private val _deleted = MutableStateFlow(false)
    val deleted: StateFlow<Boolean> = _deleted.asStateFlow()

    val uiState: StateFlow<CreditCardDetailUiState> = combine(
        container.creditCardRepository.observe(cardId),
        container.creditCardRepository.transactionsFor(cardId),
        container.categoryRepository.allCategories,
        container.settingsRepository.currencySymbol,
    ) { card, transactions, categories, currency ->
        val spent = transactions
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.amountMinor }
        val paid = transactions
            .filter { it.type == TransactionType.DEBT_PAYMENT }
            .sumOf { it.amountMinor }
        CreditCardDetailUiState(
            summary = card?.let { CreditCardSummary(it, spent, paid) },
            transactions = transactions,
            categories = categories,
            currencySymbol = currency,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CreditCardDetailUiState(),
    )

    fun addPayment(amountText: String, date: LocalDate) {
        val amount = Money.parse(amountText) ?: return
        if (amount <= 0L) return
        viewModelScope.launch {
            container.creditCardRepository.addCardPayment(cardId, amount, date)
        }
    }

    fun delete() {
        viewModelScope.launch {
            val card = container.creditCardRepository.getById(cardId) ?: return@launch
            container.creditCardRepository.delete(card)
            _deleted.value = true
        }
    }
}
