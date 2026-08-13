package com.desert.finansim.ui.screens.reports

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
import androidx.compose.foundation.lazy.item
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.desert.finansim.domain.DateUtils
import com.desert.finansim.ui.components.CategoryDonutChart
import com.desert.finansim.ui.components.IncomeExpenseBarChart
import com.desert.finansim.ui.components.NetBarChart
import com.desert.finansim.ui.components.ProgressRow
import com.desert.finansim.ui.components.SectionCard
import com.desert.finansim.ui.components.StatTile
import com.desert.finansim.ui.containerViewModel
import com.desert.finansim.ui.theme.FinansimTheme

@Composable
fun ReportsScreen(
    onOpenPlan: () -> Unit,
    onOpenBudget: () -> Unit,
) {
    val viewModel = containerViewModel { ReportsViewModel(it) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val monthKey by viewModel.monthKey.collectAsStateWithLifecycle()
    val colors = FinansimTheme.financeColors

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text(
                text = "Raporlar",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(vertical = 4.dp),
            )
        }

        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                TextButton(onClick = onOpenPlan, modifier = Modifier.weight(1f)) {
                    Text("Aylık plan")
                }
                TextButton(onClick = onOpenBudget, modifier = Modifier.weight(1f)) {
                    Text("Bütçeler")
                }
            }
        }

        // --- Son 6 ay: gelir / gider ---------------------------------------
        item {
            SectionCard(title = "Son 6 ay: gelir / gider") {
                if (state.monthlyHistory.all { it.incomeMinor == 0L && it.expenseMinor == 0L }) {
                    Text(
                        text = "Grafik için henüz yeterli veri yok.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    IncomeExpenseBarChart(
                        data = state.monthlyHistory,
                        currencySymbol = state.currencySymbol,
                    )
                }
            }
        }

        // --- Kategori dagilimi ---------------------------------------------
        item {
            SectionCard(
                title = "Gider kategorileri",
                trailing = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = viewModel::previousMonth) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Önceki ay")
                        }
                        Text(
                            text = DateUtils.formatMonthYear(monthKey),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        IconButton(onClick = viewModel::nextMonth) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Sonraki ay")
                        }
                    }
                },
            ) {
                if (state.categoryBreakdown.isEmpty()) {
                    Text(
                        text = "Bu ay için gider kaydı yok.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    CategoryDonutChart(
                        slices = state.categoryBreakdown,
                        currencySymbol = state.currencySymbol,
                    )
                }
            }
        }

        // --- Aylik net durum ------------------------------------------------
        item {
            SectionCard(title = "Son 12 ay: net durum") {
                if (state.longHistory.all { it.netMinor == 0L }) {
                    Text(
                        text = "Net durum grafiği için henüz veri yok.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    NetBarChart(data = state.longHistory)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Net durum = gelir − (gider + borç ödemesi)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // --- Borc istatistikleri ---------------------------------------------
        item {
            SectionCard(title = "Borç istatistikleri") {
                Row(Modifier.fillMaxWidth()) {
                    StatTile(
                        label = "Toplam borç",
                        amountMinor = state.debtStatistics.totalMinor,
                        currencySymbol = state.currencySymbol,
                        modifier = Modifier.weight(1f),
                    )
                    StatTile(
                        label = "Toplam ödenen",
                        amountMinor = state.debtStatistics.paidMinor,
                        currencySymbol = state.currencySymbol,
                        valueColor = colors.income,
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth()) {
                    StatTile(
                        label = "Toplam kalan",
                        amountMinor = state.debtStatistics.remainingMinor,
                        currencySymbol = state.currencySymbol,
                        valueColor = colors.expense,
                        modifier = Modifier.weight(1f),
                    )
                    StatTile(
                        label = "Bu ay ödenecek",
                        amountMinor = state.debtStatistics.monthlyLoadMinor,
                        currencySymbol = state.currencySymbol,
                        valueColor = colors.debt,
                        modifier = Modifier.weight(1f),
                    )
                }

                if (state.debtStatistics.totalMinor > 0) {
                    Spacer(Modifier.height(14.dp))
                    ProgressRow(
                        progress = com.desert.finansim.domain.Money.percent(
                            state.debtStatistics.paidMinor,
                            state.debtStatistics.totalMinor,
                        ),
                        color = colors.income,
                        height = 10,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "${state.debtStatistics.openCount} açık borç",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
