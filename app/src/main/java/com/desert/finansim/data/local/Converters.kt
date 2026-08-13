package com.desert.finansim.data.local

import androidx.room.TypeConverter
import com.desert.finansim.domain.model.CategoryKind
import com.desert.finansim.domain.model.DebtType
import com.desert.finansim.domain.model.PaymentMethod
import com.desert.finansim.domain.model.TransactionType

/**
 * Enum'lar veritabaninda isim (String) olarak tutulur; sira degisse bile
 * kayitli veri bozulmaz. Taninmayan bir deger okunursa guvenli varsayilana
 * dusulur, boylece bozuk tek satir uygulamayi cokertmez.
 */
class Converters {

    @TypeConverter
    fun fromTransactionType(value: TransactionType): String = value.name

    @TypeConverter
    fun toTransactionType(value: String): TransactionType =
        runCatching { TransactionType.valueOf(value) }.getOrDefault(TransactionType.EXPENSE)

    @TypeConverter
    fun fromCategoryKind(value: CategoryKind): String = value.name

    @TypeConverter
    fun toCategoryKind(value: String): CategoryKind =
        runCatching { CategoryKind.valueOf(value) }.getOrDefault(CategoryKind.EXPENSE)

    @TypeConverter
    fun fromPaymentMethod(value: PaymentMethod): String = value.name

    @TypeConverter
    fun toPaymentMethod(value: String): PaymentMethod =
        runCatching { PaymentMethod.valueOf(value) }.getOrDefault(PaymentMethod.OTHER)

    @TypeConverter
    fun fromDebtType(value: DebtType): String = value.name

    @TypeConverter
    fun toDebtType(value: String): DebtType =
        runCatching { DebtType.valueOf(value) }.getOrDefault(DebtType.OTHER)
}
