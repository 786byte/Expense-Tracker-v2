@file:OptIn(ExperimentalMaterial3Api::class)

package com.byte786.expensetracker.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.byte786.expensetracker.ExpenseApp
import com.byte786.expensetracker.data.Category
import com.byte786.expensetracker.data.TxnType
import com.byte786.expensetracker.ui.Dates
import com.byte786.expensetracker.ui.Money
import com.byte786.expensetracker.ui.Routes
import com.byte786.expensetracker.ui.components.CategoryIcon
import com.byte786.expensetracker.ui.components.EmptyState
import com.byte786.expensetracker.ui.components.Lookup
import com.byte786.expensetracker.ui.components.MonthSwitcher
import com.byte786.expensetracker.ui.components.SectionTitle
import com.byte786.expensetracker.ui.theme.LocalCurrency
import com.byte786.expensetracker.ui.theme.MoneyColors
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

private data class SubRow(val sub: Category?, val sum: Long)
private data class CatRow(val catId: Long?, val cat: Category?, val sum: Long, val subs: List<SubRow>)

@Composable
fun ReportsScreen(nav: NavController) {
    val repo = ExpenseApp.instance.repo
    val txns by repo.txns.collectAsState()
    val cats by repo.categories.collectAsState()
    val accs by repo.accounts.collectAsState()
    val lk = remember(cats, accs) { Lookup(cats, accs.map { it.account }) }
    val currency = LocalCurrency.current

    var period by rememberSaveable { mutableIntStateOf(0) } // 0 month, 1 year, 2 all time
    var month by rememberSaveable { mutableStateOf(YearMonth.now()) }
    var year by rememberSaveable { mutableIntStateOf(YearMonth.now().year) }
    var type by rememberSaveable { mutableIntStateOf(TxnType.EXPENSE) }
    var expanded by remember { mutableStateOf(setOf<Long?>()) }

    val range: Pair<Long, Long> = when (period) {
        0 -> Dates.monthRange(month)
        1 -> Dates.yearRange(year)
        else -> -1L to -1L
    }
    val rows = remember(txns, lk, range, type) {
        txns.filter { it.type == type && (range.first < 0 || (it.date >= range.first && it.date < range.second)) }
            .groupBy { it.categoryId }
            .map { (catId, list) ->
                CatRow(
                    catId, catId?.let { lk.cat[it] }, list.sumOf { it.amount },
                    list.groupBy { it.subcategoryId }
                        .map { (subId, sl) -> SubRow(subId?.let { lk.cat[it] }, sl.sumOf { it.amount }) }
                        .sortedByDescending { it.sum },
                )
            }
            .sortedByDescending { it.sum }
    }
    val total = rows.sumOf { it.sum }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reports") },
                navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            )
        },
    ) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(bottom = 32.dp)) {
            item(key = "controls") {
                Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        listOf("Month", "Year", "All time").forEachIndexed { i, label ->
                            SegmentedButton(selected = period == i, onClick = { period = i }, shape = SegmentedButtonDefaults.itemShape(i, 3)) { Text(label) }
                        }
                    }
                    when (period) {
                        0 -> MonthSwitcher(month, { month = it })
                        1 -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { year-- }) { Icon(Icons.Filled.ChevronLeft, "Previous year") }
                            Text(year.toString(), style = MaterialTheme.typography.titleMedium)
                            IconButton(onClick = { year++ }) { Icon(Icons.Filled.ChevronRight, "Next year") }
                        }
                    }
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        SegmentedButton(selected = type == TxnType.EXPENSE, onClick = { type = TxnType.EXPENSE }, shape = SegmentedButtonDefaults.itemShape(0, 2)) { Text("Expense") }
                        SegmentedButton(selected = type == TxnType.INCOME, onClick = { type = TxnType.INCOME }, shape = SegmentedButtonDefaults.itemShape(1, 2)) { Text("Income") }
                    }
                }
            }

            item(key = "chart") {
                ElevatedCard(Modifier.fillMaxWidth().padding(16.dp)) {
                    if (total == 0L) {
                        EmptyState("No ${if (type == TxnType.EXPENSE) "expenses" else "income"} in this period")
                    } else {
                        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            Donut(rows.map { (it.cat?.color ?: 0xFF9E9E9E) to it.sum }, Modifier.size(200.dp))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Total", style = MaterialTheme.typography.labelMedium)
                                Text(
                                    Money.format(total, currency), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                                    color = if (type == TxnType.EXPENSE) MoneyColors.expense else MoneyColors.income,
                                )
                            }
                        }
                    }
                }
            }

            if (rows.isNotEmpty()) item(key = "bycat") { SectionTitle("By category  ·  tap to see subcategories") }
            items(rows, key = { "c-${it.catId}" }) { r ->
                val isOpen = r.catId in expanded
                Column {
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            expanded = if (isOpen) expanded - r.catId else expanded + r.catId
                        }.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CategoryIcon(r.cat?.icon ?: "category", r.cat?.color ?: 0xFF9E9E9E)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(r.cat?.name ?: "Uncategorized", Modifier.weight(1f), fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(Money.format(r.sum, currency), fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                LinearProgressIndicator(
                                    progress = { r.sum.toFloat() / total }, color = Color(r.cat?.color ?: 0xFF9E9E9E),
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    pct(r.sum, total), style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.width(48.dp), textAlign = TextAlign.End,
                                )
                            }
                        }
                        Icon(if (isOpen) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null)
                    }
                    AnimatedVisibility(isOpen) {
                        Column(Modifier.padding(start = 68.dp, end = 16.dp, bottom = 8.dp)) {
                            r.subs.forEach { s ->
                                Row(
                                    Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable {
                                        val cat = s.sub?.id ?: r.catId ?: -1L
                                        nav.navigate(Routes.search(cat = cat, from = range.first, to = range.second, type = type))
                                    }.padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    if (s.sub != null) CategoryIcon(s.sub.icon, s.sub.color, size = 24.dp)
                                    else Spacer(Modifier.size(24.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(s.sub?.name ?: "(no subcategory)", Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(pct(s.sum, r.sum) + "  ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(Money.format(s.sum, currency))
                                }
                            }
                        }
                    }
                }
            }

            item(key = "trend") {
                SectionTitle("Last 6 months")
                val end = if (period == 0) month else YearMonth.now()
                TrendChart(txns, end)
            }
        }
    }
}

