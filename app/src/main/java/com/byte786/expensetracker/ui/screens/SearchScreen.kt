@file:OptIn(ExperimentalMaterial3Api::class)

package com.byte786.expensetracker.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.byte786.expensetracker.ExpenseApp
import com.byte786.expensetracker.data.Category
import com.byte786.expensetracker.data.TxnType
import com.byte786.expensetracker.ui.Dates
import com.byte786.expensetracker.ui.Money
import com.byte786.expensetracker.ui.Routes
import com.byte786.expensetracker.ui.components.CategoryIcon
import com.byte786.expensetracker.ui.components.DatePickDialog
import com.byte786.expensetracker.ui.components.DateRangeDialog
import com.byte786.expensetracker.ui.components.RecordSheet
import com.byte786.expensetracker.ui.components.rememberLookup
import com.byte786.expensetracker.ui.components.EmptyState
import com.byte786.expensetracker.ui.components.Lookup
import com.byte786.expensetracker.ui.components.MonthPickerDialog
import com.byte786.expensetracker.ui.components.txnDayGroups
import com.byte786.expensetracker.ui.theme.LocalCurrency
import com.byte786.expensetracker.ui.theme.MoneyColors
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun SearchScreen(
    nav: NavController,
    initialCat: Long,
    initialFrom: Long,
    initialTo: Long,
    initialType: Int,
    initialQuery: String,
) {
    val repo = ExpenseApp.instance.repo
    val txns by repo.txns.collectAsState()
    val cats by repo.categories.collectAsState()
    val accs by repo.accounts.collectAsState()
    val lk = rememberLookup(cats, accs, txns)
    var openId by remember { mutableStateOf<Long?>(null) }
    val currency = LocalCurrency.current

    var query by rememberSaveable { mutableStateOf(initialQuery) }
    var type by rememberSaveable { mutableIntStateOf(initialType) }
    var from by rememberSaveable { mutableLongStateOf(initialFrom) }
    var to by rememberSaveable { mutableLongStateOf(initialTo) }
    var catId by rememberSaveable { mutableLongStateOf(initialCat) }

    var dateMenu by remember { mutableStateOf(false) }
    var pickDay by remember { mutableStateOf(false) }
    var pickMonth by remember { mutableStateOf(false) }
    var pickRange by remember { mutableStateOf(false) }
    var pickCat by remember { mutableStateOf(false) }

    val selectedCat = lk.cat[catId]
    val results = remember(txns, lk, query, type, from, to, catId) {
        val q = query.trim().lowercase()
        txns.filter { t ->
            (type < 0 || t.type == type) &&
                (from < 0 || (t.date >= from && t.date < to)) &&
                (selectedCat == null || (if (selectedCat.parentId == null) t.categoryId == catId else t.subcategoryId == catId)) &&
                (q.isEmpty() || lk.searchable(t).lowercase().contains(q))
        }.let { lk.merged(it) }
    }
    val totalExpense = results.filter { it.type == TxnType.EXPENSE && lk.counts(it) }.sumOf { it.amount }
    val totalIncome = results.filter { it.type == TxnType.INCOME && lk.counts(it) }.sumOf { it.amount }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search") },
                navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    TextButton(onClick = { query = ""; type = -1; from = -1; to = -1; catId = -1 }) { Text("Clear all") }
                },
            )
        },
    ) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(bottom = 32.dp)) {
            item(key = "filters") {
                Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = query, onValueChange = { query = it },
                        placeholder = { Text("Search memo, category, account, amount") },
                        leadingIcon = { Icon(Icons.Filled.Search, null) },
                        trailingIcon = {
                            if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Filled.Clear, "Clear") }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(-1 to "All", TxnType.EXPENSE to "Expense", TxnType.INCOME to "Income", TxnType.TRANSFER to "Transfer")
                            .forEach { (v, label) ->
                                FilterChip(selected = type == v, onClick = { type = v }, label = { Text(label) })
                            }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box {
                            FilterChip(
                                selected = from >= 0,
                                onClick = { dateMenu = true },
                                label = { Text(Dates.describeRange(from, to)) },
                                trailingIcon = { Icon(Icons.Filled.ArrowDropDown, null) },
                            )
                            DropdownMenu(expanded = dateMenu, onDismissRequest = { dateMenu = false }) {
                                fun set(r: Pair<Long, Long>?) { from = r?.first ?: -1; to = r?.second ?: -1; dateMenu = false }
                                DropdownMenuItem(text = { Text("All time") }, onClick = { set(null) })
                                DropdownMenuItem(text = { Text("Today") }, onClick = { set(Dates.dayRange(LocalDate.now())) })
                                DropdownMenuItem(text = { Text("This month") }, onClick = { set(Dates.monthRange(YearMonth.now())) })
                                DropdownMenuItem(text = { Text("Last month") }, onClick = { set(Dates.monthRange(YearMonth.now().minusMonths(1))) })
                                DropdownMenuItem(text = { Text("This year") }, onClick = { set(Dates.yearRange(LocalDate.now().year)) })
                                HorizontalDivider()
                                DropdownMenuItem(text = { Text("Pick a date…") }, onClick = { dateMenu = false; pickDay = true })
                                DropdownMenuItem(text = { Text("Pick a month…") }, onClick = { dateMenu = false; pickMonth = true })
                                DropdownMenuItem(text = { Text("Custom range…") }, onClick = { dateMenu = false; pickRange = true })
                            }
                        }
                        FilterChip(
                            selected = selectedCat != null,
                            onClick = { pickCat = true },
                            label = { Text(selectedCat?.let { c -> c.parentId?.let { "${lk.cat[it]?.name} › ${c.name}" } ?: c.name } ?: "Any category") },
                            leadingIcon = selectedCat?.let { c -> @Composable { CategoryIcon(c.icon, c.color, size = 20.dp) } },
                            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, null) },
                        )
                    }
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(12.dp)) {
                            Column(Modifier.weight(1f)) {
                                Text("Records", style = MaterialTheme.typography.labelMedium)
                                Text(results.size.toString(), fontWeight = FontWeight.Bold)
                            }
                            Column(Modifier.weight(1f)) {
                                Text("Expense", style = MaterialTheme.typography.labelMedium)
                                Text(Money.format(totalExpense, currency), fontWeight = FontWeight.Bold, color = MoneyColors.expense)
                            }
                            Column(Modifier.weight(1f)) {
                                Text("Income", style = MaterialTheme.typography.labelMedium)
                                Text(Money.format(totalIncome, currency), fontWeight = FontWeight.Bold, color = MoneyColors.income)
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }
            if (results.isEmpty()) item(key = "empty") { EmptyState("No matching records") }
            txnDayGroups(results, lk) { openId = it.id }
        }
    }

    if (pickDay) {
        DatePickDialog(if (from >= 0) Dates.date(from) else LocalDate.now(), onDismiss = { pickDay = false }) { d ->
            val (f, t) = Dates.dayRange(d); from = f; to = t; pickDay = false
        }
    }
    if (pickMonth) {
        MonthPickerDialog(if (from >= 0) YearMonth.from(Dates.date(from)) else YearMonth.now(), onDismiss = { pickMonth = false }) { m ->
            val (f, t) = Dates.monthRange(m); from = f; to = t; pickMonth = false
        }
    }
    if (pickRange) {
        DateRangeDialog(onDismiss = { pickRange = false }) { f, t -> from = f; to = t; pickRange = false }
    }
    openId?.let { RecordSheet(it, nav) { openId = null } }
    if (pickCat) {
        CategoryPickerDialog(cats, onDismiss = { pickCat = false }) { catId = it; pickCat = false }
    }
}

