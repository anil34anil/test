package com.desert.finansim.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.desert.finansim.domain.model.CategoryKind
import com.desert.finansim.domain.model.DebtType
import com.desert.finansim.domain.model.PaymentMethod
import com.desert.finansim.domain.model.RecurrenceFrequency
import com.desert.finansim.domain.model.TransactionType
import kotlinx.serialization.Serializable

/**
 * Tum tutarlar "minor" (kurus) cinsinden Long, tum tarihler epochDay cinsinden Long.
 */

@Entity(
    tableName = "categories",
    indices = [Index("kind"), Index(value = ["name", "kind"], unique = true)],
)
@Serializable
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val kind: CategoryKind,
    val iconKey: String = "other",
    val colorArgb: Long = 0xFF7E8A97,
    val sortOrder: Int = 0,
    val isArchived: Boolean = false,
)

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = CreditCardEntity::class,
            parentColumns = ["id"],
            childColumns = ["creditCardId"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = DebtEntity::class,
            parentColumns = ["id"],
            childColumns = ["debtId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ReceivableEntity::class,
            parentColumns = ["id"],
            childColumns = ["receivableId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("date"), Index("type"), Index("categoryId"),
        Index("creditCardId"), Index("debtId"), Index("receivableId"),
        Index("installmentId"), Index("recurringRuleId"),
    ],
)
@Serializable
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: TransactionType,
    /** Her zaman pozitif. Yon [type] tarafindan belirlenir. */
    val amountMinor: Long,
    val date: Long,
    val title: String,
    val note: String = "",
    val categoryId: Long? = null,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val creditCardId: Long? = null,
    val debtId: Long? = null,
    val installmentId: Long? = null,
    val receivableId: Long? = null,
    val recurringRuleId: Long? = null,
    val createdAt: Long = 0L,
)

@Entity(
    tableName = "debts",
    indices = [Index("isClosed"), Index("creditCardId")],
)
@Serializable
data class DebtEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val counterparty: String = "",
    val type: DebtType,
    /** Toplam borc (anapara + varsa faiz dahil planlanan tutar). */
    val totalAmountMinor: Long,
    val interestRate: Double? = null,
    val monthlyPaymentMinor: Long? = null,
    val installmentCount: Int? = null,
    val startDate: Long,
    val endDate: Long? = null,
    /** Taksitsiz borclarda son odeme gunu. */
    val dueDate: Long? = null,
    val note: String = "",
    val isClosed: Boolean = false,
    val creditCardId: Long? = null,
    val createdAt: Long = 0L,
)

@Entity(
    tableName = "installments",
    foreignKeys = [
        ForeignKey(
            entity = DebtEntity::class,
            parentColumns = ["id"],
            childColumns = ["debtId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("debtId"), Index("dueDate"), Index("isPaid")],
)
@Serializable
data class InstallmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val debtId: Long,
    /** 1 tabanli: 1/12, 2/12 ... */
    val number: Int,
    val totalCount: Int,
    val amountMinor: Long,
    val dueDate: Long,
    val isPaid: Boolean = false,
    val paidDate: Long? = null,
)

@Entity(
    tableName = "credit_cards",
    indices = [Index("isArchived")],
)
@Serializable
data class CreditCardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val bank: String = "",
    val limitMinor: Long,
    /** Ekstre kesim gunu (1-31). */
    val statementDay: Int = 1,
    /** Son odeme gunu (1-31). */
    val dueDay: Int = 10,
    val colorArgb: Long = 0xFF2F6F62,
    val isArchived: Boolean = false,
)

@Entity(
    tableName = "receivables",
    indices = [Index("isClosed")],
)
@Serializable
data class ReceivableEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personName: String,
    val title: String = "",
    val totalAmountMinor: Long,
    val startDate: Long,
    val dueDate: Long? = null,
    val note: String = "",
    val isClosed: Boolean = false,
)

@Entity(
    tableName = "budgets",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["categoryId", "monthKey"], unique = true), Index("monthKey")],
)
@Serializable
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: Long,
    /** yyyyMM, ornegin 202608 */
    val monthKey: Int,
    val amountMinor: Long,
)

/**
 * Sabit gelir/gider tanimi. Vakti geldiginde gercek bir [TransactionEntity]
 * uretir; uretim [lastGeneratedDate] ile takip edilir, boylece ayni donem
 * icin iki kez islenmez.
 */
@Entity(
    tableName = "recurring_rules",
    indices = [Index("isActive"), Index("categoryId")],
)
@Serializable
data class RecurringRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    /** Yalnizca INCOME veya EXPENSE. */
    val type: TransactionType,
    val amountMinor: Long,
    val categoryId: Long? = null,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val creditCardId: Long? = null,
    val frequency: RecurrenceFrequency = RecurrenceFrequency.MONTHLY,
    /** MONTHLY icin ayin gunu (1-31). */
    val dayOfMonth: Int? = 1,
    /** WEEKLY icin haftanin gunu (1=Pazartesi .. 7=Pazar). */
    val dayOfWeek: Int? = null,
    /** YEARLY icin ay (1-12). */
    val monthOfYear: Int? = null,
    val startDate: Long,
    val endDate: Long? = null,
    val isActive: Boolean = true,
    val lastGeneratedDate: Long? = null,
    val note: String = "",
)
