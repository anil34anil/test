package com.desert.finansim.data.backup

import com.desert.finansim.data.local.CategoryEntity
import com.desert.finansim.data.local.DebtEntity
import com.desert.finansim.data.local.InstallmentEntity
import com.desert.finansim.data.local.TransactionEntity
import kotlinx.serialization.Serializable

/**
 * Yedek dosyasinin kok yapisi.
 *
 * [formatVersion] dosya bicimini, [schemaVersion] verinin uretildigi
 * veritabani surumunu belirtir. Daha yeni bir surumden alinmis yedek
 * geri yuklenmeye calisilirsa islem reddedilir; boylece eksik alanlar
 * sessizce varsayilana dusup finansal veri bozulmaz.
 */
@Serializable
data class BackupData(
    val formatVersion: Int = CURRENT_FORMAT_VERSION,
    val schemaVersion: Int,
    val exportedAt: Long,
    val appVersionName: String,
    val settings: BackupSettings,
    val categories: List<CategoryEntity> = emptyList(),
    val debts: List<DebtEntity> = emptyList(),
    val installments: List<InstallmentEntity> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
) {
    companion object {
        const val CURRENT_FORMAT_VERSION = 1
    }
}

@Serializable
data class BackupSettings(
    val themeMode: String = "SYSTEM",
    val currencySymbol: String = "₺",
    val openingBalanceMinor: Long = 0L,
    val notificationsEnabled: Boolean = false,
    val reminderDaysBefore: Int = 2,
)

/** Geri yukleme sonucu ozeti; kullaniciya ne kadar kayit geldigi gosterilir. */
data class RestoreStats(
    val categories: Int = 0,
    val debts: Int = 0,
    val installments: Int = 0,
    val transactions: Int = 0,
) {
    val total: Int
        get() = categories + debts + installments + transactions
}

/** Geri yukleme stratejisi. */
enum class RestoreMode {
    /** Mevcut tum veriyi siler, yedegi birebir yerine koyar. */
    REPLACE,

    /** Mevcut veriyi korur, yedektekileri yeni kayit olarak ekler. */
    MERGE,
}
