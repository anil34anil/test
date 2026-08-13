package com.desert.finansim.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.desert.finansim.domain.model.ThemeMode

private val LightColors = lightColorScheme(
    primary = Teal70,
    onPrimary = Color.White,
    primaryContainer = Teal10,
    onPrimaryContainer = Teal90,
    secondary = DebtBlue,
    onSecondary = Color.White,
    tertiary = Teal50,
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightOutline,
    error = ExpenseRed,
    onError = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = Teal50,
    onPrimary = Color(0xFF00281F),
    primaryContainer = Teal90,
    onPrimaryContainer = Teal10,
    secondary = DebtBlueDark,
    onSecondary = Color(0xFF06243D),
    tertiary = Teal30,
    onTertiary = Color(0xFF00281F),
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutline,
    error = ExpenseRedDark,
    onError = Color(0xFF3B0A0A),
)

/**
 * Anlam renkleri Material renk semasinda karsiligi olmadigi icin ayri
 * tasinir; boylece acik/koyu temada dogru kontrasti korur.
 */
data class FinanceColors(
    val income: Color,
    val expense: Color,
    val warning: Color,
    val debt: Color,
)

val LocalFinanceColors = staticCompositionLocalOf {
    FinanceColors(IncomeGreen, ExpenseRed, WarningAmber, DebtBlue)
}

object FinansimTheme {
    val financeColors: FinanceColors
        @Composable get() = LocalFinanceColors.current
}

@Composable
fun FinansimTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = if (darkTheme) DarkColors else LightColors
    val financeColors = if (darkTheme) {
        FinanceColors(IncomeGreenDark, ExpenseRedDark, WarningAmberDark, DebtBlueDark)
    } else {
        FinanceColors(IncomeGreen, ExpenseRed, WarningAmber, DebtBlue)
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? android.app.Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(LocalFinanceColors provides financeColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = FinansimTypography,
            content = content,
        )
    }
}
