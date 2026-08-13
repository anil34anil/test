package com.desert.finansim.ui.screens.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.desert.finansim.ui.components.AmountField
import com.desert.finansim.ui.components.CircleIcon
import com.desert.finansim.ui.containerViewModel

/**
 * Ilk acilis. Bilerek kisa tutuldu: tek ekran, tum alanlar istege bagli.
 * Kullanici "Atla" diyerek dogrudan ana ekrana gecebilir.
 */
@Composable
fun OnboardingScreen() {
    val viewModel = containerViewModel { OnboardingViewModel(it) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(64.dp))

        CircleIcon(
            icon = Icons.Default.TrendingUp,
            tint = MaterialTheme.colorScheme.primary,
            size = 84,
        )

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Finansını kontrol et.",
            style = MaterialTheme.typography.displaySmall,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(10.dp))

        Text(
            text = "Gelirin, giderin ve borçların tek ekranda. " +
                "Tüm veriler sadece bu cihazda kalır.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(36.dp))

        Text(
            text = "İstersen şimdi doldur, istersen sonra. Hiçbiri zorunlu değil.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(16.dp))

        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            AmountField(
                value = state.openingBalanceText,
                onValueChange = viewModel::setOpeningBalance,
                currencySymbol = state.currencySymbol,
                label = "Şu an elindeki para",
            )
            AmountField(
                value = state.monthlyIncomeText,
                onValueChange = viewModel::setMonthlyIncome,
                currencySymbol = state.currencySymbol,
                label = "Aylık geliriniz (maaş)",
            )
            AmountField(
                value = state.fixedExpenseText,
                onValueChange = viewModel::setFixedExpense,
                currencySymbol = state.currencySymbol,
                label = "Aylık sabit gideriniz (ör. kira)",
            )
        }

        Spacer(Modifier.height(28.dp))

        Button(
            onClick = viewModel::finish,
            enabled = !state.isSaving,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            Text("Başla")
        }

        TextButton(onClick = viewModel::skip, enabled = !state.isSaving) {
            Text("Atla")
        }

        Spacer(Modifier.height(32.dp))
    }
}
