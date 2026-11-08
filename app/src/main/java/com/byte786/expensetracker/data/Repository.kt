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

    /** Saves a bill: replaces the records [oldIds] (empty for a new one) with [records]. */
    suspend fun saveRecords(oldIds: List<Long>, records: List<Txn>, oldPhoto: String?) {
        dao.replaceTxns(oldIds, records)
        if (oldPhoto != null && records.none { it.photo == oldPhoto }) deletePhotoIfUnused(oldPhoto)
        changed()
    }

    /** Deletes a record, and the other parts of it when it was paid from several accounts. */
    suspend fun deleteTxn(t: Txn) {
        val members = t.groupId?.let { dao.group(it) }.orEmpty().ifEmpty { listOf(t) }
        dao.deleteTxnIds(members.map { it.id })
        members.mapNotNull { it.photo }.distinct().forEach { deletePhotoIfUnused(it) }
        changed()
    }

    private suspend fun deletePhotoIfUnused(name: String) {
        if (dao.photoUsage(name) == 0) photoFile(name).delete()
    }

    /** Filled by voice input on Home; picked up by the Add screen it opens. */
    @Volatile var voiceDraft: VoiceParser.Draft? = null

    suspend fun saveCategory(c: Category): Long = dao.putCategory(c).also { changed() }
    suspend fun deleteCategory(c: Category) { dao.deleteCategory(c); changed() }
    suspend fun moveCategory(c: Category, newParent: Long?) { dao.moveCategory(c, newParent); changed() }

    /** Backup taken automatically right before "Delete & reset", so it can be undone. */
    val safetyFile: File get() = File(ctx.filesDir, "safety/before-reset.zip")

    /** Deletes every record (and its photo). Accounts and categories stay. */
    suspend fun deleteAllRecords() {
        Backup.backupToFile(safetyFile)
        dao.clearTxns()
        photosDir.listFiles()?.forEach { it.delete() }
        changed()
    }

    /** Deletes everything and starts over with the default accounts and categories. */
    suspend fun resetEverything() {
        Backup.backupToFile(safetyFile)
        dao.replaceAll(emptyList(), emptyList(), emptyList())
        photosDir.listFiles()?.forEach { it.delete() }
        Seed.run(dao)
        changed()
    }

    suspend fun saveAccount(a: Account): Long = dao.putAccount(a).also { changed() }
    suspend fun deleteAccount(id: Long): Boolean {
        if (dao.accountUsage(id) > 0) return false
        dao.deleteAccount(id); changed(); return true
    }
}
