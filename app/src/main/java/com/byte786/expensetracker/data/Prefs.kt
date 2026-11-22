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

    // ---- Look & feel ----
    /** Colour theme, see ui.theme.CatPalette (by name). */
    val palette = MutableStateFlow(sp.getString("palette", "GINGER") ?: "GINGER")
    fun setPalette(v: String) { palette.value = v; sp.edit { putString("palette", v) } }

    /** Pure black backgrounds in dark mode. */
    val amoled = MutableStateFlow(sp.getBoolean("amoled", false))
    fun setAmoled(v: Boolean) { amoled.value = v; sp.edit { putBoolean("amoled", v) } }

    /** Text size: 0 small, 1 normal, 2 large, 3 extra large. */
    val fontSize = MutableStateFlow(sp.getInt("fontSize", 1))
    fun setFontSize(v: Int) { fontSize.value = v; sp.edit { putInt("fontSize", v) } }

    /** Icon shown on records in lists: false = the category's, true = the subcategory's. */
    val listIconSub = MutableStateFlow(sp.getBoolean("listIconSub", false))
    fun setListIconSub(v: Boolean) { listIconSub.value = v; sp.edit { putBoolean("listIconSub", v) } }

    // ---- Currency format ----
    val symbolAfter = MutableStateFlow(sp.getBoolean("symbolAfter", false))
    fun setSymbolAfter(v: Boolean) { symbolAfter.value = v; sp.edit { putBoolean("symbolAfter", v) } }
    val decimals = MutableStateFlow(sp.getBoolean("decimals", true))
    fun setDecimals(v: Boolean) { decimals.value = v; sp.edit { putBoolean("decimals", v) } }
    val indianGrouping = MutableStateFlow(sp.getBoolean("indianGrouping", false))
    fun setIndianGrouping(v: Boolean) { indianGrouping.value = v; sp.edit { putBoolean("indianGrouping", v) } }

    // ---- Home layout ----
    /** Order of Home's blocks: hero (cats + balance), totals (month income/expense), records. */
    val homeOrder = MutableStateFlow(
        (sp.getString("homeOrder", null) ?: "hero,totals,records").split(",").filter { it in HOME_BLOCKS }
            .let { o -> o + HOME_BLOCKS.filter { it !in o } }
    )
    fun setHomeOrder(v: List<String>) { homeOrder.value = v; sp.edit { putString("homeOrder", v.joinToString(",")) } }

    /** Hidden Home parts: statusCat, runningCat, balance, totals. */
    val homeHidden = MutableStateFlow(sp.getStringSet("homeHidden", emptySet())?.toSet() ?: emptySet())
    fun setHomeHidden(v: Set<String>) { homeHidden.value = v; sp.edit { putStringSet("homeHidden", v) } }

    // ---- Status cat ----
    /** Within ± this % of the usual monthly income counts as "even". */
    val moodNeutralPct = MutableStateFlow(sp.getInt("moodNeutralPct", 10))
    fun setMoodNeutralPct(v: Int) { moodNeutralPct.value = v; sp.edit { putInt("moodNeutralPct", v) } }
    /** Beyond this % of the usual monthly income counts as "very" in debt / in plus. */
    val moodVeryPct = MutableStateFlow(sp.getInt("moodVeryPct", 50))
    fun setMoodVeryPct(v: Int) { moodVeryPct.value = v; sp.edit { putInt("moodVeryPct", v) } }

    companion object {
        val HOME_BLOCKS = listOf("hero", "totals", "records")
    }
}