private fun pct(part: Long, whole: Long) = if (whole == 0L) "0%" else "%.1f%%".format(part * 100.0 / whole)

@Composable
private fun Donut(slices: List<Pair<Long, Long>>, modifier: Modifier) {
    val total = slices.sumOf { it.second }.toFloat()
    Canvas(modifier) {
        val stroke = size.minDimension * 0.16f
        val d = size.minDimension - stroke
        val topLeft = Offset((size.width - d) / 2, (size.height - d) / 2)
        var start = -90f
        slices.forEach { (color, value) ->
            val sweep = value / total * 360f
            drawArc(
                Color(color), start, (sweep - 1f).coerceAtLeast(0.5f), useCenter = false,
                topLeft = topLeft, size = Size(d, d), style = Stroke(width = stroke),
            )
            start += sweep
        }
    }
}

@Composable
private fun TrendChart(txns: List<com.byte786.expensetracker.data.Txn>, end: YearMonth) {
    val currency = LocalCurrency.current
    val months = (5 downTo 0).map { end.minusMonths(it.toLong()) }
    val data = months.map { m ->
        val (f, t) = Dates.monthRange(m)
        val inMonth = txns.filter { it.date >= f && it.date < t }
        Triple(m, inMonth.filter { it.type == TxnType.INCOME }.sumOf { it.amount }, inMonth.filter { it.type == TxnType.EXPENSE }.sumOf { it.amount })
    }
    val max = data.maxOf { maxOf(it.second, it.third) }.coerceAtLeast(1)
    val incomeColor = MoneyColors.income
    val expenseColor = MoneyColors.expense
    ElevatedCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth().height(140.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
                data.forEach { (_, inc, exp) ->
                    Row(Modifier.fillMaxHeight(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Bar(inc.toFloat() / max, incomeColor)
                        Bar(exp.toFloat() / max, expenseColor)
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                data.forEach { (m, _, _) ->
                    Text(m.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()), style = MaterialTheme.typography.labelSmall)
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Legend(incomeColor, "Income"); Legend(expenseColor, "Expense")
            }
            val last = data.last()
            Text(
                "${Dates.month(last.first)}: saved ${Money.format(last.second - last.third, currency)}",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun Bar(fraction: Float, color: Color) {
    Box(
        Modifier.width(12.dp).fillMaxHeight(fraction.coerceIn(0.01f, 1f))
            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)).background(color)
    )
}

@Composable
private fun Legend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(color))
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}
