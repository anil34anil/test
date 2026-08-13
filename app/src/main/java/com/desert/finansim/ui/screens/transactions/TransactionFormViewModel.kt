package com.desert.finansim.ui.screens.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.desert.finansim.data.local.CategoryEntity
import com.desert.finansim.data.local.TransactionEntity
import com.desert.finansim.di.AppContainer
import com.desert.finansim.domain.DateUtils
import com.desert.finansim.domain.Money
import com.desert.finansim.domain.model.CategoryKind
import com.desert.finansim.domain.model.DebtSummary
import com.desert.finansim.domain.model.PaymentMethod
import com.desert.finansim.domain.model.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class TransactionFormState(
    val type: TransactionType = TransactionType.EXPENSE,
    val amountText: String = "",
    val title: String = "",
    val note: String = "",
    val date: LocalDate = DateUtils.today(),
    val categoryId: Long? = null,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val debtId: Long? = null,
    val isEditing: Boolean = false,
    val amountError: String? = null,
    val targetError: String? = null,
    val saved: Boolean = false,
    val isSaving: Boolean = false,
)

data class TransactionFormOptions(
    val categories: List<CategoryEntity> = emptyList(),
    val debts: List<DebtSummary> = emptyList(),
    val currencySymbol: String = Money.DEFAULT_SYMBOL,
)

class TransactionFormViewModel(
    private val container: AppContainer,
    private val initialType: TransactionType,
    private val transactionId: Long,
) : ViewModel() {

    private val _state = MutableStateFlow(TransactionFormState(type = initialType))
    val state: StateFlow<TransactionFormState> = _state.asStateFlow()

    val options: StateFlow<TransactionFormOptions> = combine(
        container.categoryRepository.activeCategories,
        container.debtRepository.debtSummaries.map { list -> list.filter { !it.debt.isClosed } },
        container.settingsRepository.currencySymbol,
    ) { categories, debts, currency ->
        TransactionFormOptions(categories, debts, currency)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TransactionFormOptions(),
    )

    init {
        if (transactionId != 0L) loadExisting()
    }

    private fun loadExisting() {
        viewModelScope.launch {
            val transaction = container.transactionRepository.getById(transactionId) ?: return@launch
            _state.value = TransactionFormState(
                type = transaction.type,
                amountText = Money.formatRaw(transaction.amountMinor),
                title = transaction.title,
                note = transaction.note,
                date = DateUtils.fromEpochDay(transaction.date),
                categoryId = transaction.categoryId,
                paymentMethod = transaction.paymentMethod,
                debtId = transaction.debtId,
                isEditing = true,
            )
        }
    }

    fun setAmount(text: String) {
        _state.value = _state.value.copy(amountText = text, amountError = null)
    }

    fun setTitle(text: String) {
        _state.value = _state.value.copy(title = text)
    }

    fun setNote(text: String) {
        _state.value = _state.value.copy(note = text)
    }

    fun setDate(date: LocalDate) {
        _state.value = _state.value.copy(date = date)
    }

    fun setCategory(id: Long?) {
        _state.value = _state.value.copy(categoryId = id)
    }

    fun setPaymentMethod(method: PaymentMethod) {
        _state.value = _state.value.copy(paymentMethod = method)
    }

    fun setDebt(id: Long?) {
        _state.value = _state.value.copy(debtId = id, targetError = null)
    }

    /**
     * Kaydeder. Tur ne olursa olsun tutar pozitif olmak zorunda; borc odemesi
     * icin hedef borc secilmis olmali, aksi halde hangi borcun azaldigi
     * belirsiz kalirdi.
     */
    fun save() {
        val current = _state.value
        if (current.isSaving) return

        val amount = Money.parse(current.amountText)
        if (amount == null || amount <= 0L) {
            _state.value = current.copy(amountError = "Geçerli bir tutar girin")
            return
        }

        if (current.type == TransactionType.DEBT_PAYMENT && current.debtId == null) {
            _state.value = current.copy(targetError = "Ödemenin hangi borca ait olduğunu seçin")
            return
        }

        _state.value = current.copy(isSaving = true)

        viewModelScope.launch {
            try {
                val resolvedTitle = current.title.trim().ifBlank { defaultTitle(current) }

                container.transactionRepository.save(
                    TransactionEntity(
                        id = transactionId,
                        type = current.type,
                        amountMinor = amount,
                        date = current.date.toEpochDay(),
                        title = resolvedTitle,
                        note = current.note.trim(),
                        categoryId = current.categoryId.takeIf {
                            current.type == TransactionType.INCOME ||
                                current.type == TransactionType.EXPENSE
                        },
                        paymentMethod = current.paymentMethod,
                        debtId = current.debtId,
                    )
                )

                _state.value = _state.value.copy(saved = true, isSaving = false)
            } catch (error: Exception) {
                _state.value = _state.value.copy(
                    isSaving = false,
                    amountError = "Kaydedilemedi: ${error.message ?: "bilinmeyen hata"}",
                )
            }
        }
    }

    private suspend fun defaultTitle(state: TransactionFormState): String {
        state.categoryId?.let { id ->
            container.categoryRepository.getById(id)?.let { return it.name }
        }
        state.debtId?.let { id ->
            container.debtRepository.getDebt(id)?.let { return it.name }
        }
        return state.type.label
    }

    fun delete() {
        if (transactionId == 0L) return
        viewModelScope.launch {
            container.transactionRepository.deleteById(transactionId)
            _state.value = _state.value.copy(saved = true)
        }
    }

    /** Form acildiginda ilk uygun kategoriyi secer (daha az tiklama). */
    fun preselectCategoryIfNeeded() {
        viewModelScope.launch {
            if (_state.value.categoryId != null || transactionId != 0L) return@launch
            val kind = when (_state.value.type) {
                TransactionType.INCOME -> CategoryKind.INCOME
                TransactionType.EXPENSE -> CategoryKind.EXPENSE
                else -> return@launch
            }
            val first = container.categoryRepository.activeCategories.first()
                .firstOrNull { it.kind == kind }
            if (first != null && _state.value.categoryId == null) {
                _state.value = _state.value.copy(categoryId = first.id)
            }
        }
    }
}
