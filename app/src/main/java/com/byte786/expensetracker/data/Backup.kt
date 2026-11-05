package com.byte786.expensetracker.data

import android.content.Context
import android.net.Uri
import com.byte786.expensetracker.ExpenseApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.OutputStream
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Backup file = a .zip containing data.json and a photos/ folder.
 * It can be saved anywhere the Android file picker reaches, including Google Drive.
 */
object Backup {
    private const val VERSION = 1

    private fun openOut(ctx: Context, uri: Uri): OutputStream {
        val r = ctx.contentResolver
        return runCatching { r.openOutputStream(uri, "wt") }.getOrNull()
            ?: r.openOutputStream(uri, "w")
            ?: error("Cannot open backup file for writing")
    }

    suspend fun backupTo(ctx: Context, uri: Uri) = withContext(Dispatchers.IO) {
        openOut(ctx, uri).use { writeZip(it) }
    }

    /** Writes a backup into a private file (used for the safety copy taken before a reset). */
    suspend fun backupToFile(file: File) = withContext(Dispatchers.IO) {
        file.parentFile?.mkdirs()
        file.outputStream().use { writeZip(it) }
    }

    private suspend fun writeZip(out: OutputStream) {
        val repo = ExpenseApp.instance.repo
        val dao = repo.dao
        val accounts = dao.allAccounts()
        val categories = dao.allCategories()
        val txns = dao.allTxns()
        val json = JSONObject().apply {
            put("app", "expense-tracker")
            put("version", VERSION)
            put("exportedAt", System.currentTimeMillis())
            put("accounts", JSONArray(accounts.map { it.toJson() }))
            put("categories", JSONArray(categories.map { it.toJson() }))
            put("transactions", JSONArray(txns.map { it.toJson() }))
        }
        run {
            ZipOutputStream(out.buffered()).use { zip ->
                zip.putNextEntry(ZipEntry("data.json"))
                zip.write(json.toString().toByteArray())
                zip.closeEntry()
                txns.mapNotNull { it.photo }.distinct().forEach { name ->
                    val f = repo.photoFile(name)
                    if (f.exists()) {
                        zip.putNextEntry(ZipEntry("photos/$name"))
                        f.inputStream().use { it.copyTo(zip) }
                        zip.closeEntry()
                    }
                }
            }
        }
    }

    /** Replaces ALL current data with the backup's contents. */
    suspend fun restoreFrom(ctx: Context, uri: Uri) = withContext(Dispatchers.IO) {
        val repo = ExpenseApp.instance.repo
        var json: JSONObject? = null
        val tmpDir = File(ctx.cacheDir, "restore").apply { deleteRecursively(); mkdirs() }
        ctx.contentResolver.openInputStream(uri)?.use { input ->
            ZipInputStream(input.buffered()).use { zip ->
                while (true) {
                    val e = zip.nextEntry ?: break
                    when {
                        e.name == "data.json" -> json = JSONObject(zip.readBytes().decodeToString())
                        e.name.startsWith("photos/") && !e.isDirectory -> {
                            val name = File(e.name).name
                            File(tmpDir, name).outputStream().use { zip.copyTo(it) }
                        }
                    }
                }
            }
        } ?: error("Cannot open file")
        val data = json ?: error("This file is not an Expense Tracker backup")
        if (data.optString("app") != "expense-tracker") error("This file is not an Expense Tracker backup")

        val accounts = data.getJSONArray("accounts").objects().map { it.toAccount() }
        val categories = data.getJSONArray("categories").objects().map { it.toCategory() }
        val txns = data.getJSONArray("transactions").objects().map { it.toTxn() }

        repo.photosDir.listFiles()?.forEach { it.delete() }
        tmpDir.listFiles()?.forEach { it.copyTo(repo.photoFile(it.name), overwrite = true) }
        tmpDir.deleteRecursively()
        repo.dao.replaceAll(accounts, categories, txns)
    }

    suspend fun exportCsv(ctx: Context, uri: Uri) = withContext(Dispatchers.IO) {
        val repo = ExpenseApp.instance.repo
        val cats = repo.dao.allCategories().associateBy { it.id }
        val accs = repo.dao.allAccounts().associateBy { it.id }
        val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        fun esc(s: String) = "\"" + s.replace("\"", "\"\"") + "\""
        openOut(ctx, uri).bufferedWriter().use { w ->
            w.write("Date,Type,Amount,Account,To account,Category,Subcategory,Memo\n")
            repo.dao.allTxns().forEach { t ->
                val type = when (t.type) { TxnType.INCOME -> "Income"; TxnType.TRANSFER -> "Transfer"; else -> "Expense" }
                val row = listOf(
                    fmt.format(Instant.ofEpochMilli(t.date).atZone(ZoneId.systemDefault())),
                    type,
                    BigDecimal.valueOf(t.amount, 2).toPlainString(),
                    accs[t.accountId]?.name ?: "",
                    t.toAccountId?.let { accs[it]?.name } ?: "",
                    t.categoryId?.let { cats[it]?.name } ?: "",
                    t.subcategoryId?.let { cats[it]?.name } ?: "",
                    t.memo,
                )
                w.write(row.joinToString(",") { esc(it) })
                w.write("\n")
            }
        }
    }

    // ---- JSON helpers ----
    private fun JSONArray.objects() = (0 until length()).map { getJSONObject(it) }
    private fun JSONObject.optLongOrNull(k: String): Long? = if (isNull(k) || !has(k)) null else getLong(k)
    private fun JSONObject.optStringOrNull(k: String): String? = if (isNull(k) || !has(k)) null else getString(k)

    private fun Account.toJson() = JSONObject().apply {
        put("id", id); put("name", name); put("icon", icon); put("color", color)
        put("initialBalance", initialBalance); put("sortOrder", sortOrder)
    }
    private fun JSONObject.toAccount() = Account(
        id = getLong("id"), name = getString("name"), icon = optString("icon", "wallet"),
        color = optLong("color", 0xFF00897B), initialBalance = optLong("initialBalance"), sortOrder = optInt("sortOrder"),
    )

    private fun Category.toJson() = JSONObject().apply {
        put("id", id); put("name", name); put("type", type); put("parentId", parentId ?: JSONObject.NULL)
        put("icon", icon); put("color", color); put("sortOrder", sortOrder)
    }
    private fun JSONObject.toCategory() = Category(
        id = getLong("id"), name = getString("name"), type = getInt("type"), parentId = optLongOrNull("parentId"),
        icon = optString("icon", "category"), color = optLong("color", 0xFF757575), sortOrder = optInt("sortOrder"),
    )

    private fun Txn.toJson() = JSONObject().apply {
        put("id", id); put("type", type); put("amount", amount); put("date", date); put("accountId", accountId)
        put("toAccountId", toAccountId ?: JSONObject.NULL)
        put("categoryId", categoryId ?: JSONObject.NULL)
        put("subcategoryId", subcategoryId ?: JSONObject.NULL)
        put("memo", memo); put("photo", photo ?: JSONObject.NULL)
    }
    private fun JSONObject.toTxn() = Txn(
        id = getLong("id"), type = getInt("type"), amount = getLong("amount"), date = getLong("date"),
        accountId = getLong("accountId"), toAccountId = optLongOrNull("toAccountId"),
        categoryId = optLongOrNull("categoryId"), subcategoryId = optLongOrNull("subcategoryId"),
        memo = optString("memo", ""), photo = optStringOrNull("photo"),
    )
}
