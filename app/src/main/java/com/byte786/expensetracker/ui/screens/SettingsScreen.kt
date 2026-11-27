@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.byte786.expensetracker.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.byte786.expensetracker.ExpenseApp
import com.byte786.expensetracker.data.Backup
import com.byte786.expensetracker.data.BackupScheduler
import com.byte786.expensetracker.data.CsvImport
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Slider
import androidx.compose.ui.Alignment
import com.byte786.expensetracker.data.CatMood
import com.byte786.expensetracker.ui.Money
import com.byte786.expensetracker.ui.components.CatFace
import com.byte786.expensetracker.ui.theme.CatPalette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import com.byte786.expensetracker.ui.Dates
import com.byte786.expensetracker.ui.components.ConfirmDialog
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun SettingsScreen(nav: NavController, action: String = "") {
    val repo = ExpenseApp.instance.repo
    val prefs = repo.prefs
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    val currency by prefs.currency.collectAsState()
    val themeMode by prefs.themeMode.collectAsState()
    val dynamic by prefs.dynamicColor.collectAsState()
    val paletteName by prefs.palette.collectAsState()
    val amoled by prefs.amoled.collectAsState()
    val handmade by prefs.handmade.collectAsState()
    val fontSize by prefs.fontSize.collectAsState()
    val listIconSub by prefs.listIconSub.collectAsState()
    val symbolAfter by prefs.symbolAfter.collectAsState()
    val decimals by prefs.decimals.collectAsState()
    val indian by prefs.indianGrouping.collectAsState()
    val homeOrder by prefs.homeOrder.collectAsState()
    val homeHidden by prefs.homeHidden.collectAsState()
    val neutralPct by prefs.moodNeutralPct.collectAsState()
    val veryPct by prefs.moodVeryPct.collectAsState()
    var confirmIcons by remember { mutableStateOf(false) }
    val backupUri by prefs.backupUri.collectAsState()
    val autoBackup by prefs.autoBackup.collectAsState()
    val lastBackup by prefs.lastBackup.collectAsState()
    val lastError by prefs.lastBackupError.collectAsState()
    var busy by remember { mutableStateOf(false) }
    var restoreUri by remember { mutableStateOf<Uri?>(null) }
    var importResult by remember { mutableStateOf<CsvImport.Summary?>(null) }

    fun fileName(uri: String?): String? = uri?.let { u ->
        runCatching {
            ctx.contentResolver.query(Uri.parse(u), arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        }.getOrNull() ?: "Chosen file"
    }

    val backupName = remember(backupUri) { fileName(backupUri) }

    fun runBackup(uri: Uri) {
        busy = true
        scope.launch {
            val result = runCatching { Backup.backupTo(ctx, uri) }
            prefs.setBackupResult(System.currentTimeMillis(), result.exceptionOrNull()?.message)
            busy = false
            snackbar.showSnackbar(if (result.isSuccess) "Backup saved" else "Backup failed: ${result.exceptionOrNull()?.message}")
        }
    }

    val chooseLocation = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) {
            runCatching {
                ctx.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
            }
            prefs.setBackupUri(uri.toString())
            BackupScheduler.refreshSchedule(ctx)
            runBackup(uri)
        }
    }
    val oneOffBackup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) runBackup(uri)
    }
    val pickRestore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) restoreUri = uri
    }
    val importCsv = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            busy = true
            scope.launch {
                val r = runCatching { CsvImport.import(ctx, uri) }
                busy = false
                r.onSuccess { importResult = it }
                    .onFailure { snackbar.showSnackbar("Import failed: ${it.message}") }
            }
        }
    }
    val exportCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) scope.launch {
            val r = runCatching { Backup.exportCsv(ctx, uri) }
            snackbar.showSnackbar(if (r.isSuccess) "CSV exported" else "Export failed: ${r.exceptionOrNull()?.message}")
        }
    }

    // Opened from the side menu's Export / Import CSV.
    var actionHandled by rememberSaveable { mutableStateOf(false) }
    var exportChooser by remember { mutableStateOf(false) }
    LaunchedEffect(action) {
        if (!actionHandled) {
            actionHandled = true
            when (action) {
                "export" -> exportChooser = true
                "import" -> importCsv.launch(arrayOf("text/csv", "text/comma-separated-values", "text/plain", "application/vnd.ms-excel", "*/*"))
            }
        }
    }
    var hasSafetyCopy by remember { mutableStateOf(repo.safetyFile.exists()) }
    var confirmUndo by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Backup & settings") },
                navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            )
        },
    ) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ---------- Backup ----------
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Backup to Google Drive", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Tap “Choose backup file” and pick Google Drive in the file picker (or any folder). " +
                            "The app keeps that file up to date automatically: about 30 seconds after every change, and once a day.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    ListItem(
                        headlineContent = { Text(if (backupUri == null) "No backup file chosen" else backupName ?: "") },
                        supportingContent = {
                            Text(
                                when {
                                    lastError != null -> "Last attempt failed: $lastError"
                                    lastBackup > 0 -> "Last backup: ${Dates.shortDay(lastBackup)} ${Dates.time(lastBackup)}"
                                    else -> "Never backed up"
                                },
                                color = if (lastError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        leadingContent = { Icon(Icons.Filled.CloudUpload, null) },
                    )
                    Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Text("Automatic backup", Modifier.weight(1f))
                        Switch(checked = autoBackup, onCheckedChange = {
                            prefs.setAutoBackup(it); BackupScheduler.refreshSchedule(ctx)
                        })
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { chooseLocation.launch("ExpenseTracker-backup.zip") }, enabled = !busy) {
                            Icon(Icons.Filled.FolderOpen, null); androidx.compose.foundation.layout.Spacer(Modifier.width(6.dp))
                            Text(if (backupUri == null) "Choose backup file" else "Change file")
                        }
                        OutlinedButton(
                            onClick = { backupUri?.let { runBackup(Uri.parse(it)) } },
                            enabled = !busy && backupUri != null,
                        ) {
                            Icon(Icons.Filled.Backup, null); androidx.compose.foundation.layout.Spacer(Modifier.width(6.dp)); Text("Back up now")
                        }
                    }
                }
            }

            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Restore & export", style = MaterialTheme.typography.titleMedium)
                    if (hasSafetyCopy) {
                        Button(onClick = { confirmUndo = true }, enabled = !busy) {
                            Icon(Icons.Filled.Restore, null); androidx.compose.foundation.layout.Spacer(Modifier.width(6.dp)); Text("Undo the last reset")
                        }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { pickRestore.launch(arrayOf("application/zip", "application/octet-stream", "*/*")) }, enabled = !busy) {
                            Icon(Icons.Filled.Restore, null); androidx.compose.foundation.layout.Spacer(Modifier.width(6.dp)); Text("Restore backup")
                        }
                        OutlinedButton(onClick = { oneOffBackup.launch("ExpenseTracker-${LocalDate.now()}.zip") }, enabled = !busy) {
                            Icon(Icons.Filled.Backup, null); androidx.compose.foundation.layout.Spacer(Modifier.width(6.dp)); Text("Save a copy")
                        }
                        OutlinedButton(onClick = { exportCsv.launch("expenses-${LocalDate.now()}.csv") }) {
                            Icon(Icons.Filled.FileDownload, null); androidx.compose.foundation.layout.Spacer(Modifier.width(6.dp)); Text("Export CSV")
                        }
                        OutlinedButton(
                            onClick = { importCsv.launch(arrayOf("text/csv", "text/comma-separated-values", "text/plain", "application/vnd.ms-excel", "*/*")) },
                            enabled = !busy,
                        ) {
                            Icon(Icons.Filled.FileUpload, null); androidx.compose.foundation.layout.Spacer(Modifier.width(6.dp)); Text("Import CSV")
                        }
                    }
                    Text(
                        "Import CSV adds records from another app's export (columns like TIME, TYPE, AMOUNT, CATEGORY, ACCOUNT, NOTES). " +
                            "Missing accounts and categories are created. Records that are already here are skipped, so importing twice is safe.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "Android's own Google backup also saves this app's data to your Google account " +
                            "(Settings → Google → Backup), so a reinstall on the same account brings it back.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // ---------- Appearance ----------
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Appearance", style = MaterialTheme.typography.titleMedium)
                    SwitchRow("Handmade look", "Handwriting font and pencil-drawn boxes, like a notebook", handmade) { prefs.setHandmade(it) }
                    Text("Theme", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        CatPalette.entries.forEach { p ->
                            FilterChip(
                                selected = paletteName == p.name && !dynamic,
                                onClick = { prefs.setPalette(p.name); if (dynamic) prefs.setDynamicColor(false) },
                                label = { Text(p.label) },
                                leadingIcon = {
                                    CatFace(CatMood.PLAIN, Modifier.size(22.dp), fur = p.cat.fur, eyes = p.cat.eyes, nose = p.cat.ink, animate = false)
                                },
                            )
                        }
                    }
                    Text("Light or dark", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("System", "Light", "Dark").forEachIndexed { i, label ->
                            FilterChip(selected = themeMode == i, onClick = { prefs.setThemeMode(i) }, label = { Text(label) })
                        }
                    }
                    SwitchRow("Pure black (AMOLED)", "Black backgrounds in dark mode; saves battery on OLED screens", amoled) { prefs.setAmoled(it) }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        SwitchRow("Wallpaper colors", "Match the app to your phone's theme instead of a cat theme", dynamic) { prefs.setDynamicColor(it) }
                    }
                    Text("Text size", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Small", "Normal", "Large", "Huge").forEachIndexed { i, label ->
                            FilterChip(selected = fontSize == i, onClick = { prefs.setFontSize(i) }, label = { Text(label) })
                        }
                    }
                    Text("Icon on records in lists", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = !listIconSub, onClick = { prefs.setListIconSub(false) }, label = { Text("Category") })
                        FilterChip(selected = listIconSub, onClick = { prefs.setListIconSub(true) }, label = { Text("Subcategory") })
                    }
                    OutlinedButton(onClick = { confirmIcons = true }) {
                        Icon(Icons.Filled.AutoFixHigh, null); Spacer(Modifier.width(6.dp)); Text("Pick icons from category names")
                    }
                }
            }

            // ---------- Home layout ----------
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Home screen", style = MaterialTheme.typography.titleMedium)
                    Text("Show", style = MaterialTheme.typography.labelLarge)
                    listOf(
                        "statusCat" to "Status cat (top left)",
                        "runningCat" to "Running cat",
                        "balance" to "Total balance",
                        "totals" to "Month income & expense",
                    ).forEach { (key, label) ->
                        SwitchRow(label, null, key !in homeHidden) { on ->
                            prefs.setHomeHidden(if (on) homeHidden - key else homeHidden + key)
                        }
                    }
                    Text("Order", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
                    val names = mapOf("hero" to "Cats & balance", "totals" to "Month totals & switcher", "records" to "Records")
                    homeOrder.forEachIndexed { i, key ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${i + 1}. ${names[key] ?: key}", Modifier.weight(1f))
                            IconButton(onClick = { prefs.setHomeOrder(homeOrder.toMutableList().apply { add(i - 1, removeAt(i)) }) }, enabled = i > 0) {
                                Icon(Icons.Filled.KeyboardArrowUp, "Move up")
                            }
                            IconButton(onClick = { prefs.setHomeOrder(homeOrder.toMutableList().apply { add(i + 1, removeAt(i)) }) }, enabled = i < homeOrder.size - 1) {
                                Icon(Icons.Filled.KeyboardArrowDown, "Move down")
                            }
                        }
                    }
                }
            }

            // ---------- Status cat ----------
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Status cat", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "The cat compares your total balance with your usual monthly income (average of the last 3 months).",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text("“About even” within ±$neutralPct% of it", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
                    Slider(
                        value = neutralPct.toFloat(), onValueChange = { prefs.setMoodNeutralPct(it.toInt()) },
                        valueRange = 0f..50f, steps = 9,
                    )
                    Text("“Very” in debt or in plus beyond $veryPct%", style = MaterialTheme.typography.labelLarge)
                    Slider(
                        value = veryPct.toFloat(), onValueChange = { prefs.setMoodVeryPct(it.toInt()) },
                        valueRange = 20f..300f, steps = 27,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        listOf(CatMood.VERY_DEBT, CatMood.DEBT, CatMood.ANGRY, CatMood.NEUTRAL, CatMood.PLUS, CatMood.RICH).forEach {
                            CatFace(it, Modifier.size(40.dp), animate = false)
                        }
                    }
                }
            }

            // ---------- Currency ----------
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Currency", style = MaterialTheme.typography.titleMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("৳", "Tk ", "$", "€", "£", "₹").forEach { s ->
                            FilterChip(selected = currency == s, onClick = { prefs.setCurrency(s) }, label = { Text(s.trim()) })
                        }
                    }
                    OutlinedTextField(
                        value = currency, onValueChange = { if (it.length <= 5) prefs.setCurrency(it) },
                        label = { Text("Custom symbol") }, singleLine = true,
                    )
                    SwitchRow("Symbol after the number", "1,250 ৳ instead of ৳1,250", symbolAfter) { prefs.setSymbolAfter(it) }
                    SwitchRow("Show paisa / cents", "৳1,250.50; off rounds to whole taka", decimals) { prefs.setDecimals(it) }
                    SwitchRow("Indian / Bangladeshi grouping", "1,00,000 instead of 100,000", indian) { prefs.setIndianGrouping(it) }
                    Text(
                        "Example: " + Money.format(123456750, currency),
                        style = MaterialTheme.typography.bodyMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    )
                }
            }
        }
    }

    if (confirmIcons) {
        ConfirmDialog(
            "Pick icons from names?",
            "Every category gets an icon that matches its name (e.g. Deen → mosque, Fragrance → perfume, Pulsar → bike). " +
                "Icons you made from your own pictures are kept. You can still change any icon afterwards.",
            confirm = "Update icons",
            onDismiss = { confirmIcons = false },
        ) {
            confirmIcons = false
            scope.launch {
                val n = repo.refreshCategoryIcons()
                snackbar.showSnackbar(if (n == 0) "All icons already match" else "Updated $n icon(s)")
            }
        }
    }

    importResult?.let { s ->
        AlertDialog(
            onDismissRequest = { importResult = null },
            title = { Text(if (s.imported > 0) "Import finished" else "Nothing imported") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Added ${s.imported} record(s).")
                    if (s.duplicates > 0) Text("Skipped ${s.duplicates} already in the app.")
                    if (s.newAccounts.isNotEmpty()) Text("New accounts: ${s.newAccounts.joinToString()}")
                    if (s.newCategories.isNotEmpty()) Text("New categories: ${s.newCategories.joinToString()}")
                    if (s.newAccounts.isNotEmpty()) {
                        Text(
                            "Tip: set each account's starting balance (Accounts tab → Edit) so the balances match reality.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    if (s.errors.isNotEmpty()) {
                        Text("Couldn't read ${s.errors.size} row(s):", color = MaterialTheme.colorScheme.error)
                        s.errors.take(10).forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { importResult = null }) { Text("OK") } },
        )
    }

    if (exportChooser) {
        AlertDialog(
            onDismissRequest = { exportChooser = false },
            title = { Text("Export") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("CSV: a spreadsheet of all records (opens in Excel / Google Sheets).", style = MaterialTheme.typography.bodySmall)
                    Text("Backup copy: everything incl. photos, can be restored into this app.", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = { exportChooser = false; exportCsv.launch("expenses-${LocalDate.now()}.csv") }) { Text("CSV") }
            },
            dismissButton = {
                TextButton(onClick = { exportChooser = false; oneOffBackup.launch("ExpenseTracker-${LocalDate.now()}.zip") }) { Text("Backup copy") }
            },
        )
    }
    if (confirmUndo) {
        ConfirmDialog(
            "Undo the last reset?",
            "Everything in the app now will be replaced by the data saved just before your last “Delete & reset”.",
            confirm = "Restore",
            onDismiss = { confirmUndo = false },
        ) {
            confirmUndo = false
            busy = true
            scope.launch {
                val r = runCatching { Backup.restoreFrom(ctx, Uri.fromFile(repo.safetyFile)) }
                busy = false
                if (r.isSuccess) { repo.safetyFile.delete(); hasSafetyCopy = false; BackupScheduler.onDataChanged(ctx) }
                snackbar.showSnackbar(if (r.isSuccess) "Data from before the reset is back" else "Restore failed: ${r.exceptionOrNull()?.message}")
            }
        }
    }

    restoreUri?.let { uri ->
        ConfirmDialog(
            "Restore this backup?",
            "All records, categories and accounts currently in the app will be replaced by the backup's contents.",
            confirm = "Restore",
            onDismiss = { restoreUri = null },
        ) {
            restoreUri = null
            busy = true
            scope.launch {
                val r = runCatching { Backup.restoreFrom(ctx, uri) }
                busy = false
                snackbar.showSnackbar(if (r.isSuccess) "Backup restored" else "Restore failed: ${r.exceptionOrNull()?.message}")
            }
        }
    }
}

@Composable
private fun SwitchRow(title: String, hint: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title)
            if (hint != null) Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