@Composable
fun CategoryPickerDialog(cats: List<Category>, onDismiss: () -> Unit, onPick: (Long) -> Unit) {
    var tab by remember { mutableIntStateOf(TxnType.EXPENSE) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("Category") },
        text = {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = tab == TxnType.EXPENSE, onClick = { tab = TxnType.EXPENSE }, label = { Text("Expense") })
                    FilterChip(selected = tab == TxnType.INCOME, onClick = { tab = TxnType.INCOME }, label = { Text("Income") })
                }
                val parents = cats.filter { it.parentId == null && it.type == tab }
                LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    item {
                        Text(
                            "Any category", Modifier.fillMaxWidth().clickable { onPick(-1) }.padding(vertical = 12.dp),
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    parents.forEach { p ->
                        item(key = p.id) { CatLine(p, indent = false) { onPick(p.id) } }
                        items(cats.filter { it.parentId == p.id }, key = { it.id }) { s -> CatLine(s, indent = true) { onPick(s.id) } }
                    }
                }
            }
        },
    )
}

@Composable
private fun CatLine(c: Category, indent: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(start = if (indent) 32.dp else 0.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryIcon(c.icon, c.color, size = if (indent) 24.dp else 32.dp)
        Spacer(Modifier.width(12.dp))
        Text(c.name, fontWeight = if (indent) FontWeight.Normal else FontWeight.Medium)
    }
}
