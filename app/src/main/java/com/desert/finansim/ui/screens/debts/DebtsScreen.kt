package com.desert.finansim.ui.screens.debts

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.RequestQuote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.desert.finansim.domain.DateUtils
import com.desert.finansim.domain.Money
import com.desert.finansim.domain.model.DebtSummary
import com.desert.finansim.ui.components.EmptyState
import com.desert.finansim.ui.components.ProgressRow
import com.desert.finansim.ui.components.SectionCard
import com.desert.finansim.ui.components.StatTile
import com.desert.finansim.ui.containerViewModel
import com.desert.finansim.ui.theme.FinansimTheme

@Composable
fun DebtsScreen(
    onAddDebt: () -> Unit,
    onOpenDebt: (Long) -> Unit,
) {
    val viewModel = containerViewModel { DebtsViewModel(it) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = FinansimTheme.financeColors

    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text("Borçlar", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(12.dp))
            StatTile(
                label = "Toplam Borç",
                amountMinor = state.totalDebtRemaining,
                currencySymbol = state.currencySymbol,
                valueColor = colors.expense,
            )
        }

        if (state.debts.isEmpty()) {
            EmptyState(
                icon = Icons.Default.RequestQuote,
                title = "Kayıtlı borç yok",
                message = "Kredi, taksit veya kişisel borçlarını ekleyerek ödeme planını takip et.",
                actionLabel = "Borç ekle",
                onAction = onAddDebt,
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.debts, key = { "debt-${it.debt.id}" }) { summary ->
                    DebtCard(summary, state.currencySymbol) { onOpenDebt(summary.debt.id) }
                }
                item {
                    TextButton(onClick = onAddDebt, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Yeni borç ekle")
                    }
                }
            }
        }
    }
}

@Composable
private fun DebtCard(
    summary: DebtSummary,
    currencySymbol: String,
    onClick: () -> Unit,
) {
    val colors = FinansimTheme.financeColors
    SectionCard(modifier = Modifier.clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = summary.debt.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = buildString {
                        append(summary.debt.type.label)
                        if (summary.debt.counterparty.isNotBlank()) {
                            append(" • ")
                            append(summary.debt.counterparty)
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = Money.format(summary.remainingMinor, currencySymbol),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (summary.isSettled) colors.income else colors.expense,
                )
                Text(
                    text = if (summary.isSettled) "kapandı" else "kalan",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        ProgressRow(
            progress = summary.progress,
            color = if (summary.isSettled) colors.income else colors.debt,
        )
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Ödenen ${Money.format(summary.paidMinor, currencySymbol)} / " +
                    Money.format(summary.debt.totalAmountMinor, currencySymbol),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            summary.nextDueDate()?.let { due ->
                Text(
                    text = "Sonraki: ${DateUtils.formatDayMonth(due)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
