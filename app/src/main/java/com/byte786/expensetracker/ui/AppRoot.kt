@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.byte786.expensetracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.byte786.expensetracker.ExpenseApp
import com.byte786.expensetracker.ui.components.CatFace
import com.byte786.expensetracker.ui.components.statusMood
import com.byte786.expensetracker.ui.components.DatePickDialog
import com.byte786.expensetracker.ui.components.MonthPickerDialog
import com.byte786.expensetracker.ui.screens.AccountsTab
import com.byte786.expensetracker.ui.screens.AnalysisScreen
import com.byte786.expensetracker.ui.screens.EditTransactionScreen
import com.byte786.expensetracker.ui.screens.GuideScreen
import com.byte786.expensetracker.ui.screens.HomeScreen
import com.byte786.expensetracker.ui.screens.SearchScreen
import com.byte786.expensetracker.ui.screens.SettingsScreen
import com.byte786.expensetracker.ui.theme.LocalCurrency
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

/** Switches bottom-bar tabs without piling up screens; Home is always the base. */
fun NavHostController.goTab(route: String) {
    if (route == Routes.HOME) {
        popBackStack(Routes.HOME, inclusive = false)
        return
    }
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun AppRoot() {
    val nav = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val openDrawer: () -> Unit = { scope.launch { drawerState.open() } }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = route in Routes.TABS || drawerState.isOpen,
        drawerContent = { AppDrawer(nav) { scope.launch { drawerState.close() } } },
    ) {
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = { if (route in Routes.TABS) BottomBar(route) { nav.goTab(it) } },
        ) { pad ->
            NavHost(nav, startDestination = Routes.HOME, modifier = Modifier.padding(pad).consumeWindowInsets(pad)) {
                composable(Routes.HOME) { HomeScreen(nav, openDrawer) }
                composable(Routes.ACCOUNTS) { AccountsTab(nav, openDrawer) }
                composable(Routes.ANALYSIS) { AnalysisScreen(nav, openDrawer) }
                composable(
                    Routes.EDIT,
                    arguments = listOf(
                        navArgument("id") { type = NavType.LongType; defaultValue = 0L },
                        navArgument("type") { type = NavType.IntType; defaultValue = 0 },
                    ),
                ) { e ->
                    EditTransactionScreen(nav, e.arguments?.getLong("id") ?: 0L, e.arguments?.getInt("type") ?: 0)
                }
                composable(
                    Routes.SEARCH,
                    arguments = listOf(
                        navArgument("cat") { type = NavType.LongType; defaultValue = -1L },
                        navArgument("from") { type = NavType.LongType; defaultValue = -1L },
                        navArgument("to") { type = NavType.LongType; defaultValue = -1L },
                        navArgument("type") { type = NavType.IntType; defaultValue = -1 },
                        navArgument("q") { type = NavType.StringType; defaultValue = "" },
                    ),
                ) { e ->
                    val a = e.arguments
                    SearchScreen(
                        nav,
                        initialCat = a?.getLong("cat") ?: -1L,
                        initialFrom = a?.getLong("from") ?: -1L,
                        initialTo = a?.getLong("to") ?: -1L,
                        initialType = a?.getInt("type") ?: -1,
                        initialQuery = a?.getString("q") ?: "",
                    )
                }
                composable(
                    Routes.SETTINGS,
                    arguments = listOf(navArgument("action") { type = NavType.StringType; defaultValue = "" }),
                ) { e -> SettingsScreen(nav, e.arguments?.getString("action") ?: "") }
                composable(Routes.GUIDE) { GuideScreen(nav) }
            }
        }
    }
}

@Composable
private fun BottomBar(route: String?, onTab: (String) -> Unit) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest) {
        listOf(
            Triple(Routes.ACCOUNTS, "Accounts", Icons.Filled.AccountBalanceWallet),
            Triple(Routes.HOME, "Home", Icons.Filled.Home),
            Triple(Routes.ANALYSIS, "Analysis", Icons.Filled.PieChart),
        ).forEach { (r, label, icon) ->
            NavigationBarItem(
                selected = route == r,
                onClick = { onTab(r) },
                icon = { Icon(icon, null) },
                label = { Text(label, fontWeight = FontWeight.Bold) },
            )
        }
    }
}

