# Handoff: Expense Tracker (Android)

Read this first in a new coding session. It sums up everything built so far, how
to build and test it, the owner's preferences, and the **next tasks** (backlog).

## 1. What this is

A personal, native Android expense tracker with a **cat theme**. Kotlin +
Jetpack Compose + Room. No web app, no AI services. The owner sideloads the APK
from this repo's GitHub **Releases** page.

Work branch: `claude/android-expense-tracker-go5jzv` (not merged into `main`; no PR).

## 2. Building, testing, releasing

- **This cloud container cannot build Android.** `dl.google.com` and Google Maven
  are blocked, so there is no Android SDK, AGP or androidx here.
- **GitHub Actions builds everything**: `.github/workflows/build.yml` runs
  `./gradlew testReleaseUnitTest assembleRelease` on every push and publishes
  `ExpenseTracker.apk` as a Release `build-<run_number>` (versionCode = run number,
  so each build installs as an update).
  - Check runs with `gh api repos/786byte/expense-tracker-v2/actions/runs`, and
    logs with the GitHub MCP `get_job_logs`.
  - **Flaky downloads**: GitHub sometimes returns 504s or times out on the Gradle
    or Kotlin downloads. That is not a code error. Re-run with
    `actions_run_trigger` `rerun_failed_jobs` (it took 4 attempts once).
  - Pushes that change only `**.md` or `docs/**` don't trigger a build.
- **Local unit tests for pure-Kotlin code** (parsers, calculator, `Mood`, `Advice`, `Money` formatting, `guessIcon`) work without
  Android. Download `kotlin-compiler-embeddable` 2.1.0, `kotlin-stdlib`, trove4j,
  `kotlin-script-runtime`, `kotlin-reflect`, annotations, coroutines, junit and
  hamcrest from `https://repo.maven.apache.org/maven2` (repo1.maven.org is often
  rate-limited). Then compile with
  `java -cp <jars> org.jetbrains.kotlin.cli.jvm.K2JVMCompiler -no-stdlib ...` and
  run `org.junit.runner.JUnitCore`. A stub `object TxnType` (and a plain `data class Txn`
  without Room annotations, for Mood/Advice) is needed when compiling outside the app.
- **Signing**: `app/keystore/shared.keystore` (password `android`, alias
  `expensetracker`) is committed so updates install over each other. The repo is
  public, so the key is public too. The owner knows this.
- Versions: AGP 8.7.3, Kotlin 2.1.0 (compose plugin), KSP 2.1.0-1.0.29, Gradle
  8.11.1, Compose BOM 2024.12.01 (Material3 1.3.x), Room 2.6.1, WorkManager 2.10,
  Navigation 2.8.5, Coil 2.7, compile/target SDK 35, min SDK 26.

## 3. Code map (`app/src/main/java/com/byte786/expensetracker/`)

