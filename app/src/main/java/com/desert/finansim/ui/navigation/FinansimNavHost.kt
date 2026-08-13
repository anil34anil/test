package com.desert.finansim.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CreditScore
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.desert.finansim.domain.model.TransactionType
import com.desert.finansim.ui.components.SettingRow
import com.desert.finansim.ui.screens.budget.BudgetScreen
import com.desert.finansim.ui.screens.cards.CreditCardDetailScreen
import com.desert.finansim.ui.screens.cards.CreditCardFormScreen
import com.desert.finansim.ui.screens.categories.CategoriesScreen
import com.desert.finansim.ui.screens.dashboard.DashboardScreen
import com.desert.finansim.ui.screens.debts.DebtDetailScreen
import com.desert.finansim.ui.screens.debts.DebtFormScreen
import com.desert.finansim.ui.screens.debts.DebtsScreen
import com.desert.finansim.ui.screens.plan.MonthlyPlanScreen
import com.desert.finansim.ui.screens.receivables.ReceivableDetailScreen
import com.desert.finansim.ui.screens.receivables.ReceivableFormScreen
import com.desert.finansim.ui.screens.recurring.RecurringFormScreen
import com.desert.finansim.ui.screens.recurring.RecurringListScreen
import com.desert.finansim.ui.screens.reports.ReportsScreen
import com.desert.finansim.ui.screens.settings.SettingsScreen
import com.desert.finansim.ui.screens.transactions.TransactionFormScreen
import com.desert.finansim.ui.screens.transactions.TransactionsScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinansimNavHost(
    navController: NavHostController = rememberNavController(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val isTopLevel = currentRoute in TopLevelDestination.routes

    var showAddSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    Scaffold(
        bottomBar = {
            AnimatedVisibility(visible = isTopLevel) {
                NavigationBar {
                    TopLevelDestination.entries.forEach { destination ->
                        NavigationBarItem(
                            selected = currentRoute == destination.route,
                            onClick = {
                                if (currentRoute != destination.route) {
                                    navController.navigate(destination.route) {
                                        popUpTo(Routes.DASHBOARD) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = { Icon(destination.icon, contentDescription = destination.label) },
                            label = {
                                Text(
                                    text = destination.label,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (isTopLevel && currentRoute != Routes.SETTINGS) {
                FloatingActionButton(
                    onClick = { showAddSheet = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Ekle")
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            NavHost(
                navController = navController,
                startDestination = Routes.DASHBOARD,
            ) {
                composable(Routes.DASHBOARD) {
                    DashboardScreen(
                        onSeeAllTransactions = { navController.navigate(Routes.TRANSACTIONS) },
                        onOpenDebts = { navController.navigate(Routes.DEBTS) },
                        onOpenDebt = { navController.navigate(Routes.debtDetail(it)) },
                        onOpenCard = { navController.navigate(Routes.cardDetail(it)) },
                        onOpenPlan = { navController.navigate(Routes.MONTHLY_PLAN) },
                        onOpenBudget = { navController.navigate(Routes.BUDGET) },
                        onEditTransaction = { id, type ->
                            navController.navigate(Routes.transactionForm(type, id))
                        },
                    )
                }

                composable(Routes.TRANSACTIONS) {
                    TransactionsScreen(
                        onEditTransaction = { id, type ->
                            navController.navigate(Routes.transactionForm(type, id))
                        },
                    )
                }

                composable(Routes.DEBTS) {
                    DebtsScreen(
                        onAddDebt = { navController.navigate(Routes.debtForm()) },
                        onOpenDebt = { navController.navigate(Routes.debtDetail(it)) },
                        onAddReceivable = { navController.navigate(Routes.receivableForm()) },
                        onOpenReceivable = { navController.navigate(Routes.receivableDetail(it)) },
                        onAddCard = { navController.navigate(Routes.cardForm()) },
                        onOpenCard = { navController.navigate(Routes.cardDetail(it)) },
                    )
                }

                composable(Routes.REPORTS) {
                    ReportsScreen(
                        onOpenPlan = { navController.navigate(Routes.MONTHLY_PLAN) },
                        onOpenBudget = { navController.navigate(Routes.BUDGET) },
                    )
                }

                composable(Routes.SETTINGS) {
                    SettingsScreen(
                        onOpenCategories = { navController.navigate(Routes.CATEGORIES) },
                        onOpenRecurring = { navController.navigate(Routes.RECURRING_LIST) },
                        onOpenBudget = { navController.navigate(Routes.BUDGET) },
                    )
                }

                composable(
                    route = Routes.TRANSACTION_FORM,
                    arguments = listOf(
                        navArgument("type") {
                            type = NavType.StringType
                            defaultValue = TransactionType.EXPENSE.name
                        },
                        navArgument("id") {
                            type = NavType.LongType
                            defaultValue = 0L
                        },
                    ),
                ) { entry ->
                    val typeName = entry.arguments?.getString("type")
                        ?: TransactionType.EXPENSE.name
                    val type = runCatching { TransactionType.valueOf(typeName) }
                        .getOrDefault(TransactionType.EXPENSE)
                    TransactionFormScreen(
                        transactionType = type,
                        transactionId = entry.arguments?.getLong("id") ?: 0L,
                        onDone = { navController.popBackStack() },
                    )
                }

                composable(
                    route = Routes.DEBT_FORM,
                    arguments = listOf(
                        navArgument("id") { type = NavType.LongType; defaultValue = 0L },
                    ),
                ) { entry ->
                    DebtFormScreen(
                        debtId = entry.arguments?.getLong("id") ?: 0L,
                        onDone = { navController.popBackStack() },
                    )
                }

                composable(
                    route = Routes.DEBT_DETAIL,
                    arguments = listOf(navArgument("id") { type = NavType.LongType }),
                ) { entry ->
                    DebtDetailScreen(
                        debtId = entry.arguments?.getLong("id") ?: 0L,
                        onBack = { navController.popBackStack() },
                        onEdit = { navController.navigate(Routes.debtForm(it)) },
                    )
                }

                composable(
                    route = Routes.RECEIVABLE_FORM,
                    arguments = listOf(
                        navArgument("id") { type = NavType.LongType; defaultValue = 0L },
                    ),
                ) { entry ->
                    ReceivableFormScreen(
                        receivableId = entry.arguments?.getLong("id") ?: 0L,
                        onDone = { navController.popBackStack() },
                    )
                }

                composable(
                    route = Routes.RECEIVABLE_DETAIL,
                    arguments = listOf(navArgument("id") { type = NavType.LongType }),
                ) { entry ->
                    ReceivableDetailScreen(
                        receivableId = entry.arguments?.getLong("id") ?: 0L,
                        onBack = { navController.popBackStack() },
                        onEdit = { navController.navigate(Routes.receivableForm(it)) },
                    )
                }

                composable(
                    route = Routes.CARD_FORM,
                    arguments = listOf(
                        navArgument("id") { type = NavType.LongType; defaultValue = 0L },
                    ),
                ) { entry ->
                    CreditCardFormScreen(
                        cardId = entry.arguments?.getLong("id") ?: 0L,
                        onDone = { navController.popBackStack() },
                    )
                }

                composable(
                    route = Routes.CARD_DETAIL,
                    arguments = listOf(navArgument("id") { type = NavType.LongType }),
                ) { entry ->
                    CreditCardDetailScreen(
                        cardId = entry.arguments?.getLong("id") ?: 0L,
                        onBack = { navController.popBackStack() },
                        onEdit = { navController.navigate(Routes.cardForm(it)) },
                    )
                }

                composable(Routes.RECURRING_LIST) {
                    RecurringListScreen(
                        onBack = { navController.popBackStack() },
                        onAdd = { navController.navigate(Routes.recurringForm()) },
                        onEdit = { navController.navigate(Routes.recurringForm(it)) },
                    )
                }

                composable(
                    route = Routes.RECURRING_FORM,
                    arguments = listOf(
                        navArgument("id") { type = NavType.LongType; defaultValue = 0L },
                    ),
                ) { entry ->
                    RecurringFormScreen(
                        ruleId = entry.arguments?.getLong("id") ?: 0L,
                        onDone = { navController.popBackStack() },
                    )
                }

                composable(Routes.BUDGET) {
                    BudgetScreen(onBack = { navController.popBackStack() })
                }

                composable(Routes.CATEGORIES) {
                    CategoriesScreen(onBack = { navController.popBackStack() })
                }

                composable(Routes.MONTHLY_PLAN) {
                    MonthlyPlanScreen(onBack = { navController.popBackStack() })
                }
            }
        }
    }

    if (showAddSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddSheet = false },
            sheetState = sheetState,
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 12.dp)
            ) {
                val go: (String) -> Unit = { route ->
                    showAddSheet = false
                    navController.navigate(route)
                }
                SettingRow(
                    icon = Icons.Default.ArrowUpward,
                    title = "Gelir Ekle",
                    subtitle = "Maaş, prim, ek gelir",
                    onClick = { go(Routes.transactionForm(TransactionType.INCOME)) },
                )
                SettingRow(
                    icon = Icons.Default.ArrowDownward,
                    title = "Gider Ekle",
                    subtitle = "Market, fatura, kira",
                    onClick = { go(Routes.transactionForm(TransactionType.EXPENSE)) },
                )
                SettingRow(
                    icon = Icons.Default.CreditScore,
                    title = "Borç Ekle",
                    subtitle = "Kredi, taksit, kişisel borç",
                    onClick = { go(Routes.debtForm()) },
                )
                SettingRow(
                    icon = Icons.Default.Group,
                    title = "Alacak Ekle",
                    subtitle = "Birinden alacağın para",
                    onClick = { go(Routes.receivableForm()) },
                )
                SettingRow(
                    icon = Icons.Default.Payments,
                    title = "Ödeme Ekle",
                    subtitle = "Borç veya kredi kartı ödemesi",
                    onClick = { go(Routes.transactionForm(TransactionType.DEBT_PAYMENT)) },
                )
            }
        }
    }
}
