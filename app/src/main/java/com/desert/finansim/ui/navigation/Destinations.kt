package com.desert.finansim.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.ui.graphics.vector.ImageVector
import com.desert.finansim.domain.model.TransactionType

object Routes {
    const val DASHBOARD = "dashboard"
    const val TRANSACTIONS = "transactions"
    const val DEBTS = "debts"
    const val SETTINGS = "settings"

    const val ONBOARDING = "onboarding"

    const val TRANSACTION_FORM = "transaction_form?type={type}&id={id}"
    const val DEBT_FORM = "debt_form?id={id}"
    const val DEBT_DETAIL = "debt_detail/{id}"

    fun transactionForm(type: TransactionType, id: Long = 0L): String =
        "transaction_form?type=${type.name}&id=$id"

    fun debtForm(id: Long = 0L): String = "debt_form?id=$id"
    fun debtDetail(id: Long): String = "debt_detail/$id"
}

/** Alt menudeki dort ana bolum. */
enum class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    DASHBOARD(Routes.DASHBOARD, "Ana Sayfa", Icons.Default.Home),
    TRANSACTIONS(Routes.TRANSACTIONS, "İşlemler", Icons.Default.SwapVert),
    DEBTS(Routes.DEBTS, "Borçlar", Icons.Default.AccountBalanceWallet),
    SETTINGS(Routes.SETTINGS, "Ayarlar", Icons.Default.Settings);

    companion object {
        val routes: Set<String> = entries.map { it.route }.toSet()
    }
}
