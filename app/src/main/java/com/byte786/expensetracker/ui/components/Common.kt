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
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.byte786.expensetracker.data.Photos
import kotlinx.coroutines.launch
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.FilterChip
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.byte786.expensetracker.ExpenseApp
import com.byte786.expensetracker.data.AccountBalance
import com.byte786.expensetracker.data.own
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
class Lookup(
    cats: List<Category>,
    accs: List<Account>,
    txns: List<Txn> = emptyList(),
    /** Show the subcategory's icon on records instead of the category's. */
    private val subIcon: Boolean = false,
) {
    val cat: Map<Long, Category> = cats.associateBy { it.id }
    val acc: Map<Long, Account> = accs.associateBy { it.id }
    /** Tracking-only accounts (e.g. Mom's grant): logged, but not your own money. */
    val tracking: Set<Long> = accs.filter { it.tracking }.map { it.id }.toSet()

    /** Whether this record (or merged row) counts in your income/expense totals. */
    fun counts(t: Txn): Boolean = t.type == TxnType.TRANSFER || t.accountId !in tracking

    /** Only the records that count as your own income/expense. */
    fun own(list: List<Txn>): List<Txn> = list.own(tracking)
    /** Parts of bills paid from several accounts, by groupId. */
    val groups: Map<Long, List<Txn>> = txns.filter { it.groupId != null }.groupBy { it.groupId!! }

    /**
     * For display: each bill paid from several accounts becomes one record
     * (the first part, carrying the bill's whole amount).
     */
    fun merged(list: List<Txn>): List<Txn> {
        val seen = HashSet<Long>()
        return list.mapNotNull { t ->
            val g = t.groupId ?: return@mapNotNull t
            if (!seen.add(g)) return@mapNotNull null
            val parts = groups[g] ?: return@mapNotNull t
            // The row stands for your own share; parts paid from tracking accounts are left out.
            val mine = parts.filter { counts(it) }
            val rep = mine.firstOrNull() ?: parts.first()
            rep.copy(amount = (mine.ifEmpty { parts }).sumOf { it.amount })
        }
    }

    fun title(t: Txn): String {
        if (t.type == TxnType.TRANSFER) return "Transfer"
        val c = t.categoryId?.let { cat[it] } ?: return "Uncategorized"
        val s = t.subcategoryId?.let { cat[it] } ?: return c.name
        return "${c.name} › ${s.name}"
    }

    fun accountText(t: Txn): String {
        t.groupId?.let { g ->
            val parts = groups[g]
            if (parts != null && parts.size > 1) return parts.joinToString(" + ") { p ->
                (acc[p.accountId]?.name ?: "?") + if (p.accountId in tracking) " (${Money.format(p.amount, "")})" else ""
            }
        }
        val from = acc[t.accountId]?.name ?: "?"
        return if (t.type == TxnType.TRANSFER) "$from → ${t.toAccountId?.let { acc[it]?.name } ?: "?"}" else from
    }

    fun icon(t: Txn): Pair<String, Long> {
        if (t.type == TxnType.TRANSFER) return "transfer" to 0xFF546E7A
        val c = if (subIcon) t.subcategoryId?.let { cat[it] } ?: t.categoryId?.let { cat[it] }
        else t.categoryId?.let { cat[it] } ?: t.subcategoryId?.let { cat[it] }
        return (c?.icon ?: "category") to (c?.color ?: 0xFF9E9E9E)
    }

    /** Text used by search. */
    fun searchable(t: Txn): String =
        listOf(title(t), accountText(t), t.memo, t.description.orEmpty(), Money.toInput(t.amount)).joinToString(" ")
}

/** A [Lookup] that follows the "Icon in lists" setting. */
@Composable
fun rememberLookup(cats: List<Category>, accs: List<AccountBalance>, txns: List<Txn>): Lookup {
    val sub by ExpenseApp.instance.repo.prefs.listIconSub.collectAsState()
    return remember(cats, accs, txns, sub) { Lookup(cats, accs.map { it.account }, txns, sub) }
}

/** Icon keys starting with this point to a picture in the app's icons folder. */
const val FILE_ICON = "file:"

@Composable
fun CategoryIcon(icon: String, color: Long, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    if (icon.startsWith(FILE_ICON)) {
        AsyncImage(
            model = ExpenseApp.instance.repo.iconFile(icon.removePrefix(FILE_ICON)),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier.size(size).clip(CircleShape).background(Color(color)),
        )
        return
    }
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
        item(key = "day-$day") { DayHeader(day, list, lk) }
        items(list, key = { "t-${it.id}" }) { t -> TxnRow(t, lk) { onClick(t) } }
    }
}

