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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.byte786.expensetracker.ExpenseApp
import com.byte786.expensetracker.data.Account
import com.byte786.expensetracker.data.TxnType
import com.byte786.expensetracker.ui.Money
import com.byte786.expensetracker.ui.Routes
import com.byte786.expensetracker.ui.components.CategoryIcon
import com.byte786.expensetracker.ui.components.IconColorPicker
import com.byte786.expensetracker.ui.theme.LocalCurrency
import com.byte786.expensetracker.ui.theme.MoneyColors
import kotlinx.coroutines.launch

@Composable
fun AccountsScreen(nav: NavController) {
    val repo = ExpenseApp.instance.repo
    val accs by repo.accounts.collectAsState()
    val currency = LocalCurrency.current
    var editing by remember { mutableStateOf<Account?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val screenScope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Accounts") },
                navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    IconButton(onClick = { nav.navigate(Routes.edit(type = TxnType.TRANSFER)) }) {
                        Icon(Icons.Filled.SwapHoriz, "Transfer between accounts")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { editing = Account(name = "", sortOrder = accs.size) },
                icon = { Icon(Icons.Filled.Add, null) }, text = { Text("Account") },
            )
        },
    ) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(bottom = 96.dp)) {
            item {
                ElevatedCard(Modifier.fillMaxWidth().padding(16.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Total balance", style = MaterialTheme.typography.labelMedium)
                        val total = accs.sumOf { it.balance }
                        Text(
                            Money.format(total, currency), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
                            color = if (total >= 0) MoneyColors.income else MoneyColors.expense,
                        )
                    }
                }
            }
            items(accs, key = { it.account.id }) { a ->
                Row(
                    Modifier.fillMaxWidth().clickable { editing = a.account }.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CategoryIcon(a.account.icon, a.account.color)
                    Spacer(Modifier.width(12.dp))
                    Text(a.account.name, Modifier.weight(1f), fontWeight = FontWeight.Medium)
                    Text(
                        Money.format(a.balance, currency), fontWeight = FontWeight.SemiBold,
                        color = if (a.balance >= 0) MaterialTheme.colorScheme.onSurface else MoneyColors.expense,
                    )
                }
                HorizontalDivider()
            }
        }
    }

    editing?.let { a ->
        AccountEditDialog(a, onDismiss = { editing = null }, onMessage = { msg -> screenScope.launch { snackbar.showSnackbar(msg) } })
    }
}

@Composable
private fun AccountEditDialog(initial: Account, onDismiss: () -> Unit, onMessage: (String) -> Unit) {
    val repo = ExpenseApp.instance.repo
    val currency = LocalCurrency.current
    var name by remember { mutableStateOf(initial.name) }
    var icon by remember { mutableStateOf(initial.icon) }
    var color by remember { mutableStateOf(initial.color) }
    var opening by remember { mutableStateOf(Money.toInput(initial.initialBalance)) }
    val isNew = initial.id == 0L

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isNew) "New account" else "Edit ${initial.name}") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CategoryIcon(icon, color, size = 48.dp)
                    Spacer(Modifier.width(12.dp))
                    OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true)
                }
                OutlinedTextField(
                    opening, { v -> if (v.all { it.isDigit() || it == '.' || it == '-' }) opening = v },
                    label = { Text("Starting balance") }, prefix = { Text(currency) }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    supportingText = { Text("Money already in this account before you started tracking") },
                )
                IconColorPicker(icon, color, { icon = it }, { color = it })
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank()) {
                    val bal = Money.parse(opening) ?: 0L
                    repo.launch { repo.saveAccount(initial.copy(name = name.trim(), icon = icon, color = color, initialBalance = bal)) }
                    onDismiss()
                }
            }) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (!isNew) TextButton(onClick = {
                    onDismiss()
                    repo.launch {
                        if (!repo.deleteAccount(initial.id)) {
                            onMessage("Can't delete: this account has records. Move or delete them first.")
                        }
                    }
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}
