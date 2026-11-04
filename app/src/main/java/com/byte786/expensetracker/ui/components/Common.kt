@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.byte786.expensetracker.ui.components

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.byte786.expensetracker.data.Account
import com.byte786.expensetracker.data.Category
import com.byte786.expensetracker.data.Txn
import com.byte786.expensetracker.data.TxnType
import com.byte786.expensetracker.ui.AppIcons
import com.byte786.expensetracker.ui.Dates
import com.byte786.expensetracker.ui.Money
import com.byte786.expensetracker.ui.theme.LocalCurrency
import com.byte786.expensetracker.ui.theme.MoneyColors
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/** Fast lookups of categories/accounts by id, plus display helpers for a transaction. */
class Lookup(cats: List<Category>, accs: List<Account>) {
    val cat: Map<Long, Category> = cats.associateBy { it.id }
    val acc: Map<Long, Account> = accs.associateBy { it.id }

    fun title(t: Txn): String {
        if (t.type == TxnType.TRANSFER) return "Transfer"
        val c = t.categoryId?.let { cat[it] } ?: return "Uncategorized"
        val s = t.subcategoryId?.let { cat[it] } ?: return c.name
        return "${c.name} › ${s.name}"
    }

    fun accountText(t: Txn): String {
        val from = acc[t.accountId]?.name ?: "?"
        return if (t.type == TxnType.TRANSFER) "$from → ${t.toAccountId?.let { acc[it]?.name } ?: "?"}" else from
    }

    fun icon(t: Txn): Pair<String, Long> {
        if (t.type == TxnType.TRANSFER) return "transfer" to 0xFF546E7A
        val c = t.subcategoryId?.let { cat[it] } ?: t.categoryId?.let { cat[it] }
        return (c?.icon ?: "category") to (c?.color ?: 0xFF9E9E9E)
    }

    /** Text used by search. */
    fun searchable(t: Txn): String = listOf(title(t), accountText(t), t.memo, Money.toInput(t.amount)).joinToString(" ")
}

@Composable
fun CategoryIcon(icon: String, color: Long, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    Box(
        modifier.size(size).clip(CircleShape).background(Color(color)),
        contentAlignment = Alignment.Center,
    ) {
        val vector = if (icon == "transfer") Icons.Filled.SwapHoriz else AppIcons.get(icon)
        Icon(vector, null, tint = Color.White, modifier = Modifier.size(size * 0.55f))
    }
}

@Composable
fun amountColor(type: Int): Color = when (type) {
    TxnType.EXPENSE -> MoneyColors.expense
    TxnType.INCOME -> MoneyColors.income
    else -> MoneyColors.transfer
}

fun signFor(type: Int) = when (type) { TxnType.EXPENSE -> "-"; TxnType.INCOME -> "+"; else -> "" }

