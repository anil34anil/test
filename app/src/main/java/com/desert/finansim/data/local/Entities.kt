package com.desert.finansim.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.desert.finansim.domain.model.CategoryKind
import com.desert.finansim.domain.model.DebtType
import com.desert.finansim.domain.model.PaymentMethod
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
            entity = DebtEntity::class,
            parentColumns = ["id"],
            childColumns = ["debtId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("date"), Index("type"), Index("categoryId"),
        Index("debtId"), Index("installmentId"),
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
    val debtId: Long? = null,
    val installmentId: Long? = null,
    val createdAt: Long = 0L,
)

@Entity(
    tableName = "debts",
    indices = [Index("isClosed")],
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
