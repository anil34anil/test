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

    companion object {
        const val VERSION = 2
        private const val DB_NAME = "finansim.db"

        /**
         * Ileride sema degistiginde buraya Migration eklenir. Ornek:
         *
         *   val MIGRATION_2_3 = object : Migration(2, 3) {
         *       override fun migrate(db: SupportSQLiteDatabase) {
         *           db.execSQL("ALTER TABLE debts ADD COLUMN reminderDays INTEGER")
         *       }
         *   }
         *
         * Finansal veri kaybini onlemek icin destructive migration genel
         * ilke olarak KULLANILMAZ; eksik migration derleme/calisma aninda
         * hata verir ki fark edilmeden veri silinmesin.
         *
         * VERSION 1 -> 2 ISTISNASI: Uygulama "sadece temel ozellikler"
         * kapsamina indirgenirken (bütçe, sabit gider, kredi kartı, alacak
         * tabloları kaldırıldı) buradaki genel ilkeye bilinçli bir istisna
         * yapılıp fallbackToDestructiveMigration() kullanıldı (aşağıda).
         * Gerekçe: bu değişiklik yapıldığı sırada üretimde henüz gerçek
         * kullanıcı verisi yoktu (uygulama yeni kurulmuştu) ve cihaz/emülatör
         * erişimi olmadan elle yazılmış bir DROP+CREATE migration'ı güvenle
         * doğrulayacak bir ortam yoktu — hatalı bir migration, gelecekte
         * gerçek veri olan bir sürümde sessiz veri bozulmasına yol açardı.
         * VERSION 3 ve sonrası için bu istisna GEÇERLİ DEĞİLDİR; gerçek
         * kullanıcı verisi söz konusu olduğunda yine elle Migration yazılmalı.
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
                .fallbackToDestructiveMigration(dropAllTables = true)
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
