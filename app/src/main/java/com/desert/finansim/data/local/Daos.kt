package com.desert.finansim.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.desert.finansim.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow

/** Gruplanmis toplamlar icin ortak tasiyici (borc basina odenen, kategori basina harcama...). */
data class IdTotal(val id: Long, val total: Long)

@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories WHERE isArchived = 0 ORDER BY sortOrder ASC, name ASC")
    fun observeActive(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY sortOrder ASC, name ASC")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: Long): CategoryEntity?

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(category: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(categories: List<CategoryEntity>): List<Long>

    @Update
    suspend fun update(category: CategoryEntity)

    @Delete
    suspend fun delete(category: CategoryEntity)

    @Query("DELETE FROM categories")
    suspend fun deleteAll()
}

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions ORDER BY date DESC, id DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY date DESC, id DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE date >= :startDay AND date <= :endDay ORDER BY date DESC, id DESC")
    fun observeBetween(startDay: Long, endDay: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE debtId = :debtId ORDER BY date DESC, id DESC")
    fun observeForDebt(debtId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE receivableId = :receivableId ORDER BY date DESC, id DESC")
    fun observeForReceivable(receivableId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE creditCardId = :cardId ORDER BY date DESC, id DESC")
    fun observeForCard(cardId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions")
    suspend fun getAllOnce(): List<TransactionEntity>

    /** Borc basina toplam odenen tutar. */
    @Query(
        """
        SELECT debtId AS id, COALESCE(SUM(amountMinor), 0) AS total
        FROM transactions
        WHERE debtId IS NOT NULL AND type = :type
        GROUP BY debtId
        """
    )
    fun observeTotalsByDebt(type: TransactionType): Flow<List<IdTotal>>

    /** Alacak basina toplam tahsil edilen tutar. */
    @Query(
        """
        SELECT receivableId AS id, COALESCE(SUM(amountMinor), 0) AS total
        FROM transactions
        WHERE receivableId IS NOT NULL AND type = :type
        GROUP BY receivableId
        """
    )
    fun observeTotalsByReceivable(type: TransactionType): Flow<List<IdTotal>>

    /** Kart basina toplam tutar (harcama ya da kart odemesi). */
    @Query(
        """
        SELECT creditCardId AS id, COALESCE(SUM(amountMinor), 0) AS total
        FROM transactions
        WHERE creditCardId IS NOT NULL AND type = :type
        GROUP BY creditCardId
        """
    )
    fun observeTotalsByCard(type: TransactionType): Flow<List<IdTotal>>

    @Query("SELECT COALESCE(SUM(amountMinor), 0) FROM transactions WHERE type = :type")
    fun observeTotalOfType(type: TransactionType): Flow<Long>

    @Query("SELECT COALESCE(SUM(amountMinor), 0) FROM transactions WHERE debtId = :debtId AND type = :type")
    suspend fun totalForDebt(debtId: Long, type: TransactionType): Long

    @Query("SELECT COALESCE(SUM(amountMinor), 0) FROM transactions WHERE receivableId = :receivableId AND type = :type")
    suspend fun totalForReceivable(receivableId: Long, type: TransactionType): Long

    @Insert
    suspend fun insert(transaction: TransactionEntity): Long

    @Insert
    suspend fun insertAll(transactions: List<TransactionEntity>)

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM transactions WHERE installmentId = :installmentId")
    suspend fun deleteByInstallmentId(installmentId: Long)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()
}

@Dao
interface DebtDao {

    @Query("SELECT * FROM debts ORDER BY isClosed ASC, id DESC")
    fun observeAll(): Flow<List<DebtEntity>>

    @Query("SELECT * FROM debts WHERE isClosed = 0 ORDER BY id DESC")
    fun observeOpen(): Flow<List<DebtEntity>>

    @Query("SELECT * FROM debts WHERE id = :id")
    fun observeById(id: Long): Flow<DebtEntity?>

    @Query("SELECT * FROM debts WHERE id = :id")
    suspend fun getById(id: Long): DebtEntity?

    @Query("SELECT * FROM debts")
    suspend fun getAllOnce(): List<DebtEntity>

    @Insert
    suspend fun insert(debt: DebtEntity): Long

    @Insert
    suspend fun insertAll(debts: List<DebtEntity>)

    @Update
    suspend fun update(debt: DebtEntity)

    @Delete
    suspend fun delete(debt: DebtEntity)

    @Query("UPDATE debts SET isClosed = :closed WHERE id = :id")
    suspend fun setClosed(id: Long, closed: Boolean)

    @Query("DELETE FROM debts")
    suspend fun deleteAll()
}

@Dao
interface InstallmentDao {

    @Query("SELECT * FROM installments WHERE debtId = :debtId ORDER BY number ASC")
    fun observeForDebt(debtId: Long): Flow<List<InstallmentEntity>>

    @Query("SELECT * FROM installments WHERE isPaid = 0 AND dueDate <= :untilDay ORDER BY dueDate ASC")
    fun observeUpcoming(untilDay: Long): Flow<List<InstallmentEntity>>

    @Query("SELECT * FROM installments WHERE isPaid = 0 ORDER BY dueDate ASC")
    fun observeAllPending(): Flow<List<InstallmentEntity>>

    @Query("SELECT * FROM installments WHERE isPaid = 0 AND dueDate <= :untilDay ORDER BY dueDate ASC")
    suspend fun getUpcomingOnce(untilDay: Long): List<InstallmentEntity>

    @Query("SELECT * FROM installments WHERE id = :id")
    suspend fun getById(id: Long): InstallmentEntity?

    @Query("SELECT * FROM installments")
    suspend fun getAllOnce(): List<InstallmentEntity>

    @Query("SELECT * FROM installments WHERE debtId = :debtId ORDER BY number ASC")
    suspend fun getForDebtOnce(debtId: Long): List<InstallmentEntity>

    @Insert
    suspend fun insert(installment: InstallmentEntity): Long

    @Insert
    suspend fun insertAll(installments: List<InstallmentEntity>)

    @Update
    suspend fun update(installment: InstallmentEntity)

    @Query("DELETE FROM installments WHERE debtId = :debtId")
    suspend fun deleteForDebt(debtId: Long)

    @Query("DELETE FROM installments")
    suspend fun deleteAll()
}

@Dao
interface CreditCardDao {

    @Query("SELECT * FROM credit_cards WHERE isArchived = 0 ORDER BY name ASC")
    fun observeActive(): Flow<List<CreditCardEntity>>

    @Query("SELECT * FROM credit_cards ORDER BY isArchived ASC, name ASC")
    fun observeAll(): Flow<List<CreditCardEntity>>

    @Query("SELECT * FROM credit_cards WHERE id = :id")
    fun observeById(id: Long): Flow<CreditCardEntity?>

    @Query("SELECT * FROM credit_cards WHERE id = :id")
    suspend fun getById(id: Long): CreditCardEntity?

    @Query("SELECT * FROM credit_cards")
    suspend fun getAllOnce(): List<CreditCardEntity>

    @Insert
    suspend fun insert(card: CreditCardEntity): Long

    @Insert
    suspend fun insertAll(cards: List<CreditCardEntity>)

    @Update
    suspend fun update(card: CreditCardEntity)

    @Delete
    suspend fun delete(card: CreditCardEntity)

    @Query("DELETE FROM credit_cards")
    suspend fun deleteAll()
}

@Dao
interface ReceivableDao {

    @Query("SELECT * FROM receivables ORDER BY isClosed ASC, id DESC")
    fun observeAll(): Flow<List<ReceivableEntity>>

    @Query("SELECT * FROM receivables WHERE isClosed = 0 ORDER BY id DESC")
    fun observeOpen(): Flow<List<ReceivableEntity>>

    @Query("SELECT * FROM receivables WHERE id = :id")
    fun observeById(id: Long): Flow<ReceivableEntity?>

    @Query("SELECT * FROM receivables WHERE id = :id")
    suspend fun getById(id: Long): ReceivableEntity?

    @Query("SELECT * FROM receivables")
    suspend fun getAllOnce(): List<ReceivableEntity>

    @Insert
    suspend fun insert(receivable: ReceivableEntity): Long

    @Insert
    suspend fun insertAll(receivables: List<ReceivableEntity>)

    @Update
    suspend fun update(receivable: ReceivableEntity)

    @Delete
    suspend fun delete(receivable: ReceivableEntity)

    @Query("UPDATE receivables SET isClosed = :closed WHERE id = :id")
    suspend fun setClosed(id: Long, closed: Boolean)

    @Query("DELETE FROM receivables")
    suspend fun deleteAll()
}

@Dao
interface BudgetDao {

    @Query("SELECT * FROM budgets WHERE monthKey = :monthKey")
    fun observeForMonth(monthKey: Int): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE categoryId = :categoryId AND monthKey = :monthKey LIMIT 1")
    suspend fun getFor(categoryId: Long, monthKey: Int): BudgetEntity?

    @Query("SELECT * FROM budgets")
    suspend fun getAllOnce(): List<BudgetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(budget: BudgetEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(budgets: List<BudgetEntity>)

    @Query("DELETE FROM budgets WHERE categoryId = :categoryId AND monthKey = :monthKey")
    suspend fun deleteFor(categoryId: Long, monthKey: Int)

    @Query("DELETE FROM budgets")
    suspend fun deleteAll()
}

@Dao
interface RecurringRuleDao {

    @Query("SELECT * FROM recurring_rules ORDER BY isActive DESC, title ASC")
    fun observeAll(): Flow<List<RecurringRuleEntity>>

    @Query("SELECT * FROM recurring_rules WHERE isActive = 1")
    suspend fun getActiveOnce(): List<RecurringRuleEntity>

    @Query("SELECT * FROM recurring_rules WHERE id = :id")
    suspend fun getById(id: Long): RecurringRuleEntity?

    @Query("SELECT * FROM recurring_rules")
    suspend fun getAllOnce(): List<RecurringRuleEntity>

    @Insert
    suspend fun insert(rule: RecurringRuleEntity): Long

    @Insert
    suspend fun insertAll(rules: List<RecurringRuleEntity>)

    @Update
    suspend fun update(rule: RecurringRuleEntity)

    @Delete
    suspend fun delete(rule: RecurringRuleEntity)

    @Query("UPDATE recurring_rules SET isActive = :active WHERE id = :id")
    suspend fun setActive(id: Long, active: Boolean)

    @Query("DELETE FROM recurring_rules")
    suspend fun deleteAll()
}
