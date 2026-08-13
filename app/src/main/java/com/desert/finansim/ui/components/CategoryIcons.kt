package com.desert.finansim.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector
import com.desert.finansim.domain.model.TransactionType

/** Kategori [iconKey] degerlerini Material ikonlarina esler. */
object CategoryIcons {

    private val map: Map<String, ImageVector> = mapOf(
        // gelir
        "salary" to Icons.Default.Payments,
        "bonus" to Icons.Default.Redeem,
        "extra" to Icons.Default.TrendingUp,
        "rent_income" to Icons.Default.AccountBalance,
        "freelance" to Icons.Default.Work,
        // gider
        "grocery" to Icons.Default.ShoppingCart,
        "bill" to Icons.Default.Receipt,
        "home" to Icons.Default.Home,
        "transport" to Icons.Default.DirectionsBus,
        "food" to Icons.Default.Fastfood,
        "shopping" to Icons.Default.ShoppingBag,
        "health" to Icons.Default.LocalHospital,
        "fun" to Icons.Default.SportsEsports,
        "subscription" to Icons.Default.Repeat,
        "education" to Icons.Default.MenuBook,
        "card" to Icons.Default.CreditCard,
        "other" to Icons.Default.MoreHoriz,
    )

    /** Kategori secim ekraninda kullanicinin secebilecegi ikonlar. */
    val selectableKeys: List<String> = map.keys.toList()

    fun forKey(key: String?): ImageVector = map[key] ?: Icons.Default.MoreHoriz

    fun forTransactionType(type: TransactionType): ImageVector = when (type) {
        TransactionType.INCOME -> Icons.Default.TrendingUp
        TransactionType.EXPENSE -> Icons.Default.ShoppingCart
        TransactionType.DEBT_PAYMENT -> Icons.Default.AccountBalance
    }
}
