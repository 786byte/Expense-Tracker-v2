@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.byte786.expensetracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.byte786.expensetracker.ExpenseApp
import com.byte786.expensetracker.ui.components.DatePickDialog
import com.byte786.expensetracker.ui.components.MonthPickerDialog
import com.byte786.expensetracker.ui.screens.AccountsScreen
import com.byte786.expensetracker.ui.screens.CategoriesScreen
import com.byte786.expensetracker.ui.screens.EditTransactionScreen
import com.byte786.expensetracker.ui.screens.GuideScreen
import com.byte786.expensetracker.ui.screens.HomeScreen
import com.byte786.expensetracker.ui.screens.ReportsScreen
import com.byte786.expensetracker.ui.screens.SearchScreen
import com.byte786.expensetracker.ui.screens.SettingsScreen
import com.byte786.expensetracker.ui.theme.LocalCurrency
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun AppRoot() {
    val nav = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = route == Routes.HOME || drawerState.isOpen,
        drawerContent = {
            AppDrawer(nav, route) { scope.launch { drawerState.close() } }
        },
    ) {
        NavHost(nav, startDestination = Routes.HOME) {
            composable(Routes.HOME) { HomeScreen(nav) { scope.launch { drawerState.open() } } }
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
            composable(Routes.REPORTS) { ReportsScreen(nav) }
            composable(Routes.CATEGORIES) { CategoriesScreen(nav) }
            composable(Routes.ACCOUNTS) { AccountsScreen(nav) }
            composable(Routes.SETTINGS) { SettingsScreen(nav) }
            composable(Routes.GUIDE) { GuideScreen(nav) }
        }
    }
}

@Composable
private fun AppDrawer(nav: NavHostController, route: String?, close: () -> Unit) {
    val repo = ExpenseApp.instance.repo
    val accounts by repo.accounts.collectAsState()
    val currency = LocalCurrency.current
    var query by remember { mutableStateOf("") }
    var pickDate by remember { mutableStateOf(false) }
    var pickMonth by remember { mutableStateOf(false) }

    fun go(r: String) {
        close()
        nav.navigate(r) { launchSingleTop = true }
    }

    ModalDrawerSheet {
        androidx.compose.foundation.layout.Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 16.dp)) {
            Text("Expense Tracker", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 24.dp, top = 24.dp))
            Text(
                "Total balance: " + Money.format(accounts.sumOf { it.balance }, currency),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 24.dp, top = 4.dp, bottom = 12.dp),
            )

            Text("Search", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 24.dp, bottom = 4.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Memo, category, amount…") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { go(Routes.search(q = query)) }),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )
            FlowRow(
                Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
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

            DrawerItem("Home", Icons.Filled.Home, route == Routes.HOME) {
                close(); nav.popBackStack(Routes.HOME, inclusive = false)
            }
            DrawerItem("Search & filter", Icons.Filled.Search, route?.startsWith("search") == true) { go(Routes.search()) }
            DrawerItem("Reports", Icons.Filled.PieChart, route == Routes.REPORTS) { go(Routes.REPORTS) }
            DrawerItem("Accounts", Icons.Filled.AccountBalanceWallet, route == Routes.ACCOUNTS) { go(Routes.ACCOUNTS) }
            DrawerItem("Categories", Icons.Filled.Category, route == Routes.CATEGORIES) { go(Routes.CATEGORIES) }
            DrawerItem("Backup & settings", Icons.Filled.Settings, route == Routes.SETTINGS) { go(Routes.SETTINGS) }
            DrawerItem("Guide", Icons.Filled.MenuBook, route == Routes.GUIDE) { go(Routes.GUIDE) }
            Spacer(Modifier.height(8.dp))
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
}

@Composable
private fun DrawerItem(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    NavigationDrawerItem(
        label = { Text(label) },
        icon = { Icon(icon, null) },
        selected = selected,
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 12.dp),
    )
}
