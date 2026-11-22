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
- **Local unit tests for pure-Kotlin code** (parsers, calculator) work without
  Android. Download `kotlin-compiler-embeddable` 2.1.0, `kotlin-stdlib`, trove4j,
  `kotlin-script-runtime`, `kotlin-reflect`, annotations, coroutines, junit and
  hamcrest from `https://repo.maven.apache.org/maven2` (repo1.maven.org is often
  rate-limited). Then compile with
  `java -cp <jars> org.jetbrains.kotlin.cli.jvm.K2JVMCompiler -no-stdlib ...` and
  run `org.junit.runner.JUnitCore`. A one-line stub `object TxnType` is needed when
  compiling outside the app.
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
| `data/AppDatabase.kt` | Room **v2**; `MIGRATION_1_2` adds `description` and `groupId` (both nullable, no defaults) |
| `data/AppDao.kt` | Queries, account balances SQL, `replaceTxns`, `moveCategory`, `replaceAll` |
| `data/Repository.kt` | StateFlows (`txns`, `categories`, `accounts`), `launch{}` for writes that must survive screen close, `saveRecords`, `deleteTxn` (deletes the whole split group), resets with a safety copy, `voiceDraft` |
| `data/Backup.kt` / `BackupScheduler.kt` | Zip backup (data.json + photos) to a user-chosen file (e.g. Google Drive) via SAF; auto backup 30 s after changes plus daily; restore; CSV export |
| `data/CsvParser.kt` / `CsvImport.kt` | Import from other apps' CSV (tolerant quoting, multi-line notes split into title/description, `From->To` transfers, dedupe, `Bike X` → Bike › X) |
| `data/Calc.kt` | Calculator expression evaluator (`600+400-50`) |
| `data/VoiceParser.kt` | Spoken English → draft record, matching the user's own category, subcategory and account names |
| `data/Prefs.kt` | SharedPreferences: currency, theme mode, dynamic colour, backup URI, flags |
| `data/Seed.kt` | Default accounts and categories on first run |
| `ui/AppRoot.kt` | Drawer (search box + chips, Backup & settings, Export, Import CSV, Guide, Delete & reset) + bottom bar (Accounts / Home / Analysis) + NavHost |
| `ui/Routes.kt` | Routes; `settings?action=export|import` |
| `ui/AppIcons.kt` | Icon keys → Material icons, colour palette |
| `ui/theme/Theme.kt` | Cat palette (cream/ginger light, dark-brown dark), `MoneyColors`, `LocalCurrency` |
| `ui/components/Common.kt` | `Lookup` (maps + `merged()` that turns split bills into one row), `TxnRow`, day-grouped lists, `PeriodBar`, dialogs, icon/colour picker |
| `ui/components/Cats.kt` | Canvas cats: `CatFace` (HAPPY/CRYING/ANGRY/NEUTRAL/PLAIN + badge), `statusMood()`, `RunningCat` with 7 weekday `CatCoat`s |
| `ui/components/Pickers.kt` | `CalculatorSheet`, `CategorySheet` (icon grid), `SubcategorySheet` (name list), both with edit mode |
| `ui/components/VoiceInput.kt` | `RecognizerIntent` (en-IN) launcher + parse |
| `ui/screens/HomeScreen.kt` | Status cat (tap → tooltip mood), running cat, total balance, month totals + switcher, records list, + menu with 3 cats, mic button |
| `ui/screens/EditTransactionScreen.kt` | Add/edit: type, calculator amount, category/sub buttons, **several source accounts** (first gets the rest), transfer from/to, date/time, title, description, photo, voice fill |
| `ui/screens/AccountsTab.kt` | Categories → subcategories → records; Accounts → records; Edit mode; own period |
| `ui/screens/AnalysisScreen.kt` | Expense/Income/Both donut + legend, 6-month bars, biggest records |
| `ui/screens/SearchScreen.kt`, `SettingsScreen.kt`, `GuideScreen.kt`, `ManageDialogs.kt` | Search & filters; backup/restore/import/export/undo reset/theme/currency; in-app guide; category & account edit dialogs |

Conventions:
- Amounts are `Long` paisa.
- Writes started from dialogs use `repo.launch {}` so closing the dialog doesn't cancel them.
- Lists call `lk.merged(list)` so split bills show as one row.
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

## 5. Backlog: the owner's latest requests (not built yet)

Interpretations are mine. Confirm the items marked **(ask)** before building.

