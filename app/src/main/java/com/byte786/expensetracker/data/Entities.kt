package com.byte786.expensetracker.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

object TxnType {
    const val EXPENSE = 0
    const val INCOME = 1
    const val TRANSFER = 2
}

@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val icon: String = "wallet",
    val color: Long = 0xFF00897B,
    /** Starting balance in minor units (paisa / cents). */
    val initialBalance: Long = 0,
    val sortOrder: Int = 0,
)

data class AccountBalance(
    @Embedded val account: Account,
    val balance: Long,
)

/**
 * A category or a subcategory. Subcategories have [parentId] set.
 * [type] is [TxnType.EXPENSE] or [TxnType.INCOME].
 */
@Entity(tableName = "categories", indices = [Index("parentId")])
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: Int,
    val parentId: Long? = null,
    val icon: String = "category",
    val color: Long = 0xFF757575,
    val sortOrder: Int = 0,
)

@Entity(
    tableName = "transactions",
    indices = [Index("date"), Index("categoryId"), Index("accountId")],
)
data class Txn(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: Int,
    /** Always positive, in minor units (paisa / cents). */
    val amount: Long,
    /** Epoch millis. */
    val date: Long,
    val accountId: Long,
    /** Destination account for transfers. */
    val toAccountId: Long? = null,
    val categoryId: Long? = null,
    val subcategoryId: Long? = null,
    val memo: String = "",
    /** File name inside the app's photos folder. */
    val photo: String? = null,
)