@Composable
fun TxnRow(t: Txn, lk: Lookup, showDate: Boolean = false, onClick: () -> Unit) {
    val currency = LocalCurrency.current
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val (icon, color) = lk.icon(t)
        CategoryIcon(icon, color)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                lk.title(t), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            val sub = lk.accountText(t) + " · " + (if (showDate) Dates.shortDay(t.date) + " " else "") + Dates.time(t.date)
            Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            if (t.memo.isNotBlank()) {
                Text(
                    t.memo, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (t.photo != null) {
            Icon(Icons.Filled.Image, "Has photo", Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(6.dp))
        }
        Text(
            Money.format(t.amount, currency, signFor(t.type)),
            color = amountColor(t.type), fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

/** Adds transactions to a LazyColumn, grouped under a header per day. [txns] must be sorted newest first. */
fun LazyListScope.txnDayGroups(txns: List<Txn>, lk: Lookup, onClick: (Txn) -> Unit) {
    txns.groupBy { Dates.date(it.date) }.forEach { (day, list) ->
        item(key = "day-$day") { DayHeader(day, list) }
        items(list, key = { "t-${it.id}" }) { t -> TxnRow(t, lk) { onClick(t) } }
    }
}

@Composable
private fun DayHeader(day: LocalDate, list: List<Txn>) {
    val currency = LocalCurrency.current
    val net = list.sumOf { when (it.type) { TxnType.INCOME -> it.amount; TxnType.EXPENSE -> -it.amount; else -> 0L } }
    Row(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(Dates.dayLabel(day), style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
        Text(
            Money.format(net, currency, if (net > 0) "+" else if (net < 0) "-" else ""),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 16.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        action?.invoke()
    }
}

@Composable
fun EmptyState(text: String) {
    Text(
        text, modifier = Modifier.fillMaxWidth().padding(32.dp), textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
fun MonthSwitcher(month: YearMonth, onChange: (YearMonth) -> Unit, modifier: Modifier = Modifier) {
    var pick by remember { mutableStateOf(false) }
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        IconButton(onClick = { onChange(month.minusMonths(1)) }) { Icon(Icons.Filled.ChevronLeft, "Previous month") }
        TextButton(onClick = { pick = true }) {
            Text(Dates.month(month), style = MaterialTheme.typography.titleMedium)
        }
        IconButton(onClick = { onChange(month.plusMonths(1)) }) { Icon(Icons.Filled.ChevronRight, "Next month") }
    }
    if (pick) MonthPickerDialog(month, onDismiss = { pick = false }) { onChange(it); pick = false }
}

@Composable
fun MonthPickerDialog(initial: YearMonth, onDismiss: () -> Unit, onPick: (YearMonth) -> Unit) {
    var year by remember { mutableIntStateOf(initial.year) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { year-- }) { Icon(Icons.Filled.ChevronLeft, "Previous year") }
                Text(year.toString(), Modifier.weight(1f), textAlign = TextAlign.Center)
                IconButton(onClick = { year++ }) { Icon(Icons.Filled.ChevronRight, "Next year") }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (r in 0 until 4) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (c in 0 until 3) {
                            val m = r * 3 + c + 1
                            val label = Month.of(m).getDisplayName(TextStyle.SHORT, Locale.getDefault())
                            val selected = year == initial.year && m == initial.monthValue
                            if (selected) {
                                FilledTonalButton(onClick = { onPick(YearMonth.of(year, m)) }, Modifier.weight(1f)) { Text(label) }
                            } else {
                                TextButton(onClick = { onPick(YearMonth.of(year, m)) }, Modifier.weight(1f)) { Text(label) }
                            }
                        }
                    }
                }
            }
        },
    )
}

@Composable
fun DatePickDialog(initial: LocalDate, onDismiss: () -> Unit, onPick: (LocalDate) -> Unit) {
    val state = rememberDatePickerState(initialSelectedDateMillis = Dates.toPicker(initial))
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { state.selectedDateMillis?.let { onPick(Dates.fromPicker(it)) } ?: onDismiss() }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    ) { DatePicker(state) }
}

@Composable
fun ConfirmDialog(title: String, text: String, confirm: String = "Delete", onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirm) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** Color swatches + icon grid, used when editing categories and accounts. */
@Composable
fun IconColorPicker(icon: String, color: Long, onIcon: (String) -> Unit, onColor: (Long) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Color", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AppIcons.palette.forEach { c ->
                Box(
                    Modifier.size(30.dp).clip(CircleShape).background(Color(c))
                        .then(if (c == color) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
                        .clickable { onColor(c) }
                )
            }
        }
        Text("Icon", style = MaterialTheme.typography.labelLarge)
        LazyVerticalGrid(GridCells.Adaptive(48.dp), Modifier.fillMaxWidth().height(200.dp)) {
            items(AppIcons.all.keys.toList()) { key ->
                val selected = key == icon
                Box(Modifier.padding(4.dp), contentAlignment = Alignment.Center) {
                    Box(
                        Modifier.size(42.dp).clip(CircleShape)
                            .background(if (selected) Color(color) else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { onIcon(key) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            AppIcons.get(key), key,
                            tint = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }
        }
    }
}
