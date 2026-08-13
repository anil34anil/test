package com.desert.finansim.ui.screens.settings

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.desert.finansim.data.backup.BackupData
import com.desert.finansim.data.backup.RestoreMode
import com.desert.finansim.data.repository.AppSettings
import com.desert.finansim.di.AppContainer
import com.desert.finansim.domain.model.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Ayarlar ekraninda kullaniciya gosterilecek tek seferlik mesaj. */
sealed interface SettingsMessage {
    data class Info(val text: String) : SettingsMessage
    data class Error(val text: String) : SettingsMessage
    /** Geri yukleme onayi icin ayristirilmis yedek. */
    data class RestorePrompt(val data: BackupData, val fileSummary: String) : SettingsMessage
}

class SettingsViewModel(private val container: AppContainer) : ViewModel() {

    val settings: StateFlow<AppSettings> = container.settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    private val _message = MutableStateFlow<SettingsMessage?>(null)
    val message: StateFlow<SettingsMessage?> = _message.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    fun clearMessage() { _message.value = null }

    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch { container.settingsRepository.setThemeMode(mode) }
    }

    fun setCurrencySymbol(symbol: String) {
        viewModelScope.launch { container.settingsRepository.setCurrencySymbol(symbol) }
    }

    fun setOpeningBalance(minor: Long) {
        viewModelScope.launch { container.settingsRepository.setOpeningBalance(minor) }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { container.settingsRepository.setNotificationsEnabled(enabled) }
    }

    fun setReminderDays(days: Int) {
        viewModelScope.launch { container.settingsRepository.setReminderDaysBefore(days) }
    }

    fun setBudgetAlerts(enabled: Boolean) {
        viewModelScope.launch { container.settingsRepository.setBudgetAlertsEnabled(enabled) }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch { container.settingsRepository.setBiometricEnabled(enabled) }
    }

    fun setPin(pin: String) {
        viewModelScope.launch {
            container.settingsRepository.setPin(pin)
            _message.value = SettingsMessage.Info("PIN kaydedildi")
        }
    }

    fun clearPin() {
        viewModelScope.launch {
            container.settingsRepository.clearPin()
            _message.value = SettingsMessage.Info("Uygulama kilidi kaldırıldı")
        }
    }

    fun backupFileName(): String = container.backupRepository.suggestedFileName()

    fun csvFileName(): String = container.csvExporter.suggestedFileName()

    /** Tum veriyi JSON olarak secilen dosyaya yazar. */
    fun exportBackup(uri: Uri, resolver: ContentResolver) {
        runIo(
            work = {
                val json = container.backupRepository.export()
                writeText(resolver, uri, json)
            },
            successMessage = "Yedek kaydedildi",
            failureMessage = "Yedek kaydedilemedi",
        )
    }

    /** Islemleri CSV olarak secilen dosyaya yazar. */
    fun exportCsv(uri: Uri, resolver: ContentResolver) {
        runIo(
            work = {
                val csv = container.csvExporter.exportTransactions()
                writeText(resolver, uri, csv)
            },
            successMessage = "CSV dosyası kaydedildi",
            failureMessage = "CSV dışa aktarılamadı",
        )
    }

    /**
     * Yedek dosyasini okur ve DOGRULAR ama heniz uygulamaz; kullanicidan
     * "mevcut veriler silinsin mi" onayi alindiktan sonra [restore] cagrilir.
     */
    fun prepareRestore(uri: Uri, resolver: ContentResolver) {
        viewModelScope.launch {
            _busy.value = true
            try {
                val content = withContext(Dispatchers.IO) {
                    resolver.openInputStream(uri)?.use { stream ->
                        stream.readBytes().toString(Charsets.UTF_8)
                    } ?: error("Dosya okunamadı")
                }
                container.backupRepository.parse(content)
                    .onSuccess { data ->
                        val summary = buildString {
                            append("${data.transactions.size} işlem, ")
                            append("${data.debts.size} borç, ")
                            append("${data.receivables.size} alacak, ")
                            append("${data.creditCards.size} kart")
                        }
                        _message.value = SettingsMessage.RestorePrompt(data, summary)
                    }
                    .onFailure { error ->
                        _message.value = SettingsMessage.Error(
                            error.message ?: "Yedek dosyası okunamadı"
                        )
                    }
            } catch (error: Exception) {
                _message.value = SettingsMessage.Error(
                    "Dosya okunamadı: ${error.message ?: "bilinmeyen hata"}"
                )
            } finally {
                _busy.value = false
            }
        }
    }

    fun restore(data: BackupData, mode: RestoreMode) {
        viewModelScope.launch {
            _busy.value = true
            container.backupRepository.restore(data, mode)
                .onSuccess { stats ->
                    _message.value = SettingsMessage.Info(
                        "Geri yükleme tamamlandı: ${stats.total} kayıt"
                    )
                }
                .onFailure { error ->
                    _message.value = SettingsMessage.Error(
                        "Geri yükleme başarısız: ${error.message ?: "bilinmeyen hata"}. " +
                            "Mevcut verileriniz değiştirilmedi."
                    )
                }
            _busy.value = false
        }
    }

    private fun runIo(
        work: suspend () -> Unit,
        successMessage: String,
        failureMessage: String,
    ) {
        viewModelScope.launch {
            _busy.value = true
            try {
                withContext(Dispatchers.IO) { work() }
                _message.value = SettingsMessage.Info(successMessage)
            } catch (error: Exception) {
                _message.value = SettingsMessage.Error(
                    "$failureMessage: ${error.message ?: "bilinmeyen hata"}"
                )
            } finally {
                _busy.value = false
            }
        }
    }

    private fun writeText(resolver: ContentResolver, uri: Uri, text: String) {
        resolver.openOutputStream(uri)?.use { stream ->
            stream.write(text.toByteArray(Charsets.UTF_8))
            stream.flush()
        } ?: error("Dosya yazılamadı")
    }
}
