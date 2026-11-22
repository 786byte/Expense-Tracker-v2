@file:OptIn(ExperimentalMaterial3Api::class)

package com.byte786.expensetracker.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
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
import androidx.compose.material3.TopAppBarDefaults
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
import com.byte786.expensetracker.data.Txn
import com.byte786.expensetracker.data.TxnType
import com.byte786.expensetracker.ui.Dates
import com.byte786.expensetracker.ui.Money
import com.byte786.expensetracker.ui.Routes
import com.byte786.expensetracker.ui.components.EmptyState
import com.byte786.expensetracker.ui.components.Lookup
import com.byte786.expensetracker.ui.components.PeriodBar
import com.byte786.expensetracker.ui.components.PeriodState
import com.byte786.expensetracker.ui.components.RecordSheet
import com.byte786.expensetracker.ui.components.rememberLookup
import com.byte786.expensetracker.ui.components.rememberPeriodState
import com.byte786.expensetracker.ui.theme.LocalCurrency
import com.byte786.expensetracker.ui.theme.MoneyColors
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

private const val BOTH = 2

private data class Slice(val name: String, val icon: String?, val color: Color, val value: Long)

@Composable
fun AnalysisScreen(nav: NavController, openDrawer: () -> Unit) {
    val repo = ExpenseApp.instance.repo
    val txns by repo.txns.collectAsState()
    val cats by repo.categories.collectAsState()
    val accs by repo.accounts.collectAsState()
    val lk = rememberLookup(cats, accs, txns)
    val currency = LocalCurrency.current

    var mode by rememberSaveable { mutableIntStateOf(TxnType.EXPENSE) } // EXPENSE, INCOME or BOTH
    val periodState = rememberPeriodState()
    var openId by remember { mutableStateOf<Long?>(null) }
    val range = periodState.range
    val period = remember(txns, range) { txns.filter { periodState.contains(it.date) } }
    val income = period.filter { it.type == TxnType.INCOME }.sumOf { it.amount }
    val expense = period.filter { it.type == TxnType.EXPENSE }.sumOf { it.amount }
    val incomeColor = MoneyColors.income
    val expenseColor = MoneyColors.expense

    val slices = if (mode == BOTH) {
        listOf(Slice("Income", null, incomeColor, income), Slice("Expense", null, expenseColor, expense)).filter { it.value > 0 }
    } else {
        period.filter { it.type == mode }.groupBy { it.categoryId }
            .map { (id, list) ->
                val c = id?.let { lk.cat[it] }
                Slice(c?.name ?: "Uncategorized", c?.icon ?: "category", Color(c?.color ?: 0xFF9E9E9E), list.sumOf { it.amount })
            }
            .sortedByDescending { it.value }
    }
    val total = slices.sumOf { it.value }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Analysis", fontWeight = FontWeight.Black) },
                navigationIcon = { IconButton(onClick = openDrawer) { Icon(Icons.Filled.Menu, "Menu") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "controls") {
                Column {
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        listOf(TxnType.EXPENSE to "Expense", TxnType.INCOME to "Income", BOTH to "Both").forEachIndexed { i, (v, label) ->
                            SegmentedButton(mode == v, { mode = v }, SegmentedButtonDefaults.itemShape(i, 3)) { Text(label) }
                        }
                    }
                    PeriodBar(periodState)
                }
            }

            item(key = "pie") {
                Panel {
                    if (total == 0L) {
                        EmptyState("Nothing to show for ${periodState.label}")
                    } else {
                        Box(Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.Center) {
                            Donut(slices.map { it.color to it.value }, Modifier.size(200.dp))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                val label = when (mode) { BOTH -> "Saved"; TxnType.EXPENSE -> "Spent"; else -> "Earned" }
                                val value = if (mode == BOTH) income - expense else total
                                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    Money.format(value, currency), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black,
                                    color = if (mode == TxnType.EXPENSE || (mode == BOTH && value < 0)) expenseColor else incomeColor,
                                )
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        slices.forEach { sl ->
                            Column(Modifier.padding(vertical = 6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(sl.color))
                                    Spacer(Modifier.width(8.dp))
                                    Text(sl.name, Modifier.weight(1f), fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        "%.1f%%".format(sl.value * 100.0 / total), style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        Money.format(sl.value, currency), Modifier.width(96.dp),
                                        fontWeight = FontWeight.Black, textAlign = TextAlign.End,
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { sl.value.toFloat() / total }, color = sl.color,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.fillMaxWidth(), drawStopIndicator = {},
                                )
                            }
                        }
                    }
                }
            }

            item(key = "trend") {
                Panel {
                    Text("Income vs expense by month", fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(12.dp))
                    TrendChart(
                        txns,
                        when (periodState.mode) {
                            PeriodState.MONTH -> periodState.month
                            PeriodState.RANGE -> YearMonth.from(Dates.date(periodState.until - 1))
                            else -> YearMonth.now()
                        },
                        incomeColor, expenseColor,
                    )
                }
            }

            item(key = "records") {
                val bills = lk.merged(txns)
                val bigExp = bills.filter { it.type == TxnType.EXPENSE }.maxByOrNull { it.amount }
                val bigInc = bills.filter { it.type == TxnType.INCOME }.maxByOrNull { it.amount }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    RecordCard("Biggest expense ever", bigExp, lk, expenseColor, Modifier.weight(1f)) { openId = it.id }
                    RecordCard("Biggest income ever", bigInc, lk, incomeColor, Modifier.weight(1f)) { openId = it.id }
                }
            }
        }
    }
    openId?.let { RecordSheet(it, nav) { openId = null } }
}

