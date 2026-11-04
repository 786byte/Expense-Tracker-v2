@file:OptIn(ExperimentalMaterial3Api::class)

package com.byte786.expensetracker.ui.screens

import androidx.compose.animation.animateContentSize
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

private data class GuideTopic(val icon: ImageVector, val title: String, val body: String)

private val topics = listOf(
    GuideTopic(
        Icons.Filled.Add, "Adding a record",
        "Tap the round + button on the home screen.\n\n" +
            "• Pick Expense, Income or Transfer at the top.\n" +
            "• Type the amount.\n" +
            "• Choose a category, then (optionally) a subcategory, e.g. Bike › Mobil.\n" +
            "• Choose the account the money came from / went to.\n" +
            "• Date and time default to now; tap them to change.\n" +
            "• Add a memo and a photo if you like, then tap Save.\n\n" +
            "Tap any record in a list to edit or delete it.",
    ),
    GuideTopic(
        Icons.Filled.Category, "Categories & subcategories",
        "Every expense or income can have a category and a subcategory. Example: Expense → Bike → Service.\n\n" +
            "Open the menu (☰) → Categories to add, rename, recolor or delete them. Tap a category to show its " +
            "subcategories, and use “Add subcategory” to add one. While adding a record you can also tap “New” in the " +
            "subcategory row to create one on the spot.\n\n" +
            "To reorganise, edit a category and use “Place it”: put a main category inside another one " +
            "(e.g. Bike Maintenance inside Bike) or move a subcategory elsewhere. Its records move with it.\n\n" +
            "Deleting a category keeps your records; they just become “Uncategorized”.",
    ),
    GuideTopic(
        Icons.Filled.Palette, "Changing icons & colors",
        "In Categories or Accounts, tap the pencil (or the account) to edit it. Pick any color and any icon from the " +
            "grid, then Save. The new look shows up everywhere straight away.",
    ),
    GuideTopic(
        Icons.Filled.AccountBalanceWallet, "Accounts",
        "Accounts are where your money lives: Cash, Bank, bKash, etc. Add or edit them from menu → Accounts. " +
            "Set a starting balance for money you already had before using the app; after that the balance updates " +
            "from your records automatically.",
    ),
    GuideTopic(
        Icons.Filled.SwapHoriz, "Transfers",
        "Moving money between your own accounts (e.g. Bank → bKash, or a cash withdrawal) is a Transfer. " +
            "It changes both account balances but is NOT counted as income or expense.",
    ),
    GuideTopic(
        Icons.Filled.Home, "Home screen",
        "• Month switcher: use the arrows or tap the month name to jump to any month.\n" +
            "• Income / Expense / Balance for that month (tap Income or Expense to see those records).\n" +
            "• Accounts with their current balances.\n" +
            "• Where your money goes: your top spending categories this month, or all time.\n" +
            "• All-time records: your single biggest expense and biggest income ever.\n" +
            "• Every record of the month, grouped by day.",
    ),
    GuideTopic(
        Icons.Filled.Search, "Search (side menu)",
        "Swipe from the left edge or tap ☰ to open the side menu. Type in the search box, or use the quick buttons: " +
            "Today, This month, Last month, This year, Pick a date, Pick a month.\n\n" +
            "On the Search page you can combine filters: text, type (expense/income/transfer), date (day, month, year " +
            "or a custom range) and category or subcategory. Totals for the results are shown at the top.",
    ),
    GuideTopic(
        Icons.Filled.PieChart, "Reports",
        "Menu → Reports (or the chart icon on the home screen). Choose Month, Year or All time, and Expense or Income. " +
            "You get a chart and a list by category; tap a category to see how much went to each subcategory. " +
            "Tap a subcategory to see those exact records. The bottom card compares income and expense for the last 6 months.",
    ),
    GuideTopic(
        Icons.Filled.CameraAlt, "Memos & photos",
        "Every record has a memo field and can hold one photo: take one with the camera or pick one from the gallery. " +
            "Photos are shrunk automatically so they don't fill your phone. Tap the photo to see it full screen. " +
            "Memos are searchable.",
    ),
    GuideTopic(
        Icons.Filled.CloudUpload, "Backup to Google Drive",
        "Menu → Backup & settings → Choose backup file. In the file picker open the ☰ menu, choose Google Drive and " +
            "a folder, then Save. From then on the app updates that file automatically after every change and once a day. " +
            "The file contains all records, categories, accounts and photos.\n\n" +
            "To move to a new phone: install the app, then Backup & settings → Restore backup and pick the file from Drive.\n\n" +
            "Export CSV creates a spreadsheet you can open in Excel or Google Sheets.\n\n" +
            "Import CSV brings in records exported from another expense app. Missing accounts and categories are created, " +
            "names like “Bike Maintenance” go under Bike automatically, and records already in the app are skipped.",
    ),
)

@Composable
fun GuideScreen(nav: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Guide") },
                navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            )
        },
    ) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(16.dp)) {
            items(topics) { t -> TopicCard(t) }
        }
    }
}

@Composable
private fun TopicCard(t: GuideTopic) {
    var open by rememberSaveable { mutableStateOf(false) }
    ElevatedCard(onClick = { open = !open }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).animateContentSize()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(t.icon, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Text(t.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Icon(if (open) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null)
            }
            if (open) {
                Text(t.body, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 12.dp))
            }
        }
    }
}
