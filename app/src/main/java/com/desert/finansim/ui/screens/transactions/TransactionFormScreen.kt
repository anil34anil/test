package com.desert.finansim.ui.screens.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.desert.finansim.domain.Money
import com.desert.finansim.domain.model.CategoryKind
import com.desert.finansim.domain.model.PaymentMethod
import com.desert.finansim.domain.model.TransactionType
import com.desert.finansim.ui.components.AmountField
import com.desert.finansim.ui.components.ConfirmDialog
import com.desert.finansim.ui.components.DateField
import com.desert.finansim.ui.components.DropdownField
import com.desert.finansim.ui.containerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionFormScreen(
    transactionType: TransactionType,
    transactionId: Long,
    onDone: () -> Unit,
) {
    val viewModel = containerViewModel(key = "tx-$transactionType-$transactionId") {
        TransactionFormViewModel(it, transactionType, transactionId)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val options by viewModel.options.collectAsStateWithLifecycle()
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.preselectCategoryIfNeeded() }
    LaunchedEffect(state.saved) { if (state.saved) onDone() }

    val screenTitle = when {
        state.isEditing -> "${state.type.label} Düzenle"
        else -> "${state.type.label} Ekle"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(screenTitle) },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                actions = {
                    if (state.isEditing) {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Sil",
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
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

            // Tutar en ustte: en sik girilen alan en once gelsin.
            AmountField(
                value = state.amountText,
                onValueChange = viewModel::setAmount,
                currencySymbol = options.currencySymbol,
                isError = state.amountError != null,
                errorMessage = state.amountError,
            )

            when (state.type) {
                TransactionType.INCOME, TransactionType.EXPENSE -> {
                    val kind = if (state.type == TransactionType.INCOME) {
                        CategoryKind.INCOME
                    } else {
                        CategoryKind.EXPENSE
                    }
                    val categories = options.categories.filter { it.kind == kind }
                    DropdownField(
                        label = "Kategori",
                        options = categories,
                        selected = categories.firstOrNull { it.id == state.categoryId },
                        optionLabel = { it.name },
                        onSelect = { viewModel.setCategory(it.id) },
                    )
                }

                TransactionType.DEBT_PAYMENT -> {
                    DropdownField(
                        label = "Borç",
                        options = options.debts,
                        selected = options.debts.firstOrNull { it.debt.id == state.debtId },
                        optionLabel = { summary ->
                            "${summary.debt.name} • kalan ${
                                Money.format(summary.remainingMinor, options.currencySymbol)
                            }"
                        },
                        onSelect = { viewModel.setDebt(it.debt.id) },
                        isError = state.targetError != null,
                        placeholder = "Borç seçin",
                    )
                    if (state.targetError != null) {
                        Text(
                            text = state.targetError!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            DateField(date = state.date, onDateChange = viewModel::setDate)

            if (state.type == TransactionType.EXPENSE) {
                DropdownField(
                    label = "Ödeme yöntemi",
                    options = PaymentMethod.entries,
                    selected = state.paymentMethod,
                    optionLabel = { it.label },
                    onSelect = viewModel::setPaymentMethod,
                )
            }

            OutlinedTextField(
                value = state.title,
                onValueChange = viewModel::setTitle,
                label = { Text("Başlık (isteğe bağlı)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = state.note,
                onValueChange = viewModel::setNote,
                label = { Text("Açıklama (isteğe bağlı)") },
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
                Text(if (state.isEditing) "Güncelle" else "Kaydet")
            }

            Spacer(Modifier.height(24.dp).navigationBarsPadding())
        }
    }

    if (showDeleteConfirm) {
        ConfirmDialog(
            title = "İşlem silinsin mi?",
            message = "Bu işlem kalıcı olarak silinecek.",
            confirmLabel = "Sil",
            destructive = true,
            onConfirm = {
                showDeleteConfirm = false
                viewModel.delete()
            },
            onDismiss = { showDeleteConfirm = false },
        )
    }
}
