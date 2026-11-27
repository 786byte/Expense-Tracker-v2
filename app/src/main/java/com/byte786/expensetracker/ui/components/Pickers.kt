@file:OptIn(ExperimentalMaterial3Api::class)

package com.byte786.expensetracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.byte786.expensetracker.data.Calc
import com.byte786.expensetracker.data.Category
import com.byte786.expensetracker.ui.Money
import com.byte786.expensetracker.ui.screens.CategoryEditDialog
import com.byte786.expensetracker.ui.theme.LocalCurrency

/**
 * Calculator keypad for amounts: 600+400-50 etc. [onDone] gets the result in minor units.
 */
@Composable
fun CalculatorSheet(title: String, initialMinor: Long, onDismiss: () -> Unit, onDone: (Long) -> Unit) {
    val currency = LocalCurrency.current
    var expr by remember { mutableStateOf(Money.toInput(initialMinor)) }
    val result = Calc.evalMinor(expr)
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    fun press(k: String) {
        val ops = "+-×÷"
        expr = when {
            k == "C" -> ""
            k == "⌫" -> expr.dropLast(1)
            k in ops && expr.isEmpty() -> if (k == "-") "-" else expr
            k in ops && expr.last() in ops -> expr.dropLast(1) + k
            k == "." && expr.takeLastWhile { it !in ops }.contains('.') -> expr
            else -> expr + k
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet) {
        Column(Modifier.padding(horizontal = 16.dp).navigationBarsPadding().padding(bottom = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(16.dp),
                horizontalAlignment = Alignment.End,
            ) {
                Text(
                    expr.ifEmpty { "0" }, fontSize = 22.sp, maxLines = 1, overflow = TextOverflow.Clip,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    if (result != null) Money.format(result, currency) else "…",
                    fontSize = 32.sp, fontWeight = FontWeight.Black,
                )
            }
            Spacer(Modifier.height(12.dp))
            val rows = listOf(
                listOf("C", "⌫", "÷", "×"),
                listOf("7", "8", "9", "-"),
                listOf("4", "5", "6", "+"),
                listOf("1", "2", "3", "="),
                listOf("0", "00", ".", "OK"),
            )
            rows.forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { k ->
                        val mod = Modifier.weight(1f).height(56.dp)
                        when (k) {
                            "OK" -> Button(
                                onClick = { if (result != null && result > 0) onDone(result) },
                                enabled = result != null && result > 0, modifier = mod,
                            ) { Icon(Icons.Filled.Check, "Done") }
                            "=" -> FilledTonalButton(onClick = { result?.let { expr = Money.toInput(it) } }, modifier = mod) { Text("=", fontSize = 22.sp) }
                            "⌫" -> FilledTonalButton(onClick = { press(k) }, modifier = mod) { Icon(Icons.AutoMirrored.Filled.Backspace, "Delete") }
                            "C", "÷", "×", "-", "+" -> FilledTonalButton(onClick = { press(k) }, modifier = mod) { Text(if (k == "-") "−" else k, fontSize = 22.sp) }
                            else -> OutlinedButton(onClick = { press(k) }, modifier = mod) { Text(k, fontSize = 22.sp, fontWeight = FontWeight.Bold) }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

/** All categories of one type as icon tiles. Pencil switches to editing. */
@Composable
fun CategorySheet(type: Int, cats: List<Category>, selectedId: Long?, onDismiss: () -> Unit, onPick: (Category) -> Unit) {
    var editing by remember { mutableStateOf(false) }
    var editCat by remember { mutableStateOf<Category?>(null) }
    val parents = cats.filter { it.parentId == null && it.type == type }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.navigationBarsPadding().padding(bottom = 12.dp)) {
            SheetHeader(if (editing) "Tap a category to edit it" else "Choose category", editing) { editing = !editing }
            LazyVerticalGrid(
                GridCells.Adaptive(84.dp),
                Modifier.fillMaxWidth().heightIn(max = 520.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(parents, key = { it.id }) { c ->
                    val selected = c.id == selectedId
                    Column(
                        Modifier.clip(RoundedCornerShape(14.dp))
                            .then(if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(14.dp)) else Modifier)
                            .clickable { if (editing) editCat = c else onPick(c) }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(contentAlignment = Alignment.TopEnd) {
                            CategoryIcon(c.icon, c.color, size = 48.dp)
                            if (editing) Icon(
                                Icons.Filled.Edit, null, Modifier.size(18.dp).clip(RoundedCornerShape(9.dp))
                                    .background(MaterialTheme.colorScheme.surface).padding(2.dp),
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            c.name, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center,
                            maxLines = 2, overflow = TextOverflow.Ellipsis,
                            fontWeight = if (selected) FontWeight.Black else FontWeight.Medium,
                        )
                    }
                }
                item(key = "new") {
                    Column(
                        Modifier.clip(RoundedCornerShape(14.dp))
                            .clickable { editCat = Category(name = "", type = type, icon = "category", color = 0xFFB85A12, sortOrder = parents.size) }
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            Modifier.size(48.dp).clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.Filled.Add, null) }
                        Spacer(Modifier.height(4.dp))
                        Text("New", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
    editCat?.let { CategoryEditDialog(it, cats, onDismiss = { editCat = null }) }
}

/** Subcategories of [parent] as a plain list of names. */
@Composable
fun SubcategorySheet(parent: Category, cats: List<Category>, selectedId: Long?, onDismiss: () -> Unit, onPick: (Long?) -> Unit) {
    var editing by remember { mutableStateOf(false) }
    var editCat by remember { mutableStateOf<Category?>(null) }
    val subs = cats.filter { it.parentId == parent.id }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.navigationBarsPadding().padding(bottom = 12.dp)) {
            SheetHeader(if (editing) "Tap a subcategory to edit it" else "${parent.name}: choose subcategory", editing) { editing = !editing }
            LazyColumn(Modifier.heightIn(max = 520.dp)) {
                if (!editing) item(key = "none") { SubLine("None", selectedId == null) { onPick(null) } }
                items(subs, key = { it.id }) { s ->
                    SubLine(s.name, s.id == selectedId, editing) { if (editing) editCat = s else onPick(s.id) }
                }
                item(key = "new") {
                    TextButton(
                        onClick = { editCat = Category(name = "", type = parent.type, parentId = parent.id, icon = parent.icon, color = parent.color, sortOrder = subs.size) },
                        modifier = Modifier.padding(horizontal = 12.dp),
                    ) { Icon(Icons.Filled.Add, null); Spacer(Modifier.width(6.dp)); Text("New subcategory") }
                }
            }
        }
    }
    editCat?.let { CategoryEditDialog(it, cats, onDismiss = { editCat = null }) }
}

@Composable
private fun SheetHeader(title: String, editing: Boolean, onToggle: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        if (editing) TextButton(onClick = onToggle) { Text("Done") }
        else IconButton(onClick = onToggle) { Icon(Icons.Filled.Edit, "Edit") }
    }
}

@Composable
private fun SubLine(name: String, selected: Boolean, editing: Boolean = false, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(name, Modifier.weight(1f), fontWeight = if (selected) FontWeight.Black else FontWeight.Normal)
        if (editing) Icon(Icons.Filled.Edit, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        else if (selected) Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.primary)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}
