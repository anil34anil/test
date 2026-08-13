package com.desert.finansim.ui.screens.debts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.desert.finansim.data.local.InstallmentEntity
import com.desert.finansim.data.local.TransactionEntity
import com.desert.finansim.di.AppContainer
import com.desert.finansim.domain.DateUtils
import com.desert.finansim.domain.Money
import com.desert.finansim.domain.model.DebtSummary
import com.desert.finansim.domain.model.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class DebtDetailUiState(
    val summary: DebtSummary? = null,
    val payments: List<TransactionEntity> = emptyList(),
    val currencySymbol: String = Money.DEFAULT_SYMBOL,
    val deleted: Boolean = false,
)

class DebtDetailViewModel(
    private val container: AppContainer,
    private val debtId: Long,
) : ViewModel() {

    private val _deleted = MutableStateFlow(false)
    val deleted: StateFlow<Boolean> = _deleted.asStateFlow()

    val uiState: StateFlow<DebtDetailUiState> = combine(
        container.debtRepository.observeDebt(debtId),
        container.debtRepository.installmentsFor(debtId),
        container.database.transactionDao().observeForDebt(debtId),
        container.settingsRepository.currencySymbol,
    ) { debt, installments, transactions, currency ->
        val paid = transactions
            .filter { it.type == TransactionType.DEBT_PAYMENT }
            .sumOf { it.amountMinor }
        DebtDetailUiState(
            summary = debt?.let {
                DebtSummary(debt = it, paidMinor = paid, installments = installments)
            },
            payments = transactions,
            currencySymbol = currency,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DebtDetailUiState(),
    )

    fun markPaid(installment: InstallmentEntity, date: LocalDate = DateUtils.today()) {
        viewModelScope.launch {
            container.debtRepository.markInstallmentPaid(installment, date)
        }
    }

    fun undoPaid(installment: InstallmentEntity) {
        viewModelScope.launch {
            container.debtRepository.unmarkInstallmentPaid(installment)
        }
    }

    /** Taksit disinda serbest odeme (ornegin erken kapama). */
    fun addPayment(amountText: String, date: LocalDate, onError: (String) -> Unit) {
        val amount = Money.parse(amountText)
        if (amount == null || amount <= 0L) {
            onError("Geçerli bir tutar girin")
            return
        }
        viewModelScope.launch {
            container.debtRepository.addPayment(debtId, amount, date)
        }
    }

    fun toggleClosed(closed: Boolean) {
        viewModelScope.launch { container.debtRepository.setClosed(debtId, closed) }
    }

    fun deleteDebt() {
        viewModelScope.launch {
            val debt = container.debtRepository.getDebt(debtId) ?: return@launch
            container.debtRepository.deleteDebt(debt)
            _deleted.value = true
        }
    }
}
