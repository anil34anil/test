package com.desert.finansim.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.desert.finansim.domain.Money
import com.desert.finansim.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "finansim_settings")

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val currencySymbol: String = Money.DEFAULT_SYMBOL,
    val openingBalanceMinor: Long = 0L,
    val onboardingCompleted: Boolean = false,
    val notificationsEnabled: Boolean = false,
    val reminderDaysBefore: Int = 2,
)

/** Uygulama ayarlari DataStore'da tutulur. */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val THEME = stringPreferencesKey("theme_mode")
        val CURRENCY = stringPreferencesKey("currency_symbol")
        val OPENING_BALANCE = longPreferencesKey("opening_balance_minor")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_completed")
        val NOTIFICATIONS = booleanPreferencesKey("notifications_enabled")
        val REMINDER_DAYS = intPreferencesKey("reminder_days_before")
    }

    private val preferences: Flow<Preferences> = context.settingsDataStore.data
        .catch { throwable ->
            // Bozuk/okunamayan ayar dosyasi uygulamayi cokertmesin.
            if (throwable is IOException) emit(emptyPreferences()) else throw throwable
        }

    val settings: Flow<AppSettings> = preferences.map { prefs ->
        AppSettings(
            themeMode = runCatching {
                ThemeMode.valueOf(prefs[Keys.THEME] ?: ThemeMode.SYSTEM.name)
            }.getOrDefault(ThemeMode.SYSTEM),
            currencySymbol = prefs[Keys.CURRENCY] ?: Money.DEFAULT_SYMBOL,
            openingBalanceMinor = prefs[Keys.OPENING_BALANCE] ?: 0L,
            onboardingCompleted = prefs[Keys.ONBOARDING_DONE] ?: false,
            notificationsEnabled = prefs[Keys.NOTIFICATIONS] ?: false,
            reminderDaysBefore = prefs[Keys.REMINDER_DAYS] ?: 2,
        )
    }

    val openingBalanceMinor: Flow<Long> = preferences.map { it[Keys.OPENING_BALANCE] ?: 0L }

    val currencySymbol: Flow<String> = preferences.map { it[Keys.CURRENCY] ?: Money.DEFAULT_SYMBOL }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { it[Keys.THEME] = mode.name }
    }

    suspend fun setCurrencySymbol(symbol: String) {
        context.settingsDataStore.edit {
            it[Keys.CURRENCY] = symbol.ifBlank { Money.DEFAULT_SYMBOL }
        }
    }

    suspend fun setOpeningBalance(minor: Long) {
        context.settingsDataStore.edit { it[Keys.OPENING_BALANCE] = minor }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.settingsDataStore.edit { it[Keys.ONBOARDING_DONE] = completed }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.NOTIFICATIONS] = enabled }
    }

    suspend fun setReminderDaysBefore(days: Int) {
        context.settingsDataStore.edit { it[Keys.REMINDER_DAYS] = days.coerceIn(0, 14) }
    }
}
