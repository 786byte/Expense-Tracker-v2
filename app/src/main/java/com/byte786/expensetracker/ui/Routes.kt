package com.byte786.expensetracker.ui

import android.net.Uri

object Routes {
    // Bottom-bar tabs
    const val HOME = "home"
    const val ACCOUNTS = "accounts"
    const val ANALYSIS = "analysis"
    val TABS = setOf(HOME, ACCOUNTS, ANALYSIS)

    const val GUIDE = "guide"
    const val SETTINGS = "settings?action={action}"
    const val EDIT = "edit?id={id}&type={type}"
    const val SEARCH = "search?cat={cat}&from={from}&to={to}&type={type}&q={q}"

    /** [action]: "" or "export" / "import" to open that file picker straight away. */
    fun settings(action: String = "") = "settings?action=$action"

    fun edit(id: Long = 0, type: Int = 0) = "edit?id=$id&type=$type"

    /** -1 means "no filter". [to] is exclusive. */
    fun search(cat: Long = -1, from: Long = -1, to: Long = -1, type: Int = -1, q: String = "") =
        "search?cat=$cat&from=$from&to=$to&type=$type&q=${Uri.encode(q)}"
}
