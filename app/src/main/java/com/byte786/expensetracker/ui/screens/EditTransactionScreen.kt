@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.byte786.expensetracker.ui.screens

import android.app.TimePickerDialog
import android.net.Uri
import android.speech.RecognizerIntent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.byte786.expensetracker.ExpenseApp
import com.byte786.expensetracker.data.Account
import com.byte786.expensetracker.data.Photos
import com.byte786.expensetracker.data.Txn
import com.byte786.expensetracker.data.TxnType
import com.byte786.expensetracker.data.VoiceParser
import com.byte786.expensetracker.ui.Dates
import com.byte786.expensetracker.ui.Money
import com.byte786.expensetracker.ui.components.CalculatorSheet
import com.byte786.expensetracker.ui.components.CategoryIcon
import com.byte786.expensetracker.ui.components.CategorySheet
import com.byte786.expensetracker.ui.components.ConfirmDialog
import com.byte786.expensetracker.ui.components.CropDialog
import com.byte786.expensetracker.ui.components.DatePickDialog
import com.byte786.expensetracker.ui.components.SubcategorySheet
import com.byte786.expensetracker.ui.components.VoiceInput
import com.byte786.expensetracker.ui.components.amountColor
import com.byte786.expensetracker.ui.theme.LocalCurrency
import com.byte786.expensetracker.ui.theme.MoneyColors
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDateTime
import java.util.UUID

private const val CALC_NONE = -2
private const val CALC_TOTAL = -1

