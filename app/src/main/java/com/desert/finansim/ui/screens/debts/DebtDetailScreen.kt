package com.desert.finansim.ui.screens.debts

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.desert.finansim.data.local.InstallmentEntity
import com.desert.finansim.domain.DateUtils
import com.desert.finansim.domain.Money
import com.desert.finansim.domain.model.InstallmentStatus
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
fun DebtDetailScreen(
    debtId: Long,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
) {
    val viewModel = containerViewModel(key = "debt-detail-$debtId") {
        DebtDetailViewModel(it, debtId)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val deleted by viewModel.deleted.collectAsStateWithLifecycle()
    val colors = FinansimTheme.financeColors

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showPaymentDialog by remember { mutableStateOf(false) }

    LaunchedEffect(deleted) { if (deleted) onBack() }

    val summary = state.summary

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = summary?.debt?.name ?: "Borç",
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
                    IconButton(onClick = { onEdit(debtId) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Düzenle")
                    }
                    IconButton(onClick = { showDeleteConfirm = true }) {
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
                Text("Borç bulunamadı", style = MaterialTheme.typography.bodyLarge)
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
                            label = "Toplam borç",
                            amountMinor = summary.debt.totalAmountMinor,
                            currencySymbol = state.currencySymbol,
                            modifier = Modifier.weight(1f),
                        )
                        StatTile(
                            label = "Ödenen",
                            amountMinor = summary.paidMinor,
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
                        valueColor = if (summary.isSettled) colors.income else colors.expense,
                        emphasize = true,
                    )
                    Spacer(Modifier.height(12.dp))
                    ProgressRow(
                        progress = summary.progress,
                        color = if (summary.isSettled) colors.income else colors.debt,
                        height = 10,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "%${(summary.progress * 100).toInt()} tamamlandı",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    summary.nextDueDate()?.let { due ->
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "Sonraki ödeme: ${DateUtils.formatFull(due)} " +
                                "(${DateUtils.relativeLabel(due)})",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }

                    if (summary.debt.interestRate != null) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Yıllık faiz: %${summary.debt.interestRate}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (summary.debt.note.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = summary.debt.note,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Button(
                        onClick = { showPaymentDialog = true },
                        modifier = Modifier.weight(1f),
                    ) { Text("Ödeme ekle") }
                    OutlinedButton(
                        onClick = { viewModel.toggleClosed(!summary.debt.isClosed) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(if (summary.debt.isClosed) "Yeniden aç" else "Kapat")
                    }
                }
            }

            if (summary.installments.isNotEmpty()) {
                item {
                    Text(
                        text = "Taksitler",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                items(summary.installments, key = { "inst-${it.id}" }) { installment ->
                    InstallmentRow(
                        installment = installment,
                        status = summary.statusOf(installment),
                        currencySymbol = state.currencySymbol,
                        onMarkPaid = { viewModel.markPaid(installment) },
                        onUndo = { viewModel.undoPaid(installment) },
                    )
                }
            }

            if (state.payments.isNotEmpty()) {
                item {
                    Text(
                        text = "Ödeme geçmişi",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                items(state.payments, key = { "pay-${it.id}" }) { payment ->
                    SectionCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = payment.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = DateUtils.formatFull(
                                        DateUtils.fromEpochDay(payment.date)
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                text = Money.format(payment.amountMinor, state.currencySymbol),
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.debt,
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        ConfirmDialog(
            title = "Borç silinsin mi?",
            message = "Bu borç, taksitleri ve ona yapılmış tüm ödeme kayıtları kalıcı olarak " +
                "silinecek. Bu işlem geri alınamaz.",
            confirmLabel = "Sil",
            destructive = true,
            onConfirm = {
                showDeleteConfirm = false
                viewModel.deleteDebt()
            },
            onDismiss = { showDeleteConfirm = false },
        )
    }

    if (showPaymentDialog) {
        PaymentDialog(
            currencySymbol = state.currencySymbol,
            onDismiss = { showPaymentDialog = false },
            onConfirm = { amountText, date ->
                viewModel.addPayment(amountText, date) { }
                showPaymentDialog = false
            },
        )
    }
}

@Composable
private fun InstallmentRow(
    installment: InstallmentEntity,
    status: InstallmentStatus,
    currencySymbol: String,
    onMarkPaid: () -> Unit,
    onUndo: () -> Unit,
) {
    val colors = FinansimTheme.financeColors
    val statusColor = when (status) {
        InstallmentStatus.PAID -> colors.income
        InstallmentStatus.OVERDUE -> colors.expense
        InstallmentStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "${installment.number}/${installment.totalCount}",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = "${DateUtils.formatFull(DateUtils.fromEpochDay(installment.dueDate))}" +
                        " • ${status.label}",
                    style = MaterialTheme.typography.bodySmall,
                    color = statusColor,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = Money.format(installment.amountMinor, currencySymbol),
                    style = MaterialTheme.typography.titleMedium,
                )
                if (status == InstallmentStatus.PAID) {
                    TextButton(
                        onClick = onUndo,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    ) { Text("Geri al", style = MaterialTheme.typography.labelMedium) }
                } else {
                    TextButton(
                        onClick = onMarkPaid,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    ) { Text("Ödendi", style = MaterialTheme.typography.labelMedium) }
                }
            }
        }
    }
}

/** Serbest odeme girisi. */
@Composable
private fun PaymentDialog(
    currencySymbol: String,
    onDismiss: () -> Unit,
    onConfirm: (String, LocalDate) -> Unit,
) {
    var amountText by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(DateUtils.today()) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ödeme ekle") },
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
                    // Dogrulama burada yapilir; gecersizse dialog kapanmaz.
                    val parsed = Money.parse(amountText)
                    if (parsed == null || parsed <= 0L) {
                        error = "Geçerli bir tutar girin"
                    } else {
                        onConfirm(amountText, date)
                    }
                }
            ) { Text("Kaydet") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Vazgeç") }
        },
    )
}