| File | What it does |
|---|---|
| `ExpenseApp.kt` | Application; creates `Repository`, schedules backup |
| `data/Entities.kt` | `Account`, `Category` (parentId = subcategory), `Txn` (type, amount in **paisa**, date millis, accountId, toAccountId, categoryId, subcategoryId, `memo` = title, `description`, `photo`, `groupId`) |
| `data/AppDatabase.kt` | Room **v4** (`MIGRATION_3_4` adds `accounts.countInCategories INTEGER NOT NULL DEFAULT 0`); `MIGRATION_1_2` adds `description` and `groupId` (nullable); `MIGRATION_2_3` adds `accounts.tracking INTEGER NOT NULL DEFAULT 0` (entity has `@ColumnInfo(defaultValue = "0")`) |
| `data/AppDao.kt` | Queries, account balances SQL, `replaceTxns`, `moveCategory`, `replaceAll` |
| `data/Repository.kt` | StateFlows (`txns`, `categories`, `accounts`), `launch{}` for writes that must survive screen close, `saveRecords`, `deleteTxn` (deletes the whole split group), resets with a safety copy, `voiceDraft`, `iconsDir`/`iconFile`/`customIcons`, `mergeCategory`, `refreshCategoryIcons`, orphan photo/icon cleanup at start |
| `data/Backup.kt` / `BackupScheduler.kt` | Zip backup (data.json + photos/ + icons/) to a user-chosen file (e.g. Google Drive) via SAF; auto backup 30 s after changes plus daily; restore; CSV export |
| `data/CsvParser.kt` / `CsvImport.kt` | Import from other apps' CSV (tolerant quoting, multi-line notes split into title/description, `From->To` transfers, dedupe, `Bike X` → Bike › X); public `guessIcon()` (word-start rules, first match wins) |
| `data/Calc.kt` | Calculator expression evaluator (`600+400-50`) |
| `data/VoiceParser.kt` | Spoken English → draft record, matching the user's own category, subcategory and account names |
| `data/Prefs.kt` | SharedPreferences: currency + format (symbol after, decimals, Indian grouping), theme mode, palette, AMOLED, font size, dynamic colour, list icon (category/sub), Home order/hidden parts, mood thresholds, backup URI, flags |
| `data/Mood.kt` | `CatMood` enum (VERY_DEBT, DEBT, ANGRY, NEUTRAL, PLUS, RICH, PLAIN) + `MoodRules.mood()` (balance vs usual monthly income, % thresholds) and `averageIncome()` |
| `data/Advice.kt` | Rule-based tips for the status-cat sheet (balance, month vs last month to same day, fastest-growing category, saving rate, daily budget, loan/negative accounts) |
| `data/Photos.kt` | `load()` (EXIF-upright, downscaled Bitmap), `savePhoto()` JPEG ≤1600 px, `saveIcon()` PNG 256 px |
| `data/Seed.kt` | Default accounts and categories on first run |
| `ui/AppRoot.kt` | Drawer (status cat + balance, Backup & settings, Export, Import CSV, Guide, Delete & reset; no search) + bottom bar (Accounts / Home / Analysis) + NavHost |
| `ui/Routes.kt` | Routes; `settings?action=export|import` |
| `ui/AppIcons.kt` | Icon keys → Material icons (90+), `CustomIcons` (Perfume, Mosque vectors), colour palette. Icon key `file:<name>` = picture in `filesDir/icons` |
| `ui/Format.kt` | `Money` (format with global `MoneyStyle`, Indian grouping), `Dates` |
| `ui/theme/Theme.kt` | `CatPalette` enum (GINGER hand-tuned; MIDNIGHT, SIAMESE, GREY_TABBY, CALICO derived with `lerp`), AMOLED override, font scale via `LocalDensity`, `LocalMoneyStyle` (static, forces full recomposition), `LocalCatStyle` (status cat colours per theme), `MoneyColors`, `LocalCurrency` |
| `ui/components/Common.kt` | `Lookup` (maps + `merged()`; `subIcon` flag) and `rememberLookup()` (follows the list-icon setting), `CategoryIcon` (handles `file:`), `TxnRow`, day-grouped lists, `PeriodState`/`rememberPeriodState()`/`PeriodBar` (month · custom range · all time), `DateRangeDialog`, dialogs, `IconColorPicker` (with “use a picture” + crop) |
| `ui/components/RecordSheet.kt` | Read-only record summary bottom sheet (parts of split bills, description, photo) with Edit / Delete; used by every list |
| `ui/components/AdviceSheet.kt` | `Status` + `rememberStatus()` (mood inputs, usual income = avg of last 3 months with income), `AdviceSheet`, `moodReason()` |
| `ui/components/Crop.kt` | `CropDialog`: full-screen crop (move box, drag corners, free/square, rotate 90°) |
| `ui/components/Cats.kt` | Canvas cats: `CatFace` (6 moods + PLAIN, small animations: tears, sweat, shake, bob, blink, sparkle; colours from `LocalCatStyle`), `statusMood()`, `moodName()`, `RunningCat` with 7 weekday `CatCoat`s |
| `ui/components/Pickers.kt` | `CalculatorSheet`, `CategorySheet` (icon grid), `SubcategorySheet` (name list), both with edit mode |
| `ui/components/VoiceInput.kt` | `RecognizerIntent` (en-IN) launcher + parse |
| `ui/screens/HomeScreen.kt` | Blocks in `prefs.homeOrder` (hero / totals / records), parts hideable; status cat (tap → AdviceSheet), running cat, total balance, month totals + switcher, records (tap → RecordSheet), + menu with 3 cats, mic and search buttons |
| `ui/screens/EditTransactionScreen.kt` | Add/edit: type, calculator amount, category/sub buttons, **several source accounts** (first gets the rest), transfer from/to, date/time, title, description, photo (camera/gallery → CropDialog → save), voice fill |
| `ui/screens/AccountsTab.kt` | Title menu “Accounts ▾” switches mode: Accounts (default) → records; Categories → subcategories → records; Subcategories (flat) → records. Record counts shown under amounts. Edit mode; own `PeriodState` |
| `ui/screens/AnalysisScreen.kt` | Expense/Income/Both donut + legend, 6-month bars, biggest records (tap → RecordSheet); `PeriodState` |
| `ui/screens/SearchScreen.kt`, `SettingsScreen.kt`, `GuideScreen.kt`, `ManageDialogs.kt` | Search & filters (date menu incl. custom range / All time); backup/restore/import/export/undo reset, Appearance (themes, AMOLED, text size, list icon, “pick icons from names”), Home screen layout, Status cat sliders, Currency format; in-app guide; category (place it / merge into) & account edit dialogs |

