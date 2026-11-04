# Expense Tracker (Android)

A native, installable Android app (Kotlin + Jetpack Compose). Your data stays on your phone and is backed up to a file you choose, such as one in your Google Drive.

## Install

1. On your phone, open this repository's **Releases** page and download `ExpenseTracker.apk` from the newest release.
2. Open the file. If Android asks, allow your browser or file manager to "install unknown apps".
3. Each new build installs as an update over the old one, and your data is kept.

Every push is built automatically by GitHub Actions (`.github/workflows/build.yml`).

## Features

| Your request | In the app |
|---|---|
| Category **and** subcategory (Expense → Bike → Mobil / Modification / Parts / Service) | Every record has a category and an optional subcategory. Manage them under menu → Categories, or tap **New** while adding a record. |
| Totals per category and per subcategory | **Reports**: choose month, year or all time; tap a category to see its subcategories. |
| One **+** button for expense, income and transfer | Home screen **+** button, with Expense / Income / Transfer tabs. |
| Home: this month's income and expense | Month card. Arrows switch the month; tapping the name jumps to any month. |
| Biggest expense and income ever | "All-time records" cards. |
| Where I spend most | "Where your money goes" card (this month or all time). |
| Search in a side menu by date, month, time and category | Swipe from the left or tap ☰: search box plus Today, This month, Last month, This year, Pick a date and Pick a month. The full Search page also filters by category, subcategory, type and date range. |
| Upload to Google Drive automatically | Backup & settings → **Choose backup file** → pick Google Drive. The app updates that file about 30 s after every change and once a day. Restore from it on a new phone. Android's own Google backup is also on. |
| Memo and photo | Every record has a memo field and one photo (camera or gallery). |
| Change icons | Any category or account: pick from 60+ icons and 20 colors. |
| In-app guide | Menu → Guide (a banner on Home links to it the first time). |
| Looks right on any screen | Material 3, light and dark themes, adaptive launcher icon. |
| Installable app, not web-based | A native APK. |

Other extras: accounts with live balances (Cash, Bank, bKash…), CSV export, a choice of currency symbol (৳ by default), and an option to use your wallpaper's colors.

## Project layout

```
app/src/main/java/com/byte786/expensetracker/
  data/   Room database, backup/restore, photo storage, settings
  ui/     Compose screens (home, add/edit, search, reports, categories, accounts, settings, guide)
```

The APK is signed with `app/keystore/shared.keystore`, so updates install over each other. Because the repository is public, this key is public too: only install APKs from this repository's Releases page.
