package com.desert.finansim.ui.screens.recurring

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.item
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.desert.finansim.data.local.RecurringRuleEntity
import com.desert.finansim.domain.DateUtils
import com.desert.finansim.domain.Money
import com.desert.finansim.domain.model.PaymentMethod
import com.desert.finansim.domain.model.RecurrenceFrequency
import com.desert.finansim.domain.model.TransactionType
import com.desert.finansim.ui.components.AmountField
import com.desert.finansim.ui.components.ConfirmDialog
import com.desert.finansim.ui.components.DateField
import com.desert.finansim.ui.components.DropdownField
import com.desert.finansim.ui.components.EmptyState
import com.desert.finansim.ui.components.NumberField
import com.desert.finansim.ui.components.SectionCard
import com.desert.finansim.ui.components.StatTile
import com.desert.finansim.ui.containerViewModel
import com.desert.finansim.ui.theme.FinansimTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringListScreen(
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
) {
    val viewModel = containerViewModel { RecurringListViewModel(it) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = FinansimTheme.financeColors
    var pendingDelete by remember { mutableStateOf<RecurringRuleEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sabit Gelir & Giderler") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Icon(Icons.Default.Add, contentDescription = "Ekle")
            }
        },
    ) { padding ->
        if (state.items.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(padding)) {
                EmptyState(
                    icon = Icons.Default.Repeat,
                    title = "Sabit gider tanımlı değil",
                    message = "Kira, internet, abonelik gibi her ay tekrarlayan ödemeleri " +
                        "tanımla; uygulama bunları otomatik olarak ilgili aya işlesin.",
                    actionLabel = "Sabit gider ekle",
                    onAction = onAdd,
                )
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SectionCard {
                    Row(Modifier.fillMaxWidth()) {
                        StatTile(
                            label = "Aylık sabit gelir",
                            amountMinor = state.monthlyIncomeTotal,
                            currencySymbol = state.currencySymbol,
                            valueColor = colors.income,
                            modifier = Modifier.weight(1f),
                        )
                        StatTile(
                            label = "Aylık sabit gider",
                            amountMinor = state.monthlyExpenseTotal,
                            currencySymbol = state.currencySymbol,
                            valueColor = colors.expense,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            items(state.items, key = { "rule-${it.rule.id}" }) { item ->
                SectionCard(modifier = Modifier.clickable { onEdit(item.rule.id) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = item.rule.title,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = buildString {
                                    append(item.rule.frequency.label)
                                    if (item.rule.frequency == RecurrenceFrequency.MONTHLY) {
                                        append(" • ayın ${item.rule.dayOfMonth ?: 1}'i")
                                    }
                                    item.categoryName?.let { append(" • $it") }
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            item.nextOccurrence?.let { next ->
                                Text(
                                    text = "Sonraki: ${DateUtils.formatFull(next)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = Money.format(item.rule.amountMinor, state.currencySymbol),
                                style = MaterialTheme.typography.titleMedium,
                                color = if (item.rule.type == TransactionType.INCOME) {
                                    colors.income
                                } else {
                                    colors.expense
                                },
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Switch(
                                    checked = item.rule.isActive,
                                    onCheckedChange = {
                                        viewModel.setActive(item.rule.id, it)
                                    },
                                )
                                IconButton(onClick = { pendingDelete = item.rule }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Sil",
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Sabit kayıtlar uygulama her açıldığında ve günde bir kez otomatik " +
                        "olarak işlenir. Geçmiş aylar da dahil, atlanmış dönemler yakalanır.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    pendingDelete?.let { rule ->
        ConfirmDialog(
            title = "Sabit kayıt silinsin mi?",
            message = "\"${rule.title}\" bundan sonra otomatik oluşturulmayacak. " +
                "Daha önce oluşturulmuş işlemler silinmez.",
            confirmLabel = "Sil",
            destructive = true,
            onConfirm = {
                viewModel.delete(rule)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringFormScreen(
    ruleId: Long,
    onDone: () -> Unit,
) {
    val viewModel = containerViewModel(key = "rec-rule-$ruleId") {
        RecurringFormViewModel(it, ruleId)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val options by viewModel.options.collectAsStateWithLifecycle()

    LaunchedEffect(state.saved) { if (state.saved) onDone() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditing) "Sabit Kaydı Düzenle" else "Sabit Kayıt Ekle") },
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

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.type == TransactionType.EXPENSE,
                    onClick = { viewModel.setType(TransactionType.EXPENSE) },
                    label = { Text("Gider") },
                )
                FilterChip(
                    selected = state.type == TransactionType.INCOME,
                    onClick = { viewModel.setType(TransactionType.INCOME) },
                    label = { Text("Gelir") },
                )
            }

            OutlinedTextField(
                value = state.title,
                onValueChange = viewModel::setTitle,
                label = { Text("Ad (ör. Kira, Netflix)") },
                singleLine = true,
                isError = state.titleError != null,
                supportingText = state.titleError?.let {
                    { Text(it, color = MaterialTheme.colorScheme.error) }
                },
                modifier = Modifier.fillMaxWidth(),
            )

            AmountField(
                value = state.amountText,
                onValueChange = viewModel::setAmount,
                currencySymbol = options.currencySymbol,
                isError = state.amountError != null,
                errorMessage = state.amountError,
            )

            val categories = viewModel.categoriesForType(options.categories)
            DropdownField(
                label = "Kategori",
                options = categories,
                selected = categories.firstOrNull { it.id == state.categoryId },
                optionLabel = { it.name },
                onSelect = { viewModel.setCategory(it.id) },
            )

            DropdownField(
                label = "Tekrar sıklığı",
                options = RecurrenceFrequency.entries,
                selected = state.frequency,
                optionLabel = { it.label },
                onSelect = viewModel::setFrequency,
            )

            when (state.frequency) {
                RecurrenceFrequency.MONTHLY -> NumberField(
                    value = state.dayOfMonthText,
                    onValueChange = viewModel::setDayOfMonth,
                    label = "Ayın kaçında",
                    isError = state.dayError != null,
                    errorMessage = state.dayError,
                    suffix = ". günü",
                )

                RecurrenceFrequency.WEEKLY -> DropdownField(
                    label = "Haftanın günü",
                    options = (1..7).toList(),
                    selected = state.dayOfWeek,
                    optionLabel = { DateUtils.DAY_NAMES[it - 1] },
                    onSelect = viewModel::setDayOfWeek,
                )

                RecurrenceFrequency.YEARLY -> {
                    DropdownField(
                        label = "Ay",
                        options = (1..12).toList(),
                        selected = state.monthOfYear,
                        optionLabel = { DateUtils.MONTH_NAMES[it - 1] },
                        onSelect = viewModel::setMonthOfYear,
                    )
                    NumberField(
                        value = state.dayOfMonthText,
                        onValueChange = viewModel::setDayOfMonth,
                        label = "Ayın kaçında",
                        isError = state.dayError != null,
                        errorMessage = state.dayError,
                    )
                }
            }

            if (state.type == TransactionType.EXPENSE) {
                DropdownField(
                    label = "Ödeme yöntemi",
                    options = PaymentMethod.entries,
                    selected = state.paymentMethod,
                    optionLabel = { it.label },
                    onSelect = viewModel::setPaymentMethod,
                )
                if (state.paymentMethod == PaymentMethod.CREDIT_CARD && options.cards.isNotEmpty()) {
                    DropdownField(
                        label = "Kart",
                        options = options.cards,
                        selected = options.cards.firstOrNull { it.id == state.creditCardId },
                        optionLabel = { it.name },
                        onSelect = { viewModel.setCreditCard(it.id) },
                        placeholder = "Kart seçin",
                    )
                }
            }

            DateField(
                date = state.startDate,
                onDateChange = viewModel::setStartDate,
                label = "Başlangıç tarihi",
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Bitiş tarihi var", style = MaterialTheme.typography.bodyLarge)
                }
                Switch(checked = state.hasEndDate, onCheckedChange = viewModel::setHasEndDate)
            }
            if (state.hasEndDate) {
                DateField(
                    date = state.endDate,
                    onDateChange = viewModel::setEndDate,
                    label = "Bitiş tarihi",
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Aktif", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = "Pasif yaparsan yeni işlem oluşturulmaz",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = state.isActive, onCheckedChange = viewModel::setActive)
            }

            Button(
                onClick = viewModel::save,
                enabled = !state.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Text(if (state.isEditing) "Güncelle" else "Kaydet")
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
