@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.byte786.expensetracker.ui.screens

import android.app.TimePickerDialog
import android.net.Uri
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
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.byte786.expensetracker.ExpenseApp
import com.byte786.expensetracker.data.Category
import com.byte786.expensetracker.data.Photos
import com.byte786.expensetracker.data.Txn
import com.byte786.expensetracker.data.TxnType
import com.byte786.expensetracker.ui.Dates
import com.byte786.expensetracker.ui.Money
import com.byte786.expensetracker.ui.Routes
import com.byte786.expensetracker.ui.components.CategoryIcon
import com.byte786.expensetracker.ui.components.ConfirmDialog
import com.byte786.expensetracker.ui.components.DatePickDialog
import com.byte786.expensetracker.ui.components.amountColor
import com.byte786.expensetracker.ui.theme.LocalCurrency
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDateTime
import java.util.UUID

@Composable
fun EditTransactionScreen(nav: NavController, id: Long, initialType: Int) {
    val repo = ExpenseApp.instance.repo
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val cats by repo.categories.collectAsState()
    val accs by repo.accounts.collectAsState()
    val currency = LocalCurrency.current

    var loaded by rememberSaveable { mutableStateOf(id == 0L) }
    var type by rememberSaveable { mutableIntStateOf(initialType) }
    var amountText by rememberSaveable { mutableStateOf("") }
    var catId by rememberSaveable { mutableStateOf<Long?>(null) }
    var subId by rememberSaveable { mutableStateOf<Long?>(null) }
    var accountId by rememberSaveable { mutableStateOf<Long?>(null) }
    var toAccountId by rememberSaveable { mutableStateOf<Long?>(null) }
    var date by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }
    var memo by rememberSaveable { mutableStateOf("") }
    var photo by rememberSaveable { mutableStateOf<String?>(null) }
    var originalPhoto by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingCamera by rememberSaveable { mutableStateOf<String?>(null) }

    var pickDate by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var viewPhoto by remember { mutableStateOf(false) }
    var addSubFor by remember { mutableStateOf<Category?>(null) }

    LaunchedEffect(id) {
        if (!loaded) {
            repo.dao.txn(id)?.let { t ->
                type = t.type; amountText = Money.toInput(t.amount); catId = t.categoryId; subId = t.subcategoryId
                accountId = t.accountId; toAccountId = t.toAccountId; date = t.date; memo = t.memo
                photo = t.photo; originalPhoto = t.photo
            }
            loaded = true
        }
    }
    LaunchedEffect(accs) {
        if (accountId == null) accountId = accs.firstOrNull()?.account?.id
        if (toAccountId == null) toAccountId = accs.getOrNull(1)?.account?.id
    }

    /** Leave without saving: throw away a photo that was added during this edit. */
    fun cancel() {
        if (photo != null && photo != originalPhoto) repo.photoFile(photo!!).delete()
        nav.popBackStack()
    }
    BackHandler { cancel() }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            val name = Photos.import(ctx, uri, repo.photosDir)
            if (name == null) snackbar.showSnackbar("Couldn't read that image") else {
                if (photo != null && photo != originalPhoto) repo.photoFile(photo!!).delete()
                photo = name
            }
        }
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val tmp = pendingCamera?.let { File(ctx.cacheDir, "camera/$it") }
        pendingCamera = null
        if (ok && tmp != null && tmp.exists()) scope.launch {
            val name = Photos.import(ctx, Uri.fromFile(tmp), repo.photosDir)
            tmp.delete()
            if (name != null) {
                if (photo != null && photo != originalPhoto) repo.photoFile(photo!!).delete()
                photo = name
            }
        }
    }
    fun openCamera() {
        val dir = File(ctx.cacheDir, "camera").apply { mkdirs() }
        val name = "${UUID.randomUUID()}.jpg"
        val file = File(dir, name)
        pendingCamera = name
        val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".fileprovider", file)
        runCatching { cameraLauncher.launch(uri) }.onFailure {
            scope.launch { snackbar.showSnackbar("No camera app found") }
        }
    }

    fun save() {
        val amount = Money.parse(amountText)
        val acc = accountId
        val error = when {
            amount == null || amount <= 0 -> "Enter an amount"
            acc == null -> "Add an account first (menu → Accounts)"
            type == TxnType.TRANSFER && (toAccountId == null || toAccountId == acc) -> "Choose a different account to transfer to"
            else -> null
        }
        if (error != null) {
            scope.launch { snackbar.showSnackbar(error) }
            return
        }
        val t = Txn(
            id = id, type = type, amount = amount!!, date = date, accountId = acc!!,
            toAccountId = if (type == TxnType.TRANSFER) toAccountId else null,
            categoryId = if (type == TxnType.TRANSFER) null else catId,
            subcategoryId = if (type == TxnType.TRANSFER) null else subId,
            memo = memo.trim(), photo = photo,
        )
        scope.launch {
            repo.saveTxn(t)
            if (originalPhoto != null && originalPhoto != photo) repo.photoFile(originalPhoto!!).delete()
            nav.popBackStack()
        }
    }

    val parents = cats.filter { it.parentId == null && it.type == type }
    val subs = catId?.let { p -> cats.filter { it.parentId == p } } ?: emptyList()
    val ldt = Dates.local(date)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(if (id == 0L) "Add" else "Edit") },
                navigationIcon = { IconButton(onClick = { cancel() }) { Icon(Icons.Filled.Close, "Cancel") } },
                actions = {
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
                        onClick = {
                            if (type != i) { type = i; catId = null; subId = null }
                        },
                        shape = SegmentedButtonDefaults.itemShape(i, 3),
                    ) { Text(label) }
                }
            }

            OutlinedTextField(
                value = amountText,
                onValueChange = { v -> if (v.all { it.isDigit() || it == '.' || it == ',' }) amountText = v },
                label = { Text("Amount") },
                prefix = { Text(currency) },
                singleLine = true,
                textStyle = MaterialTheme.typography.headlineMedium.copy(color = amountColor(type), fontWeight = FontWeight.SemiBold),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )

            if (type == TxnType.TRANSFER) {
                AccountChooser("From account", accs.map { it.account }, accountId) { accountId = it }
                AccountChooser("To account", accs.map { it.account }, toAccountId) { toAccountId = it }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Category", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        TextButton(onClick = { nav.navigate(Routes.CATEGORIES) }) { Text("Manage") }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        parents.forEach { c ->
                            CategoryTile(c, selected = c.id == catId) {
                                if (catId != c.id) { catId = c.id; subId = null }
                            }
                        }
                    }
                }
                val parent = catId?.let { p -> cats.firstOrNull { it.id == p } }
                if (parent != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Subcategory of ${parent.name}", style = MaterialTheme.typography.titleSmall)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = subId == null, onClick = { subId = null }, label = { Text("None") })
                            subs.forEach { s ->
                                FilterChip(
                                    selected = subId == s.id,
                                    onClick = { subId = s.id },
                                    label = { Text(s.name) },
                                    leadingIcon = { CategoryIcon(s.icon, s.color, size = 20.dp) },
                                )
                            }
                            FilterChip(
                                selected = false, onClick = { addSubFor = parent },
                                label = { Text("New") }, leadingIcon = { Icon(Icons.Filled.Add, null, Modifier.size(18.dp)) },
                            )
                        }
                    }
                }
                AccountChooser(if (type == TxnType.INCOME) "Received in" else "Paid from", accs.map { it.account }, accountId) { accountId = it }
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
                value = memo, onValueChange = { memo = it },
                label = { Text("Memo / note") },
                minLines = 2,
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

            Button(onClick = { save() }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text("Save", fontSize = 16.sp)
            }
        }
    }

    if (pickDate) {
        DatePickDialog(ldt.toLocalDate(), onDismiss = { pickDate = false }) { d ->
            date = Dates.millis(LocalDateTime.of(d, ldt.toLocalTime()))
            pickDate = false
        }
    }
    if (confirmDelete) {
        ConfirmDialog("Delete this record?", "This cannot be undone.", onDismiss = { confirmDelete = false }) {
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
    addSubFor?.let { parent ->
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { addSubFor = null },
            title = { Text("New subcategory in ${parent.name}") },
            text = { OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true) },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isNotBlank()) scope.launch {
                        val newId = repo.saveCategory(
                            Category(name = name.trim(), type = parent.type, parentId = parent.id, icon = parent.icon, color = parent.color, sortOrder = 999)
                        )
                        subId = newId
                    }
                    addSubFor = null
                }) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { addSubFor = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun CategoryTile(c: Category, selected: Boolean, onClick: () -> Unit) {
    Column(
        Modifier.width(76.dp).clip(RoundedCornerShape(12.dp))
            .then(if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)) else Modifier)
            .clickable(onClick = onClick).padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CategoryIcon(c.icon, c.color, size = 44.dp)
        Spacer(Modifier.height(4.dp))
        Text(
            c.name, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center,
            maxLines = 2, overflow = TextOverflow.Ellipsis,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

@Composable
private fun AccountChooser(
    label: String,
    accounts: List<com.byte786.expensetracker.data.Account>,
    selected: Long?,
    onSelect: (Long) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            accounts.forEach { a ->
                FilterChip(
                    selected = a.id == selected,
                    onClick = { onSelect(a.id) },
                    label = { Text(a.name) },
                    leadingIcon = { CategoryIcon(a.icon, a.color, size = 20.dp) },
                )
            }
        }
    }
}
