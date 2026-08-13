package com.desert.finansim.ui.screens.receivables

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.desert.finansim.data.local.ReceivableEntity
import com.desert.finansim.data.local.TransactionEntity
import com.desert.finansim.di.AppContainer
import com.desert.finansim.domain.DateUtils
import com.desert.finansim.domain.Money
import com.desert.finansim.domain.model.ReceivableSummary
import com.desert.finansim.domain.model.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class ReceivableFormState(
    val personName: String = "",
    val title: String = "",
    val amountText: String = "",
    val startDate: LocalDate = DateUtils.today(),
    val hasDueDate: Boolean = false,
    val dueDate: LocalDate = DateUtils.today().plusMonths(1),
    val note: String = "",
    val isEditing: Boolean = false,
    val nameError: String? = null,
    val amountError: String? = null,
    val saved: Boolean = false,
    val isSaving: Boolean = false,
)

class ReceivableFormViewModel(
    private val container: AppContainer,
    private val receivableId: Long,
) : ViewModel() {

    private val _state = MutableStateFlow(ReceivableFormState())
    val state: StateFlow<ReceivableFormState> = _state.asStateFlow()

    val currencySymbol: StateFlow<String> = container.settingsRepository.currencySymbol
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Money.DEFAULT_SYMBOL)

    init {
        if (receivableId != 0L) load()
    }

    private fun load() {
        viewModelScope.launch {
            val receivable = container.receivableRepository.getById(receivableId) ?: return@launch
            _state.value = ReceivableFormState(
                personName = receivable.personName,
                title = receivable.title,
                amountText = Money.format(receivable.totalAmountMinor, withSymbol = false),
                startDate = DateUtils.fromEpochDay(receivable.startDate),
                hasDueDate = receivable.dueDate != null,
                dueDate = receivable.dueDate?.let { DateUtils.fromEpochDay(it) }
                    ?: DateUtils.today().plusMonths(1),
                note = receivable.note,
                isEditing = true,
            )
        }
    }

    fun setPersonName(value: String) {
        _state.value = _state.value.copy(personName = value, nameError = null)
    }

    fun setTitle(value: String) { _state.value = _state.value.copy(title = value) }
    fun setNote(value: String) { _state.value = _state.value.copy(note = value) }
    fun setStartDate(value: LocalDate) { _state.value = _state.value.copy(startDate = value) }
    fun setDueDate(value: LocalDate) { _state.value = _state.value.copy(dueDate = value) }
    fun setHasDueDate(value: Boolean) { _state.value = _state.value.copy(hasDueDate = value) }

    fun setAmount(value: String) {
        _state.value = _state.value.copy(amountText = value, amountError = null)
    }

    fun save() {
        val current = _state.value
        if (current.isSaving) return

        if (current.personName.isBlank()) {
            _state.value = current.copy(nameError = "Kişi adı boş olamaz")
            return
        }
        val amount = Money.parse(current.amountText)
        if (amount == null || amount <= 0L) {
            _state.value = current.copy(amountError = "Geçerli bir tutar girin")
            return
        }

        _state.value = current.copy(isSaving = true)
        viewModelScope.launch {
            try {
                val entity = ReceivableEntity(
                    id = receivableId,
                    personName = current.personName.trim(),
                    title = current.title.trim(),
                    totalAmountMinor = amount,
                    startDate = current.startDate.toEpochDay(),
                    dueDate = if (current.hasDueDate) current.dueDate.toEpochDay() else null,
                    note = current.note.trim(),
                )
                if (receivableId == 0L) {
                    container.receivableRepository.create(entity)
                } else {
                    val existing = container.receivableRepository.getById(receivableId)
                    container.receivableRepository.update(
                        entity.copy(isClosed = existing?.isClosed ?: false)
                    )
                }
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

data class ReceivableDetailUiState(
    val summary: ReceivableSummary? = null,
    val collections: List<TransactionEntity> = emptyList(),
    val currencySymbol: String = Money.DEFAULT_SYMBOL,
)

class ReceivableDetailViewModel(
    private val container: AppContainer,
    private val receivableId: Long,
) : ViewModel() {

    private val _deleted = MutableStateFlow(false)
    val deleted: StateFlow<Boolean> = _deleted.asStateFlow()

    val uiState: StateFlow<ReceivableDetailUiState> = combine(
        container.receivableRepository.observe(receivableId),
        container.receivableRepository.collectionsFor(receivableId),
        container.settingsRepository.currencySymbol,
    ) { receivable, collections, currency ->
        val collected = collections
            .filter { it.type == TransactionType.RECEIVABLE_COLLECTION }
            .sumOf { it.amountMinor }
        ReceivableDetailUiState(
            summary = receivable?.let { ReceivableSummary(it, collected) },
            collections = collections,
            currencySymbol = currency,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ReceivableDetailUiState(),
    )

    fun addCollection(amountText: String, date: LocalDate) {
        val amount = Money.parse(amountText) ?: return
        if (amount <= 0L) return
        viewModelScope.launch {
            container.receivableRepository.addCollection(receivableId, amount, date)
        }
    }

    fun delete() {
        viewModelScope.launch {
            val receivable = container.receivableRepository.getById(receivableId) ?: return@launch
            container.receivableRepository.delete(receivable)
            _deleted.value = true
        }
    }
}
