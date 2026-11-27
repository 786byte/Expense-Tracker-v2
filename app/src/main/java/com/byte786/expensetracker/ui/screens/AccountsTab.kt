@file:OptIn(ExperimentalMaterial3Api::class)

package com.byte786.expensetracker.ui.screens

import com.byte786.expensetracker.ui.components.sketchBorder

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import com.byte786.expensetracker.ui.components.RecordSheet
import com.byte786.expensetracker.ui.components.rememberLookup
import com.byte786.expensetracker.ui.components.rememberPeriodState
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.byte786.expensetracker.ExpenseApp
import com.byte786.expensetracker.data.Account
import com.byte786.expensetracker.data.Category
import com.byte786.expensetracker.data.Txn
import com.byte786.expensetracker.data.TxnType
import com.byte786.expensetracker.data.totalBalance
import com.byte786.expensetracker.ui.Dates
import com.byte786.expensetracker.ui.Money
import com.byte786.expensetracker.ui.Routes
import com.byte786.expensetracker.ui.components.CategoryIcon
import com.byte786.expensetracker.ui.components.ConfirmDialog
import com.byte786.expensetracker.ui.components.EmptyState
import com.byte786.expensetracker.ui.components.Lookup
import com.byte786.expensetracker.ui.components.PeriodBar
import com.byte786.expensetracker.ui.components.TxnRow
import com.byte786.expensetracker.ui.theme.LocalCurrency
import com.byte786.expensetracker.ui.theme.MoneyColors
import kotlinx.coroutines.launch
import java.time.YearMonth

private const val NO_SUB = -1L
private const val ACCOUNTS = 0
private const val CATEGORIES = 1
private const val SUBCATEGORIES = 2
private val MODE_NAMES = listOf("Accounts", "Categories", "Subcategories")
private val MODE_ICONS = listOf(Icons.Filled.AccountBalanceWallet, Icons.Filled.Category, Icons.AutoMirrored.Filled.List)

private fun records(n: Int) = if (n == 1) "1 record" else "$n records"

/**
 * Accounts tab. Tap the title to switch between Accounts (default), Categories and Subcategories.
 * Categories: list → one category (its subcategories) → one subcategory (all records).
 * Subcategories: flat list → one subcategory's records. Accounts: list → one account's records.
 * Tapping never edits; editing happens only after pressing Edit, and records open a summary first.
 */