@Composable
private fun AppDrawer(nav: NavHostController, close: () -> Unit) {
    val repo = ExpenseApp.instance.repo
    val accounts by repo.accounts.collectAsState()
    val currency = LocalCurrency.current
    var query by remember { mutableStateOf("") }
    var pickDate by remember { mutableStateOf(false) }
    var pickMonth by remember { mutableStateOf(false) }
    var resetStep by remember { mutableIntStateOf(0) } // 0 closed, 1 choose, 2 confirm
    var resetAll by remember { mutableStateOf(false) }
    val total = accounts.sumOf { it.balance }

    fun go(r: String) {
        close()
        nav.navigate(r) { launchSingleTop = true }
    }

    ModalDrawerSheet(drawerContainerColor = MaterialTheme.colorScheme.background) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 16.dp)) {
            Row(Modifier.padding(start = 20.dp, top = 20.dp, bottom = 12.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                val txns by repo.txns.collectAsState()
                val (cf, ct) = Dates.monthRange(YearMonth.now())
                val nowTx = txns.filter { it.date >= cf && it.date < ct }
                CatFace(
                    statusMood(
                        total,
                        nowTx.filter { it.type == com.byte786.expensetracker.data.TxnType.INCOME }.sumOf { it.amount },
                        nowTx.filter { it.type == com.byte786.expensetracker.data.TxnType.EXPENSE }.sumOf { it.amount },
                    ),
                    Modifier.size(48.dp),
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Expense Tracker", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    Text(
                        "Total balance: " + Money.format(total, currency),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search memo, category, amount…") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { go(Routes.search(q = query)) }),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )
            FlowRow(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val today = LocalDate.now()
                val month = YearMonth.now()
                AssistChip(onClick = { val (f, t) = Dates.dayRange(today); go(Routes.search(from = f, to = t)) }, label = { Text("Today") })
                AssistChip(onClick = { val (f, t) = Dates.monthRange(month); go(Routes.search(from = f, to = t)) }, label = { Text("This month") })
                AssistChip(onClick = { val (f, t) = Dates.monthRange(month.minusMonths(1)); go(Routes.search(from = f, to = t)) }, label = { Text("Last month") })
                AssistChip(onClick = { val (f, t) = Dates.yearRange(today.year); go(Routes.search(from = f, to = t)) }, label = { Text("This year") })
                AssistChip(onClick = { pickDate = true }, label = { Text("Pick a date") }, leadingIcon = { Icon(Icons.Filled.Event, null) })
                AssistChip(onClick = { pickMonth = true }, label = { Text("Pick a month") }, leadingIcon = { Icon(Icons.Filled.CalendarMonth, null) })
                AssistChip(onClick = { go(Routes.search()) }, label = { Text("By category…") }, leadingIcon = { Icon(Icons.Filled.Category, null) })
            }
            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            DrawerItem("Backup & settings", "Google Drive backup, theme, currency", Icons.Filled.CloudUpload) { go(Routes.settings()) }
            DrawerItem("Export", "Save a CSV or a full backup copy", Icons.Filled.FileDownload) { go(Routes.settings("export")) }
            DrawerItem("Import CSV", "Bring in records from another app", Icons.Filled.FileUpload) { go(Routes.settings("import")) }
            DrawerItem("Guide", "How everything works", Icons.Filled.MenuBook) { go(Routes.GUIDE) }
            DrawerItem("Delete & reset", "Erase records, or everything", Icons.Filled.DeleteForever, danger = true) {
                resetAll = false; resetStep = 1
            }
        }
    }

    if (pickDate) {
        DatePickDialog(LocalDate.now(), onDismiss = { pickDate = false }) { d ->
            pickDate = false
            val (f, t) = Dates.dayRange(d)
            go(Routes.search(from = f, to = t))
        }
    }
    if (pickMonth) {
        MonthPickerDialog(YearMonth.now(), onDismiss = { pickMonth = false }) { m ->
            pickMonth = false
            val (f, t) = Dates.monthRange(m)
            go(Routes.search(from = f, to = t))
        }
    }

    if (resetStep == 1) {
        AlertDialog(
            onDismissRequest = { resetStep = 0 },
            icon = { Icon(Icons.Filled.DeleteForever, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete & reset") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    ResetOption(
                        "Records only",
                        "Deletes every expense, income and transfer (and their photos). Accounts and categories stay.",
                        selected = !resetAll,
                    ) { resetAll = false }
                    ResetOption(
                        "Everything",
                        "Deletes all records, accounts and categories, and starts over with the default ones.",
                        selected = resetAll,
                    ) { resetAll = true }
                }
            },
            confirmButton = { TextButton(onClick = { resetStep = 2 }) { Text("Next") } },
            dismissButton = { TextButton(onClick = { resetStep = 0 }) { Text("Cancel") } },
        )
    }
    if (resetStep == 2) {
        AlertDialog(
            onDismissRequest = { resetStep = 0 },
            title = { Text(if (resetAll) "Delete everything?" else "Delete all records?") },
            text = {
                Text(
                    "A safety copy is kept on this phone, so you can bring the data back from " +
                        "Backup & settings → “Undo the last reset”. Your Google Drive backup will be " +
                        "updated to the new, emptier data."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val all = resetAll
                    resetStep = 0
                    close()
                    repo.launch { if (all) repo.resetEverything() else repo.deleteAllRecords() }
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { resetStep = 0 }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun ResetOption(title: String, text: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().selectable(selected = selected, onClick = onClick, role = Role.RadioButton).padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top,
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(8.dp))
        Column {
            Text(title, fontWeight = FontWeight.Bold)
            Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DrawerItem(label: String, hint: String, icon: ImageVector, danger: Boolean = false, onClick: () -> Unit) {
    val color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    NavigationDrawerItem(
        label = {
            Column(Modifier.padding(vertical = 4.dp)) {
                Text(label, fontWeight = FontWeight.Bold, color = color)
                Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        icon = { Icon(icon, null, tint = color) },
        selected = false,
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 12.dp).height(64.dp),
    )
}
