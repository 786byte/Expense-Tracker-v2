# Expense Tracker (Android)

A native, installable Android app (Kotlin + Jetpack Compose). Your data stays on your phone and is backed up to a file you choose, such as one in your Google Drive.

## Install

1. On your phone, open this repository's **Releases** page and download `ExpenseTracker.apk` from the newest release.
2. Open the file. If Android asks, allow your browser or file manager to "install unknown apps".
3. Each new build installs as an update over the old one, and your data is kept.

Every push is built automatically by GitHub Actions (`.github/workflows/build.yml`).

## Features

| Area | In the app |
|---|---|
| Home | Status cat (tap for advice), a running cat that changes daily, total balance, this month's income and expense, records by day. 🔍 search at the top, 🎤 add by voice, **+** for expense / income / transfer. |
| Status cat | Six moods from your total balance compared with your usual monthly income: deep in debt (crying, empty bowl), a bit in debt (worried), overspending (angry), even, in plus, well in plus (sunglasses). Tap it for rule-based advice: balance, this month vs last month, fastest-growing category, saving rate, daily budget, loan warnings. |
| Records | Category › subcategory, one or several source accounts (split bills), title, description, date/time, one photo (cropped and rotated before saving). Tapping a record shows a summary; editing only after pressing **Edit**. |
| Accounts tab | Opens on Accounts with balances. Tap the title (“Accounts ▾”) to switch to Categories or Subcategories. Period: month, custom range or all time. **Edit** to add, change, move, merge or delete. |
| Analysis tab | Expense / income / both pie, 6-month bars, biggest records. Same period choices. |
| Customisation | Themes (Ginger, Midnight black cat, Siamese cream, Grey tabby, Calico) in light/dark, AMOLED black, wallpaper colors, text size, Home sections show/hide and order, list icon (category or subcategory), currency symbol/position/decimals/1,00,000 grouping, status-cat strictness. Icons: 90+ built-in, or your own picture cropped to a circle. |
| Backup | Backup & settings → **Choose backup file** → pick Google Drive. Updated about 30 s after every change and daily, including photos and custom icons. Restore on a new phone. CSV export and import from other apps. |
| Guide | Menu → Guide. |

## Project layout

```
app/src/main/java/com/byte786/expensetracker/
  data/   Room database, backup/restore, photo storage, settings
  ui/     Compose screens (home, add/edit, accounts, analysis, search, settings, guide) and components
```

The APK is signed with `app/keystore/shared.keystore`, so updates install over each other. Because the repository is public, this key is public too: only install APKs from this repository's Releases page.
