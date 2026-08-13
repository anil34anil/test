package com.desert.finansim.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.desert.finansim.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow

/** Gruplanmis toplamlar icin ortak tasiyici (borc basina odenen tutar). */
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

    @Query("SELECT COALESCE(SUM(amountMinor), 0) FROM transactions WHERE type = :type")
    fun observeTotalOfType(type: TransactionType): Flow<Long>

    @Query("SELECT COALESCE(SUM(amountMinor), 0) FROM transactions WHERE debtId = :debtId AND type = :type")
    suspend fun totalForDebt(debtId: Long, type: TransactionType): Long

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
