package com.desert.finansim.ui.screens.debts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.RequestQuote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.desert.finansim.domain.DateUtils
import com.desert.finansim.domain.Money
import com.desert.finansim.domain.model.CreditCardSummary
import com.desert.finansim.domain.model.DebtSummary
import com.desert.finansim.domain.model.ReceivableSummary
import com.desert.finansim.ui.components.EmptyState
import com.desert.finansim.ui.components.ProgressRow
import com.desert.finansim.ui.components.SectionCard
import com.desert.finansim.ui.components.StatTile
import com.desert.finansim.ui.containerViewModel
import com.desert.finansim.ui.theme.FinansimTheme

private val TABS = listOf("Borçlar", "Alacaklar", "Kartlar")

@Composable
fun DebtsScreen(
    onAddDebt: () -> Unit,
    onOpenDebt: (Long) -> Unit,
    onAddReceivable: () -> Unit,
    onOpenReceivable: (Long) -> Unit,
    onAddCard: () -> Unit,
    onOpenCard: (Long) -> Unit,
) {
    val viewModel = containerViewModel { DebtsViewModel(it) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    val colors = FinansimTheme.financeColors

    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text("Borçlar & Alacaklar", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth()) {
                StatTile(
                    label = "Toplam Borç",
                    amountMinor = state.totalDebtRemaining,
                    currencySymbol = state.currencySymbol,
                    valueColor = colors.expense,
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    label = "Toplam Alacak",
                    amountMinor = state.totalReceivableRemaining,
                    currencySymbol = state.currencySymbol,
                    valueColor = colors.income,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        TabRow(selectedTabIndex = selectedTab) {
            TABS.forEachIndexed { index, label ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(label) },
                )
            }
        }

        when (selectedTab) {
            0 -> DebtList(state, onAddDebt, onOpenDebt)
            1 -> ReceivableList(state, onAddReceivable, onOpenReceivable)
            else -> CardList(state, onAddCard, onOpenCard)
        }
    }
}

@Composable
private fun DebtList(
    state: DebtsUiState,
    onAdd: () -> Unit,
    onOpen: (Long) -> Unit,
) {
    if (state.debts.isEmpty()) {
        EmptyState(
            icon = Icons.Default.RequestQuote,
            title = "Kayıtlı borç yok",
            message = "Kredi, taksit veya kişisel borçlarını ekleyerek ödeme planını takip et.",
            actionLabel = "Borç ekle",
            onAction = onAdd,
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(state.debts, key = { "debt-${it.debt.id}" }) { summary ->
            DebtCard(summary, state.currencySymbol) { onOpen(summary.debt.id) }
        }
        item {
            TextButton(onClick = onAdd, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Yeni borç ekle")
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

@Composable
private fun ReceivableList(
    state: DebtsUiState,
    onAdd: () -> Unit,
    onOpen: (Long) -> Unit,
) {
    if (state.receivables.isEmpty()) {
        EmptyState(
            icon = Icons.Default.Group,
            title = "Kayıtlı alacak yok",
            message = "Birinden alacağın parayı ekle, ödemeleri tek tek işaretle.",
            actionLabel = "Alacak ekle",
            onAction = onAdd,
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(state.receivables, key = { "rec-${it.receivable.id}" }) { summary ->
            ReceivableCard(summary, state.currencySymbol) { onOpen(summary.receivable.id) }
        }
        item {
            TextButton(onClick = onAdd, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Yeni alacak ekle")
            }
        }
    }
}

@Composable
private fun ReceivableCard(
    summary: ReceivableSummary,
    currencySymbol: String,
    onClick: () -> Unit,
) {
    val colors = FinansimTheme.financeColors
    SectionCard(modifier = Modifier.clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = summary.receivable.personName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = summary.receivable.title.ifBlank { "Alacak" },
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
                    color = colors.income,
                )
                Text(
                    text = if (summary.isSettled) "tahsil edildi" else "kalan",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        ProgressRow(progress = summary.progress, color = colors.income)
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Tahsil edilen ${Money.format(summary.collectedMinor, currencySymbol)} / " +
                Money.format(summary.receivable.totalAmountMinor, currencySymbol),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CardList(
    state: DebtsUiState,
    onAdd: () -> Unit,
    onOpen: (Long) -> Unit,
) {
    if (state.cards.isEmpty()) {
        EmptyState(
            icon = Icons.Default.CreditCard,
            title = "Kayıtlı kart yok",
            message = "Kredi kartlarını ekle; limit kullanımını ve son ödeme tarihini takip et.",
            actionLabel = "Kart ekle",
            onAction = onAdd,
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(state.cards, key = { "card-${it.card.id}" }) { summary ->
            CreditCardItem(summary, state.currencySymbol) { onOpen(summary.card.id) }
        }
        item {
            TextButton(onClick = onAdd, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Yeni kart ekle")
            }
        }
    }
}

@Composable
private fun CreditCardItem(
    summary: CreditCardSummary,
    currencySymbol: String,
    onClick: () -> Unit,
) {
    val colors = FinansimTheme.financeColors
    val accent = Color(summary.card.colorArgb)

    SectionCard(modifier = Modifier.clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.CreditCard, contentDescription = null, tint = accent)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = summary.card.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = summary.card.bank.ifBlank { "Kredi kartı" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = Money.format(summary.availableLimitMinor, currencySymbol),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.income,
                )
                Text(
                    text = "kullanılabilir",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        ProgressRow(
            progress = summary.usageRatio,
            color = if (summary.usageRatio > 0.85f) colors.expense else accent,
        )
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Kullanılan ${Money.format(summary.usedLimitMinor, currencySymbol)} / " +
                    Money.format(summary.card.limitMinor, currencySymbol),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Son ödeme: ${DateUtils.formatDayMonth(summary.nextDueDate())}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
