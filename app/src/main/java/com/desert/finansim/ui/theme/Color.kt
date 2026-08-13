package com.desert.finansim.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Profesyonel finans paleti: koyu yesil ana renk, notr griler ve
 * anlam tasiyan üç durum rengi (gelir / gider / uyari).
 */

// Ana renk — guven veren koyu yesil
val Teal90 = Color(0xFF0B3D33)
val Teal70 = Color(0xFF0F6B57)
val Teal50 = Color(0xFF2E9C81)
val Teal30 = Color(0xFF7FD3BC)
val Teal10 = Color(0xFFD8F1EA)

// Anlam renkleri
val IncomeGreen = Color(0xFF1E8E5A)
val IncomeGreenDark = Color(0xFF4CC38A)
val ExpenseRed = Color(0xFFC63B3B)
val ExpenseRedDark = Color(0xFFF07171)
val WarningAmber = Color(0xFFB86E00)
val WarningAmberDark = Color(0xFFE9A93C)
val DebtBlue = Color(0xFF3A6EA5)
val DebtBlueDark = Color(0xFF7FB2E8)

// Notrler — acik tema
val LightBackground = Color(0xFFF7F9FC)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFEEF2F6)
val LightOutline = Color(0xFFD3DBE3)
val LightOnSurface = Color(0xFF10171D)
val LightOnSurfaceVariant = Color(0xFF56626D)

// Notrler — koyu tema
val DarkBackground = Color(0xFF0F1417)
val DarkSurface = Color(0xFF171E23)
val DarkSurfaceVariant = Color(0xFF222B31)
val DarkOutline = Color(0xFF37434B)
val DarkOnSurface = Color(0xFFE7EDF2)
val DarkOnSurfaceVariant = Color(0xFFA5B2BC)

/** Grafiklerde kategori rengi tanimli degilse kullanilan yedek palet. */
val ChartPalette = listOf(
    Color(0xFF2E9C81),
    Color(0xFF5B8FF9),
    Color(0xFFE2703A),
    Color(0xFF9B5DE5),
    Color(0xFF00A9A5),
    Color(0xFFF15BB5),
    Color(0xFFFF9F1C),
    Color(0xFF3D5A80),
    Color(0xFF6A7FDB),
    Color(0xFF7E8A97),
)

/** Ayni palet, veritabaninda saklanan ARGB Long bicimiyle. */
val ChartPaletteArgb = listOf(
    0xFF2E9C81, 0xFF5B8FF9, 0xFFE2703A, 0xFF9B5DE5, 0xFF00A9A5,
    0xFFF15BB5, 0xFFFF9F1C, 0xFF3D5A80, 0xFF6A7FDB, 0xFF7E8A97,
)
