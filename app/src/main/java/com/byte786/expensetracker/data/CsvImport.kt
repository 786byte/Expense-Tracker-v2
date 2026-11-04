package com.byte786.expensetracker.data

import android.content.Context
import android.net.Uri
import com.byte786.expensetracker.ExpenseApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Imports a CSV (e.g. from another expense app) into the database, creating accounts/categories as needed. */
object CsvImport {

    data class Summary(
        val imported: Int,
        val duplicates: Int,
        val newAccounts: List<String>,
        val newCategories: List<String>,
        val errors: List<String>,
    )

    private val palette = listOf(
        0xFFE53935, 0xFF8E24AA, 0xFF3949AB, 0xFF039BE5, 0xFF00897B, 0xFF7CB342,
        0xFFFFB300, 0xFFF4511E, 0xFF6D4C41, 0xFFD81B60, 0xFF5E35B1, 0xFF00ACC1,
    )

    /** Picks a sensible icon from words in a category or account name. */
    private fun guessIcon(name: String, fallback: String): String {
        val n = name.lowercase()
        val rules = listOf(
            "fuel" to "fuel", "petrol" to "fuel", "octane" to "fuel", "bike" to "bike", "car" to "car",
            "pc" to "laptop", "computer" to "laptop", "mobile" to "phone", "phone" to "phone", "bkash" to "phone",
            "subscription" to "card", "game" to "games", "theatre" to "movie", "movie" to "movie", "cinema" to "movie",
            "salary" to "salary", "tution" to "education", "tuition" to "education", "tax" to "bills", "bill" to "bills",
            "gym" to "fitness", "barber" to "haircut", "hair" to "haircut", "groom" to "beauty", "education" to "education",
            "book" to "book", "sale" to "cart", "sold" to "cart", "lottery" to "star", "grant" to "gift", "gift" to "gift",
            "blessing" to "heart", "mom" to "heart", "food" to "food", "lara" to "food", "restaurant" to "food",
            "job" to "salary", "loan" to "bank", "bank" to "bank", "cash" to "cash", "vape" to "cafe", "rent" to "rent",
            "medic" to "medicine", "doctor" to "health", "travel" to "travel", "shop" to "shopping",
        )
        return rules.firstOrNull { (k, _) -> n.contains(k) }?.second ?: fallback
    }

    suspend fun import(ctx: Context, uri: Uri): Summary = withContext(Dispatchers.IO) {
        val text = ctx.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
            ?: error("Cannot open file")
        val parsed = CsvParser.parse(text)
        if (parsed.rows.isEmpty()) return@withContext Summary(0, 0, emptyList(), emptyList(), parsed.errors)

        val repo = ExpenseApp.instance.repo
        val dao = repo.dao
        val accounts = dao.allAccounts().toMutableList()
        val cats = dao.allCategories().toMutableList()
        val parentsBefore = cats.filter { it.parentId == null }.toList()
        val newAccounts = mutableListOf<String>()
        val newCategories = mutableListOf<String>()

        suspend fun accountId(name: String): Long {
            accounts.firstOrNull { it.name.trim().equals(name.trim(), ignoreCase = true) }?.let { return it.id }
            val a = Account(
                name = name.trim(), icon = guessIcon(name, "wallet"),
                color = palette[accounts.size % palette.size], sortOrder = accounts.size,
            )
            val id = dao.putAccount(a)
            accounts.add(a.copy(id = id)); newAccounts.add(a.name)
            return id
        }

        suspend fun createCategory(name: String, type: Int, parent: Category?): Category {
            val siblings = cats.count { it.parentId == parent?.id && it.type == type }
            val c = Category(
                name = name, type = type, parentId = parent?.id,
                icon = guessIcon(name, parent?.icon ?: "category"),
                color = parent?.color ?: palette[(cats.size + siblings) % palette.size],
                sortOrder = siblings,
            )
            val id = dao.putCategory(c)
            return c.copy(id = id).also {
                cats.add(it); newCategories.add(if (parent != null) "${parent.name} › $name" else name)
            }
        }

        /** Returns (categoryId, subcategoryId). */
        suspend fun categoryIds(type: Int, name: String?, subName: String?): Pair<Long?, Long?> {
            if (name == null) return null to null
            fun eq(a: String, b: String) = a.trim().equals(b.trim(), ignoreCase = true)

            suspend fun sub(parent: Category, s: String?): Long? {
                if (s == null) return null
                return (cats.firstOrNull { it.parentId == parent.id && eq(it.name, s) } ?: createCategory(s.trim(), type, parent)).id
            }

            // 1. An existing top-level category with that name.
            cats.firstOrNull { it.parentId == null && it.type == type && eq(it.name, name) }?.let { return it.id to sub(it, subName) }
            if (subName == null) {
                // 2. An existing subcategory with that name, e.g. "Fuel" -> Bike › Fuel.
                cats.firstOrNull { it.parentId != null && it.type == type && eq(it.name, name) }?.let { return it.parentId to it.id }
                // 3. "Bike Maintenance" -> Bike › Maintenance, when "Bike" already existed.
                parentsBefore.filter { it.type == type }.sortedByDescending { it.name.length }.firstOrNull {
                    name.trim().length > it.name.length + 1 && name.trim().startsWith(it.name + " ", ignoreCase = true)
                }?.let { p ->
                    val rest = name.trim().substring(p.name.length).trim().replaceFirstChar { it.uppercase() }
                    return p.id to sub(p, rest)
                }
            }
            // 4. Create it.
            val p = createCategory(name.trim(), type, null)
            return p.id to sub(p, subName)
        }

        val existingKeys = dao.allTxns().map { key(it) }.toMutableSet()
        val toInsert = mutableListOf<Txn>()
        var duplicates = 0
        for (r in parsed.rows) {
            val (catId, subId) = categoryIds(r.type, r.category, r.subcategory)
            val t = Txn(
                type = r.type, amount = r.amount, date = r.date,
                accountId = accountId(r.account),
                toAccountId = r.toAccount?.let { accountId(it) },
                categoryId = catId, subcategoryId = subId, memo = r.memo,
            )
            if (!existingKeys.add(key(t))) { duplicates++; continue }
            toInsert.add(t)
        }
        dao.insertTxns(toInsert)
        BackupScheduler.onDataChanged(ctx)
        Summary(toInsert.size, duplicates, newAccounts, newCategories, parsed.errors)
    }

    /** Same date (to the minute), type, amount, account and memo = treated as the same record. */
    private fun key(t: Txn) = listOf(t.date / 60_000, t.type, t.amount, t.accountId, t.toAccountId, t.memo.trim()).joinToString("|")
}
