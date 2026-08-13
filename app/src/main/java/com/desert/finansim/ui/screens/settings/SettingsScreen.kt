package com.desert.finansim.ui.screens.settings

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.desert.finansim.data.backup.RestoreMode
import com.desert.finansim.domain.Money
import com.desert.finansim.domain.model.ThemeMode
import com.desert.finansim.ui.components.AmountField
import com.desert.finansim.ui.components.SectionCard
import com.desert.finansim.ui.components.SettingRow
import com.desert.finansim.ui.containerViewModel
import com.desert.finansim.work.ReminderWorker

@Composable
fun SettingsScreen(
    onOpenCategories: () -> Unit,
    onOpenRecurring: () -> Unit,
    onOpenBudget: () -> Unit,
) {
    val viewModel = containerViewModel { SettingsViewModel(it) }
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showPinDialog by remember { mutableStateOf(false) }
    var showBalanceDialog by remember { mutableStateOf(false) }

    val backupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) viewModel.exportBackup(uri, context.contentResolver)
    }

    val csvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) viewModel.exportCsv(uri, context.contentResolver)
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) viewModel.prepareRestore(uri, context.contentResolver)
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.setNotificationsEnabled(granted)
        if (granted) ReminderWorker.schedule(context)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text(
                text = "Ayarlar",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(vertical = 4.dp),
            )
        }

        if (busy) {
            item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        }

        // --- Gorunum -------------------------------------------------------
        item {
            SectionCard(title = "Görünüm") {
                SettingRow(
                    icon = Icons.Default.DarkMode,
                    title = "Tema",
                    subtitle = settings.themeMode.label,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = settings.themeMode == mode,
                            onClick = { viewModel.setTheme(mode) },
                            label = { Text(mode.label) },
                        )
                    }
                }
            }
        }

        // --- Finansal ------------------------------------------------------
        item {
            SectionCard(title = "Finansal") {
                SettingRow(
                    icon = Icons.Default.Wallet,
                    title = "Başlangıç bakiyesi",
                    subtitle = "Eldeki para hesabına eklenir: " +
                        Money.format(settings.openingBalanceMinor, settings.currencySymbol),
                    onClick = { showBalanceDialog = true },
                )
                SettingRow(
                    icon = Icons.Default.Repeat,
                    title = "Sabit gelir & giderler",
                    subtitle = "Kira, abonelik, düzenli ödemeler",
                    onClick = onOpenRecurring,
                )
                SettingRow(
                    icon = Icons.Default.Savings,
                    title = "Bütçeler",
                    subtitle = "Kategori bazlı aylık limitler",
                    onClick = onOpenBudget,
                )
                SettingRow(
                    icon = Icons.Default.Category,
                    title = "Kategoriler",
                    subtitle = "Gelir ve gider kategorilerini düzenle",
                    onClick = onOpenCategories,
                )
            }
        }

        // --- Bildirimler ---------------------------------------------------
        item {
            SectionCard(title = "Bildirimler") {
                SettingRow(
                    icon = Icons.Default.Notifications,
                    title = "Ödeme hatırlatmaları",
                    subtitle = "Yaklaşan ödemeler için bildirim gönder",
                    trailing = {
                        Switch(
                            checked = settings.notificationsEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notificationPermissionLauncher.launch(
                                        android.Manifest.permission.POST_NOTIFICATIONS
                                    )
                                } else {
                                    viewModel.setNotificationsEnabled(enabled)
                                    if (enabled) ReminderWorker.schedule(context)
                                }
                            },
                        )
                    },
                )
                if (settings.notificationsEnabled) {
                    SettingRow(
                        icon = Icons.Default.Info,
                        title = "Kaç gün önce hatırlatılsın",
                        subtitle = "${settings.reminderDaysBefore} gün önce",
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0, 1, 2, 3, 7).forEach { days ->
                            FilterChip(
                                selected = settings.reminderDaysBefore == days,
                                onClick = { viewModel.setReminderDays(days) },
                                label = { Text(if (days == 0) "Aynı gün" else "$days gün") },
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    SettingRow(
                        icon = Icons.Default.Savings,
                        title = "Bütçe uyarıları",
                        subtitle = "Bütçenin %90'ı aşıldığında bildir",
                        trailing = {
                            Switch(
                                checked = settings.budgetAlertsEnabled,
                                onCheckedChange = viewModel::setBudgetAlerts,
                            )
                        },
                    )
                }
            }
        }

        // --- Guvenlik ------------------------------------------------------
        item {
            SectionCard(title = "Güvenlik") {
                SettingRow(
                    icon = Icons.Default.Lock,
                    title = if (settings.hasPin) "PIN kilidi açık" else "PIN kilidi",
                    subtitle = if (settings.hasPin) {
                        "Uygulama açılışında PIN sorulur"
                    } else {
                        "Uygulamayı 4-8 haneli PIN ile kilitle"
                    },
                    onClick = { showPinDialog = true },
                )
                if (settings.hasPin) {
                    SettingRow(
                        icon = Icons.Default.Fingerprint,
                        title = "Biyometrik ile aç",
                        subtitle = "Parmak izi / yüz tanıma",
                        trailing = {
                            Switch(
                                checked = settings.biometricEnabled,
                                onCheckedChange = viewModel::setBiometricEnabled,
                            )
                        },
                    )
                    SettingRow(
                        icon = Icons.Default.Lock,
                        title = "Kilidi kaldır",
                        subtitle = "PIN ve biyometrik kapatılır",
                        onClick = viewModel::clearPin,
                    )
                }
            }
        }

        // --- Veri ------------------------------------------------------------
        item {
            SectionCard(title = "Veri") {
                SettingRow(
                    icon = Icons.Default.Backup,
                    title = "Verileri yedekle",
                    subtitle = "Tüm veriyi JSON dosyası olarak kaydet",
                    onClick = { backupLauncher.launch(viewModel.backupFileName()) },
                )
                SettingRow(
                    icon = Icons.Default.Restore,
                    title = "Yedekten geri yükle",
                    subtitle = "Daha önce aldığın yedeği geri yükle",
                    onClick = { restoreLauncher.launch(arrayOf("application/json", "*/*")) },
                )
                SettingRow(
                    icon = Icons.Default.Description,
                    title = "İşlemleri CSV olarak dışa aktar",
                    subtitle = "Excel ile açılabilir dosya",
                    onClick = { csvLauncher.launch(viewModel.csvFileName()) },
                )
            }
        }

        item {
            SectionCard(title = "Hakkında") {
                Text(
                    text = "Finansım — kişisel finans ve borç takibi",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Tüm verileriniz yalnızca bu cihazda saklanır. Uygulamanın internet " +
                        "izni yoktur; hiçbir finansal bilgi dışarı gönderilmez.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    // --- Mesajlar / dialoglar ------------------------------------------------
    when (val current = message) {
        is SettingsMessage.Info -> InfoDialog(current.text) { viewModel.clearMessage() }
        is SettingsMessage.Error -> InfoDialog(current.text, isError = true) {
            viewModel.clearMessage()
        }

        is SettingsMessage.RestorePrompt -> RestoreDialog(
            summary = current.fileSummary,
            onDismiss = { viewModel.clearMessage() },
            onReplace = {
                viewModel.clearMessage()
                viewModel.restore(current.data, RestoreMode.REPLACE)
            },
            onMerge = {
                viewModel.clearMessage()
                viewModel.restore(current.data, RestoreMode.MERGE)
            },
        )

        null -> Unit
    }

    if (showPinDialog) {
        PinDialog(
            hasExistingPin = settings.hasPin,
            onDismiss = { showPinDialog = false },
            onSave = { pin ->
                viewModel.setPin(pin)
                showPinDialog = false
            },
        )
    }

    if (showBalanceDialog) {
        BalanceDialog(
            currencySymbol = settings.currencySymbol,
            currentMinor = settings.openingBalanceMinor,
            onDismiss = { showBalanceDialog = false },
            onSave = { minor ->
                viewModel.setOpeningBalance(minor)
                showBalanceDialog = false
            },
        )
    }
}

@Composable
private fun InfoDialog(text: String, isError: Boolean = false, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isError) "Hata" else "Tamamlandı") },
        text = {
            Text(
                text = text,
                color = if (isError) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Tamam") } },
    )
}

