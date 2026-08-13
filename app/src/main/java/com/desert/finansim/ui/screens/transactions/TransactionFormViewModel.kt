package com.desert.finansim.ui.screens.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.desert.finansim.data.local.CategoryEntity
import com.desert.finansim.data.local.CreditCardEntity
import com.desert.finansim.data.local.RecurringRuleEntity
import com.desert.finansim.data.local.TransactionEntity
import com.desert.finansim.di.AppContainer
import com.desert.finansim.domain.DateUtils
import com.desert.finansim.domain.Money
import com.desert.finansim.domain.model.CategoryKind
import com.desert.finansim.domain.model.DebtSummary
import com.desert.finansim.domain.model.PaymentMethod
import com.desert.finansim.domain.model.RecurrenceFrequency
import com.desert.finansim.domain.model.ReceivableSummary
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
    val creditCardId: Long? = null,
    val debtId: Long? = null,
    val receivableId: Long? = null,
    val isRecurring: Boolean = false,
    val frequency: RecurrenceFrequency = RecurrenceFrequency.MONTHLY,
    val isEditing: Boolean = false,
    val amountError: String? = null,
    val targetError: String? = null,
    val saved: Boolean = false,
    val isSaving: Boolean = false,
)

data class TransactionFormOptions(
    val categories: List<CategoryEntity> = emptyList(),
    val cards: List<CreditCardEntity> = emptyList(),
    val debts: List<DebtSummary> = emptyList(),
    val receivables: List<ReceivableSummary> = emptyList(),
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
        container.creditCardRepository.activeCards,
        container.debtRepository.debtSummaries.map { list -> list.filter { !it.debt.isClosed } },
        container.receivableRepository.summaries.map { list -> list.filter { !it.receivable.isClosed } },
        container.settingsRepository.currencySymbol,
    ) { categories, cards, debts, receivables, currency ->
        TransactionFormOptions(categories, cards, debts, receivables, currency)
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
                amountText = Money.format(
                    transaction.amountMinor,
                    withSymbol = false,
                ),
                title = transaction.title,
                note = transaction.note,
                date = DateUtils.fromEpochDay(transaction.date),
                categoryId = transaction.categoryId,
                paymentMethod = transaction.paymentMethod,
                creditCardId = transaction.creditCardId,
                debtId = transaction.debtId,
                receivableId = transaction.receivableId,
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
        _state.value = _state.value.copy(
            paymentMethod = method,
            // Kredi karti secilmediyse kart baglantisini temizle.
            creditCardId = if (method == PaymentMethod.CREDIT_CARD) _state.value.creditCardId else null,
        )
    }

    fun setCreditCard(id: Long?) {
        _state.value = _state.value.copy(creditCardId = id, targetError = null)
    }

    fun setDebt(id: Long?) {
        _state.value = _state.value.copy(debtId = id, creditCardId = null, targetError = null)
    }

    fun setReceivable(id: Long?) {
        _state.value = _state.value.copy(receivableId = id, targetError = null)
    }

    fun setRecurring(enabled: Boolean) {
        _state.value = _state.value.copy(isRecurring = enabled)
    }

    fun setFrequency(frequency: RecurrenceFrequency) {
        _state.value = _state.value.copy(frequency = frequency)
    }

    /**
     * Kaydeder. Tur ne olursa olsun tutar pozitif olmak zorunda; borc odemesi
     * ve tahsilat icin hedef kayit secilmis olmali, aksi halde hangi borcun
     * azaldigi belirsiz kalirdi.
     */
    fun save() {
        val current = _state.value
        if (current.isSaving) return

        val amount = Money.parse(current.amountText)
        if (amount == null || amount <= 0L) {
            _state.value = current.copy(amountError = "Geçerli bir tutar girin")
            return
        }

        val needsDebtTarget = current.type == TransactionType.DEBT_PAYMENT
        if (needsDebtTarget && current.debtId == null && current.creditCardId == null) {
            _state.value = current.copy(targetError = "Ödemenin hangi borca ait olduğunu seçin")
            return
        }
        if (current.type == TransactionType.RECEIVABLE_COLLECTION && current.receivableId == null) {
            _state.value = current.copy(targetError = "Tahsilatın hangi alacağa ait olduğunu seçin")
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
                        creditCardId = current.creditCardId,
                        debtId = current.debtId,
                        receivableId = current.receivableId,
                    )
                )

                // "Düzenli" isaretlendiyse ayni bilgilerle bir sabit kural olustur.
                if (current.isRecurring && !current.isEditing) {
                    container.recurringRepository.save(
                        RecurringRuleEntity(
                            title = resolvedTitle,
                            type = current.type,
                            amountMinor = amount,
                            categoryId = current.categoryId,
                            paymentMethod = current.paymentMethod,
                            creditCardId = current.creditCardId,
                            frequency = current.frequency,
                            dayOfMonth = current.date.dayOfMonth,
                            dayOfWeek = current.date.dayOfWeek.value,
                            monthOfYear = current.date.monthValue,
                            startDate = current.date.toEpochDay(),
                            // Bu ayki kayit zaten elle eklendi; tekrar uretilmesin.
                            lastGeneratedDate = current.date.toEpochDay(),
                        )
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

    private suspend fun defaultTitle(state: TransactionFormState): String {
        state.categoryId?.let { id ->
            container.categoryRepository.getById(id)?.let { return it.name }
        }
        state.debtId?.let { id ->
            container.debtRepository.getDebt(id)?.let { return it.name }
        }
        state.receivableId?.let { id ->
            container.receivableRepository.getById(id)?.let { return it.personName }
        }
        state.creditCardId?.let { id ->
            container.creditCardRepository.getById(id)?.let { return it.name }
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
