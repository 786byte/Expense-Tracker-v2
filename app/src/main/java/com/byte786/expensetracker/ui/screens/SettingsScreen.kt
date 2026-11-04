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
import com.byte786.expensetracker.ui.Dates
import com.byte786.expensetracker.ui.components.ConfirmDialog
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun SettingsScreen(nav: NavController) {
    val repo = ExpenseApp.instance.repo
    val prefs = repo.prefs
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    val currency by prefs.currency.collectAsState()
    val themeMode by prefs.themeMode.collectAsState()
    val dynamic by prefs.dynamicColor.collectAsState()
    val backupUri by prefs.backupUri.collectAsState()
    val autoBackup by prefs.autoBackup.collectAsState()
    val lastBackup by prefs.lastBackup.collectAsState()
    val lastError by prefs.lastBackupError.collectAsState()
    var busy by remember { mutableStateOf(false) }
    var restoreUri by remember { mutableStateOf<Uri?>(null) }

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
    val exportCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) scope.launch {
            val r = runCatching { Backup.exportCsv(ctx, uri) }
            snackbar.showSnackbar(if (r.isSuccess) "CSV exported" else "Export failed: ${r.exceptionOrNull()?.message}")
        }
    }

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
                    }
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
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("System", "Light", "Dark").forEachIndexed { i, label ->
                            FilterChip(selected = themeMode == i, onClick = { prefs.setThemeMode(i) }, label = { Text(label) })
                        }
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Wallpaper colors")
                                Text("Match the app to your phone's theme", style = MaterialTheme.typography.bodySmall)
                            }
                            Switch(checked = dynamic, onCheckedChange = { prefs.setDynamicColor(it) })
                        }
                    }
                }
            }

            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Currency symbol", style = MaterialTheme.typography.titleMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("৳", "Tk ", "$", "€", "£", "₹").forEach { s ->
                            FilterChip(selected = currency == s, onClick = { prefs.setCurrency(s) }, label = { Text(s.trim()) })
                        }
                    }
                    OutlinedTextField(
                        value = currency, onValueChange = { if (it.length <= 5) prefs.setCurrency(it) },
                        label = { Text("Custom") }, singleLine = true,
                    )
                }
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