/**
 * Geri yukleme onayi. Mevcut verilerin silinip silinmeyecegi burada acikca
 * sorulur; varsayilan olarak hicbir sey silinmez.
 */
@Composable
private fun RestoreDialog(
    summary: String,
    onDismiss: () -> Unit,
    onReplace: () -> Unit,
    onMerge: () -> Unit,
) {
    var confirmReplace by remember { mutableStateOf(false) }

    if (confirmReplace) {
        AlertDialog(
            onDismissRequest = { confirmReplace = false },
            title = { Text("Mevcut veriler silinecek") },
            text = {
                Text(
                    "Bu işlem şu andaki tüm işlem, borç, alacak, kart ve bütçe kayıtlarını " +
                        "SİLECEK ve yerine yedektekileri koyacak. Bu işlem geri alınamaz."
                )
            },
            confirmButton = {
                TextButton(onClick = onReplace) {
                    Text("Sil ve geri yükle", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmReplace = false }) { Text("Vazgeç") }
            },
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Yedek geri yüklensin mi?") },
        text = {
            Column {
                Text("Dosya içeriği: $summary")
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Mevcut verilerinle ne yapmak istersin?",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "• Ekle: mevcut kayıtlar korunur, yedektekiler yeni kayıt olarak eklenir.\n" +
                        "• Sil ve geri yükle: mevcut tüm kayıtlar silinir, yedek birebir yerine konur.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = { TextButton(onClick = onMerge) { Text("Ekle") } },
        dismissButton = {
            Row {
                TextButton(onClick = onDismiss) { Text("Vazgeç") }
                TextButton(onClick = { confirmReplace = true }) {
                    Text("Sil ve geri yükle", color = MaterialTheme.colorScheme.error)
                }
            }
        },
    )
}

@Composable
private fun PinDialog(
    hasExistingPin: Boolean,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (hasExistingPin) "PIN'i değiştir" else "PIN belirle") },
        text = {
            Column {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it.filter { ch -> ch.isDigit() }.take(8); error = null },
                    label = { Text("PIN (4-8 hane)") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = confirm,
                    onValueChange = {
                        confirm = it.filter { ch -> ch.isDigit() }.take(8); error = null
                    },
                    label = { Text("PIN tekrar") },
                    singleLine = true,
                    isError = error != null,
                    supportingText = error?.let {
                        { Text(it, color = MaterialTheme.colorScheme.error) }
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "PIN'iniz düz metin olarak saklanmaz; yalnızca cihazda üretilen " +
                        "rastgele bir tuz ile birlikte özeti tutulur.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    when {
                        pin.length < 4 -> error = "PIN en az 4 haneli olmalı"
                        pin != confirm -> error = "PIN'ler eşleşmiyor"
                        else -> onSave(pin)
                    }
                }
            ) { Text("Kaydet") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } },
    )
}

@Composable
private fun BalanceDialog(
    currencySymbol: String,
    currentMinor: Long,
    onDismiss: () -> Unit,
    onSave: (Long) -> Unit,
) {
    var amountText by remember {
        mutableStateOf(
            if (currentMinor != 0L) Money.format(currentMinor, withSymbol = false) else ""
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Başlangıç bakiyesi") },
        text = {
            Column {
                AmountField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    currencySymbol = currencySymbol,
                    label = "Şu an elindeki para",
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Uygulamayı kullanmaya başlarken elinde olan para. " +
                        "Eldeki para hesabına bu tutar eklenir.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(Money.parse(amountText) ?: 0L) }
            ) { Text("Kaydet") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } },
    )
}
