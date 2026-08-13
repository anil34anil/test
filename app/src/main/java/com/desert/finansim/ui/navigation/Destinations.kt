package com.desert.finansim.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.ui.graphics.vector.ImageVector
import com.desert.finansim.domain.model.TransactionType

object Routes {
    const val DASHBOARD = "dashboard"
    const val TRANSACTIONS = "transactions"
    const val DEBTS = "debts"
    const val REPORTS = "reports"
    const val SETTINGS = "settings"

    const val ONBOARDING = "onboarding"

    const val TRANSACTION_FORM = "transaction_form?type={type}&id={id}"
    const val DEBT_FORM = "debt_form?id={id}"
    const val DEBT_DETAIL = "debt_detail/{id}"
    const val RECEIVABLE_FORM = "receivable_form?id={id}"
    const val RECEIVABLE_DETAIL = "receivable_detail/{id}"
    const val CARD_FORM = "card_form?id={id}"
    const val CARD_DETAIL = "card_detail/{id}"
    const val RECURRING_LIST = "recurring"
    const val RECURRING_FORM = "recurring_form?id={id}"
    const val BUDGET = "budget"
    const val CATEGORIES = "categories"
    const val MONTHLY_PLAN = "monthly_plan"

    fun transactionForm(type: TransactionType, id: Long = 0L): String =
        "transaction_form?type=${type.name}&id=$id"

    fun debtForm(id: Long = 0L): String = "debt_form?id=$id"
    fun debtDetail(id: Long): String = "debt_detail/$id"
    fun receivableForm(id: Long = 0L): String = "receivable_form?id=$id"
    fun receivableDetail(id: Long): String = "receivable_detail/$id"
    fun cardForm(id: Long = 0L): String = "card_form?id=$id"
    fun cardDetail(id: Long): String = "card_detail/$id"
    fun recurringForm(id: Long = 0L): String = "recurring_form?id=$id"
}

/** Alt menudeki bes ana bolum. */
enum class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    DASHBOARD(Routes.DASHBOARD, "Ana Sayfa", Icons.Default.Home),
    TRANSACTIONS(Routes.TRANSACTIONS, "İşlemler", Icons.Default.SwapVert),
    DEBTS(Routes.DEBTS, "Borçlar", Icons.Default.AccountBalanceWallet),
    REPORTS(Routes.REPORTS, "Raporlar", Icons.Default.BarChart),
    SETTINGS(Routes.SETTINGS, "Ayarlar", Icons.Default.Settings);

    companion object {
        val routes: Set<String> = entries.map { it.route }.toSet()
    }
}
