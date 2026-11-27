package com.byte786.expensetracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class AppDao {
    // ---- transactions ----
    @Query("SELECT * FROM transactions ORDER BY date DESC, id DESC")
    abstract fun txns(): Flow<List<Txn>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    abstract suspend fun txn(id: Long): Txn?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun putTxn(t: Txn): Long

    @Delete
    abstract suspend fun deleteTxn(t: Txn)

    @Query("SELECT * FROM transactions WHERE groupId = :groupId ORDER BY id")
    abstract suspend fun group(groupId: Long): List<Txn>

    @Query("DELETE FROM transactions WHERE id IN (:ids)")
    abstract suspend fun deleteTxnIds(ids: List<Long>)

    @Query("SELECT COUNT(*) FROM transactions WHERE photo = :name")
    abstract suspend fun photoUsage(name: String): Int

    /** Replaces the records with [oldIds] by [newOnes] in one go (used when saving a split bill). */
    @Transaction
    open suspend fun replaceTxns(oldIds: List<Long>, newOnes: List<Txn>) {
        if (oldIds.isNotEmpty()) deleteTxnIds(oldIds)
        insertTxns(newOnes)
    }

    // ---- categories ----
    @Query("SELECT * FROM categories ORDER BY type, sortOrder, name COLLATE NOCASE")
    abstract fun categories(): Flow<List<Category>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun putCategory(c: Category): Long

    @Query("SELECT COUNT(*) FROM transactions WHERE categoryId = :id OR subcategoryId = :id")
    abstract suspend fun categoryUsage(id: Long): Int

    @Query("UPDATE transactions SET categoryId = NULL, subcategoryId = NULL WHERE categoryId = :id")
    abstract suspend fun detachCategory(id: Long)

    @Query("UPDATE transactions SET subcategoryId = NULL WHERE subcategoryId = :id")
    abstract suspend fun detachSubcategory(id: Long)

    @Query("DELETE FROM categories WHERE id = :id OR parentId = :id")
    abstract suspend fun deleteCategoryTree(id: Long)

    @Transaction
    open suspend fun deleteCategory(c: Category) {
        if (c.parentId == null) detachCategory(c.id) else detachSubcategory(c.id)
        deleteCategoryTree(c.id)
    }

    @Query("UPDATE transactions SET categoryId = :newParent, subcategoryId = :id WHERE categoryId = :id")
    abstract suspend fun retagParentAsSub(id: Long, newParent: Long)

    @Query("UPDATE transactions SET categoryId = :newParent WHERE subcategoryId = :id")
    abstract suspend fun retagSubParent(id: Long, newParent: Long)

    @Query("UPDATE transactions SET categoryId = :id, subcategoryId = NULL WHERE subcategoryId = :id")
    abstract suspend fun retagSubAsParent(id: Long)

    /**
     * Saves [c] with a new parent and updates its records to match:
     * a top-level category can go inside another one, a subcategory can move to
     * another parent or become top-level.
     */
    @Transaction
    open suspend fun moveCategory(c: Category, newParent: Long?) {
        when {
            c.parentId == null && newParent != null -> retagParentAsSub(c.id, newParent)
            c.parentId != null && newParent != null -> retagSubParent(c.id, newParent)
            c.parentId != null && newParent == null -> retagSubAsParent(c.id)
        }
        putCategory(c.copy(parentId = newParent))
    }

    @Query("UPDATE transactions SET categoryId = :into WHERE categoryId = :from")
    abstract suspend fun retagCategory(from: Long, into: Long)

    @Query("UPDATE categories SET parentId = :into WHERE parentId = :from")
    abstract suspend fun reparentSubs(from: Long, into: Long)

    @Query("UPDATE transactions SET categoryId = :intoParent, subcategoryId = :into WHERE subcategoryId = :from")
    abstract suspend fun retagSubcategory(from: Long, into: Long, intoParent: Long)

    @Query("DELETE FROM categories WHERE id = :id")
    abstract suspend fun deleteCategoryId(id: Long)

    /** Moves everything in [from] into [into] and deletes [from]. Both are top-level, or both subcategories. */
    @Transaction
    open suspend fun mergeCategory(from: Category, into: Category) {
        if (from.id == into.id) return
        if (from.parentId == null) {
            retagCategory(from.id, into.id)
            reparentSubs(from.id, into.id)
        } else {
            retagSubcategory(from.id, into.id, into.parentId ?: return)
        }
        deleteCategoryId(from.id)
    }

    // ---- accounts ----
    @Query(
        """
        SELECT a.*, a.initialBalance
          + COALESCE((SELECT SUM(amount) FROM transactions WHERE accountId = a.id AND type = 1), 0)
          - COALESCE((SELECT SUM(amount) FROM transactions WHERE accountId = a.id AND type IN (0, 2)), 0)
          + COALESCE((SELECT SUM(amount) FROM transactions WHERE toAccountId = a.id AND type = 2), 0)
          AS balance
        FROM accounts a ORDER BY a.sortOrder, a.id
        """
    )
    abstract fun accounts(): Flow<List<AccountBalance>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun putAccount(a: Account): Long

    @Query("SELECT COUNT(*) FROM transactions WHERE accountId = :id OR toAccountId = :id")
    abstract suspend fun accountUsage(id: Long): Int

    @Query("DELETE FROM accounts WHERE id = :id")
    abstract suspend fun deleteAccount(id: Long)

    // ---- backup / restore ----
    @Query("SELECT * FROM transactions ORDER BY date")
    abstract suspend fun allTxns(): List<Txn>

    @Query("SELECT * FROM categories")
    abstract suspend fun allCategories(): List<Category>

    @Query("SELECT * FROM accounts")
    abstract suspend fun allAccounts(): List<Account>

    @Query("DELETE FROM transactions") abstract suspend fun clearTxns()
    @Query("DELETE FROM categories") abstract suspend fun clearCategories()
    @Query("DELETE FROM accounts") abstract suspend fun clearAccounts()

    @Insert abstract suspend fun insertTxns(list: List<Txn>)
    @Insert abstract suspend fun insertCategories(list: List<Category>)
    @Insert abstract suspend fun insertAccounts(list: List<Account>)

    @Transaction
    open suspend fun replaceAll(accounts: List<Account>, categories: List<Category>, txns: List<Txn>) {
        clearTxns(); clearCategories(); clearAccounts()
        insertAccounts(accounts); insertCategories(categories); insertTxns(txns)
    }
}
