package com.desert.finansim.ui.screens.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.desert.finansim.domain.DateUtils
import com.desert.finansim.domain.Money
import com.desert.finansim.ui.components.AmountField
import com.desert.finansim.ui.components.ConfirmDialog
import com.desert.finansim.ui.components.DateField
import com.desert.finansim.ui.components.NumberField
import com.desert.finansim.ui.components.ProgressRow
import com.desert.finansim.ui.components.SectionCard
import com.desert.finansim.ui.components.StatTile
import com.desert.finansim.ui.containerViewModel
import com.desert.finansim.ui.theme.FinansimTheme
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditCardFormScreen(
    cardId: Long,
    onDone: () -> Unit,
) {
    val viewModel = containerViewModel(key = "card-form-$cardId") {
        CreditCardFormViewModel(it, cardId)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currency by viewModel.currencySymbol.collectAsStateWithLifecycle()

    LaunchedEffect(state.saved) { if (state.saved) onDone() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditing) "Kartı Düzenle" else "Kredi Kartı Ekle") },
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
                value = state.name,
                onValueChange = viewModel::setName,
                label = { Text("Kart adı") },
                singleLine = true,
                isError = state.nameError != null,
                supportingText = state.nameError?.let {
                    { Text(it, color = MaterialTheme.colorScheme.error) }
                },
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = state.bank,
                onValueChange = viewModel::setBank,
                label = { Text("Banka (isteğe bağlı)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            AmountField(
                value = state.limitText,
                onValueChange = viewModel::setLimit,
                currencySymbol = currency,
                label = "Kart limiti",
                isError = state.limitError != null,
                errorMessage = state.limitError,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.weight(1f)) {
                    NumberField(
                        value = state.statementDayText,
                        onValueChange = viewModel::setStatementDay,
                        label = "Ekstre günü",
                        isError = state.dayError != null,
                    )
                }
                Box(Modifier.weight(1f)) {
                    NumberField(
                        value = state.dueDayText,
                        onValueChange = viewModel::setDueDay,
                        label = "Son ödeme günü",
                        isError = state.dayError != null,
                        errorMessage = state.dayError,
                    )
                }
            }

            Text("Kart rengi", style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CARD_COLORS.forEach { colorValue ->
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(colorValue))
                            .border(
                                width = if (state.colorArgb == colorValue) 3.dp else 0.dp,
                                color = MaterialTheme.colorScheme.onSurface,
                                shape = CircleShape,
                            )
                            .clickable { viewModel.setColor(colorValue) },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (state.colorArgb == colorValue) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }

            Button(
                onClick = viewModel::save,
                enabled = !state.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Text(if (state.isEditing) "Güncelle" else "Kartı Kaydet")
            }

            Text(
                text = "Kredi kartıyla yaptığın harcamalar giderlerine yansır ama nakitten " +
                    "düşülmez; nakit ancak ekstreyi ödediğinde azalır.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditCardDetailScreen(
    cardId: Long,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
) {
    val viewModel = containerViewModel(key = "card-detail-$cardId") {
        CreditCardDetailViewModel(it, cardId)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val deleted by viewModel.deleted.collectAsStateWithLifecycle()
    val colors = FinansimTheme.financeColors

    var showDelete by remember { mutableStateOf(false) }
    var showPayment by remember { mutableStateOf(false) }

    LaunchedEffect(deleted) { if (deleted) onBack() }

    val summary = state.summary

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = summary?.card?.name ?: "Kredi Kartı",
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
                    IconButton(onClick = { onEdit(cardId) }) {
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
                Text("Kart bulunamadı")
            }
            return@Scaffold
        }

        val accent = Color(summary.card.colorArgb)
        val categoriesById = state.categories.associateBy { it.id }

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
                            label = "Kullanılan limit",
                            amountMinor = summary.usedLimitMinor,
                            currencySymbol = state.currencySymbol,
                            valueColor = colors.expense,
                            modifier = Modifier.weight(1f),
                        )
                        StatTile(
                            label = "Kalan limit",
                            amountMinor = summary.availableLimitMinor,
                            currencySymbol = state.currencySymbol,
                            valueColor = colors.income,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    ProgressRow(
                        progress = summary.usageRatio,
                        color = if (summary.usageRatio > 0.85f) colors.expense else accent,
                        height = 10,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Toplam limit ${
                            Money.format(summary.card.limitMinor, state.currencySymbol)
                        } • %${(summary.usageRatio * 100).toInt()} kullanıldı",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Ekstre günü: her ayın ${summary.card.statementDay}'i",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = "Son ödeme: ${DateUtils.formatFull(summary.nextDueDate())}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth()) {
                        StatTile(
                            label = "Toplam harcama",
                            amountMinor = summary.spentMinor,
                            currencySymbol = state.currencySymbol,
                            modifier = Modifier.weight(1f),
                        )
                        StatTile(
                            label = "Toplam ödeme",
                            amountMinor = summary.paidMinor,
                            currencySymbol = state.currencySymbol,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            item {
                Button(
                    onClick = { showPayment = true },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Ekstre ödemesi ekle") }
            }

            if (state.transactions.isEmpty()) {
                item {
                    Text(
                        text = "Bu karta ait henüz hareket yok. Gider eklerken ödeme yöntemi " +
                            "olarak bu kartı seçtiğinde harcamalar burada görünür.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                item {
                    Text(
                        text = "Kart hareketleri",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                items(state.transactions, key = { "cardtx-${it.id}" }) { transaction ->
                    val isPayment = transaction.type ==
                        com.desert.finansim.domain.model.TransactionType.DEBT_PAYMENT
                    SectionCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = transaction.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = buildString {
                                        append(
                                            transaction.categoryId
                                                ?.let { categoriesById[it]?.name }
                                                ?: if (isPayment) "Ekstre ödemesi" else "Harcama"
                                        )
                                        append(" • ")
                                        append(
                                            DateUtils.formatDayMonth(
                                                DateUtils.fromEpochDay(transaction.date)
                                            )
                                        )
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                text = (if (isPayment) "-" else "+") +
                                    Money.format(transaction.amountMinor, state.currencySymbol),
                                style = MaterialTheme.typography.titleMedium,
                                color = if (isPayment) colors.income else colors.expense,
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDelete) {
        ConfirmDialog(
            title = "Kart silinsin mi?",
            message = "Kart silinecek. Bu karta işlenmiş harcamalar silinmez, sadece kart " +
                "bağlantıları kaldırılır.",
            confirmLabel = "Sil",
            destructive = true,
            onConfirm = {
                showDelete = false
                viewModel.delete()
            },
            onDismiss = { showDelete = false },
        )
    }

    if (showPayment) {
        CardPaymentDialog(
            currencySymbol = state.currencySymbol,
            onDismiss = { showPayment = false },
            onConfirm = { amountText, date ->
                viewModel.addPayment(amountText, date)
                showPayment = false
            },
        )
    }
}

@Composable
private fun CardPaymentDialog(
    currencySymbol: String,
    onDismiss: () -> Unit,
    onConfirm: (String, LocalDate) -> Unit,
) {
    var amountText by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(DateUtils.today()) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ekstre ödemesi") },
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
