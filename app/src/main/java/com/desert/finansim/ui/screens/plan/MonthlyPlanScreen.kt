package com.desert.finansim.ui.screens.plan

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.desert.finansim.domain.DateUtils
import com.desert.finansim.domain.Money
import com.desert.finansim.ui.components.SectionCard
import com.desert.finansim.ui.components.StatTile
import com.desert.finansim.ui.containerViewModel
import com.desert.finansim.ui.theme.FinansimTheme

/**
 * Aylik plan: gerceklesen ile beklenen tutarlar bilerek ayri satirlarda
 * gosterilir; boylece "bu ay ne oldu" ile "bu ay daha ne olacak" karismaz.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyPlanScreen(onBack: () -> Unit) {
    val viewModel = containerViewModel { MonthlyPlanViewModel(it) }
    val plan by viewModel.plan.collectAsStateWithLifecycle()
    val monthKey by viewModel.monthKey.collectAsStateWithLifecycle()
    val currency by viewModel.currencySymbol.collectAsStateWithLifecycle()
    val colors = FinansimTheme.financeColors

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Aylık Plan") },
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
                SectionCard(title = "Ay sonu tahmini") {
                    StatTile(
                        label = "Beklenen kalan",
                        amountMinor = plan.projectedNetMinor,
                        currencySymbol = currency,
                        valueColor = if (plan.projectedNetMinor >= 0) colors.income else colors.expense,
                        emphasize = true,
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(Modifier.fillMaxWidth()) {
                        StatTile(
                            label = "Beklenen gelir",
                            amountMinor = plan.projectedIncomeMinor,
                            currencySymbol = currency,
                            valueColor = colors.income,
                            modifier = Modifier.weight(1f),
                        )
                        StatTile(
                            label = "Beklenen gider",
                            amountMinor = plan.projectedExpenseMinor,
                            currencySymbol = currency,
                            valueColor = colors.expense,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    StatTile(
                        label = "Beklenen borç ödemesi",
                        amountMinor = plan.projectedDebtPaymentMinor,
                        currencySymbol = currency,
                        valueColor = colors.debt,
                    )
                }
            }

            item {
                SectionCard(title = "Gerçekleşen / Beklenen") {
                    PlanRow(
                        label = "Gelir",
                        actual = plan.actualIncomeMinor,
                        expected = plan.expectedIncomeMinor,
                        currency = currency,
                        color = colors.income,
                    )
                    HorizontalDivider(
                        Modifier.padding(vertical = 10.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                    )
                    PlanRow(
                        label = "Gider",
                        actual = plan.actualExpenseMinor,
                        expected = plan.expectedExpenseMinor,
                        currency = currency,
                        color = colors.expense,
                    )
                    HorizontalDivider(
                        Modifier.padding(vertical = 10.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                    )
                    PlanRow(
                        label = "Borç ödemesi",
                        actual = plan.actualDebtPaymentMinor,
                        expected = plan.expectedDebtPaymentMinor,
                        currency = currency,
                        color = colors.debt,
                    )
                }
            }

            item {
                SectionCard(title = "Şu ana kadar gerçekleşen") {
                    StatTile(
                        label = "Net durum",
                        amountMinor = plan.actualNetMinor,
                        currencySymbol = currency,
                        valueColor = if (plan.actualNetMinor >= 0) colors.income else colors.expense,
                        emphasize = true,
                    )
                }
            }

            item {
                Text(
                    text = "Beklenen tutarlar, tanımlı sabit gelir/giderlerin ve bu ay vadesi " +
                        "gelen taksitlerin henüz gerçekleşmemiş kısmından hesaplanır.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PlanRow(
    label: String,
    actual: Long,
    expected: Long,
    currency: String,
    color: Color,
) {
    Column(Modifier.fillMaxWidth()) {
        Text(text = label, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "Gerçekleşen",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = Money.format(actual, currency),
                    style = MaterialTheme.typography.titleMedium,
                    color = color,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Bekleyen",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = Money.format(expected, currency),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
