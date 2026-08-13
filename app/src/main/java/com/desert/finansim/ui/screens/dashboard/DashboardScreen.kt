package com.desert.finansim.ui.screens.dashboard

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
import androidx.compose.foundation.lazy.item
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.desert.finansim.domain.DateUtils
import com.desert.finansim.domain.Money
import com.desert.finansim.domain.model.TransactionType
import com.desert.finansim.domain.model.UpcomingKind
import com.desert.finansim.domain.model.UpcomingPayment
import com.desert.finansim.ui.components.CircleIcon
import com.desert.finansim.ui.components.EmptyState
import com.desert.finansim.ui.components.ProgressRow
import com.desert.finansim.ui.components.SectionCard
import com.desert.finansim.ui.components.StatTile
import com.desert.finansim.ui.components.TransactionRow
import com.desert.finansim.ui.containerViewModel
import com.desert.finansim.ui.theme.FinansimTheme

/**
 * Ana ekran: "telefonu actim ve paramin durumunu hemen gordum" hissi icin
 * en ustte bu ayin ozeti, sonra yaklasan odemeler, borc ozeti ve son islemler.
 */
@Composable
fun DashboardScreen(
    onSeeAllTransactions: () -> Unit,
    onOpenDebts: () -> Unit,
    onOpenDebt: (Long) -> Unit,
    onOpenCard: (Long) -> Unit,
    onOpenPlan: () -> Unit,
    onOpenBudget: () -> Unit,
    onEditTransaction: (Long, TransactionType) -> Unit,
) {
    val viewModel = containerViewModel { DashboardViewModel(it) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val monthKey by viewModel.monthKey.collectAsStateWithLifecycle()
    val currency by viewModel.currencySymbol.collectAsStateWithLifecycle()
    val colors = FinansimTheme.financeColors

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            MonthSelector(
                monthKey = monthKey,
                onPrevious = viewModel::showPreviousMonth,
                onNext = viewModel::showNextMonth,
                onToday = viewModel::showCurrentMonth,
                modifier = Modifier.statusBarsPadding(),
            )
        }

        // --- Bu ayin ozeti -------------------------------------------------
        item {
            SectionCard {
                Row(Modifier.fillMaxWidth()) {
                    StatTile(
                        label = "Gelir",
                        amountMinor = state.totals.incomeMinor,
                        currencySymbol = currency,
                        valueColor = colors.income,
                        modifier = Modifier.weight(1f),
                    )
                    StatTile(
                        label = "Gider",
                        amountMinor = state.totals.expenseMinor,
                        currencySymbol = currency,
                        valueColor = colors.expense,
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth()) {
                    StatTile(
                        label = "Borç Ödemesi",
                        amountMinor = state.totals.debtPaymentMinor,
                        currencySymbol = currency,
                        valueColor = colors.debt,
                        modifier = Modifier.weight(1f),
                    )
                    StatTile(
                        label = "Kalan",
                        amountMinor = state.totals.netMinor,
                        currencySymbol = currency,
                        valueColor = if (state.totals.netMinor >= 0) colors.income else colors.expense,
                        modifier = Modifier.weight(1f),
                        emphasize = true,
                    )
                }
                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Eldeki para",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = Money.format(state.cashOnHandMinor, currency),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (state.cashOnHandMinor >= 0) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            colors.expense
                        },
                    )
                }
            }
        }

        // --- Butce uyarilari ------------------------------------------------
        if (state.budgetWarnings.isNotEmpty()) {
            item {
                SectionCard(
                    title = "Bütçe uyarısı",
                    trailing = {
                        TextButton(onClick = onOpenBudget) { Text("Bütçeler") }
                    },
                ) {
                    state.budgetWarnings.take(3).forEach { status ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (status.isExceeded) colors.expense else colors.warning,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = status.categoryName,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Spacer(Modifier.height(4.dp))
                                ProgressRow(
                                    progress = status.ratio,
                                    color = if (status.isExceeded) colors.expense else colors.warning,
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = "%${(status.ratio * 100).toInt()}",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (status.isExceeded) colors.expense else colors.warning,
                            )
                        }
                    }
                }
            }
        }

        // --- Yaklasan odemeler ----------------------------------------------
        item {
            SectionCard(
                title = "Yaklaşan Ödemeler",
                trailing = {
                    TextButton(onClick = onOpenPlan) { Text("Aylık plan") }
                },
            ) {
                if (state.upcoming.isEmpty()) {
                    Text(
                        text = "Önümüzdeki 30 gün içinde ödeme görünmüyor.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    state.upcoming.take(5).forEach { payment ->
                        UpcomingPaymentRow(
                            payment = payment,
                            currencySymbol = currency,
                            onMarkPaid = payment.installmentId?.let { id ->
                                { viewModel.markInstallmentPaid(id) }
                            },
                            onClick = {
                                when {
                                    payment.debtId != null -> onOpenDebt(payment.debtId)
                                    payment.creditCardId != null -> onOpenCard(payment.creditCardId)
                                    else -> onOpenDebts()
                                }
                            },
                        )
                    }
                }
            }
        }

        // --- Borc ozeti ------------------------------------------------------
        item {
            SectionCard(
                title = "Borç Özeti",
                trailing = {
                    TextButton(onClick = onOpenDebts) { Text("Tümü") }
                },
            ) {
                Row(Modifier.fillMaxWidth()) {
                    StatTile(
                        label = "Toplam Borç",
                        amountMinor = state.totalDebtRemainingMinor,
                        currencySymbol = currency,
                        valueColor = colors.expense,
                        modifier = Modifier.weight(1f),
                    )
                    StatTile(
                        label = "Toplam Alacak",
                        amountMinor = state.totalReceivableRemainingMinor,
                        currencySymbol = currency,
                        valueColor = colors.income,
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(14.dp))
                StatTile(
                    label = "Bu ay ödenecek borç",
                    amountMinor = state.debtDueThisMonthMinor,
                    currencySymbol = currency,
                    valueColor = colors.debt,
                )
            }
        }

        // --- Son islemler -----------------------------------------------------
        item {
            SectionCard(
                title = "Son İşlemler",
                trailing = {
                    TextButton(onClick = onSeeAllTransactions) { Text("Tümü") }
                },
            ) {
                if (state.recentTransactions.isEmpty()) {
                    EmptyState(
                        icon = Icons.Default.Receipt,
                        title = "Henüz işlem yok",
                        message = "Sağ alttaki + butonuna basarak ilk gelir veya giderini ekle.",
                    )
                } else {
                    state.recentTransactions.forEach { item ->
                        TransactionRow(
                            item = item,
                            currencySymbol = currency,
                            onClick = {
                                onEditTransaction(item.transaction.id, item.transaction.type)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthSelector(
    monthKey: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isCurrentMonth = monthKey == DateUtils.currentMonthKey()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = if (isCurrentMonth) "Bu Ay" else "Seçili Ay",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = DateUtils.formatMonthYear(monthKey),
                style = MaterialTheme.typography.headlineSmall,
            )
        }
        if (!isCurrentMonth) {
            TextButton(onClick = onToday) { Text("Bugün") }
        }
        IconButton(onClick = onPrevious) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Önceki ay")
        }
        IconButton(onClick = onNext) {
            Icon(Icons.Default.ChevronRight, contentDescription = "Sonraki ay")
        }
    }
}

@Composable
private fun UpcomingPaymentRow(
    payment: UpcomingPayment,
    currencySymbol: String,
    onMarkPaid: (() -> Unit)?,
    onClick: () -> Unit,
) {
    val colors = FinansimTheme.financeColors
    val accent = when {
        payment.isOverdue -> colors.expense
        payment.isUrgent -> colors.warning
        else -> colors.debt
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Tarih rozeti — gun ve ay tek bakista gorunur.
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accent.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = payment.date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = accent,
                )
                Text(
                    text = DateUtils.MONTH_NAMES[payment.date.monthValue - 1].take(3),
                    style = MaterialTheme.typography.bodySmall,
                    color = accent,
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text(
                text = payment.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${payment.kind.label} • ${DateUtils.relativeLabel(payment.date)}",
                style = MaterialTheme.typography.bodySmall,
                color = if (payment.isUrgent) accent else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.width(8.dp))

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = Money.format(payment.amountMinor, currencySymbol),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
            )
            if (onMarkPaid != null) {
                TextButton(
                    onClick = onMarkPaid,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Ödendi", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
