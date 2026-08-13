package com.desert.finansim.data.backup

import androidx.room.withTransaction
import com.desert.finansim.data.local.BudgetEntity
import com.desert.finansim.data.local.FinansimDatabase
import com.desert.finansim.data.repository.SettingsRepository
import com.desert.finansim.domain.model.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

/**
 * Tum verinin JSON olarak disari aktarilmasi ve geri yuklenmesi.
 *
 * Geri yukleme tek bir veritabani transaction'i icinde yapilir: dosya
 * bozuksa ya da ortada hata olusursa hicbir sey degismez, mevcut veri
 * oldugu gibi kalir.
 */
class BackupRepository(
    private val db: FinansimDatabase,
    private val settingsRepository: SettingsRepository,
    private val appVersionName: String,
) {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun export(): String {
        val settings = settingsRepository.settings.first()
        val data = BackupData(
            schemaVersion = FinansimDatabase.VERSION,
            exportedAt = System.currentTimeMillis(),
            appVersionName = appVersionName,
            settings = BackupSettings(
                themeMode = settings.themeMode.name,
                currencySymbol = settings.currencySymbol,
                openingBalanceMinor = settings.openingBalanceMinor,
                notificationsEnabled = settings.notificationsEnabled,
                reminderDaysBefore = settings.reminderDaysBefore,
                budgetAlertsEnabled = settings.budgetAlertsEnabled,
            ),
            categories = db.categoryDao().observeAll().first(),
            creditCards = db.creditCardDao().getAllOnce(),
            debts = db.debtDao().getAllOnce(),
            installments = db.installmentDao().getAllOnce(),
            receivables = db.receivableDao().getAllOnce(),
            transactions = db.transactionDao().getAllOnce(),
            budgets = db.budgetDao().getAllOnce(),
            recurringRules = db.recurringRuleDao().getAllOnce(),
        )
        return json.encodeToString(BackupData.serializer(), data)
    }

    fun suggestedFileName(): String {
        val now = java.time.LocalDateTime.now()
        val stamp = "%04d%02d%02d-%02d%02d".format(
            now.year, now.monthValue, now.dayOfMonth, now.hour, now.minute
        )
        return "finansim-yedek-$stamp.json"
    }

    /** Dosyayi ayristirir ve dogrular; veritabanina dokunmaz. */
    fun parse(content: String): Result<BackupData> = runCatching {
        val data = json.decodeFromString(BackupData.serializer(), content)
        if (data.formatVersion > BackupData.CURRENT_FORMAT_VERSION) {
            error("Bu yedek dosyası uygulamanın daha yeni bir sürümüne ait. Lütfen uygulamayı güncelleyin.")
        }
        if (data.schemaVersion > FinansimDatabase.VERSION) {
            error("Yedek dosyası daha yeni bir veri sürümünden alınmış. Geri yükleme yapılamaz.")
        }
        data
    }

    suspend fun restore(data: BackupData, mode: RestoreMode): Result<RestoreStats> = runCatching {
        val stats = db.withTransaction {
            when (mode) {
                RestoreMode.REPLACE -> restoreReplacing(data)
                RestoreMode.MERGE -> restoreMerging(data)
            }
        }
        applySettings(data.settings)
        stats
    }

    private suspend fun applySettings(settings: BackupSettings) {
        settingsRepository.setThemeMode(
            runCatching { ThemeMode.valueOf(settings.themeMode) }.getOrDefault(ThemeMode.SYSTEM)
        )
        settingsRepository.setCurrencySymbol(settings.currencySymbol)
        settingsRepository.setOpeningBalance(settings.openingBalanceMinor)
        settingsRepository.setNotificationsEnabled(settings.notificationsEnabled)
        settingsRepository.setReminderDaysBefore(settings.reminderDaysBefore)
        settingsRepository.setBudgetAlertsEnabled(settings.budgetAlertsEnabled)
        settingsRepository.setOnboardingCompleted(true)
    }

    /**
     * Her seyi silip yedegi birebir yerine koyar. Kimlikler korunur,
     * boylece kayitlar arasindaki baglar (borc-taksit-islem) aynen kalir.
     * Ekleme sirasi foreign key bagimliliklarina gore secilmistir.
     */
    private suspend fun restoreReplacing(data: BackupData): RestoreStats {
        db.transactionDao().deleteAll()
        db.budgetDao().deleteAll()
        db.recurringRuleDao().deleteAll()
        db.installmentDao().deleteAll()
        db.debtDao().deleteAll()
        db.receivableDao().deleteAll()
        db.creditCardDao().deleteAll()
        db.categoryDao().deleteAll()

        db.categoryDao().insertAll(data.categories)
        db.creditCardDao().insertAll(data.creditCards)
        db.debtDao().insertAll(data.debts)
        db.receivableDao().insertAll(data.receivables)
        db.installmentDao().insertAll(data.installments)
        db.budgetDao().insertAll(data.budgets)
        db.recurringRuleDao().insertAll(data.recurringRules)
        db.transactionDao().insertAll(data.transactions)

        return RestoreStats(
            categories = data.categories.size,
            creditCards = data.creditCards.size,
            debts = data.debts.size,
            installments = data.installments.size,
            receivables = data.receivables.size,
            transactions = data.transactions.size,
            budgets = data.budgets.size,
            recurringRules = data.recurringRules.size,
        )
    }

    /**
     * Mevcut veriyi koruyarak ekler. Kimlikler cakismasin diye her kayit
     * yeni bir kimlikle yazilir ve tum baglar eski->yeni haritasi uzerinden
     * yeniden kurulur. Kategoriler ad + tur eslesmesiyle tekrar kullanilir.
     */
    private suspend fun restoreMerging(data: BackupData): RestoreStats {
        val categoryMap = mutableMapOf<Long, Long>()
        val existingCategories = db.categoryDao().observeAll().first()
            .associateBy { it.name.lowercase() to it.kind }

        var newCategories = 0
        data.categories.forEach { category ->
            val existing = existingCategories[category.name.lowercase() to category.kind]
            categoryMap[category.id] = if (existing != null) {
                existing.id
            } else {
                newCategories++
                db.categoryDao().insert(category.copy(id = 0))
            }
        }

        val cardMap = mutableMapOf<Long, Long>()
        data.creditCards.forEach { card ->
            cardMap[card.id] = db.creditCardDao().insert(card.copy(id = 0))
        }

        val debtMap = mutableMapOf<Long, Long>()
        data.debts.forEach { debt ->
            debtMap[debt.id] = db.debtDao().insert(
                debt.copy(id = 0, creditCardId = debt.creditCardId?.let { cardMap[it] })
            )
        }

        val receivableMap = mutableMapOf<Long, Long>()
        data.receivables.forEach { receivable ->
            receivableMap[receivable.id] = db.receivableDao().insert(receivable.copy(id = 0))
        }

        val installmentMap = mutableMapOf<Long, Long>()
        data.installments.forEach { installment ->
            val debtId = debtMap[installment.debtId] ?: return@forEach
            installmentMap[installment.id] =
                db.installmentDao().insert(installment.copy(id = 0, debtId = debtId))
        }

        var budgets = 0
        data.budgets.forEach { budget ->
            val categoryId = categoryMap[budget.categoryId] ?: return@forEach
            db.budgetDao().upsert(
                BudgetEntity(id = 0, categoryId = categoryId, monthKey = budget.monthKey, amountMinor = budget.amountMinor)
            )
            budgets++
        }

        var rules = 0
        data.recurringRules.forEach { rule ->
            db.recurringRuleDao().insert(
                rule.copy(
                    id = 0,
                    categoryId = rule.categoryId?.let { categoryMap[it] },
                    creditCardId = rule.creditCardId?.let { cardMap[it] },
                )
            )
            rules++
        }

        var transactions = 0
        data.transactions.forEach { transaction ->
            db.transactionDao().insert(
                transaction.copy(
                    id = 0,
                    categoryId = transaction.categoryId?.let { categoryMap[it] },
                    creditCardId = transaction.creditCardId?.let { cardMap[it] },
                    debtId = transaction.debtId?.let { debtMap[it] },
                    installmentId = transaction.installmentId?.let { installmentMap[it] },
                    receivableId = transaction.receivableId?.let { receivableMap[it] },
                    // Tekrar kurali baglantisi merge sonrasi anlamini yitirir.
                    recurringRuleId = null,
                )
            )
            transactions++
        }

        return RestoreStats(
            categories = newCategories,
            creditCards = cardMap.size,
            debts = debtMap.size,
            installments = installmentMap.size,
            receivables = receivableMap.size,
            transactions = transactions,
            budgets = budgets,
            recurringRules = rules,
        )
    }
}
