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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.byte786.expensetracker.ExpenseApp
import com.byte786.expensetracker.data.Category
import com.byte786.expensetracker.data.TxnType
import com.byte786.expensetracker.ui.components.CategoryIcon
import com.byte786.expensetracker.ui.components.ConfirmDialog
import com.byte786.expensetracker.ui.components.IconColorPicker
import kotlinx.coroutines.launch

@Composable
fun CategoriesScreen(nav: NavController) {
    val repo = ExpenseApp.instance.repo
    val cats by repo.categories.collectAsState()
    var type by rememberSaveable { mutableIntStateOf(TxnType.EXPENSE) }
    var open by remember { mutableStateOf(setOf<Long>()) }
    var editing by remember { mutableStateOf<Category?>(null) }

    val parents = cats.filter { it.parentId == null && it.type == type }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Categories") },
                navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { editing = Category(name = "", type = type, icon = "category", color = 0xFF00897B, sortOrder = parents.size) },
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text("Category") },
            )
        },
    ) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(bottom = 96.dp)) {
            item {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(16.dp)) {
                    SegmentedButton(type == TxnType.EXPENSE, { type = TxnType.EXPENSE }, SegmentedButtonDefaults.itemShape(0, 2)) { Text("Expense") }
                    SegmentedButton(type == TxnType.INCOME, { type = TxnType.INCOME }, SegmentedButtonDefaults.itemShape(1, 2)) { Text("Income") }
                }
            }
            items(parents, key = { it.id }) { p ->
                val subs = cats.filter { it.parentId == p.id }
                val isOpen = p.id in open
                Column {
                    Row(
                        Modifier.fillMaxWidth().clickable { open = if (isOpen) open - p.id else open + p.id }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CategoryIcon(p.icon, p.color)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(p.name, fontWeight = FontWeight.Medium)
                            Text(
                                if (subs.isEmpty()) "No subcategories" else subs.joinToString { it.name },
                                style = MaterialTheme.typography.bodySmall, maxLines = 1,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { editing = p }) { Icon(Icons.Filled.Edit, "Edit ${p.name}") }
                        Icon(if (isOpen) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null)
                    }
                    if (isOpen) {
                        subs.forEach { s ->
                            Row(
                                Modifier.fillMaxWidth().clickable { editing = s }.padding(start = 40.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                CategoryIcon(s.icon, s.color, size = 32.dp)
                                Spacer(Modifier.width(12.dp))
                                Text(s.name, Modifier.weight(1f))
                                Icon(Icons.Filled.Edit, "Edit ${s.name}", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        TextButton(
                            onClick = {
                                editing = Category(name = "", type = p.type, parentId = p.id, icon = p.icon, color = p.color, sortOrder = subs.size)
                            },
                            modifier = Modifier.padding(start = 32.dp),
                        ) {
                            Icon(Icons.Filled.Add, null); Spacer(Modifier.width(4.dp)); Text("Add subcategory to ${p.name}")
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }

    editing?.let { c -> CategoryEditDialog(c, cats, onDismiss = { editing = null }) }
}

@Composable
private fun CategoryEditDialog(initial: Category, cats: List<Category>, onDismiss: () -> Unit) {
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
