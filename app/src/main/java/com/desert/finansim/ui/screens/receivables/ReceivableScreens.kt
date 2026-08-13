package com.desert.finansim.ui.screens.receivables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.desert.finansim.domain.DateUtils
import com.desert.finansim.domain.Money
import com.desert.finansim.ui.components.AmountField
import com.desert.finansim.ui.components.ConfirmDialog
import com.desert.finansim.ui.components.DateField
import com.desert.finansim.ui.components.ProgressRow
import com.desert.finansim.ui.components.SectionCard
import com.desert.finansim.ui.components.StatTile
import com.desert.finansim.ui.containerViewModel
import com.desert.finansim.ui.theme.FinansimTheme
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceivableFormScreen(
    receivableId: Long,
    onDone: () -> Unit,
) {
    val viewModel = containerViewModel(key = "rec-form-$receivableId") {
        ReceivableFormViewModel(it, receivableId)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currency by viewModel.currencySymbol.collectAsStateWithLifecycle()

    LaunchedEffect(state.saved) { if (state.saved) onDone() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditing) "Alacağı Düzenle" else "Alacak Ekle") },
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
                value = state.personName,
                onValueChange = viewModel::setPersonName,
                label = { Text("Kişi / kurum") },
                singleLine = true,
                isError = state.nameError != null,
                supportingText = state.nameError?.let {
                    { Text(it, color = MaterialTheme.colorScheme.error) }
                },
                modifier = Modifier.fillMaxWidth(),
            )

            AmountField(
                value = state.amountText,
                onValueChange = viewModel::setAmount,
                currencySymbol = currency,
                label = "Alacak tutarı",
                isError = state.amountError != null,
                errorMessage = state.amountError,
            )

            OutlinedTextField(
                value = state.title,
                onValueChange = viewModel::setTitle,
                label = { Text("Açıklama (isteğe bağlı)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            DateField(
                date = state.startDate,
                onDateChange = viewModel::setStartDate,
                label = "Verildiği tarih",
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Son ödeme tarihi var", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = "Yaklaşan ödemelerde hatırlatılır",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = state.hasDueDate, onCheckedChange = viewModel::setHasDueDate)
            }
            if (state.hasDueDate) {
                DateField(
                    date = state.dueDate,
                    onDateChange = viewModel::setDueDate,
                    label = "Son ödeme tarihi",
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
                Text(if (state.isEditing) "Güncelle" else "Alacağı Kaydet")
            }

            Text(
                text = "Alacak oluşturmak gelir kaydı değildir. Gelir sayılan şey, " +
                    "parayı fiilen tahsil ettiğindir.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceivableDetailScreen(
    receivableId: Long,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
) {
    val viewModel = containerViewModel(key = "rec-detail-$receivableId") {
        ReceivableDetailViewModel(it, receivableId)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val deleted by viewModel.deleted.collectAsStateWithLifecycle()
    val colors = FinansimTheme.financeColors

    var showDelete by remember { mutableStateOf(false) }
    var showCollection by remember { mutableStateOf(false) }

    LaunchedEffect(deleted) { if (deleted) onBack() }

    val summary = state.summary

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = summary?.receivable?.personName ?: "Alacak",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                actions = {
                    IconButton(onClick = { onEdit(receivableId) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Düzenle")
                    }
                    IconButton(onClick = { showDelete = true }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Sil",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                },
            )
        },
    ) { padding ->
        if (summary == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Alacak bulunamadı")
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SectionCard {
                    Row(Modifier.fillMaxWidth()) {
                        StatTile(
                            label = "Toplam alacak",
                            amountMinor = summary.receivable.totalAmountMinor,
                            currencySymbol = state.currencySymbol,
                            modifier = Modifier.weight(1f),
                        )
                        StatTile(
                            label = "Tahsil edilen",
                            amountMinor = summary.collectedMinor,
                            currencySymbol = state.currencySymbol,
                            valueColor = colors.income,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    StatTile(
                        label = "Kalan",
                        amountMinor = summary.remainingMinor,
                        currencySymbol = state.currencySymbol,
                        emphasize = true,
                    )
                    Spacer(Modifier.height(12.dp))
                    ProgressRow(progress = summary.progress, color = colors.income, height = 10)

                    summary.receivable.dueDate?.let { due ->
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "Son ödeme: ${
                                DateUtils.formatFull(DateUtils.fromEpochDay(due))
                            }",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    if (summary.receivable.note.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = summary.receivable.note,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item {
                Button(
                    onClick = { showCollection = true },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Ödeme al") }
            }

            if (state.collections.isNotEmpty()) {
                item {
                    Text(
                        text = "Ödeme geçmişi",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                items(state.collections, key = { "col-${it.id}" }) { collection ->
                    SectionCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = DateUtils.formatFull(
                                        DateUtils.fromEpochDay(collection.date)
                                    ),
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                                if (collection.note.isNotBlank()) {
                                    Text(
                                        text = collection.note,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            Text(
                                text = "+${
                                    Money.format(collection.amountMinor, state.currencySymbol)
                                }",
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.income,
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDelete) {
        ConfirmDialog(
            title = "Alacak silinsin mi?",
            message = "Bu alacak ve ona ait tüm tahsilat kayıtları kalıcı olarak silinecek.",
            confirmLabel = "Sil",
            destructive = true,
            onConfirm = {
                showDelete = false
                viewModel.delete()
            },
            onDismiss = { showDelete = false },
        )
    }

    if (showCollection) {
        CollectionDialog(
            currencySymbol = state.currencySymbol,
            onDismiss = { showCollection = false },
            onConfirm = { amountText, date ->
                viewModel.addCollection(amountText, date)
                showCollection = false
            },
        )
    }
}

@Composable
private fun CollectionDialog(
    currencySymbol: String,
    onDismiss: () -> Unit,
    onConfirm: (String, LocalDate) -> Unit,
) {
    var amountText by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(DateUtils.today()) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ödeme al") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AmountField(
                    value = amountText,
                    onValueChange = { amountText = it; error = null },
                    currencySymbol = currencySymbol,
                    isError = error != null,
                    errorMessage = error,
                )
                DateField(date = date, onDateChange = { date = it })
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val parsed = Money.parse(amountText)
                    if (parsed == null || parsed <= 0L) {
                        error = "Geçerli bir tutar girin"
                    } else {
                        onConfirm(amountText, date)
                    }
                }
            ) { Text("Kaydet") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } },
    )
}
