package com.desert.finansim.domain.model

/**
 * Gercek para hareketinin turu.
 *
 * Onemli muhasebe kurali: borc OLUSTURMAK bir para hareketi degildir ve
 * burada karsiligi yoktur. Sadece fiilen para giris/cikisi olan islemler
 * [TransactionEntity] olarak kaydedilir.
 *
 *  - [INCOME]       : nakit girisi (maas, prim...)
 *  - [EXPENSE]       : nakit cikisi (market, fatura...)
 *  - [DEBT_PAYMENT]  : borc/taksit odemesi -> nakit cikisi, ama "gider"
 *                      degildir; yukumlulugu azaltir.
 */
enum class TransactionType {
    INCOME,
    EXPENSE,
    DEBT_PAYMENT;

    /** Kasaya para girisi mi? */
    val isCashIn: Boolean
        get() = this == INCOME

    val label: String
        get() = when (this) {
            INCOME -> "Gelir"
            EXPENSE -> "Gider"
            DEBT_PAYMENT -> "Borç Ödemesi"
        }
}

enum class CategoryKind { INCOME, EXPENSE }

enum class PaymentMethod {
    CASH, DEBIT_CARD, CREDIT_CARD, TRANSFER, OTHER;

    val label: String
        get() = when (this) {
            CASH -> "Nakit"
            DEBIT_CARD -> "Banka Kartı"
            CREDIT_CARD -> "Kredi Kartı"
            TRANSFER -> "Havale/EFT"
            OTHER -> "Diğer"
        }
}

enum class DebtType {
    LOAN, CREDIT_CARD, PERSONAL, INSTALLMENT, OTHER;

    val label: String
        get() = when (this) {
            LOAN -> "Kredi"
            CREDIT_CARD -> "Kredi Kartı"
            PERSONAL -> "Kişisel Borç"
            INSTALLMENT -> "Taksit"
            OTHER -> "Diğer"
        }
}

/**
 * Taksit durumu. Veritabaninda yalnizca [PENDING] ve [PAID] tutulur;
 * [OVERDUE] her zaman "bugun" ile karsilastirilarak turetilir, boylece
 * gun degistiginde bayat veri kalmaz.
 */
enum class InstallmentStatus {
    PENDING, PAID, OVERDUE;

    val label: String
        get() = when (this) {
            PENDING -> "Bekliyor"
            PAID -> "Ödendi"
            OVERDUE -> "Gecikti"
        }
}

enum class ThemeMode {
    SYSTEM, LIGHT, DARK;

    val label: String
        get() = when (this) {
            SYSTEM -> "Sistem"
            LIGHT -> "Açık"
            DARK -> "Koyu"
        }
}