@Composable
private fun Panel(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
            .padding(16.dp),
        content = content,
    )
}

@Composable
private fun Donut(slices: List<Pair<Color, Long>>, modifier: Modifier) {
    val total = slices.sumOf { it.second }.toFloat()
    Canvas(modifier) {
        val stroke = size.minDimension * 0.17f
        val d = size.minDimension - stroke
        val topLeft = Offset((size.width - d) / 2, (size.height - d) / 2)
        var start = -90f
        slices.forEach { (color, value) ->
            val sweep = value / total * 360f
            drawArc(
                color, start, if (slices.size > 1) (sweep - 1.2f).coerceAtLeast(0.5f) else 360f, useCenter = false,
                topLeft = topLeft, size = Size(d, d), style = Stroke(width = stroke),
            )
            start += sweep
        }
    }
}

@Composable
private fun TrendChart(txns: List<Txn>, end: YearMonth, incomeColor: Color, expenseColor: Color) {
    val currency = LocalCurrency.current
    val months = (5 downTo 0).map { end.minusMonths(it.toLong()) }
    val data = months.map { m ->
        val (f, t) = Dates.monthRange(m)
        val inMonth = txns.filter { it.date >= f && it.date < t }
        Triple(m, inMonth.filter { it.type == TxnType.INCOME }.sumOf { it.amount }, inMonth.filter { it.type == TxnType.EXPENSE }.sumOf { it.amount })
    }
    val max = data.maxOf { maxOf(it.second, it.third) }.coerceAtLeast(1)
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
            Text(m.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
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

@Composable
private fun RecordCard(title: String, t: Txn?, lk: Lookup, color: Color, modifier: Modifier, onClick: (Txn) -> Unit) {
    val currency = LocalCurrency.current
    Column(
        modifier.clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            .clickable(enabled = t != null) { t?.let(onClick) }
            .padding(12.dp),
    ) {
        Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (t == null) {
            Text("—", style = MaterialTheme.typography.titleMedium)
        } else {
            Text(Money.format(t.amount, currency), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = color)
            Text(lk.title(t), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(Dates.shortDay(t.date), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
