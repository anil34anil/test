package com.desert.finansim.ui.screens.budget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.item
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.desert.finansim.data.local.CategoryEntity
import com.desert.finansim.domain.DateUtils
import com.desert.finansim.domain.Money
import com.desert.finansim.ui.components.AmountField
import com.desert.finansim.ui.components.ProgressRow
import com.desert.finansim.ui.components.SectionCard
import com.desert.finansim.ui.components.StatTile
import com.desert.finansim.ui.containerViewModel
import com.desert.finansim.ui.theme.FinansimTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(onBack: () -> Unit) {
    val viewModel = containerViewModel { BudgetViewModel(it) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val monthKey by viewModel.monthKey.collectAsStateWithLifecycle()
    val colors = FinansimTheme.financeColors

    var editing by remember { mutableStateOf<CategoryEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bütçeler") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::previousMonth) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Önceki ay")
                    }
                    IconButton(onClick = viewModel::nextMonth) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Sonraki ay")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    text = DateUtils.formatMonthYear(monthKey),
                    style = MaterialTheme.typography.headlineSmall,
                )
            }

            item {
                SectionCard {
                    Row(Modifier.fillMaxWidth()) {
                        StatTile(
                            label = "Toplam bütçe",
                            amountMinor = state.totalBudget,
                            currencySymbol = state.currencySymbol,
                            modifier = Modifier.weight(1f),
                        )
                        StatTile(
                            label = "Harcanan",
                            amountMinor = state.totalSpent,
                            currencySymbol = state.currencySymbol,
                            valueColor = if (state.totalSpent > state.totalBudget && state.totalBudget > 0) {
                                colors.expense
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (state.totalBudget > 0) {
                        Spacer(Modifier.height(12.dp))
                        ProgressRow(
                            progress = Money.percent(state.totalSpent, state.totalBudget),
                            color = if (state.totalSpent > state.totalBudget) {
                                colors.expense
                            } else {
                                colors.income
                            },
                            height = 10,
                        )
                    }
                }
            }

            item {
                TextButton(onClick = viewModel::copyFromPreviousMonth) {
                    Text("Geçen ayın bütçelerini kopyala")
                }
            }

            items(state.rows, key = { "budget-${it.category.id}" }) { row ->
                val exceeded = row.budgetMinor > 0 && row.spentMinor > row.budgetMinor
                val nearLimit = !exceeded && row.budgetMinor > 0 &&
                    Money.percent(row.spentMinor, row.budgetMinor) >= 0.9f

                SectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = row.category.name,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = if (row.budgetMinor > 0) {
                                    "${Money.format(row.spentMinor, state.currencySymbol)} / " +
                                        Money.format(row.budgetMinor, state.currencySymbol)
                                } else {
                                    "Harcanan ${Money.format(row.spentMinor, state.currencySymbol)}" +
                                        " • bütçe yok"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        TextButton(onClick = { editing = row.category }) {
                            Text(if (row.budgetMinor > 0) "Değiştir" else "Bütçe koy")
                        }
                    }

                    if (row.budgetMinor > 0) {
                        Spacer(Modifier.height(10.dp))
                        ProgressRow(
                            progress = Money.percent(row.spentMinor, row.budgetMinor),
                            color = when {
                                exceeded -> colors.expense
                                nearLimit -> colors.warning
                                else -> Color(row.category.colorArgb)
                            },
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = when {
                                exceeded -> "Bütçe ${
                                    Money.format(
                                        row.spentMinor - row.budgetMinor,
                                        state.currencySymbol,
                                    )
                                } aşıldı"

                                else -> "${
                                    Money.format(
                                        row.budgetMinor - row.spentMinor,
                                        state.currencySymbol,
                                    )
                                } kaldı"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = when {
                                exceeded -> colors.expense
                                nearLimit -> colors.warning
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }
            }
        }
    }

    editing?.let { category ->
        BudgetEditDialog(
            categoryName = category.name,
            currentAmountMinor = state.rows.firstOrNull { it.category.id == category.id }?.budgetMinor ?: 0L,
            currencySymbol = state.currencySymbol,
            onDismiss = { editing = null },
            onConfirm = { amountText ->
                viewModel.setBudget(category.id, amountText)
                editing = null
            },
            onRemove = {
                viewModel.removeBudget(category.id)
                editing = null
            },
        )
    }
}

@Composable
private fun BudgetEditDialog(
    categoryName: String,
    currentAmountMinor: Long,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    onRemove: () -> Unit,
) {
    var amountText by remember {
        mutableStateOf(
            if (currentAmountMinor > 0) {
                Money.format(currentAmountMinor, withSymbol = false)
            } else {
                ""
            }
        )
    }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("$categoryName bütçesi") },
        text = {
            Column {
                AmountField(
                    value = amountText,
                    onValueChange = { amountText = it; error = null },
                    currencySymbol = currencySymbol,
                    label = "Aylık bütçe",
                    isError = error != null,
                    errorMessage = error,
                )
                if (currentAmountMinor > 0) {
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = onRemove) {
                        Text("Bütçeyi kaldır", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val parsed = Money.parse(amountText)
                    if (parsed == null || parsed <= 0L) {
                        error = "Geçerli bir tutar girin"
                    } else {
                        onConfirm(amountText)
                    }
                }
            ) { Text("Kaydet") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } },
    )
}