1. **Crop photos** when attaching (camera or gallery) before saving. Use a crop
   library (e.g. CanHub Android-Image-Cropper, which needs mavenCentral) or a
   simple Compose crop screen.
2. **Tapping a record never opens edit directly**, on Home, in Analysis (biggest
   records), in Search and in Accounts lists. Instead show a **summary sheet**:
   amount, type, category › subcategory, account(s) incl. split parts, date/time,
   title, **description**, photo thumbnail, with an **Edit** button (and maybe
   Delete). The Edit screen stays as it is.
3. **Every history view gets period choices: This month · Custom range · All
   time.** Today `PeriodBar` has month arrows + All time; add custom range
   (`DateRangePicker`). Apply in the Accounts tab details, Analysis and Search.
4. **Record icon in lists**: show the **category** icon by default (now it shows
   the subcategory's), plus a Settings option "Icon in lists: Category /
   Subcategory". `Lookup.icon()` decides this.
5. **Remove search from the side menu completely** (the box and the quick chips).
   Keep the search icon in Home's top bar. Remove the drawer's search code and
   update the Guide.
6. *(Same as 2 for Analysis.)*
7. **Accounts tab redesign**: the tab shows **Accounts** by default. Remove the
   Categories/Accounts segmented control. Tapping the **title at the top**
   ("Accounts ▾", not the bottom bar) opens a menu to switch the view to
   **Categories** or **Subcategories** (a flat list of all subcategories with
   totals). The bottom bar keeps Accounts on the left. **(ask)** whether
   "Subcategories" should be a separate flat view or reached through categories.
8. **Much more customisation**:
   - **Several themes**: e.g. Ginger (current), Midnight black cat, Siamese
     cream, Grey tabby, Calico, plus AMOLED black. Choose in Settings; keep
     light/dark/system and wallpaper colours.
   - **Custom icons**: let the user pick an image, crop it, and use it as a
     category or account icon. Store the file in `filesDir/icons`; the `icon`
     key could be `file:<name>`, and backups must include it.
   - **(ask)** what else to make customisable (font size? home sections
     order/visibility? currency format?).
9. **Status cat (top-left of Home) more expressive, scaled to how much in debt
   or in plus.** Ideas: very in debt (crying hard, empty bowl), slightly in debt
   (worried), even (neutral), slightly up (smile), well up (sunglasses / money
   eyes), angry when this month's spending is over income. Little animations.
   **(ask)** the thresholds (e.g. as % of monthly income, or fixed ৳ amounts set
   in Settings).
10. **Tap the status cat → advice** instead of just the mood. Rule-based, no AI:
    - Where the balance stands.
    - This month vs last month spending.
    - The category that grew most.
    - Saving rate.
    - Days left in the month with an average daily budget.
    - Loan account warnings.
    Show it in a bottom sheet with the cat.
11. **More icons**, matching their real categories (see screenshots in the
    conversation): Pulsar N160 (motorcycle), Aura Maxing (shield /
    self-improvement), Deen (mosque), Phone (smartphone), Fragrance / Attar
    (perfume bottle; may need a custom vector), Moms Tax (hand with money /
    tax), Tution / Tuition (teacher, history_edu), Education / Alliance
    Française (language / translate), Job / Application (description / work),
    Subscription / Claude (subscriptions / autorenew), Pc (computer), Salary,
    Mom's Blessing (volunteer_activism), Grooming, Vape, Theatre
    (theater_comedy), Lottery (casino / confirmation_number), Sale (sell),
    Grants (redeem), Barber (content_cut), Gym (fitness_center), Games, Fuel,
    Headphones. Then set better default icons for those category names
    (`CsvImport.guessIcon`).

Smaller things seen in the screenshots:
- Category subtitles truncate ("5 r…"). Show the record count separately or use
  two lines.
- There are two "Education" categories (an old one and the user's). Offer a
  merge-categories action (move all records of one into another).
- README.md is out of date (mentions menu → Categories); refresh it.

## 6. Prompt to paste into the new session

> Continue my Android expense tracker in repo 786byte/Expense-Tracker-v2, branch
> `claude/android-expense-tracker-go5jzv`. First read `docs/HANDOFF.md`: it has
> the code map, how builds work (GitHub Actions only), my preferences, and the
> backlog in section 5. Ask me the "(ask)" questions as multiple choice in one
> go, then build the whole backlog, push, wait for the CI build to pass (re-run
> it if GitHub has download timeouts), and tell me the release link. Also update
> HANDOFF.md at the end.
