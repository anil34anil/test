package com.desert.finansim

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.desert.finansim.data.repository.AppSettings
import com.desert.finansim.ui.LocalAppContainer
import com.desert.finansim.ui.navigation.FinansimNavHost
import com.desert.finansim.ui.screens.onboarding.OnboardingScreen
import com.desert.finansim.ui.theme.FinansimTheme

/** Tek Activity mimarisi. */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val container = (application as FinansimApp).container

        setContent {
            val settings by container.settingsRepository.settings
                .collectAsStateWithLifecycle(initialValue = AppSettings())

            FinansimTheme(themeMode = settings.themeMode) {
                CompositionLocalProvider(LocalAppContainer provides container) {
                    AppRoot(settings = settings)
                }
            }
        }
    }
}

@Composable
private fun AppRoot(settings: AppSettings) {
    if (!settings.onboardingCompleted) {
        OnboardingScreen()
    } else {
        FinansimNavHost()
    }
}
