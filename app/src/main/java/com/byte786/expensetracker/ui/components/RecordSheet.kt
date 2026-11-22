@file:OptIn(ExperimentalMaterial3Api::class)

package com.byte786.expensetracker.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.byte786.expensetracker.ExpenseApp
import com.byte786.expensetracker.data.TxnType
import com.byte786.expensetracker.ui.Dates
import com.byte786.expensetracker.ui.Money
import com.byte786.expensetracker.ui.Routes
import com.byte786.expensetracker.ui.theme.LocalCurrency

/**
 * Read-only summary of a record, opened by tapping it in any list.
 * Editing only happens after pressing Edit, so nothing changes by accident.
 */
@Composable
fun RecordSheet(id: Long, nav: NavController, onDismiss: () -> Unit) {
    val repo = ExpenseApp.instance.repo
    val txns by repo.txns.collectAsState()
    val cats by repo.categories.collectAsState()
    val accs by repo.accounts.collectAsState()
    val lk = rememberLookup(cats, accs, txns)
    val currency = LocalCurrency.current
    var confirmDelete by remember { mutableStateOf(false) }
    var viewPhoto by remember { mutableStateOf(false) }

    val t = txns.firstOrNull { it.id == id }
    if (t == null) {
        LaunchedEffect(Unit) { onDismiss() }
        return
    }
    val parts = t.groupId?.let { lk.groups[it] }.orEmpty().ifEmpty { listOf(t) }
    val total = parts.sumOf { it.amount }
    val typeName = when (t.type) { TxnType.INCOME -> "Income"; TxnType.TRANSFER -> "Transfer"; else -> "Expense" }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val (icon, color) = lk.icon(t)
                CategoryIcon(icon, color, size = 52.dp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(typeName, style = MaterialTheme.typography.labelMedium, color = amountColor(t.type))
                    Text(
                        Money.format(total, currency, signFor(t.type)),
                        fontSize = 28.sp, fontWeight = FontWeight.Black, color = amountColor(t.type),
                    )
                }
            }
            HorizontalDivider()
            if (t.type != TxnType.TRANSFER) {
                val c = t.categoryId?.let { lk.cat[it] }
                val s = t.subcategoryId?.let { lk.cat[it] }
                Field("Category", c?.name ?: "Uncategorized")
                Field("Subcategory", s?.name ?: "None")
            }
            when {
                t.type == TxnType.TRANSFER -> Field(
                    "Accounts",
                    "${lk.acc[t.accountId]?.name ?: "?"} → ${t.toAccountId?.let { lk.acc[it]?.name } ?: "?"}",
                )
                parts.size > 1 -> Field(
                    if (t.type == TxnType.INCOME) "Received in" else "Paid from",
                    parts.joinToString("\n") { "${lk.acc[it.accountId]?.name ?: "?"}: ${Money.format(it.amount, currency)}" },
                )
                else -> Field(if (t.type == TxnType.INCOME) "Received in" else "Paid from", lk.acc[t.accountId]?.name ?: "?")
            }
            Field("Date", "${Dates.fullDay(Dates.date(t.date))}, ${Dates.time(t.date)}")
            if (t.memo.isNotBlank()) Field("Title", t.memo)
            if (!t.description.isNullOrBlank()) Field("Description", t.description)
            t.photo?.let { p ->
                AsyncImage(
                    model = repo.photoFile(p), contentDescription = "Attached photo", contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(12.dp)).clickable { viewPhoto = true },
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { confirmDelete = true }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(6.dp))
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
                Button(onClick = { onDismiss(); nav.navigate(Routes.edit(t.id)) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Edit, null); Spacer(Modifier.width(6.dp)); Text("Edit")
                }
            }
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            "Delete this record?",
            if (parts.size > 1) "All ${parts.size} parts of this bill will be deleted. This cannot be undone." else "This cannot be undone.",
            onDismiss = { confirmDelete = false },
        ) {
            confirmDelete = false
            repo.launch { repo.deleteTxn(t) }
            onDismiss()
        }
    }
    if (viewPhoto && t.photo != null) {
        Dialog(onDismissRequest = { viewPhoto = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Box(Modifier.fillMaxSize().clickable { viewPhoto = false }, contentAlignment = Alignment.Center) {
                AsyncImage(model = repo.photoFile(t.photo), contentDescription = "Photo", contentScale = ContentScale.Fit, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun Field(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
    }
}
