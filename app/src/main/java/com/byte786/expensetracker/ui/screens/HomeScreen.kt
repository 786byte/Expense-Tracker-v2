@file:OptIn(ExperimentalMaterial3Api::class)

package com.byte786.expensetracker.ui.screens

import android.speech.RecognizerIntent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.Mic
import androidx.compose.ui.platform.LocalContext
import com.byte786.expensetracker.ui.components.VoiceInput
import com.byte786.expensetracker.ui.components.AdviceSheet
import com.byte786.expensetracker.ui.components.RecordSheet
import com.byte786.expensetracker.ui.components.moodName
import com.byte786.expensetracker.ui.components.rememberLookup
import com.byte786.expensetracker.ui.components.rememberStatus
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.byte786.expensetracker.ExpenseApp
import com.byte786.expensetracker.data.TxnType
import com.byte786.expensetracker.data.totalBalance
import com.byte786.expensetracker.ui.Dates
import com.byte786.expensetracker.ui.Money
import com.byte786.expensetracker.ui.Routes
import com.byte786.expensetracker.ui.components.CatFace
import com.byte786.expensetracker.data.CatMood
import com.byte786.expensetracker.ui.components.EmptyState
import com.byte786.expensetracker.ui.components.MonthPickerDialog
import com.byte786.expensetracker.ui.components.RunningCat
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
    val lk = rememberLookup(cats, accs, txns)
    val order by repo.prefs.homeOrder.collectAsState()
    val hidden by repo.prefs.homeHidden.collectAsState()
    val currency = LocalCurrency.current

    var month by rememberSaveable { mutableStateOf(YearMonth.now()) }
    var fabOpen by rememberSaveable { mutableStateOf(false) }
    var pickMonth by remember { mutableStateOf(false) }

    val range = Dates.monthRange(month)
    val monthTx = remember(txns, month) { txns.filter { it.date >= range.first && it.date < range.second } }
    val ownTx = lk.own(monthTx)
    val income = ownTx.filter { it.type == TxnType.INCOME }.sumOf { it.amount }
    val expense = ownTx.filter { it.type == TxnType.EXPENSE }.sumOf { it.amount }
    val total = accs.totalBalance()

    BackHandler(enabled = fabOpen) { fabOpen = false }

    // Status cat mood: always about the current month.
    val status = rememberStatus(txns, accs)
    val mood = status.mood
    var showAdvice by remember { mutableStateOf(false) }
    var openId by remember { mutableStateOf<Long?>(null) }
    val ctx = LocalContext.current
    val voiceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        val heard = r.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (!heard.isNullOrBlank()) {
            repo.voiceDraft = VoiceInput.parse(heard, cats, accs.map { it.account })
            nav.navigate(Routes.edit(type = repo.voiceDraft!!.type))
        }
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Home", fontWeight = FontWeight.Black) },
                    navigationIcon = { IconButton(onClick = openDrawer) { Icon(Icons.Filled.Menu, "Menu") } },
                    actions = {
                        IconButton(onClick = { VoiceInput.launch(ctx, voiceLauncher) }) { Icon(Icons.Filled.Mic, "Add by voice") }
                        IconButton(onClick = { nav.navigate(Routes.search()) }) { Icon(Icons.Filled.Search, "Search") }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { fabOpen = true },
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.onBackground,
                    contentColor = MaterialTheme.colorScheme.background,
                ) { Icon(Icons.Filled.Add, "Add expense, income or transfer", Modifier.size(28.dp)) }
            },
        ) { pad ->
            LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(bottom = 96.dp)) {
                order.forEach { block ->
                // Status cat | running cat | total balance
                val showCat = "statusCat" !in hidden
                val showRunner = "runningCat" !in hidden
                val showBalance = "balance" !in hidden
                if (block == "hero" && (showCat || showRunner || showBalance)) item(key = "hero") {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
                            .padding(horizontal = 10.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Status cat: tap it for its mood and some advice.
                        if (showCat) Box(
                            Modifier.size(72.dp).clip(CircleShape).clickable(onClickLabel = "Show mood and advice") { showAdvice = true },
                            contentAlignment = Alignment.Center,
                        ) { CatFace(mood, Modifier.size(60.dp), description = moodName(mood)) }
                        if (showRunner) RunningCat(Modifier.weight(1f).height(72.dp)) else Spacer(Modifier.weight(1f).height(72.dp))
                        if (showBalance) Column(Modifier.width(120.dp), horizontalAlignment = Alignment.End) {
                            Text("Total balance", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                Money.format(total, currency), style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black, maxLines = 1, textAlign = TextAlign.End,
                                color = if (total < 0) MoneyColors.expense else MoneyColors.income,
                            )
                        }
                    }
                }

                // Month totals (left) and month switcher (right)
                if (block == "totals") item(key = "month") {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            if ("totals" !in hidden) TotalLine("Income", "+" + Money.format(income, currency), MoneyColors.income) {
                                val (f, t) = range; nav.navigate(Routes.search(from = f, to = t, type = TxnType.INCOME))
                            }
                            if ("totals" !in hidden) TotalLine("Expense", "-" + Money.format(expense, currency), MoneyColors.expense) {
                                val (f, t) = range; nav.navigate(Routes.search(from = f, to = t, type = TxnType.EXPENSE))
                            }
                        }
                        Row(
                            Modifier.clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp)),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(onClick = { month = month.minusMonths(1) }) { Icon(Icons.Filled.ChevronLeft, "Previous month") }
                            TextButton(onClick = { pickMonth = true }) {
                                Text(Dates.shortMonth(month), fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                            }
                            IconButton(onClick = { month = month.plusMonths(1) }) { Icon(Icons.Filled.ChevronRight, "Next month") }
                        }
                    }
                }

                if (block == "records") {
                    if (monthTx.isEmpty()) item(key = "empty") { EmptyState("Nothing recorded in ${Dates.month(month)}.\nTap + to add one.") }
                    txnDayGroups(lk.merged(monthTx), lk) { openId = it.id }
                }
                }
            }
        }

        // + menu: three cats
        AnimatedVisibility(fabOpen, enter = fadeIn(), exit = fadeOut()) {
            Box(
                Modifier.fillMaxSize().background(Color(0x732A1F17))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { fabOpen = false }
            ) {
                Column(
                    Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 16.dp),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    CatChoice("Expense", MoneyColors.expense, TxnType.EXPENSE, fur = Color(0xFFE8913A)) {
                        fabOpen = false; nav.navigate(Routes.edit(type = TxnType.EXPENSE))
                    }
                    CatChoice("Income", MoneyColors.income, TxnType.INCOME, fur = Color(0xFF4A4A4A), eyes = Color(0xFFF2C14E), nose = Color(0xFFF29C9C)) {
                        fabOpen = false; nav.navigate(Routes.edit(type = TxnType.INCOME))
                    }
                    CatChoice("Transfer", MoneyColors.transfer, TxnType.TRANSFER, fur = Color(0xFFCFC7BB)) {
                        fabOpen = false; nav.navigate(Routes.edit(type = TxnType.TRANSFER))
                    }
                    FloatingActionButton(
                        onClick = { fabOpen = false },
                        shape = CircleShape,
                        containerColor = MaterialTheme.colorScheme.background,
                        contentColor = MaterialTheme.colorScheme.onBackground,
                    ) { Icon(Icons.Filled.Close, "Close add menu") }
                }
            }
        }
    }

    if (pickMonth) MonthPickerDialog(month, onDismiss = { pickMonth = false }) { month = it; pickMonth = false }
    if (showAdvice) AdviceSheet(status) { showAdvice = false }
    openId?.let { RecordSheet(it, nav) { openId = null } }
}

@Composable
private fun TotalLine(label: String, value: String, color: Color, onClick: () -> Unit) {
    Row(Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick).padding(vertical = 2.dp, horizontal = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.width(64.dp), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = color)
    }
}

@Composable
private fun CatChoice(
    label: String,
    color: Color,
    type: Int,
    fur: Color,
    eyes: Color = Color(0xFF2A1F17),
    nose: Color = Color(0xFF2A1F17),
    onClick: () -> Unit,
) {
    Row(
        Modifier.clip(RoundedCornerShape(32.dp)).clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceContainerLowest) {
            Text(label, Modifier.padding(horizontal = 14.dp, vertical = 8.dp), fontWeight = FontWeight.Black, color = color)
        }
        Spacer(Modifier.width(10.dp))
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerLowest, shadowElevation = 4.dp) {
            CatFace(CatMood.PLAIN, Modifier.padding(6.dp).size(52.dp), fur = fur, eyes = eyes, nose = nose, badge = type)
        }
    }
}
