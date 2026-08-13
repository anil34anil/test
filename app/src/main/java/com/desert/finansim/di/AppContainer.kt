package com.desert.finansim.di

import android.content.Context
import com.desert.finansim.data.backup.BackupRepository
import com.desert.finansim.data.backup.CsvExporter
import com.desert.finansim.data.local.DefaultCategories
import com.desert.finansim.data.local.FinansimDatabase
import com.desert.finansim.data.repository.AnalyticsRepository
import com.desert.finansim.data.repository.BudgetRepository
import com.desert.finansim.data.repository.CategoryRepository
import com.desert.finansim.data.repository.CreditCardRepository
import com.desert.finansim.data.repository.DebtRepository
import com.desert.finansim.data.repository.ReceivableRepository
import com.desert.finansim.data.repository.RecurringGenerator
import com.desert.finansim.data.repository.RecurringRepository
import com.desert.finansim.data.repository.SettingsRepository
import com.desert.finansim.data.repository.TransactionRepository

/**
 * Elle kurulan bagimlilik kabi.
 *
 * Uygulamanin boyutu icin Hilt/Dagger gereksiz agirlik olurdu; tek bir yerde
 * kurulan bu kap hem daha az sihir hem de daha hizli derleme demek.
 */
class AppContainer(context: Context, appVersionName: String) {

    private val appContext = context.applicationContext

    val database: FinansimDatabase = FinansimDatabase.get(appContext)

    val settingsRepository = SettingsRepository(appContext)
    val categoryRepository = CategoryRepository(database)
    val transactionRepository = TransactionRepository(database)
    val debtRepository = DebtRepository(database)
    val receivableRepository = ReceivableRepository(database)
    val creditCardRepository = CreditCardRepository(database)
    val budgetRepository = BudgetRepository(database)
    val recurringRepository = RecurringRepository(database)
    val recurringGenerator = RecurringGenerator(database)

    val analyticsRepository = AnalyticsRepository(
        db = database,
        debtRepository = debtRepository,
        receivableRepository = receivableRepository,
        creditCardRepository = creditCardRepository,
        settingsRepository = settingsRepository,
    )

    val backupRepository = BackupRepository(database, settingsRepository, appVersionName)
    val csvExporter = CsvExporter(database)

    /** Ilk acilista varsayilan kategorileri yukler. */
    suspend fun seedIfNeeded() {
        val dao = database.categoryDao()
        if (dao.count() == 0) {
            dao.insertAll(DefaultCategories.all())
        }
    }
}