@Composable
fun EditTransactionScreen(nav: NavController, id: Long, initialType: Int) {
    val repo = ExpenseApp.instance.repo
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val cats by repo.categories.collectAsState()
    val accBalances by repo.accounts.collectAsState()
    val accs = accBalances.map { it.account }
    val currency = LocalCurrency.current

    var loaded by rememberSaveable { mutableStateOf(id == 0L) }
    var type by rememberSaveable { mutableIntStateOf(initialType) }
    var amount by rememberSaveable { mutableLongStateOf(0L) }
    var catId by rememberSaveable { mutableStateOf<Long?>(null) }
    var subId by rememberSaveable { mutableStateOf<Long?>(null) }
    // Sources: account of each part; srcAmt[0] is unused (the first source gets whatever is left).
    var srcAcc by rememberSaveable { mutableStateOf(longArrayOf()) }
    var srcAmt by rememberSaveable { mutableStateOf(longArrayOf()) }
    var toAccountId by rememberSaveable { mutableStateOf<Long?>(null) }
    var date by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }
    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var photo by rememberSaveable { mutableStateOf<String?>(null) }
    var originalPhoto by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingCamera by rememberSaveable { mutableStateOf<String?>(null) }
    var oldIds by rememberSaveable { mutableStateOf(longArrayOf()) }
    var groupId by rememberSaveable { mutableStateOf<Long?>(null) }
    var calcFor by rememberSaveable { mutableIntStateOf(if (id == 0L && repo.voiceDraft == null) CALC_TOTAL else CALC_NONE) }

    var pickDate by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var viewPhoto by remember { mutableStateOf(false) }
    var catSheet by remember { mutableStateOf(false) }
    var subSheet by remember { mutableStateOf(false) }

    fun applyDraft(d: VoiceParser.Draft) {
        type = d.type
        d.total?.let { amount = it }
        if (d.type != TxnType.TRANSFER) { catId = d.categoryId; subId = d.subcategoryId }
        if (d.sources.isNotEmpty()) {
            srcAcc = d.sources.map { it.first }.toLongArray()
            srcAmt = d.sources.map { it.second ?: 0L }.toLongArray()
        }
        d.toAccountId?.let { toAccountId = it }
        if (d.title.isNotBlank()) title = d.title
        if (description.isBlank()) description = "Said: \"${d.heard}\""
        if (d.daysAgo > 0) date = Dates.millis(Dates.local(date).minusDays(d.daysAgo.toLong()))
        calcFor = if (d.total == null) CALC_TOTAL else CALC_NONE
    }

    LaunchedEffect(id) {
        if (!loaded) {
            repo.dao.txn(id)?.let { t ->
                val parts = t.groupId?.let { repo.dao.group(it) }.orEmpty().ifEmpty { listOf(t) }
                type = t.type; amount = parts.sumOf { it.amount }
                catId = t.categoryId; subId = t.subcategoryId
                srcAcc = parts.map { it.accountId }.toLongArray(); srcAmt = parts.map { it.amount }.toLongArray()
                toAccountId = t.toAccountId; date = t.date
                title = t.memo; description = t.description.orEmpty()
                photo = t.photo; originalPhoto = t.photo
                oldIds = parts.map { it.id }.toLongArray(); groupId = t.groupId
            }
            loaded = true
        }
    }
    LaunchedEffect(Unit) {
        if (id == 0L) repo.voiceDraft?.let { repo.voiceDraft = null; applyDraft(it) }
    }
    LaunchedEffect(accs) {
        if (srcAcc.isEmpty() && accs.isNotEmpty()) { srcAcc = longArrayOf(accs.first().id); srcAmt = longArrayOf(0L) }
        if (toAccountId == null) toAccountId = accs.getOrNull(1)?.id
    }

    /** Leave without saving: throw away a photo that was added during this edit. */
    fun cancel() {
        if (photo != null && photo != originalPhoto) repo.photoFile(photo!!).delete()
        nav.popBackStack()
    }
    BackHandler { cancel() }

    val voiceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        val heard = r.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (!heard.isNullOrBlank()) {
            description = ""
            applyDraft(VoiceInput.parse(heard, cats, accs))
        }
    }
    // A picked or shot photo is shown in the crop screen first, then saved.
    var cropSource by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            val bmp = Photos.load(ctx, uri)
            if (bmp == null) snackbar.showSnackbar("Couldn't read that image") else cropSource = bmp
        }
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val tmp = pendingCamera?.let { File(ctx.cacheDir, "camera/$it") }
        pendingCamera = null
        if (ok && tmp != null && tmp.exists()) scope.launch {
            val bmp = Photos.load(ctx, Uri.fromFile(tmp))
            tmp.delete()
            if (bmp == null) snackbar.showSnackbar("Couldn't read the photo") else cropSource = bmp
        }
    }
    fun openCamera() {
        val dir = File(ctx.cacheDir, "camera").apply { mkdirs() }
        val name = "${UUID.randomUUID()}.jpg"
        pendingCamera = name
        val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".fileprovider", File(dir, name))
        runCatching { cameraLauncher.launch(uri) }.onFailure { scope.launch { snackbar.showSnackbar("No camera app found") } }
    }

    val split = type != TxnType.TRANSFER && srcAcc.size > 1
    val rest = amount - srcAmt.drop(1).sum()

    fun save() {
        val error = when {
            amount <= 0 -> "Enter an amount"
            srcAcc.isEmpty() -> "Add an account first (Accounts tab → Edit)"
            type == TxnType.TRANSFER && (toAccountId == null || toAccountId == srcAcc[0]) -> "Choose a different account to transfer to"
            split && srcAmt.drop(1).any { it <= 0 } -> "Enter an amount for each extra source"
            split && rest <= 0 -> "The extra sources add up to the whole amount or more"
            split && srcAcc.toSet().size != srcAcc.size -> "Each source must be a different account"
            else -> null
        }
        if (error != null) { scope.launch { snackbar.showSnackbar(error) }; return }
        val base = Txn(
            type = type, amount = amount, date = date, accountId = srcAcc[0],
            toAccountId = if (type == TxnType.TRANSFER) toAccountId else null,
            categoryId = if (type == TxnType.TRANSFER) null else catId,
            subcategoryId = if (type == TxnType.TRANSFER) null else subId,
            memo = title.trim(), photo = photo, description = description.trim().ifBlank { null },
        )
        val records = if (!split) {
            listOf(base.copy(id = if (oldIds.size == 1) oldIds[0] else 0L))
        } else {
            val g = groupId ?: System.currentTimeMillis()
            srcAcc.mapIndexed { i, a -> base.copy(amount = if (i == 0) rest else srcAmt[i], accountId = a, groupId = g) }
        }
        scope.launch {
            repo.saveRecords(oldIds.toList(), records, originalPhoto)
            nav.popBackStack()
        }
    }

    val cat = catId?.let { c -> cats.firstOrNull { it.id == c } }
    val sub = subId?.let { s -> cats.firstOrNull { it.id == s } }
    val ldt = Dates.local(date)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(if (id == 0L) "Add" else "Edit", fontWeight = FontWeight.Black) },
                navigationIcon = { IconButton(onClick = { cancel() }) { Icon(Icons.Filled.Close, "Cancel") } },
                actions = {
                    IconButton(onClick = { VoiceInput.launch(ctx, voiceLauncher) }) { Icon(Icons.Filled.Mic, "Fill by voice") }
                    if (id != 0L) IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, "Delete") }
                    IconButton(onClick = { save() }) { Icon(Icons.Filled.Check, "Save") }
                },
            )
        },
    ) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                listOf("Expense", "Income", "Transfer").forEachIndexed { i, label ->
                    SegmentedButton(
                        selected = type == i,
                        onClick = { if (type != i) { type = i; catId = null; subId = null } },
                        shape = SegmentedButtonDefaults.itemShape(i, 3),
                    ) { Text(label) }
                }
            }

            // Amount (opens the calculator)
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                    .clickable { calcFor = CALC_TOTAL }.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(if (split) "Total amount" else "Amount", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        if (amount > 0) Money.format(amount, currency) else "Tap to enter",
                        fontSize = 28.sp, fontWeight = FontWeight.Black,
                        color = if (amount > 0) amountColor(type) else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(Icons.Filled.Calculate, "Calculator", tint = MaterialTheme.colorScheme.primary)
            }

            if (type != TxnType.TRANSFER) {
                // Category and subcategory buttons
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PickerButton("Category", cat?.name ?: "Choose", Modifier.weight(1f), icon = cat?.let { it.icon to it.color }) { catSheet = true }
                    PickerButton(
                        "Subcategory", if (cat == null) "—" else sub?.name ?: "None", Modifier.weight(1f),
                        enabled = cat != null,
                    ) { subSheet = true }
                }

                // Sources
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (type == TxnType.INCOME) "Received in" else "Paid from", style = MaterialTheme.typography.titleSmall)
                    srcAcc.forEachIndexed { i, a ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AccountPicker(accs, a, Modifier.weight(1f)) { new -> srcAcc = srcAcc.copyOf().also { it[i] = new } }
                            if (split) {
                                Spacer(Modifier.width(8.dp))
                                if (i == 0) {
                                    Column(horizontalAlignment = Alignment.End, modifier = Modifier.width(110.dp)) {
                                        Text("the rest", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            Money.format(rest, currency), fontWeight = FontWeight.Black,
                                            color = if (rest > 0) MaterialTheme.colorScheme.onSurface else MoneyColors.expense,
                                        )
                                    }
                                    Spacer(Modifier.width(48.dp))
                                } else {
                                    OutlinedButton(onClick = { calcFor = i }, modifier = Modifier.width(110.dp)) {
                                        Text(if (srcAmt[i] > 0) Money.format(srcAmt[i], currency) else "Amount", maxLines = 1)
                                    }
                                    IconButton(onClick = {
                                        srcAcc = srcAcc.filterIndexed { k, _ -> k != i }.toLongArray()
                                        srcAmt = srcAmt.filterIndexed { k, _ -> k != i }.toLongArray()
                                    }) { Icon(Icons.Filled.Close, "Remove this source") }
                                }
                            }
                        }
                    }
                    val unused = accs.firstOrNull { it.id !in srcAcc }
                    if (unused != null) {
                        TextButton(onClick = {
                            srcAcc = srcAcc + unused.id
                            srcAmt = srcAmt + 0L
                            calcFor = srcAcc.size - 1
                        }) {
                            Icon(Icons.Filled.Add, null); Spacer(Modifier.width(6.dp))
                            Text(if (type == TxnType.INCOME) "Split into another account" else "Paid from another account too")
                        }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("From account", style = MaterialTheme.typography.titleSmall)
                    if (srcAcc.isNotEmpty()) AccountPicker(accs, srcAcc[0], Modifier.fillMaxWidth()) { new ->
                        srcAcc = longArrayOf(new); srcAmt = longArrayOf(0L)
                    }
                    Text("To account", style = MaterialTheme.typography.titleSmall)
                    AccountPicker(accs, toAccountId, Modifier.fillMaxWidth()) { toAccountId = it }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { pickDate = true }, modifier = Modifier.weight(1.4f)) {
                    Icon(Icons.Filled.Event, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp))
                    Text(Dates.fullDay(ldt.toLocalDate()), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                OutlinedButton(
                    onClick = {
                        TimePickerDialog(ctx, { _, h, m ->
                            date = Dates.millis(Dates.local(date).withHour(h).withMinute(m))
                        }, ldt.hour, ldt.minute, false).show()
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Filled.Schedule, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp))
                    Text(Dates.time(date), maxLines = 1)
                }
            }

            OutlinedTextField(
                value = title, onValueChange = { title = it },
                label = { Text("Title") }, singleLine = true,
                supportingText = { Text("Shown in lists") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = description, onValueChange = { description = it },
                label = { Text("Description") }, minLines = 3,
                supportingText = { Text("Only shown when you open this record") },
                modifier = Modifier.fillMaxWidth(),
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Photo (receipt, bill…)", style = MaterialTheme.typography.titleSmall)
                if (photo != null) {
                    Box {
                        AsyncImage(
                            model = repo.photoFile(photo!!), contentDescription = "Attached photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(12.dp)).clickable { viewPhoto = true },
                        )
                        IconButton(
                            onClick = {
                                if (photo != originalPhoto) repo.photoFile(photo!!).delete()
                                photo = null
                            },
                            modifier = Modifier.align(Alignment.TopEnd),
                        ) {
                            Box(Modifier.size(32.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.55f)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.Close, "Remove photo", tint = Color.White)
                            }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { openCamera() }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.CameraAlt, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Camera")
                    }
                    OutlinedButton(
                        onClick = { galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Filled.PhotoLibrary, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Gallery")
                    }
                }
            }

            Button(onClick = { save() }, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Save", fontSize = 16.sp) }
        }
    }

    cropSource?.let { src ->
        CropDialog(src, forceSquare = false, title = "Crop the photo", onDismiss = { cropSource = null }) { cropped ->
            cropSource = null
            scope.launch {
                val name = Photos.savePhoto(cropped, repo.photosDir)
                if (name == null) snackbar.showSnackbar("Couldn't save the photo") else {
                    if (photo != null && photo != originalPhoto) repo.photoFile(photo!!).delete()
                    photo = name
                }
            }
        }
    }
    if (calcFor != CALC_NONE) {
        val forTotal = calcFor == CALC_TOTAL
        val idx = calcFor
        CalculatorSheet(
            title = if (forTotal) "Amount" else "Amount from ${accs.firstOrNull { it.id == srcAcc.getOrNull(idx) }?.name ?: "this account"}",
            initialMinor = if (forTotal) amount else srcAmt.getOrElse(idx) { 0L },
            onDismiss = { calcFor = CALC_NONE },
        ) { v ->
            if (forTotal) amount = v else if (idx in srcAmt.indices) srcAmt = srcAmt.copyOf().also { it[idx] = v }
            calcFor = CALC_NONE
        }
    }
    if (catSheet) {
        CategorySheet(type, cats, catId, onDismiss = { catSheet = false }) { c ->
            if (catId != c.id) { catId = c.id; subId = null }
            catSheet = false
            if (cats.any { it.parentId == c.id }) subSheet = true
        }
    }
    if (subSheet && cat != null) {
        SubcategorySheet(cat, cats, subId, onDismiss = { subSheet = false }) { s -> subId = s; subSheet = false }
    }
    if (pickDate) {
        DatePickDialog(ldt.toLocalDate(), onDismiss = { pickDate = false }) { d ->
            date = Dates.millis(LocalDateTime.of(d, ldt.toLocalTime()))
            pickDate = false
        }
    }
    if (confirmDelete) {
        ConfirmDialog(
            "Delete this record?",
            if (oldIds.size > 1) "All ${oldIds.size} parts of this bill will be deleted. This cannot be undone." else "This cannot be undone.",
            onDismiss = { confirmDelete = false },
        ) {
            confirmDelete = false
            scope.launch {
                repo.dao.txn(id)?.let { repo.deleteTxn(it) }
                if (photo != null && photo != originalPhoto) repo.photoFile(photo!!).delete()
                nav.popBackStack()
            }
        }
    }
    if (viewPhoto && photo != null) {
        Dialog(onDismissRequest = { viewPhoto = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Box(Modifier.fillMaxSize().clickable { viewPhoto = false }, contentAlignment = Alignment.Center) {
                AsyncImage(model = repo.photoFile(photo!!), contentDescription = "Photo", contentScale = ContentScale.Fit, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun PickerButton(
    label: String,
    value: String,
    modifier: Modifier,
    icon: Pair<String, Long>? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Row(
        modifier.clip(RoundedCornerShape(14.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            CategoryIcon(icon.first, icon.second, size = 32.dp)
            Spacer(Modifier.width(8.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                value, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(Icons.Filled.ArrowDropDown, null)
    }
}

@Composable
private fun AccountPicker(accounts: List<Account>, selected: Long?, modifier: Modifier, onSelect: (Long) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val a = accounts.firstOrNull { it.id == selected }
    Box(modifier) {
        FilterChip(
            selected = true,
            onClick = { open = true },
            label = { Text(a?.name ?: "Choose account", fontWeight = FontWeight.Bold, maxLines = 1) },
            leadingIcon = a?.let { acc -> @Composable { CategoryIcon(acc.icon, acc.color, size = 22.dp) } },
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, null) },
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            accounts.forEach { acc ->
                DropdownMenuItem(
                    text = { Text(acc.name) },
                    leadingIcon = { CategoryIcon(acc.icon, acc.color, size = 24.dp) },
                    onClick = { onSelect(acc.id); open = false },
                )
            }
        }
    }
}
