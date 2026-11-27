package com.byte786.expensetracker

import android.app.Application
import com.byte786.expensetracker.data.BackupScheduler
import com.byte786.expensetracker.data.Repository

class ExpenseApp : Application() {
    lateinit var repo: Repository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        repo = Repository(this)
        BackupScheduler.refreshSchedule(this)
    }

    companion object {
        lateinit var instance: ExpenseApp
            private set
    }
}