@Composable
fun AccountsTab(nav: NavController, openDrawer: () -> Unit) {
    val repo = ExpenseApp.instance.repo
    val txns by repo.txns.collectAsState()
    val cats by repo.categories.collectAsState()
    val accs by repo.accounts.collectAsState()
    val lk = rememberLookup(cats, accs, txns)
    val currency = LocalCurrency.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var mode by rememberSaveable { mutableIntStateOf(ACCOUNTS) }
    var modeMenu by remember { mutableStateOf(false) }
    var editing by rememberSaveable { mutableStateOf(false) }
    val period = rememberPeriodState()
    var openId by remember { mutableStateOf<Long?>(null) }
    var view by rememberSaveable { mutableStateOf("list") } // list | cat | sub | acc
    var catId by rememberSaveable { mutableLongStateOf(0L) }
    var subId by rememberSaveable { mutableLongStateOf(NO_SUB) }
    var accId by rememberSaveable { mutableLongStateOf(0L) }

    var editCat by remember { mutableStateOf<Category?>(null) }
    var editAcc by remember { mutableStateOf<Account?>(null) }
    var deleteCat by remember { mutableStateOf<Pair<Category, Int>?>(null) }

    val inPeriod: (Txn) -> Boolean = { period.contains(it.date) }
    val periodName = period.label

    fun back() {
        view = when {
            view == "sub" && mode == CATEGORIES -> "cat"
            else -> "list"
        }
    }
    BackHandler(enabled = view != "list") { back() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    // Tap the title to switch what this tab lists.
                    Box {
                        Row(
                            Modifier.clip(RoundedCornerShape(12.dp)).clickable { modeMenu = true }.padding(horizontal = 6.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(MODE_NAMES[mode], fontWeight = FontWeight.Black)
                            Icon(Icons.Filled.ArrowDropDown, "Switch view")
                        }
                        DropdownMenu(expanded = modeMenu, onDismissRequest = { modeMenu = false }) {
                            MODE_NAMES.forEachIndexed { i, name ->
                                DropdownMenuItem(
                                    text = { Text(name, fontWeight = if (i == mode) FontWeight.Black else FontWeight.Normal) },
                                    leadingIcon = { Icon(MODE_ICONS[i], null) },
                                    trailingIcon = { if (i == mode) Icon(Icons.Filled.Check, null) },
                                    onClick = { mode = i; view = "list"; modeMenu = false },
                                )
                            }
                        }
                    }
                },
                navigationIcon = { IconButton(onClick = openDrawer) { Icon(Icons.Filled.Menu, "Menu") } },
                actions = {
                    if (editing) Button(onClick = { editing = false }) { Text("Done") }
                    else OutlinedButton(onClick = { editing = true }) { Text("Edit") }
                    Spacer(Modifier.width(8.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "controls") {
                PeriodBar(period)
            }

            when {
                // ---------- All categories ----------
                mode == CATEGORIES && view == "list" -> {
                    listOf(TxnType.EXPENSE to "Expense categories", TxnType.INCOME to "Income categories").forEach { (type, title) ->
                        item(key = "cats-$type") { Column {
                            Label(title)
                            Card {
                                val parents = cats.filter { it.parentId == null && it.type == type }
                                parents.forEach { c ->
                                    val tx = txns.filter { inPeriod(it) && it.categoryId == c.id }
                                    val subs = cats.filter { it.parentId == c.id }
                                    ItemRow(
                                        icon = c.icon, color = c.color, title = c.name,
                                        subtitle = if (subs.isEmpty()) "No subcategories" else subs.joinToString { it.name },
                                        count = records(lk.merged(tx).size),
                                        amount = Money.format(lk.ownCat(tx).filter { it.type == type }.sumOf { it.amount }, currency),
                                        amountColor = if (type == TxnType.EXPENSE) MoneyColors.expense else MoneyColors.income,
                                        editing = editing,
                                        onEdit = { editCat = c },
                                        onDelete = { scope.launch { deleteCat = c to repo.dao.categoryUsage(c.id) } },
                                    ) { catId = c.id; view = "cat" }
                                }
                                if (parents.isEmpty() && !editing) EmptyState("No categories yet. Press Edit to add one.")
                                if (editing) AddRow("Add category") {
                                    editCat = Category(name = "", type = type, icon = "category", color = 0xFFB85A12, sortOrder = parents.size)
                                }
                            }
                        } }
                    }
                }

                // ---------- Every subcategory in one flat list ----------
                mode == SUBCATEGORIES && view == "list" -> {
                    listOf(TxnType.EXPENSE to "Expense subcategories", TxnType.INCOME to "Income subcategories").forEach { (type, title) ->
                        item(key = "subs-$type") { Column {
                            Label(title)
                            Card {
                                val parents = cats.filter { it.parentId == null && it.type == type }
                                val subs = parents.flatMap { p -> cats.filter { it.parentId == p.id } }
                                if (subs.isEmpty()) EmptyState("No subcategories yet.")
                                subs.forEach { sc ->
                                    val parent = lk.cat[sc.parentId ?: 0L]
                                    val tx = txns.filter { inPeriod(it) && it.subcategoryId == sc.id }
                                    ItemRow(
                                        icon = sc.icon, color = sc.color, title = sc.name,
                                        subtitle = "in ${parent?.name ?: "?"}",
                                        count = records(lk.merged(tx).size),
                                        amount = Money.format(lk.ownCat(tx).filter { it.type == type }.sumOf { it.amount }, currency),
                                        amountColor = if (type == TxnType.EXPENSE) MoneyColors.expense else MoneyColors.income,
                                        editing = editing,
                                        onEdit = { editCat = sc },
                                        onDelete = { scope.launch { deleteCat = sc to repo.dao.categoryUsage(sc.id) } },
                                    ) { catId = sc.parentId ?: 0L; subId = sc.id; view = "sub" }
                                }
                            }
                        } }
                    }
                }

                // ---------- One category: its subcategories ----------
                view == "cat" -> {
                    val c = lk.cat[catId]
                    item(key = "cat") { Column {
                        if (c == null) {
                            LaunchedEffect(Unit) { view = "list" }
                            return@Column
                        }
                        val tx = txns.filter { inPeriod(it) && it.categoryId == c.id }
                        val subs = cats.filter { it.parentId == c.id }
                        BackRow("All categories") { back() }
                        Header(c.icon, c.color, c.name, "$periodName · spent ${Money.format(lk.ownCat(tx).filter { it.type == TxnType.EXPENSE }.sumOf { it.amount }, currency)} · earned ${Money.format(lk.ownCat(tx).filter { it.type == TxnType.INCOME }.sumOf { it.amount }, currency)}")
                        Label("Subcategories · tap one for details")
                        Card {
                            val rows = subs.map { it as Category? } + (if (tx.any { it.subcategoryId == null }) listOf(null) else emptyList())
                            if (rows.isEmpty()) EmptyState("No subcategories or records here yet.")
                            rows.forEach { s ->
                                val st = tx.filter { it.subcategoryId == s?.id }
                                SubRow(
                                    name = s?.name ?: "No subcategory", count = lk.merged(st).size,
                                    spent = Money.format(lk.ownCat(st).filter { it.type == TxnType.EXPENSE }.sumOf { it.amount }, currency),
                                    earned = Money.format(lk.ownCat(st).filter { it.type == TxnType.INCOME }.sumOf { it.amount }, currency),
                                    editing = editing && s != null,
                                    onEdit = { if (s != null) editCat = s },
                                ) { subId = s?.id ?: NO_SUB; view = "sub" }
                            }
                            if (editing) AddRow("Add subcategory") {
                                editCat = Category(name = "", type = c.type, parentId = c.id, icon = c.icon, color = c.color, sortOrder = subs.size)
                            }
                        }
                    } }
                }

                // ---------- Records of one subcategory / one account ----------
                view == "sub" || view == "acc" -> {
                    val isAcc = view == "acc"
                    val list = txns.filter {
                        inPeriod(it) && if (isAcc) it.accountId == accId || it.toAccountId == accId
                        else it.categoryId == catId && it.subcategoryId == (if (subId == NO_SUB) null else subId)
                    }.let { if (isAcc) it else lk.merged(it, forCategories = true) }
                    // Account view: split-bill parts show their own amount, so no merging there.
                    // Account view: everything through that account. Category view: only your own share.
                    val sums = if (isAcc) list else list.filter { lk.countsInCategories(it) }
                    item(key = "detail-head") { Column {
                        if (isAcc && lk.acc[accId]?.tracking == true) {
                            Text(
                                if (lk.acc[accId]?.countInCategories == true) "Tracking only: not in your total balance or month totals, but counted in categories."
                                else "Tracking only: not counted in your balance, totals or categories.",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
                            )
                        }
                        val title = if (isAcc) lk.acc[accId]?.name ?: "" else
                            (lk.cat[catId]?.name ?: "") + " › " + (if (subId == NO_SUB) "No subcategory" else lk.cat[subId]?.name ?: "")
                        BackRow(
                            when {
                                isAcc -> "All accounts"
                                mode == SUBCATEGORIES -> "All subcategories"
                                else -> lk.cat[catId]?.name ?: "Back"
                            }
                        ) { back() }
                        Card {
                            Column(Modifier.padding(14.dp)) {
                                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                                Text(periodName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(Modifier.padding(top = 10.dp)) {
                                    Stat("Records", list.size.toString(), MaterialTheme.colorScheme.onSurface, Modifier.weight(1f))
                                    Stat("Spent", Money.format(sums.filter { it.type == TxnType.EXPENSE }.sumOf { it.amount }, currency), MoneyColors.expense, Modifier.weight(1f))
                                    Stat("Earned", Money.format(sums.filter { it.type == TxnType.INCOME }.sumOf { it.amount }, currency), MoneyColors.income, Modifier.weight(1f))
                                }
                            }
                        }
                    } }
                    item(key = "detail-list") {
                        Card {
                            if (list.isEmpty()) EmptyState("No records in this period.")
                            list.forEachIndexed { i, t ->
                                if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                TxnRow(t, lk, showDate = true) { openId = t.id }
                            }
                        }
                    }
                }

                // ---------- All accounts ----------
                else -> {
                    item(key = "accounts") { Column {
                        val total = accs.totalBalance()
                        Card {
                            Column(Modifier.padding(14.dp)) {
                                Text("Total balance (your own accounts)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    Money.format(total, currency), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black,
                                    color = if (total < 0) MoneyColors.expense else MoneyColors.income,
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Card {
                            accs.forEach { a ->
                                val n = txns.count { inPeriod(it) && (it.accountId == a.account.id || it.toAccountId == a.account.id) }
                                if (a.account.tracking) {
                                    // Tracking only: show how much was paid through it in this period.
                                    val mine = txns.filter { inPeriod(it) && it.accountId == a.account.id }
                                    val paid = mine.filter { it.type == TxnType.EXPENSE }.sumOf { it.amount }
                                    val got = mine.filter { it.type == TxnType.INCOME }.sumOf { it.amount }
                                    ItemRow(
                                        icon = a.account.icon, color = a.account.color, title = a.account.name,
                                        subtitle = "Tracking only · received ${Money.format(got, currency)} · " + records(n),
                                        count = "spent · $periodName",
                                        amount = Money.format(paid, currency),
                                        amountColor = MaterialTheme.colorScheme.tertiary,
                                        editing = editing, onEdit = { editAcc = a.account }, onDelete = null,
                                    ) { accId = a.account.id; view = "acc" }
                                } else {
                                    ItemRow(
                                        icon = a.account.icon, color = a.account.color, title = a.account.name,
                                        subtitle = periodName,
                                        count = records(n),
                                        amount = Money.format(a.balance, currency),
                                        amountColor = if (a.balance < 0) MoneyColors.expense else MaterialTheme.colorScheme.onSurface,
                                        editing = editing, onEdit = { editAcc = a.account }, onDelete = null,
                                    ) { accId = a.account.id; view = "acc" }
                                }
                            }
                            if (editing) AddRow("Add account") { editAcc = Account(name = "", sortOrder = accs.size) }
                        }
                    } }
                }
            }
        }
    }

    editCat?.let { CategoryEditDialog(it, cats, onDismiss = { editCat = null }) }
    openId?.let { RecordSheet(it, nav) { openId = null } }
    editAcc?.let { AccountEditDialog(it, onDismiss = { editAcc = null }, onMessage = { msg -> scope.launch { snackbar.showSnackbar(msg) } }) }
    deleteCat?.let { (c, used) ->
        val subsNote = if (c.parentId == null) " Its subcategories will be deleted too." else ""
        ConfirmDialog(
            "Delete ${c.name}?",
            (if (used > 0) "$used record(s) use it; they will become \"Uncategorized\"." else "No records use it.") + subsNote,
            onDismiss = { deleteCat = null },
        ) {
            repo.launch { repo.deleteCategory(c) }
            deleteCat = null
        }
    }
}

@Composable
private fun Card(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .sketchBorder(RoundedCornerShape(16.dp), 16.dp),
        content = content,
    )
}

@Composable
private fun Label(text: String) {
    Text(
        text, Modifier.padding(start = 4.dp, top = 4.dp, bottom = 6.dp),
        style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun BackRow(label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, contentPadding = PaddingValues(start = 0.dp, end = 12.dp)) {
        Icon(Icons.Filled.ChevronLeft, null)
        Text(label, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun Header(icon: String, color: Long, title: String, summary: String) {
    Card {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            CategoryIcon(icon, color, size = 48.dp)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun Stat(label: String, value: String, color: Color, modifier: Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Black, color = color, maxLines = 1)
    }
}

@Composable
private fun ItemRow(
    icon: String,
    color: Long,
    title: String,
    subtitle: String,
    count: String? = null,
    amount: String,
    amountColor: Color,
    editing: Boolean,
    onEdit: () -> Unit,
    onDelete: (() -> Unit)?,
    onOpen: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier.weight(1f).clickable(onClick = onOpen).padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CategoryIcon(icon, color)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(amount, fontWeight = FontWeight.Black, color = amountColor, maxLines = 1)
                if (count != null) Text(count, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
        if (editing) {
            IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, "Edit $title") }
            if (onDelete != null) IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, "Delete $title", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun SubRow(name: String, count: Int, spent: String, earned: String, editing: Boolean, onEdit: () -> Unit, onOpen: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier.weight(1f).clickable(onClick = onOpen).padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(name, fontWeight = FontWeight.Bold)
                Text("$count records", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Spent $spent", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MoneyColors.expense)
                Text("Earned $earned", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MoneyColors.income)
            }
        }
        if (editing) IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, "Edit $name") }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun AddRow(label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth().height(52.dp)) {
        Icon(Icons.Filled.Add, null)
        Spacer(Modifier.width(6.dp))
        Text(label, fontWeight = FontWeight.Bold)
    }
}
