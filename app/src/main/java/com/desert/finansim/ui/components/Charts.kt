package com.desert.finansim.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.desert.finansim.domain.DateUtils
import com.desert.finansim.domain.Money
import com.desert.finansim.domain.model.CategorySpending
import com.desert.finansim.domain.model.MonthlyTotals
import com.desert.finansim.ui.theme.FinansimTheme

/**
 * Grafikler harici kutuphane olmadan Compose Canvas ile cizilir:
 * uygulama daha hafif kalir ve tema renkleriyle birebir uyumlu olur.
 */

/** Aylik gelir/gider karsilastirmasi — her ay icin yan yana iki cubuk. */
@Composable
fun IncomeExpenseBarChart(
    data: List<MonthlyTotals>,
    currencySymbol: String,
    modifier: Modifier = Modifier,
    chartHeight: Int = 160,
) {
    if (data.isEmpty()) return

    val incomeColor = FinansimTheme.financeColors.income
    val expenseColor = FinansimTheme.financeColors.expense
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)

    val maxValue = data.maxOf { maxOf(it.incomeMinor, it.expenseMinor) }.coerceAtLeast(1L)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            LegendDot(incomeColor, "Gelir")
            LegendDot(expenseColor, "Gider")
        }
        Spacer(Modifier.height(12.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight.dp)
        ) {
            val slotWidth = size.width / data.size
            val barWidth = (slotWidth * 0.28f).coerceAtMost(22f.dp.toPx())
            val gap = barWidth * 0.25f
            val baseline = size.height

            // yatay kilavuz cizgileri
            repeat(4) { index ->
                val y = size.height * index / 4f
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f,
                )
            }

            data.forEachIndexed { index, totals ->
                val centerX = slotWidth * index + slotWidth / 2f
                val incomeHeight = size.height * (totals.incomeMinor.toFloat() / maxValue)
                val expenseHeight = size.height * (totals.expenseMinor.toFloat() / maxValue)

                drawRoundRectBar(
                    color = incomeColor,
                    left = centerX - barWidth - gap / 2f,
                    width = barWidth,
                    height = incomeHeight,
                    baseline = baseline,
                )
                drawRoundRectBar(
                    color = expenseColor,
                    left = centerX + gap / 2f,
                    width = barWidth,
                    height = expenseHeight,
                    baseline = baseline,
                )
            }
        }

        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth()) {
            data.forEach { totals ->
                Text(
                    text = DateUtils.formatMonthShort(DateUtils.yearMonthOf(totals.monthKey)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** Aylik net durum — pozitif yesil, negatif kirmizi tek cubuk. */
@Composable
fun NetBarChart(
    data: List<MonthlyTotals>,
    modifier: Modifier = Modifier,
    chartHeight: Int = 140,
) {
    if (data.isEmpty()) return

    val incomeColor = FinansimTheme.financeColors.income
    val expenseColor = FinansimTheme.financeColors.expense
    val zeroLineColor = MaterialTheme.colorScheme.outline

    val maxAbs = data.maxOf { kotlin.math.abs(it.netMinor) }.coerceAtLeast(1L)

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight.dp)
        ) {
            val slotWidth = size.width / data.size
            val barWidth = (slotWidth * 0.4f).coerceAtMost(26f.dp.toPx())
            val zeroY = size.height / 2f

            drawLine(
                color = zeroLineColor,
                start = Offset(0f, zeroY),
                end = Offset(size.width, zeroY),
                strokeWidth = 1.5f,
            )

            data.forEachIndexed { index, totals ->
                val centerX = slotWidth * index + slotWidth / 2f
                val ratio = totals.netMinor.toFloat() / maxAbs
                val barHeight = (zeroY * kotlin.math.abs(ratio)).coerceAtLeast(2f)
                val positive = totals.netMinor >= 0

                drawRoundRect(
                    color = if (positive) incomeColor else expenseColor,
                    topLeft = Offset(
                        x = centerX - barWidth / 2f,
                        y = if (positive) zeroY - barHeight else zeroY,
                    ),
                    size = Size(barWidth, barHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth()) {
            data.forEach { totals ->
                Text(
                    text = DateUtils.formatMonthShort(DateUtils.yearMonthOf(totals.monthKey)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** Gider kategorileri — halka grafik + yuzdeli liste. */
@Composable
fun CategoryDonutChart(
    slices: List<CategorySpending>,
    currencySymbol: String,
    modifier: Modifier = Modifier,
) {
    if (slices.isEmpty()) return

    val total = slices.sumOf { it.amountMinor }
    val strokeWidthDp = 26.dp

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.size(180.dp)) {
                val stroke = strokeWidthDp.toPx()
                val inset = stroke / 2f
                var startAngle = -90f
                slices.forEachIndexed { index, slice ->
                    val sweep = slice.ratio * 360f
                    drawArc(
                        color = Color(slice.colorArgb),
                        startAngle = startAngle,
                        sweepAngle = if (sweep < 1f) 1f else sweep - 1.5f,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = Size(size.width - stroke, size.height - stroke),
                        style = Stroke(width = stroke, cap = StrokeCap.Butt),
                    )
                    startAngle += sweep
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Toplam",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = Money.format(total, currencySymbol),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        slices.forEach { slice ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color(slice.colorArgb))
                )
                Spacer(Modifier.size(10.dp))
                Text(
                    text = slice.name,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "%${(slice.ratio * 100).toInt()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.size(12.dp))
                Text(
                    text = Money.format(slice.amountMinor, currencySymbol),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.size(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRoundRectBar(
    color: Color,
    left: Float,
    width: Float,
    height: Float,
    baseline: Float,
) {
    if (width <= 0f) return
    val safeHeight = height.coerceAtLeast(2f)
    drawRoundRect(
        color = color,
        topLeft = Offset(left, baseline - safeHeight),
        size = Size(width, safeHeight),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f),
    )
}
