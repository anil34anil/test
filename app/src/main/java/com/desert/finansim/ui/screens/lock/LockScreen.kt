package com.desert.finansim.ui.screens.lock

import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.desert.finansim.ui.components.CircleIcon
import com.desert.finansim.ui.containerViewModel

/**
 * Uygulama kilidi. PIN dogrulamasi ozet karsilastirmasiyla yapilir;
 * biyometrik dogrulama sistem tarafindan yonetilir.
 */
@Composable
fun LockScreen(
    biometricEnabled: Boolean,
    activity: FragmentActivity,
    onUnlocked: () -> Unit,
) {
    val viewModel = containerViewModel { LockViewModel(it) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var pin by remember { mutableStateOf("") }

    LaunchedEffect(state.unlocked) { if (state.unlocked) onUnlocked() }

    val canUseBiometric = remember(biometricEnabled) {
        biometricEnabled && BiometricManager.from(context).canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_WEAK
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }

    val promptBiometric: () -> Unit = {
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(context),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onUnlocked()
                }
            },
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Finansım")
            .setSubtitle("Devam etmek için kimliğinizi doğrulayın")
            .setNegativeButtonText("PIN kullan")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .build()
        runCatching { prompt.authenticate(info) }
    }

    // Biyometrik aciksa ekran acilir acilmaz sor.
    LaunchedEffect(canUseBiometric) {
        if (canUseBiometric) promptBiometric()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .imePadding()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircleIcon(
            icon = Icons.Default.Lock,
            tint = MaterialTheme.colorScheme.primary,
            size = 72,
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = "Finansım kilitli",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Devam etmek için PIN'inizi girin",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(28.dp))

        OutlinedTextField(
            value = pin,
            onValueChange = {
                pin = it.filter { ch -> ch.isDigit() }.take(8)
                viewModel.clearError()
            },
            label = { Text("PIN") },
            singleLine = true,
            isError = state.error != null,
            supportingText = state.error?.let {
                { Text(it, color = MaterialTheme.colorScheme.error) }
            },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(18.dp))

        Button(
            onClick = { viewModel.verify(pin) },
            enabled = pin.length >= 4 && !state.isChecking,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            Text("Kilidi aç")
        }

        if (canUseBiometric) {
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = promptBiometric) {
                Icon(Icons.Default.Fingerprint, contentDescription = null)
                Spacer(Modifier.height(0.dp))
                Text("  Biyometrik ile aç")
            }
        }
    }
}
