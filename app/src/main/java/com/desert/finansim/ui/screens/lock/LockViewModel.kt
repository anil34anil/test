package com.desert.finansim.ui.screens.lock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.desert.finansim.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LockState(
    val unlocked: Boolean = false,
    val isChecking: Boolean = false,
    val error: String? = null,
    val failedAttempts: Int = 0,
)

class LockViewModel(private val container: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow(LockState())
    val state: StateFlow<LockState> = _state.asStateFlow()

    fun clearError() {
        if (_state.value.error != null) _state.value = _state.value.copy(error = null)
    }

    fun verify(pin: String) {
        if (_state.value.isChecking) return
        _state.value = _state.value.copy(isChecking = true, error = null)

        viewModelScope.launch {
            val correct = runCatching {
                container.settingsRepository.verifyPin(pin)
            }.getOrDefault(false)

            _state.value = if (correct) {
                _state.value.copy(unlocked = true, isChecking = false)
            } else {
                val attempts = _state.value.failedAttempts + 1
                _state.value.copy(
                    isChecking = false,
                    failedAttempts = attempts,
                    error = if (attempts >= 3) {
                        "PIN hatalı ($attempts. deneme)"
                    } else {
                        "PIN hatalı"
                    },
                )
            }
        }
    }
}