Conventions:
- Amounts are `Long` paisa.
- Writes started from dialogs use `repo.launch {}` so closing the dialog doesn't cancel them.
- Lists call `lk.merged(list)` so split bills show as one row. Build lookups with `rememberLookup()`.
- Tapping a record opens `RecordSheet` (never the editor directly).
- History views use `rememberPeriodState()` + `PeriodBar(state)`.
- Bump the Room version and add a migration for any schema change: the owner has real data, so never use destructive migration.

## 4. About the owner (preferences)

- Lives in Bangladesh; currency ৳; accounts like Cash, bKash, Bank, Tuition, Loan.
  Writes notes in Banglish. Real data has been imported from their old app's CSV.
- **Token-conscious.**
  - Batch their ideas and build in one go.
  - Ask the key questions up front, ideally as multiple choice.
  - Build directly unless a preview is asked for. A clickable HTML preview was
    used once, for the home redesign.
- Wants: the cat theme, minimal clutter, never opening edit mode by accident,
  lots of customisation.
- Sends handwritten sketches and phone screenshots (currently uses dark mode).

## 5. Backlog

The whole previous backlog was built in build 7 (release `build-7`) (record summary sheet, crop, period
choices incl. custom range, list icon setting, drawer search removed, Accounts tab title
menu with a flat Subcategories view, 5 themes + AMOLED, custom picture icons, text size,
Home sections, currency format, 6-mood status cat with %-of-income thresholds and an
advice sheet, more icons + “pick icons from names”, record counts no longer truncated,
merge categories, README refreshed). Owner's answers: Subcategories = separate flat
list; mood thresholds = % of usual monthly income (sliders in Settings); extra
customisation = font size, Home sections, currency format.

Build 8 added: **tracking-only accounts** (`Account.tracking`; parts paid from them are logged but excluded from income/expense via `Lookup.counts()/own()` and `List<Txn>.own()`, merged rows show your own share, accounts tab shows “paid through it”, `totalBalance()` skips them), Home-tab fix (`goTab` pops to the start destination by id, falls back to navigate), and Gen-Z icons (custom vectors PowerUp, Crown, Sigma, Skull, Ghost, Katana, Ramen, Diamond + fire, sparkles, trophy, brain, MMA, nightlife…). Icons are generic shapes, not copies of any anime character.

Build 9 added: per-account **Count in categories** toggle for tracking accounts (`Lookup.countsInCategories()/ownCat()`, `merged(list, forCategories = true)`; used by the Accounts tab category/subcategory views and Analysis; Home month totals, status cat and total balance still use `own()`), tracking accounts list both spent and received, the **handmade look** (`prefs.handmade`, default on: Patrick Hand font from `res/font` (OFL, licence in `docs/licenses/`), `Modifier.sketchBorder()` in `ui/components/Sketch.kt` draws two wobbly pencil outlines; `LocalHandmade`), a **Sketchbook** palette (paper / chalkboard), and a new adaptive app icon (cat peeking over a taka coin; `drawable/ic_launcher_background|foreground|monochrome.xml`).

Nothing new is queued. Ideas if the owner asks for more: budgets per category with the
cat warning, recurring records, home-screen widget, re-crop an already attached photo,
drag-to-reorder categories/accounts, first day of month (salary day).

## 6. Prompt to paste into the new session

> Continue my Android expense tracker in repo 786byte/Expense-Tracker-v2, branch
> `claude/android-expense-tracker-go5jzv`. First read `docs/HANDOFF.md`: it has
> the code map, how builds work (GitHub Actions only), my preferences, and the
> backlog in section 5. Ask me the "(ask)" questions as multiple choice in one
> go, then build the whole backlog, push, wait for the CI build to pass (re-run
> it if GitHub has download timeouts), and tell me the release link. Also update
> HANDOFF.md at the end.
