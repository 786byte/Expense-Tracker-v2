package com.byte786.expensetracker.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class Repository(private val ctx: Context) {
    private val db = AppDatabase.build(ctx)
    val dao = db.dao()
    val prefs = Prefs(ctx)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val txns: StateFlow<List<Txn>> = dao.txns().stateIn(scope, SharingStarted.Eagerly, emptyList())
    val categories: StateFlow<List<Category>> = dao.categories().stateIn(scope, SharingStarted.Eagerly, emptyList())
    val accounts: StateFlow<List<AccountBalance>> = dao.accounts().stateIn(scope, SharingStarted.Eagerly, emptyList())

    val photosDir: File get() = File(ctx.filesDir, "photos").apply { mkdirs() }
    fun photoFile(name: String) = File(photosDir, name)

    init {
        scope.launch {
            if (!prefs.seeded) {
                if (dao.allAccounts().isEmpty() && dao.allCategories().isEmpty()) Seed.run(dao)
                prefs.seeded = true
            }
            cleanupOrphanPhotos()
        }
    }

    /** Removes photo files no transaction points to (e.g. picked and then cancelled). */
    private suspend fun cleanupOrphanPhotos() {
        val used = dao.allTxns().mapNotNull { it.photo }.toSet()
        photosDir.listFiles()?.forEach { if (it.name !in used) it.delete() }
    }

    /** Runs a database write that must finish even if the screen that started it closes. */
    fun launch(block: suspend () -> Unit) {
        scope.launch { block() }
    }

    private fun changed() = BackupScheduler.onDataChanged(ctx)

    suspend fun saveTxn(t: Txn): Long = dao.putTxn(t).also { changed() }

    suspend fun deleteTxn(t: Txn) {
        dao.deleteTxn(t)
        t.photo?.let { photoFile(it).delete() }
        changed()
    }

    suspend fun saveCategory(c: Category): Long = dao.putCategory(c).also { changed() }
    suspend fun deleteCategory(c: Category) { dao.deleteCategory(c); changed() }
    suspend fun moveCategory(c: Category, newParent: Long?) { dao.moveCategory(c, newParent); changed() }

    suspend fun saveAccount(a: Account): Long = dao.putAccount(a).also { changed() }
    suspend fun deleteAccount(id: Long): Boolean {
        if (dao.accountUsage(id) > 0) return false
        dao.deleteAccount(id); changed(); return true
    }
}
