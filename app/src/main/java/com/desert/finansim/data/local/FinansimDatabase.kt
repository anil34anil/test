package com.desert.finansim.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        CategoryEntity::class,
        TransactionEntity::class,
        DebtEntity::class,
        InstallmentEntity::class,
        CreditCardEntity::class,
        ReceivableEntity::class,
        BudgetEntity::class,
        RecurringRuleEntity::class,
    ],
    version = FinansimDatabase.VERSION,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class FinansimDatabase : RoomDatabase() {

    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun debtDao(): DebtDao
    abstract fun installmentDao(): InstallmentDao
    abstract fun creditCardDao(): CreditCardDao
    abstract fun receivableDao(): ReceivableDao
    abstract fun budgetDao(): BudgetDao
    abstract fun recurringRuleDao(): RecurringRuleDao

    companion object {
        const val VERSION = 1
        private const val DB_NAME = "finansim.db"

        /**
         * Ileride sema degistiginde buraya Migration eklenir. Ornek:
         *
         *   val MIGRATION_1_2 = object : Migration(1, 2) {
         *       override fun migrate(db: SupportSQLiteDatabase) {
         *           db.execSQL("ALTER TABLE debts ADD COLUMN reminderDays INTEGER")
         *       }
         *   }
         *
         * Finansal veri kaybini onlemek icin destructive migration BILEREK
         * kullanilmaz; eksik migration derleme/calisma aninda hata verir ki
         * fark edilmeden veri silinmesin.
         */
        val MIGRATIONS: Array<Migration> = emptyArray()

        @Volatile
        private var instance: FinansimDatabase? = null

        fun get(context: Context): FinansimDatabase =
            instance ?: synchronized(this) {
                instance ?: build(context.applicationContext).also { instance = it }
            }

        private fun build(context: Context): FinansimDatabase =
            Room.databaseBuilder(context, FinansimDatabase::class.java, DB_NAME)
                .addMigrations(*MIGRATIONS)
                .addCallback(object : Callback() {
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        // Foreign key kisitlari acik olsun (ON DELETE davranislari icin sart).
                        db.execSQL("PRAGMA foreign_keys=ON;")
                    }
                })
                .build()
    }
}
