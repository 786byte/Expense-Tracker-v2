package com.byte786.expensetracker.data

import android.content.Context
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.byte786.expensetracker.ExpenseApp
import java.util.concurrent.TimeUnit

class BackupWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val prefs = ExpenseApp.instance.repo.prefs
        val uri = prefs.backupUri.value ?: return Result.success()
        return try {
            Backup.backupTo(applicationContext, Uri.parse(uri))
            prefs.setBackupResult(System.currentTimeMillis(), null)
            Result.success()
        } catch (e: Exception) {
            prefs.setBackupResult(System.currentTimeMillis(), e.message ?: e.javaClass.simpleName)
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}

object BackupScheduler {
    private const val CHANGE = "backup-after-change"
    private const val DAILY = "backup-daily"

    private fun enabled(): Boolean {
        val p = ExpenseApp.instance.repo.prefs
        return p.autoBackup.value && p.backupUri.value != null
    }

    /** Called after every edit: backs up ~30 s later (bursts of edits collapse into one backup). */
    fun onDataChanged(ctx: Context) {
        if (!enabled()) return
        WorkManager.getInstance(ctx).enqueueUniqueWork(
            CHANGE, ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<BackupWorker>().setInitialDelay(30, TimeUnit.SECONDS).build(),
        )
    }

    fun refreshSchedule(ctx: Context) {
        val wm = WorkManager.getInstance(ctx)
        if (enabled()) {
            wm.enqueueUniquePeriodicWork(
                DAILY, ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<BackupWorker>(1, TimeUnit.DAYS).build(),
            )
        } else {
            wm.cancelUniqueWork(DAILY)
            wm.cancelUniqueWork(CHANGE)
        }
    }
}