@Composable
private fun DayHeader(day: LocalDate, all: List<Txn>, lk: Lookup) {
    val list = all.filter { lk.counts(it) }
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

/** Color swatches + icon grid (with your own pictures), used when editing categories and accounts. */
@Composable
fun IconColorPicker(icon: String, color: Long, onIcon: (String) -> Unit, onColor: (Long) -> Unit) {
    val repo = ExpenseApp.instance.repo
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var cropSource by remember { mutableStateOf<Bitmap?>(null) }
    var customIcons by remember { mutableStateOf(repo.customIcons()) }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch { cropSource = Photos.load(ctx, uri, 1600) }
    }
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
        val keys = listOf("+photo") + customIcons.map { FILE_ICON + it } + AppIcons.all.keys
        LazyVerticalGrid(GridCells.Adaptive(48.dp), Modifier.fillMaxWidth().height(220.dp)) {
            items(keys) { key ->
                val selected = key == icon
                Box(Modifier.padding(4.dp), contentAlignment = Alignment.Center) {
                    when {
                        key == "+photo" -> Box(
                            Modifier.size(42.dp).clip(CircleShape).border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                .clickable { pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.Filled.AddPhotoAlternate, "Use a picture", tint = MaterialTheme.colorScheme.primary) }
                        key.startsWith(FILE_ICON) -> CategoryIcon(
                            key, color,
                            Modifier.then(if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
                                .clip(CircleShape).clickable { onIcon(key) },
                            size = 42.dp,
                        )
                        else -> Box(
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
        Text(
            "Tip: the first button uses a picture from your gallery (cropped to a circle).",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    cropSource?.let { src ->
        CropDialog(src, forceSquare = true, title = "Crop the icon", onDismiss = { cropSource = null }) { cropped ->
            cropSource = null
            scope.launch {
                val name = Photos.saveIcon(cropped, repo.iconsDir)
                if (name != null) {
                    customIcons = repo.customIcons()
                    onIcon(FILE_ICON + name)
                }
            }
        }
    }
}

/** Which records a history view shows: one month, a custom date range, or all time. */
class PeriodState(month: YearMonth, mode: Int, from: Long, until: Long) {
    var month by mutableStateOf(month)
    /** [MONTH], [RANGE] or [ALL]. */
    var mode by mutableIntStateOf(mode)
    /** Custom range [from, to) in epoch millis. */
    var from by mutableLongStateOf(from)
    var until by mutableLongStateOf(until)

    val allTime get() = mode == ALL

    /** [start, end) or null for all time. */
    val range: Pair<Long, Long>?
        get() = when (mode) {
            ALL -> null
            RANGE -> Pair(from, until)
            else -> Dates.monthRange(month)
        }

    fun contains(ms: Long): Boolean = range?.let { ms >= it.first && ms < it.second } ?: true

    val label: String
        get() = when (mode) {
            ALL -> "All time"
            RANGE -> Dates.describeRange(from, until)
            else -> Dates.month(month)
        }

    companion object {
        const val MONTH = 0
        const val RANGE = 1
        const val ALL = 2

        val Saver = listSaver<PeriodState, Any>(
            save = { listOf(it.month.toString(), it.mode, it.from, it.until) },
            restore = { PeriodState(YearMonth.parse(it[0] as String), it[1] as Int, it[2] as Long, it[3] as Long) },
        )
    }
}

@Composable
fun rememberPeriodState(): PeriodState = rememberSaveable(saver = PeriodState.Saver) {
    PeriodState(YearMonth.now(), PeriodState.MONTH, -1L, -1L)
}

/** "‹ Nov 2026 ›  [Custom range]  [All time]": chooses which period a view shows. */
@Composable
fun PeriodBar(state: PeriodState, modifier: Modifier = Modifier) {
    var pickMonth by remember { mutableStateOf(false) }
    var pickRange by remember { mutableStateOf(false) }
    Row(
        modifier.horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        IconButton(onClick = { state.month = state.month.minusMonths(1); state.mode = PeriodState.MONTH }) {
            Icon(Icons.Filled.ChevronLeft, "Previous month")
        }
        FilterChip(
            selected = state.mode == PeriodState.MONTH,
            onClick = { if (state.mode != PeriodState.MONTH) state.mode = PeriodState.MONTH else pickMonth = true },
            label = { Text(Dates.shortMonth(state.month), fontWeight = FontWeight.Bold) },
        )
        IconButton(onClick = { state.month = state.month.plusMonths(1); state.mode = PeriodState.MONTH }) {
            Icon(Icons.Filled.ChevronRight, "Next month")
        }
        FilterChip(
            selected = state.mode == PeriodState.RANGE,
            onClick = { pickRange = true },
            label = {
                Text(if (state.mode == PeriodState.RANGE) Dates.describeRange(state.from, state.until) else "Custom range", fontWeight = FontWeight.Bold)
            },
            leadingIcon = { Icon(Icons.Filled.DateRange, null, Modifier.size(18.dp)) },
        )
        Spacer(Modifier.width(6.dp))
        FilterChip(
            selected = state.mode == PeriodState.ALL, onClick = { state.mode = PeriodState.ALL },
            label = { Text("All time", fontWeight = FontWeight.Bold) },
        )
        Spacer(Modifier.width(8.dp))
    }
    if (pickMonth) MonthPickerDialog(state.month, onDismiss = { pickMonth = false }) {
        state.month = it; state.mode = PeriodState.MONTH; pickMonth = false
    }
    if (pickRange) DateRangeDialog(onDismiss = { pickRange = false }) { f, t ->
        state.from = f; state.until = t; state.mode = PeriodState.RANGE; pickRange = false
    }
}

/** Material date-range picker. [onPick] gets [from, to) in epoch millis (whole days, local time). */
@Composable
fun DateRangeDialog(onDismiss: () -> Unit, onPick: (Long, Long) -> Unit) {
    val state = rememberDateRangePickerState()
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val s = state.selectedStartDateMillis
                if (s != null) {
                    val start = Dates.fromPicker(s)
                    val end = state.selectedEndDateMillis?.let { Dates.fromPicker(it) } ?: start
                    onPick(Dates.start(start), Dates.start(end.plusDays(1)))
                } else onDismiss()
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    ) {
        DateRangePicker(state, Modifier.heightIn(max = 520.dp), title = { Text("Select dates", Modifier.padding(start = 24.dp, top = 16.dp)) })
    }
}
