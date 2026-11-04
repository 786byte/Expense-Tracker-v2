@file:OptIn(ExperimentalMaterial3Api::class)

package com.byte786.expensetracker.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.byte786.expensetracker.ExpenseApp
import com.byte786.expensetracker.data.Txn
import com.byte786.expensetracker.data.TxnType
import com.byte786.expensetracker.ui.Dates
import com.byte786.expensetracker.ui.Money
import com.byte786.expensetracker.ui.Routes
import com.byte786.expensetracker.ui.components.CategoryIcon
import com.byte786.expensetracker.ui.components.EmptyState
import com.byte786.expensetracker.ui.components.Lookup
import com.byte786.expensetracker.ui.components.MonthSwitcher
import com.byte786.expensetracker.ui.components.SectionTitle
import com.byte786.expensetracker.ui.components.txnDayGroups
import com.byte786.expensetracker.ui.theme.LocalCurrency
import com.byte786.expensetracker.ui.theme.MoneyColors
import java.time.YearMonth

@Composable
fun HomeScreen(nav: NavController, openDrawer: () -> Unit) {
    val repo = ExpenseApp.instance.repo
    val txns by repo.txns.collectAsState()
    val cats by repo.categories.collectAsState()
    val accs by repo.accounts.collectAsState()
    val lk = remember(cats, accs) { Lookup(cats, accs.map { it.account }) }
    val currency = LocalCurrency.current

    var month by rememberSaveable { mutableStateOf(YearMonth.now()) }
    var topAllTime by rememberSaveable { mutableStateOf(false) }
    var showGuide by remember { mutableStateOf(!repo.prefs.guideDismissed) }

    val range = Dates.monthRange(month)
    val monthTx = remember(txns, month) { txns.filter { it.date >= range.first && it.date < range.second } }
    val income = monthTx.filter { it.type == TxnType.INCOME }.sumOf { it.amount }
    val expense = monthTx.filter { it.type == TxnType.EXPENSE }.sumOf { it.amount }
    val biggestExpense = remember(txns) { txns.filter { it.type == TxnType.EXPENSE }.maxByOrNull { it.amount } }
    val biggestIncome = remember(txns) { txns.filter { it.type == TxnType.INCOME }.maxByOrNull { it.amount } }
    val topCats = remember(txns, monthTx, topAllTime) {
        (if (topAllTime) txns else monthTx).filter { it.type == TxnType.EXPENSE }
            .groupBy { it.categoryId }
            .map { (id, list) -> id to list.sumOf { it.amount } }
            .sortedByDescending { it.second }
            .take(5)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Expense Tracker") },
                navigationIcon = { IconButton(onClick = openDrawer) { Icon(Icons.Filled.Menu, "Menu") } },
                actions = {
                    IconButton(onClick = { nav.navigate(Routes.search()) }) { Icon(Icons.Filled.Search, "Search") }
                    IconButton(onClick = { nav.navigate(Routes.REPORTS) }) { Icon(Icons.Filled.PieChart, "Reports") }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { nav.navigate(Routes.edit()) }) {
                Icon(Icons.Filled.Add, "Add expense, income or transfer")
            }
        },
    ) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(bottom = 96.dp)) {
            if (showGuide) item(key = "guide") {
                Card(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                ) {
                    Row(Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.MenuBook, null)
                        Spacer(Modifier.width(12.dp))
                        Text("New here? Read the quick guide.", Modifier.weight(1f))
                        TextButton(onClick = { nav.navigate(Routes.GUIDE) }) { Text("Open") }
                        IconButton(onClick = { showGuide = false; repo.prefs.guideDismissed = true }) {
                            Icon(Icons.Filled.Close, "Dismiss")
                        }
                    }
                }
            }

            item(key = "month") { MonthSwitcher(month, { month = it }) }

            item(key = "summary") {
                ElevatedCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Row(Modifier.padding(16.dp)) {
                        SummaryCell("Income", Money.format(income, currency), MoneyColors.income, Modifier.weight(1f)) {
                            val (f, t) = range; nav.navigate(Routes.search(from = f, to = t, type = TxnType.INCOME))
                        }
                        SummaryCell("Expense", Money.format(expense, currency), MoneyColors.expense, Modifier.weight(1f)) {
                            val (f, t) = range; nav.navigate(Routes.search(from = f, to = t, type = TxnType.EXPENSE))
                        }
                        val net = income - expense
                        SummaryCell(
                            "Balance", Money.format(net, currency),
                            if (net >= 0) MoneyColors.income else MoneyColors.expense, Modifier.weight(1f),
                        ) {}
                    }
                }
            }

            if (accs.isNotEmpty()) item(key = "accounts") {
                SectionTitle("Accounts")
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(accs, key = { it.account.id }) { a ->
                        OutlinedCard(onClick = { nav.navigate(Routes.ACCOUNTS) }) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                CategoryIcon(a.account.icon, a.account.color, size = 32.dp)
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(a.account.name, style = MaterialTheme.typography.labelMedium)
                                    Text(Money.format(a.balance, currency), fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }

            item(key = "top") {
                SectionTitle("Where your money goes") {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilterChip(selected = !topAllTime, onClick = { topAllTime = false }, label = { Text("Month") })
                        FilterChip(selected = topAllTime, onClick = { topAllTime = true }, label = { Text("All time") })
                    }
                }
                ElevatedCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    if (topCats.isEmpty()) {
                        EmptyState("No expenses yet")
                    } else {
                        val max = topCats.first().second.coerceAtLeast(1)
                        Column(Modifier.padding(vertical = 8.dp)) {
                            topCats.forEach { (catId, sum) ->
                                val c = catId?.let { lk.cat[it] }
                                Row(
                                    Modifier.fillMaxWidth().clickable {
                                        val (f, t) = if (topAllTime) -1L to -1L else range
                                        nav.navigate(Routes.search(cat = catId ?: -1, from = f, to = t, type = TxnType.EXPENSE))
                                    }.padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    CategoryIcon(c?.icon ?: "category", c?.color ?: 0xFF9E9E9E, size = 32.dp)
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Row {
                                            Text(c?.name ?: "Uncategorized", Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text(Money.format(sum, currency), fontWeight = FontWeight.SemiBold)
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        LinearProgressIndicator(
                                            progress = { sum.toFloat() / max },
                                            modifier = Modifier.fillMaxWidth(),
                                            color = Color(c?.color ?: 0xFF9E9E9E),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item(key = "records") {
                SectionTitle("All-time records")
                Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RecordCard("Biggest expense", biggestExpense, lk, Modifier.weight(1f)) { nav.navigate(Routes.edit(it.id)) }
                    RecordCard("Biggest income", biggestIncome, lk, Modifier.weight(1f)) { nav.navigate(Routes.edit(it.id)) }
                }
            }

            item(key = "list-title") { SectionTitle("${Dates.month(month)} transactions") }
            if (monthTx.isEmpty()) item(key = "empty") { EmptyState("Nothing recorded this month.\nTap + to add your first one.") }
            txnDayGroups(monthTx, lk) { nav.navigate(Routes.edit(it.id)) }
        }
    }
}

@Composable
private fun SummaryCell(label: String, value: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.clickable(onClick = onClick)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, color = color, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun RecordCard(title: String, t: Txn?, lk: Lookup, modifier: Modifier, onClick: (Txn) -> Unit) {
    val currency = LocalCurrency.current
    ElevatedCard(modifier.clickable(enabled = t != null) { t?.let(onClick) }) {
        Column(Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (t == null) {
                Text("—", style = MaterialTheme.typography.titleMedium)
            } else {
                Text(
                    Money.format(t.amount, currency), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                    color = if (t.type == TxnType.EXPENSE) MoneyColors.expense else MoneyColors.income,
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val (icon, color) = lk.icon(t)
                    CategoryIcon(icon, color, size = 20.dp)
                    Spacer(Modifier.width(6.dp))
                    Text(lk.title(t), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(Dates.shortDay(t.date), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.size(0.dp))
            }
        }
    }
}
