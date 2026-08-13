package com.desert.finansim

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.desert.finansim.data.repository.AppSettings
import com.desert.finansim.ui.LocalAppContainer
import com.desert.finansim.ui.navigation.FinansimNavHost
import com.desert.finansim.ui.screens.lock.LockScreen
import com.desert.finansim.ui.screens.onboarding.OnboardingScreen
import com.desert.finansim.ui.theme.FinansimTheme

/**
 * Tek Activity mimarisi.
 *
 * [FragmentActivity] secildi cunku androidx.biometric BiometricPrompt
 * fragment tabanli calisir ve bunu gerektirir.
 */
class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val container = (application as FinansimApp).container

        setContent {
            val settings by container.settingsRepository.settings
                .collectAsStateWithLifecycle(initialValue = AppSettings())

            FinansimTheme(themeMode = settings.themeMode) {
                CompositionLocalProvider(LocalAppContainer provides container) {
                    AppRoot(settings = settings, activity = this@MainActivity)
                }
            }
        }
    }
}

@Composable
private fun AppRoot(settings: AppSettings, activity: FragmentActivity) {
    // Uygulama acildiginda kilitli baslar; oturum boyunca acik kalir.
    var unlocked by rememberSaveable { mutableStateOf(false) }
    val needsUnlock = settings.lockEnabled && !unlocked

    when {
        !settings.onboardingCompleted -> OnboardingScreen()

        needsUnlock -> {
            LockScreen(
                biometricEnabled = settings.biometricEnabled,
                activity = activity,
                onUnlocked = { unlocked = true },
            )
        }

        else -> FinansimNavHost()
    }
}
