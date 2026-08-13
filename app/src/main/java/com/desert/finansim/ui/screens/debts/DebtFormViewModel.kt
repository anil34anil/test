package com.desert.finansim.ui.screens.debts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.desert.finansim.data.local.CreditCardEntity
import com.desert.finansim.data.local.DebtEntity
import com.desert.finansim.di.AppContainer
import com.desert.finansim.domain.DateUtils
import com.desert.finansim.domain.Money
import com.desert.finansim.domain.model.DebtType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class DebtFormState(
    val name: String = "",
    val counterparty: String = "",
    val type: DebtType = DebtType.LOAN,
    val totalText: String = "",
    val interestText: String = "",
    val hasInstallments: Boolean = true,
    val installmentCountText: String = "12",
    val startDate: LocalDate = DateUtils.today(),
    val firstInstallmentDate: LocalDate = DateUtils.today().plusMonths(1),
    val dueDate: LocalDate = DateUtils.today().plusMonths(1),
    val creditCardId: Long? = null,
    val note: String = "",
    val isEditing: Boolean = false,
    val hasExistingInstallments: Boolean = false,
    val nameError: String? = null,
    val amountError: String? = null,
    val installmentError: String? = null,
    val saved: Boolean = false,
    val isSaving: Boolean = false,
) {
    /** Taksit basina dusen tutar — formda anlik onizleme icin. */
    val installmentPreviewMinor: Long?
        get() {
            val total = Money.parse(totalText) ?: return null
            val count = installmentCountText.toIntOrNull() ?: return null
            if (!hasInstallments || count <= 0 || total <= 0) return null
            return total / count
        }
}

data class DebtFormOptions(
    val cards: List<CreditCardEntity> = emptyList(),
    val currencySymbol: String = Money.DEFAULT_SYMBOL,
)

class DebtFormViewModel(
    private val container: AppContainer,
    private val debtId: Long,
) : ViewModel() {

    private val _state = MutableStateFlow(DebtFormState())
    val state: StateFlow<DebtFormState> = _state.asStateFlow()

    val options: StateFlow<DebtFormOptions> = combine(
        container.creditCardRepository.activeCards,
        container.settingsRepository.currencySymbol,
    ) { cards, currency -> DebtFormOptions(cards, currency) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DebtFormOptions())

    init {
        if (debtId != 0L) load()
    }

    private fun load() {
        viewModelScope.launch {
            val debt = container.debtRepository.getDebt(debtId) ?: return@launch
            val installments = container.database.installmentDao().getForDebtOnce(debtId)
            _state.value = DebtFormState(
                name = debt.name,
                counterparty = debt.counterparty,
                type = debt.type,
                totalText = Money.format(debt.totalAmountMinor, withSymbol = false),
                interestText = debt.interestRate?.toString()?.replace('.', ',') ?: "",
                hasInstallments = installments.isNotEmpty(),
                installmentCountText = (debt.installmentCount ?: installments.size).toString(),
                startDate = DateUtils.fromEpochDay(debt.startDate),
                firstInstallmentDate = installments.minByOrNull { it.number }
                    ?.let { DateUtils.fromEpochDay(it.dueDate) }
                    ?: DateUtils.fromEpochDay(debt.startDate).plusMonths(1),
                dueDate = debt.dueDate?.let { DateUtils.fromEpochDay(it) }
                    ?: DateUtils.fromEpochDay(debt.startDate).plusMonths(1),
                creditCardId = debt.creditCardId,
                note = debt.note,
                isEditing = true,
                hasExistingInstallments = installments.isNotEmpty(),
            )
        }
    }

    fun setName(value: String) { _state.value = _state.value.copy(name = value, nameError = null) }
    fun setCounterparty(value: String) { _state.value = _state.value.copy(counterparty = value) }
    fun setType(value: DebtType) { _state.value = _state.value.copy(type = value) }
    fun setTotal(value: String) { _state.value = _state.value.copy(totalText = value, amountError = null) }
    fun setInterest(value: String) { _state.value = _state.value.copy(interestText = value) }
    fun setNote(value: String) { _state.value = _state.value.copy(note = value) }
    fun setStartDate(value: LocalDate) { _state.value = _state.value.copy(startDate = value) }
    fun setFirstInstallmentDate(value: LocalDate) {
        _state.value = _state.value.copy(firstInstallmentDate = value)
    }
    fun setDueDate(value: LocalDate) { _state.value = _state.value.copy(dueDate = value) }
    fun setCreditCard(id: Long?) { _state.value = _state.value.copy(creditCardId = id) }

    fun setHasInstallments(enabled: Boolean) {
        _state.value = _state.value.copy(hasInstallments = enabled, installmentError = null)
    }

    fun setInstallmentCount(value: String) {
        _state.value = _state.value.copy(installmentCountText = value, installmentError = null)
    }

    /**
     * Borcu kaydeder.
     *
     * Duzenlemede taksit plani YENIDEN URETILMEZ: odenmis taksitler ve onlara
     * bagli odeme kayitlari silinmesin diye. Taksit planini degistirmek isteyen
     * kullanici borcu silip yeniden olusturur (uyari ekranda gosterilir).
     */
    fun save() {
        val current = _state.value
        if (current.isSaving) return

        if (current.name.isBlank()) {
            _state.value = current.copy(nameError = "Borç adı boş olamaz")
            return
        }
        val total = Money.parse(current.totalText)
        if (total == null || total <= 0L) {
            _state.value = current.copy(amountError = "Geçerli bir tutar girin")
            return
        }

        var count: Int? = null
        if (current.hasInstallments && !current.isEditing) {
            count = current.installmentCountText.toIntOrNull()
            if (count == null || count <= 0) {
                _state.value = current.copy(installmentError = "Taksit sayısı en az 1 olmalı")
                return
            }
            if (count > 600) {
                _state.value = current.copy(installmentError = "Taksit sayısı çok yüksek")
                return
            }
        }

        val interest = current.interestText
            .replace(',', '.')
            .toDoubleOrNull()
            ?.takeIf { it >= 0.0 }

        _state.value = current.copy(isSaving = true)

        viewModelScope.launch {
            try {
                if (current.isEditing) {
                    val existing = container.debtRepository.getDebt(debtId)
                    if (existing != null) {
                        container.debtRepository.updateDebt(
                            existing.copy(
                                name = current.name.trim(),
                                counterparty = current.counterparty.trim(),
                                type = current.type,
                                totalAmountMinor = total,
                                interestRate = interest,
                                startDate = current.startDate.toEpochDay(),
                                dueDate = if (current.hasExistingInstallments) {
                                    existing.dueDate
                                } else {
                                    current.dueDate.toEpochDay()
                                },
                                creditCardId = current.creditCardId,
                                note = current.note.trim(),
                            )
                        )
                    }
                } else {
                    container.debtRepository.createDebt(
                        debt = DebtEntity(
                            name = current.name.trim(),
                            counterparty = current.counterparty.trim(),
                            type = current.type,
                            totalAmountMinor = total,
                            interestRate = interest,
                            startDate = current.startDate.toEpochDay(),
                            dueDate = if (current.hasInstallments) null else current.dueDate.toEpochDay(),
                            creditCardId = current.creditCardId,
                            note = current.note.trim(),
                        ),
                        installmentCount = count,
                        firstInstallmentDate = if (current.hasInstallments) {
                            current.firstInstallmentDate
                        } else {
                            null
                        },
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
