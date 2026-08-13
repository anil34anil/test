package com.desert.finansim.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.desert.finansim.di.AppContainer
import com.desert.finansim.domain.Money
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class OnboardingState(
    val openingBalanceText: String = "",
    val currencySymbol: String = Money.DEFAULT_SYMBOL,
    val isSaving: Boolean = false,
)

class OnboardingViewModel(private val container: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    fun setOpeningBalance(value: String) {
        _state.value = _state.value.copy(openingBalanceText = value)
    }

    fun skip() {
        viewModelScope.launch {
            container.settingsRepository.setOnboardingCompleted(true)
        }
    }

    fun finish() {
        if (_state.value.isSaving) return
        _state.value = _state.value.copy(isSaving = true)

        viewModelScope.launch {
            try {
                Money.parse(_state.value.openingBalanceText)?.takeIf { it > 0 }?.let { balance ->
                    container.settingsRepository.setOpeningBalance(balance)
                }
            } catch (_: Exception) {
                // Onboarding hicbir sekilde kullaniciyi kilitlemesin.
            } finally {
                container.settingsRepository.setOnboardingCompleted(true)
                _state.value = _state.value.copy(isSaving = false)
            }
        }
    }
}
