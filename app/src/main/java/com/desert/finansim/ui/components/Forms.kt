package com.desert.finansim.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.sp
import com.desert.finansim.domain.DateUtils
import com.desert.finansim.domain.Money
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Tutar girisi.
 *
 * Sadece sayisal klavye acilir. Binlik ayraci (nokta) SADECE ekranda,
 * [AmountVisualTransformation] araciligiyla gosterilir; alanin gercek
 * (duzenlenebilir) metni her zaman [Money.sanitizeAmountInput] ile
 * filtrelenmis ham rakamlar + en fazla bir virguldur, asla nokta icermez.
 *
 * Bu ayrim bilerek yapildi: eski tasarimda goruntu metni dogrudan alanin
 * kendi degerine yaziliyordu, bu da bazi Android klavyelerinde (composing
 * region tutan IME'lerde) senkron yeniden yazmanin klavyenin arabellegiyle
 * cakisip yanlis/eksik karakter eklenmesine yol aciyordu (ornegin "400000"
 * yazarken alanin "4,00" gibi beklenmedik bir degere donmesi). Goruntu ve
 * duzenlenebilir metni ayirmak bu hata sinifini kaynaginda ortadan kaldirir.
 *
 * Deger disariya ham metin olarak verilir; kurusa cevirme islemi
 * [Money.parse] ile kaydetme aninda yapilir.
 */
@Composable
fun AmountField(
    value: String,
    onValueChange: (String) -> Unit,
    currencySymbol: String,
    modifier: Modifier = Modifier,
    label: String = "Tutar",
    isError: Boolean = false,
    errorMessage: String? = null,
    imeAction: ImeAction = ImeAction.Next,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { raw -> onValueChange(Money.sanitizeAmountInput(raw)) },
        visualTransformation = AmountVisualTransformation(),
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        suffix = { Text(currencySymbol, fontWeight = FontWeight.SemiBold) },
        singleLine = true,
        isError = isError,
        supportingText = if (isError && errorMessage != null) {
            { Text(errorMessage, color = MaterialTheme.colorScheme.error) }
        } else {
            null
        },
        textStyle = MaterialTheme.typography.headlineSmall.copy(fontSize = 24.sp),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = imeAction,
        ),
    )
}

/**
 * [AmountField]'in duzenlenebilir metnini (nokta icermeyen ham rakamlar)
 * ekranda bin ayracli bicimde gosterir. Imleç her zaman metnin sonuna
 * sabitlenir — tutar alanlarinda ortadan duzenleme ihtiyaci olmadigi icin
 * (silme/ekleme sondan yapilir) bu, dogru-ama-kirilgan bir offset esleme
 * yazmaktan cok daha guvenli bir basitlestirmedir.
 */
private class AmountVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val display = Money.formatWhileTyping(text.text)
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = display.length
            override fun transformedToOriginal(offset: Int): Int = text.text.length
        }
        return TransformedText(AnnotatedString(display), mapping)
    }
}

/** Salt okunur tarih alani; dokununca takvim acilir. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(
    date: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Tarih",
) {
    var showPicker by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = DateUtils.formatFull(date),
        onValueChange = {},
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        readOnly = true,
        singleLine = true,
        trailingIcon = {
            IconButton(onClick = { showPicker = true }) {
                Icon(Icons.Default.DateRange, contentDescription = "Tarih seç")
            }
        },
    )

    if (showPicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = date.toEpochDay() * MILLIS_PER_DAY,
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let { millis ->
                            onDateChange(
                                Instant.ofEpochMilli(millis)
                                    .atZone(ZoneOffset.UTC)
                                    .toLocalDate()
                            )
                        }
                        showPicker = false
                    }
                ) { Text("Tamam") }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("Vazgeç") }
            },
        ) {
            DatePicker(state = state)
        }
    }
}

private const val MILLIS_PER_DAY = 86_400_000L

/** Genel amacli acilir liste. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> DropdownField(
    label: String,
    options: List<T>,
    selected: T?,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Seçiniz",
    isError: Boolean = false,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = selected?.let(optionLabel) ?: placeholder,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            isError = isError,
            singleLine = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

/** Sayi girisi (taksit sayisi, ayin gunu gibi). */
@Composable
fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    errorMessage: String? = null,
    suffix: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { raw -> onValueChange(raw.filter { it.isDigit() }.take(9)) },
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
        isError = isError,
        suffix = suffix?.let { { Text(it) } },
        supportingText = if (isError && errorMessage != null) {
            { Text(errorMessage, color = MaterialTheme.colorScheme.error) }
        } else {
            null
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Next,
        ),
    )
}

/** Ondalikli oran girisi (faiz). */
@Composable
fun DecimalField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    suffix: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { raw ->
            val filtered = buildString {
                var decimalSeen = false
                raw.forEach { ch ->
                    when {
                        ch.isDigit() -> append(ch)
                        (ch == ',' || ch == '.') && !decimalSeen && isNotEmpty() -> {
                            decimalSeen = true
                            append(',')
                        }
                    }
                }
            }
            onValueChange(filtered.take(8))
        },
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
        suffix = suffix?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Next,
        ),
    )
}
