package com.desert.finansim.ui.screens.transactions

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.desert.finansim.domain.DateUtils
import com.desert.finansim.domain.Money
import com.desert.finansim.domain.model.TransactionItem
import com.desert.finansim.domain.model.TransactionType
import com.desert.finansim.ui.components.ConfirmDialog
import com.desert.finansim.ui.components.EmptyState
import com.desert.finansim.ui.components.SectionCard
import com.desert.finansim.ui.components.TransactionRow
import com.desert.finansim.ui.containerViewModel
import com.desert.finansim.ui.theme.FinansimTheme

@Composable
fun TransactionsScreen(
    onEditTransaction: (Long, TransactionType) -> Unit,
) {
    val viewModel = containerViewModel { TransactionsViewModel(it) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = FinansimTheme.financeColors

    var pendingDelete by remember { mutableStateOf<TransactionItem?>(null) }
    var showCategoryFilter by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "İşlemler",
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(Modifier.height(10.dp))

            // Tur filtreleri
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = state.filters.type == null,
                    onClick = { viewModel.setType(null) },
                    label = { Text("Tümü") },
                )
                TransactionType.entries.forEach { type ->
                    FilterChip(
                        selected = state.filters.type == type,
                        onClick = { viewModel.setType(type) },
                        label = { Text(type.label) },
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Tarih filtreleri
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DateRangeFilter.entries.forEach { range ->
                    FilterChip(
                        selected = state.filters.customRange == null &&
                            state.filters.dateRange == range,
                        onClick = { viewModel.setDateRange(range) },
                        label = { Text(range.label) },
                    )
                }
                FilterChip(
                    selected = state.filters.categoryId != null,
                    onClick = { showCategoryFilter = !showCategoryFilter },
                    label = {
                        Text(
                            state.categories
                                .firstOrNull { it.id == state.filters.categoryId }
                                ?.name
                                ?: "Kategori"
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.FilterList,
                            contentDescription = null,
                            modifier = Modifier.height(16.dp),
                        )
                    },
                )
            }

            if (showCategoryFilter) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = state.filters.categoryId == null,
                        onClick = { viewModel.setCategory(null) },
                        label = { Text("Hepsi") },
                    )
                    state.categories.forEach { category ->
                        FilterChip(
                            selected = state.filters.categoryId == category.id,
                            onClick = { viewModel.setCategory(category.id) },
                            label = { Text(category.name) },
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))

        if (state.items.isEmpty()) {
            EmptyState(
                icon = Icons.Default.Receipt,
                title = "Bu filtrede işlem yok",
                message = "Farklı bir tarih aralığı seçebilir veya filtreleri temizleyebilirsin.",
                actionLabel = "Filtreleri temizle",
                onAction = viewModel::clearFilters,
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    SectionCard {
                        Row(Modifier.fillMaxWidth()) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = "Giriş",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = Money.format(state.incomeTotal, state.currencySymbol),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = colors.income,
                                )
                            }
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = "Çıkış",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = Money.format(state.expenseTotal, state.currencySymbol),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = colors.expense,
                                )
                            }
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = "Fark",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = Money.format(
                                        state.incomeTotal - state.expenseTotal,
                                        state.currencySymbol,
                                    ),
                                    style = MaterialTheme.typography.titleLarge,
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "${state.items.size} işlem",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // Gune gore grupla
                val grouped = state.items.groupBy { it.date }
                grouped.forEach { (date, dayItems) ->
                    item(key = "header-${date.toEpochDay()}") {
                        Text(
                            text = DateUtils.formatFull(date),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    items(
                        items = dayItems,
                        key = { "tx-${it.transaction.id}" },
                    ) { item ->
                        SectionCard {
                            TransactionRow(
                                item = item,
                                currencySymbol = state.currencySymbol,
                                showDate = false,
                                onClick = {
                                    onEditTransaction(item.transaction.id, item.transaction.type)
                                },
                            )
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                            ) {
                                TextButton(onClick = { pendingDelete = item }) {
                                    Text("Sil", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    pendingDelete?.let { item ->
        ConfirmDialog(
            title = "İşlem silinsin mi?",
            message = "\"${item.transaction.title}\" işlemi kalıcı olarak silinecek. " +
                "Bu işlem bir taksite bağlıysa taksit yeniden \"bekliyor\" durumuna dönmez; " +
                "taksiti borç detayından geri alabilirsin.",
            confirmLabel = "Sil",
            destructive = true,
            onConfirm = {
                viewModel.delete(item.transaction.id)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }
}
