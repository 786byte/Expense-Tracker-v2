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
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
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
        Icons.Filled.Home, "The home screen",
        "• Top left: the status cat. Tap it to see why it feels that way:\n" +
            "    😿 crying: your total balance is below zero\n" +
            "    😾 angry: this month you spent more than you earned\n" +
            "    🐱 neutral: this month is about even (or nothing recorded yet)\n" +
            "    😺 happy: this month you earned more than you spent\n" +
            "• Middle: a cat chasing money. There's a different cat for every day of the week.\n" +
            "• Top right: your total balance across all accounts (it can be negative).\n" +
            "• Below: income and expense of the month you're looking at. Tap either to see those records.\n" +
            "• The month box on the right: arrows go back and forward, tapping the name jumps to any month.\n" +
            "• Then every record of that month, grouped by day. Tap one to edit or delete it.",
    ),
    GuideTopic(
        Icons.Filled.Add, "Adding a record",
        "Tap the round + button on Home. Three cats pop up:\n" +
            "• Ginger cat with − : Expense\n" +
            "• Black cat with + : Income\n" +
            "• Grey cat with ⇄ : Transfer between your own accounts\n\n" +
            "• Amount: a calculator opens. You can type sums like 600+400-50.\n" +
            "• Category: tap the button to see all categories as icons and pick one. If it has subcategories, " +
            "the subcategory list opens next (or tap the Subcategory button). The pencil in each list lets you edit or add.\n" +
            "• Paid from: the account. Paid from two accounts? Tap “Paid from another account too”, choose the account " +
            "and how much came from it. The first account automatically gets the rest. It's saved as one bill.\n" +
            "• Title is shown in lists; Description is only shown when you open the record.\n" +
            "• Date, time and photo are optional. Then tap Save.",
    ),
    GuideTopic(
        Icons.Filled.Mic, "Adding by voice",
        "Tap the microphone on Home (or on the Add screen) and speak in English. The app picks out the amount, " +
            "category, subcategory and accounts from your own names, fills the Add screen, and you check it and tap Save.\n\n" +
            "Examples:\n" +
            "• “500 taka bike mobil from cash”\n" +
            "• “1000 bills, 600 cash 400 bKash” (one bill from two accounts)\n" +
            "• “got 5000 salary in tuition”\n" +
            "• “transfer 2000 from cash to bKash”\n" +
            "• “yesterday 230 for lunch, food restaurant, from cash” (words after “for” become the title)\n\n" +
            "It uses your phone's speech recognition (the Google app), no AI service.",
    ),
    GuideTopic(
        Icons.Filled.AccountBalanceWallet, "Accounts tab",
        "The bottom bar's left button. It has two parts:\n\n" +
            "Categories: every category with how much went through it. Tap a category to see its subcategories " +
            "with what you spent and earned on each. Tap a subcategory to see every record in it.\n\n" +
            "Accounts: Cash, bKash, Bank… with their balances. Tap one to see its records.\n\n" +
            "At the top you choose the period: a month (arrows, or tap it to pick) or All time.",
    ),
    GuideTopic(
        Icons.Filled.Palette, "Adding, editing and deleting categories and accounts",
        "Tapping in the Accounts tab never changes anything. To change things, press Edit (top right):\n" +
            "• ✎ next to a category, subcategory or account edits its name, icon, color (and starting balance for accounts).\n" +
            "• 🗑 deletes a category; its records stay, as “Uncategorized”.\n" +
            "• “Add category / subcategory / account” buttons appear at the end of each list.\n" +
            "• In a category's editor, “Place it” moves it inside another category, e.g. Bike Maintenance inside Bike; " +
            "its records move with it.\n" +
            "Press Done when finished.",
    ),
    GuideTopic(
        Icons.Filled.SwapHoriz, "Transfers",
        "Moving money between your own accounts (e.g. Bank → bKash, paying back the Loan account) is a Transfer. " +
            "It changes both balances but is not counted as income or expense.",
    ),
    GuideTopic(
        Icons.Filled.PieChart, "Analysis tab",
        "The bottom bar's right button. Choose Expense, Income or Both, and a month or All time.\n" +
            "• The pie shows where the money went (or came from); Both compares income with expense.\n" +
            "• The bars compare income and expense over the last 6 months.\n" +
            "• At the bottom: your biggest expense and biggest income ever.",
    ),
    GuideTopic(
        Icons.Filled.Search, "Side menu & search",
        "Tap ☰ or swipe from the left edge. At the top is the search box and quick buttons: Today, This month, " +
            "Last month, This year, Pick a date, Pick a month, By category. Below: Backup & settings, Export, Import CSV, " +
            "this Guide, and Delete & reset.",
    ),
    GuideTopic(
        Icons.Filled.CameraAlt, "Memos & photos",
        "Every record has a memo and can hold one photo (camera or gallery). Photos are shrunk automatically. " +
            "Memos are searchable.",
    ),
    GuideTopic(
        Icons.Filled.CloudUpload, "Backup to Google Drive, export & import",
        "Side menu → Backup & settings → Choose backup file. In the file picker choose Google Drive and a folder, then Save. " +
            "The app then updates that file automatically after every change and once a day, including photos.\n\n" +
            "New phone: install the app, then Backup & settings → Restore backup and pick the file.\n\n" +
            "Side menu → Export: a CSV spreadsheet or a full backup copy.\n" +
            "Side menu → Import CSV: bring in records exported from another expense app. Records already here are skipped.",
    ),
    GuideTopic(
        Icons.Filled.DeleteForever, "Delete & reset",
        "Side menu → Delete & reset. Choose:\n" +
            "• Records only: deletes every record; accounts and categories stay.\n" +
            "• Everything: deletes it all and starts again with the default accounts and categories.\n\n" +
            "It asks twice. A safety copy is kept on the phone: Backup & settings → “Undo the last reset” brings the data back.",
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
