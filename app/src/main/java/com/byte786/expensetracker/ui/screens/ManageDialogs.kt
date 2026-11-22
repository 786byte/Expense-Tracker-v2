@file:OptIn(ExperimentalMaterial3Api::class)

package com.byte786.expensetracker.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.CallMerge
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.byte786.expensetracker.ExpenseApp
import com.byte786.expensetracker.data.Account
import com.byte786.expensetracker.data.Category
import com.byte786.expensetracker.data.TxnType
import com.byte786.expensetracker.ui.Money
import com.byte786.expensetracker.ui.Routes
import com.byte786.expensetracker.ui.components.CategoryIcon
import com.byte786.expensetracker.ui.components.ConfirmDialog
import com.byte786.expensetracker.ui.components.IconColorPicker
import com.byte786.expensetracker.ui.theme.LocalCurrency
import com.byte786.expensetracker.ui.theme.MoneyColors
import kotlinx.coroutines.launch

@Composable
fun CategoryEditDialog(initial: Category, cats: List<Category>, onDismiss: () -> Unit) {
    val repo = ExpenseApp.instance.repo
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(initial.name) }
    var icon by remember { mutableStateOf(initial.icon) }
    var color by remember { mutableStateOf(initial.color) }
    var confirmDelete by remember { mutableStateOf<Int?>(null) }
    val isNew = initial.id == 0L
    val parentName = initial.parentId?.let { p -> cats.firstOrNull { it.id == p }?.name }
    var newParent by remember { mutableStateOf(initial.parentId) }
    var parentMenu by remember { mutableStateOf(false) }
    // A top-level category can only go inside another one if it has no subcategories itself.
    val canMove = !isNew && (initial.parentId != null || cats.none { it.parentId == initial.id })
    val parentChoices = cats.filter { it.parentId == null && it.type == initial.type && it.id != initial.id }
    // Merge: top-level into another top-level, subcategory into any other subcategory of the same type.
    val mergeChoices = if (isNew) emptyList() else if (initial.parentId == null) parentChoices
    else cats.filter { it.parentId != null && it.type == initial.type && it.id != initial.id }
        .sortedBy { c -> (cats.firstOrNull { it.id == c.parentId }?.name ?: "") + " " + c.name }
    var mergeMenu by remember { mutableStateOf(false) }
    var mergeInto by remember { mutableStateOf<Category?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                when {
                    isNew && parentName != null -> "New subcategory of $parentName"
                    isNew -> "New category"
                    else -> "Edit ${initial.name}"
                }
            )
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CategoryIcon(icon, color, size = 48.dp)
                    Spacer(Modifier.width(12.dp))
                    OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true)
                }
                if (canMove) {
                    Column {
                        Text("Place it", style = MaterialTheme.typography.labelLarge)
                        Box {
                            OutlinedButton(onClick = { parentMenu = true }) {
                                Text(newParent?.let { p -> "Inside " + (cats.firstOrNull { it.id == p }?.name ?: "?") } ?: "As a main category")
                                Icon(Icons.Filled.ArrowDropDown, null)
                            }
                            DropdownMenu(expanded = parentMenu, onDismissRequest = { parentMenu = false }) {
                                DropdownMenuItem(text = { Text("As a main category") }, onClick = { newParent = null; parentMenu = false })
                                parentChoices.forEach { p ->
                                    DropdownMenuItem(
                                        text = { Text("Inside ${p.name}") },
                                        leadingIcon = { CategoryIcon(p.icon, p.color, size = 24.dp) },
                                        onClick = { newParent = p.id; parentMenu = false },
                                    )
                                }
                            }
                        }
                        if (newParent != initial.parentId) {
                            Text(
                                "Its records move along with it.",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                if (mergeChoices.isNotEmpty()) {
                    Column {
                        Text("Merge", style = MaterialTheme.typography.labelLarge)
                        Box {
                            OutlinedButton(onClick = { mergeMenu = true }) {
                                Icon(Icons.Filled.CallMerge, null); Spacer(Modifier.width(6.dp))
                                Text("Merge into another…")
                            }
                            DropdownMenu(expanded = mergeMenu, onDismissRequest = { mergeMenu = false }) {
                                mergeChoices.forEach { c ->
                                    val label = c.parentId?.let { p -> "${cats.firstOrNull { it.id == p }?.name ?: "?"} › ${c.name}" } ?: c.name
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        leadingIcon = { CategoryIcon(c.icon, c.color, size = 24.dp) },
                                        onClick = { mergeInto = c; mergeMenu = false },
                                    )
                                }
                            }
                        }
                        Text(
                            "Moves all its records" + (if (initial.parentId == null) " and subcategories" else "") +
                                " into the one you pick, then removes this one.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                IconColorPicker(icon, color, { icon = it }, { color = it })
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank()) {
                    val updated = initial.copy(name = name.trim(), icon = icon, color = color)
                    repo.launch {
                        if (newParent != initial.parentId) repo.moveCategory(updated, newParent) else repo.saveCategory(updated)
                    }
                    onDismiss()
                }
            }) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (!isNew) TextButton(onClick = {
                    scope.launch { confirmDelete = repo.dao.categoryUsage(initial.id) }
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )

    mergeInto?.let { target ->
        ConfirmDialog(
            "Merge ${initial.name} into ${target.name}?",
            "All records of ${initial.name}" + (if (initial.parentId == null) " and its subcategories" else "") +
                " move to ${target.name}, and ${initial.name} is removed.",
            confirm = "Merge",
            onDismiss = { mergeInto = null },
        ) {
            repo.launch { repo.mergeCategory(initial, target) }
            mergeInto = null
            onDismiss()
        }
    }

    confirmDelete?.let { used ->
        val subsNote = if (initial.parentId == null) " Its subcategories will be deleted too." else ""
        ConfirmDialog(
            "Delete ${initial.name}?",
            (if (used > 0) "$used record(s) use it; they will become \"Uncategorized\"." else "No records use it.") + subsNote,
            onDismiss = { confirmDelete = null },
        ) {
            repo.launch { repo.deleteCategory(initial) }
            confirmDelete = null
            onDismiss()
        }
    }
}

@Composable
fun AccountEditDialog(initial: Account, onDismiss: () -> Unit, onMessage: (String) -> Unit) {
    val repo = ExpenseApp.instance.repo
    val currency = LocalCurrency.current
    var name by remember { mutableStateOf(initial.name) }
    var icon by remember { mutableStateOf(initial.icon) }
    var color by remember { mutableStateOf(initial.color) }
    var opening by remember { mutableStateOf(Money.toInput(initial.initialBalance)) }
    var tracking by remember { mutableStateOf(initial.tracking) }
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Tracking only", fontWeight = FontWeight.Bold)
                        Text(
                            "For money someone else pays, e.g. Mom's grant. Parts paid from it are logged here " +
                                "but don't count as your expense or income, and it's left out of your total balance. " +
                                "The Accounts tab shows how much was paid through it.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = tracking, onCheckedChange = { tracking = it })
                }
                if (!tracking) OutlinedTextField(
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
                    repo.launch { repo.saveAccount(initial.copy(name = name.trim(), icon = icon, color = color, initialBalance = bal, tracking = tracking)) }
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
