package com.desert.finansim.ui.screens.debts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.desert.finansim.domain.Money
import com.desert.finansim.domain.model.DebtType
import com.desert.finansim.ui.components.AmountField
import com.desert.finansim.ui.components.DateField
import com.desert.finansim.ui.components.DecimalField
import com.desert.finansim.ui.components.DropdownField
import com.desert.finansim.ui.components.NumberField
import com.desert.finansim.ui.containerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtFormScreen(
    debtId: Long,
    onDone: () -> Unit,
) {
    val viewModel = containerViewModel(key = "debt-form-$debtId") {
        DebtFormViewModel(it, debtId)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val options by viewModel.options.collectAsStateWithLifecycle()

    LaunchedEffect(state.saved) { if (state.saved) onDone() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditing) "Borcu Düzenle" else "Borç Ekle") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Spacer(Modifier.height(4.dp))

            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::setName,
                label = { Text("Borç adı") },
                singleLine = true,
                isError = state.nameError != null,
                supportingText = state.nameError?.let {
                    { Text(it, color = MaterialTheme.colorScheme.error) }
                },
                modifier = Modifier.fillMaxWidth(),
            )

            DropdownField(
                label = "Borç türü",
                options = DebtType.entries,
                selected = state.type,
                optionLabel = { it.label },
                onSelect = viewModel::setType,
            )

            OutlinedTextField(
                value = state.counterparty,
                onValueChange = viewModel::setCounterparty,
                label = { Text("Kurum / kişi (isteğe bağlı)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            AmountField(
                value = state.totalText,
                onValueChange = viewModel::setTotal,
                currencySymbol = options.currencySymbol,
                label = "Toplam borç",
                isError = state.amountError != null,
                errorMessage = state.amountError,
            )

            DecimalField(
                value = state.interestText,
                onValueChange = viewModel::setInterest,
                label = "Yıllık faiz oranı (isteğe bağlı)",
                suffix = "%",
            )

            DateField(
                date = state.startDate,
                onDateChange = viewModel::setStartDate,
                label = "Başlangıç tarihi",
            )

            if (options.cards.isNotEmpty() && state.type == DebtType.CREDIT_CARD) {
                DropdownField(
                    label = "Bağlı kart (isteğe bağlı)",
                    options = options.cards,
                    selected = options.cards.firstOrNull { it.id == state.creditCardId },
                    optionLabel = { it.name },
                    onSelect = { viewModel.setCreditCard(it.id) },
                    placeholder = "Kart seçin",
                )
            }

            // --- Taksit plani ---------------------------------------------
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Taksitli borç", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = "Taksitler otomatik oluşturulur (1/12, 2/12 ...)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = state.hasInstallments,
                    onCheckedChange = viewModel::setHasInstallments,
                    enabled = !state.isEditing,
                )
            }

            if (state.hasInstallments) {
                NumberField(
                    value = state.installmentCountText,
                    onValueChange = viewModel::setInstallmentCount,
                    label = "Taksit sayısı",
                    isError = state.installmentError != null,
                    errorMessage = state.installmentError,
                    suffix = "taksit",
                )
                if (!state.isEditing) {
                    DateField(
                        date = state.firstInstallmentDate,
                        onDateChange = viewModel::setFirstInstallmentDate,
                        label = "İlk taksit tarihi",
                    )
                }
                state.installmentPreviewMinor?.let { perInstallment ->
                    Text(
                        text = "Aylık yaklaşık ${
                            Money.format(perInstallment, options.currencySymbol)
                        } ödeyeceksin.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            } else {
                DateField(
                    date = state.dueDate,
                    onDateChange = viewModel::setDueDate,
                    label = "Son ödeme tarihi",
                )
            }

            if (state.isEditing && state.hasExistingInstallments) {
                Text(
                    text = "Not: Ödenmiş taksitler ve onlara bağlı ödeme kayıtları korunur, " +
                        "bu yüzden düzenlerken taksit planı yeniden oluşturulmaz. " +
                        "Planı değiştirmek istersen borcu silip yeniden oluşturmalısın.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            OutlinedTextField(
                value = state.note,
                onValueChange = viewModel::setNote,
                label = { Text("Not (isteğe bağlı)") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = viewModel::save,
                enabled = !state.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Text(if (state.isEditing) "Güncelle" else "Borcu Kaydet")
            }

            Text(
                text = "Borç oluşturmak bir gider kaydı değildir; sadece yükümlülüğünü tanımlar. " +
                    "Gider olarak sayılan şey, yaptığın ödemelerdir.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(24.dp).navigationBarsPadding())
        }
    }
}
