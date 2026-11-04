package com.byte786.expensetracker.ui

import android.net.Uri

object Routes {
    const val HOME = "home"
    const val REPORTS = "reports"
    const val CATEGORIES = "categories"
    const val ACCOUNTS = "accounts"
    const val SETTINGS = "settings"
    const val GUIDE = "guide"
    const val EDIT = "edit?id={id}&type={type}"
    const val SEARCH = "search?cat={cat}&from={from}&to={to}&type={type}&q={q}"

    fun edit(id: Long = 0, type: Int = 0) = "edit?id=$id&type=$type"

    /** -1 means "no filter". [to] is exclusive. */
    fun search(cat: Long = -1, from: Long = -1, to: Long = -1, type: Int = -1, q: String = "") =
        "search?cat=$cat&from=$from&to=$to&type=$type&q=${Uri.encode(q)}"
}
