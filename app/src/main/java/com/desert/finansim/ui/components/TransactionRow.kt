package com.desert.finansim.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.desert.finansim.domain.DateUtils
import com.desert.finansim.domain.Money
import com.desert.finansim.domain.model.TransactionItem
import com.desert.finansim.domain.model.TransactionType
import com.desert.finansim.ui.theme.FinansimTheme

/** Islem listelerinde kullanilan tek satir. */
@Composable
fun TransactionRow(
    item: TransactionItem,
    currencySymbol: String,
    modifier: Modifier = Modifier,
    showDate: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    val colors = FinansimTheme.financeColors
    val transaction = item.transaction

    val accent: Color = when (transaction.type) {
        TransactionType.INCOME -> colors.income
        TransactionType.EXPENSE -> item.category?.let { Color(it.colorArgb) } ?: colors.expense
        TransactionType.DEBT_PAYMENT -> colors.debt
    }

    val amountColor = if (transaction.type.isCashIn) colors.income else colors.expense

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircleIcon(
            icon = item.category?.let { CategoryIcons.forKey(it.iconKey) }
                ?: CategoryIcons.forTransactionType(transaction.type),
            tint = accent,
        )
        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text(
                text = transaction.title.ifBlank { item.displayCategory },
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = buildString {
                    append(item.displayCategory)
                    if (showDate) {
                        append(" • ")
                        append(DateUtils.formatDayMonth(item.date))
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(Modifier.width(8.dp))
        Text(
            text = Money.formatSigned(
                transaction.amountMinor,
                transaction.type.isCashIn,
                currencySymbol,
            ),
            style = MaterialTheme.typography.titleMedium,
            color = amountColor,
            maxLines = 1,
        )
    }
}
