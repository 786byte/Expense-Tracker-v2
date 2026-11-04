package com.byte786.expensetracker.data

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow

class Prefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("settings", Context.MODE_PRIVATE)

    val currency = MutableStateFlow(sp.getString("currency", "৳") ?: "৳")
    fun setCurrency(v: String) { currency.value = v; sp.edit { putString("currency", v) } }

    /** 0 = follow system, 1 = light, 2 = dark */
    val themeMode = MutableStateFlow(sp.getInt("themeMode", 0))
    fun setThemeMode(v: Int) { themeMode.value = v; sp.edit { putInt("themeMode", v) } }

    val dynamicColor = MutableStateFlow(sp.getBoolean("dynamicColor", false))
    fun setDynamicColor(v: Boolean) { dynamicColor.value = v; sp.edit { putBoolean("dynamicColor", v) } }

    val backupUri = MutableStateFlow(sp.getString("backupUri", null))
    fun setBackupUri(v: String?) { backupUri.value = v; sp.edit { putString("backupUri", v) } }

    val autoBackup = MutableStateFlow(sp.getBoolean("autoBackup", true))
    fun setAutoBackup(v: Boolean) { autoBackup.value = v; sp.edit { putBoolean("autoBackup", v) } }

    val lastBackup = MutableStateFlow(sp.getLong("lastBackup", 0L))
    val lastBackupError = MutableStateFlow(sp.getString("lastBackupError", null))
    fun setBackupResult(time: Long, error: String?) {
        if (error == null) lastBackup.value = time
        lastBackupError.value = error
        sp.edit {
            if (error == null) putLong("lastBackup", time)
            putString("lastBackupError", error)
        }
    }

    var seeded: Boolean
        get() = sp.getBoolean("seeded", false)
        set(v) = sp.edit { putBoolean("seeded", v) }

    var guideDismissed: Boolean
        get() = sp.getBoolean("guideDismissed", false)
        set(v) = sp.edit { putBoolean("guideDismissed", v) }
}
